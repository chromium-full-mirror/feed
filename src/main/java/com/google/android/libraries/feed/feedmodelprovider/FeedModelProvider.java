// Copyright 2018 The Feed Authors.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package com.google.android.libraries.feed.feedmodelprovider;

import android.database.Observable;
import android.support.annotation.VisibleForTesting;
import com.google.android.libraries.feed.api.common.InternalProtocolUtils;
import com.google.android.libraries.feed.api.common.ThreadUtils;
import com.google.android.libraries.feed.api.common.UiRunnableHandler;
import com.google.android.libraries.feed.api.modelprovider.FeatureChange;
import com.google.android.libraries.feed.api.modelprovider.FeatureChangeObserver;
import com.google.android.libraries.feed.api.modelprovider.ModelChild;
import com.google.android.libraries.feed.api.modelprovider.ModelChild.Type;
import com.google.android.libraries.feed.api.modelprovider.ModelFeature;
import com.google.android.libraries.feed.api.modelprovider.ModelMutator;
import com.google.android.libraries.feed.api.modelprovider.ModelProvider;
import com.google.android.libraries.feed.api.modelprovider.ModelProviderObserver;
import com.google.android.libraries.feed.api.modelprovider.ModelToken;
import com.google.android.libraries.feed.api.modelprovider.TokenCompleted;
import com.google.android.libraries.feed.api.modelprovider.TokenCompletedObserver;
import com.google.android.libraries.feed.api.sessionmanager.SessionManager;
import com.google.android.libraries.feed.common.Dumpable;
import com.google.android.libraries.feed.common.Dumper;
import com.google.android.libraries.feed.common.Logger;
import com.google.android.libraries.feed.common.TimingUtils;
import com.google.android.libraries.feed.common.TimingUtils.ElapsedTimeTracker;
import com.google.android.libraries.feed.common.Validators;
import com.google.android.libraries.feed.feedmodelprovider.internal.CursorProvider;
import com.google.android.libraries.feed.feedmodelprovider.internal.FeatureChangeImpl;
import com.google.android.libraries.feed.feedmodelprovider.internal.ModelChildBinder;
import com.google.android.libraries.feed.feedmodelprovider.internal.ModelChildFactory;
import com.google.android.libraries.feed.feedmodelprovider.internal.ModelCursorImpl;
import com.google.android.libraries.feed.feedmodelprovider.internal.ModelMutatorImpl;
import com.google.android.libraries.feed.feedmodelprovider.internal.ModelMutatorImpl.ChildMutation;
import com.google.android.libraries.feed.feedmodelprovider.internal.ModelMutatorImpl.ChildUpdateMutation;
import com.google.android.libraries.feed.feedmodelprovider.internal.ModelMutatorImpl.ModelMutatorCommitter;
import com.google.android.libraries.feed.feedmodelprovider.internal.UpdatableModelChild;
import com.google.android.libraries.feed.feedmodelprovider.internal.UpdatableModelFeature;
import com.google.android.libraries.feed.feedmodelprovider.internal.UpdatableModelToken;
import com.google.protobuf.ByteString;
import com.google.search.now.feed.client.StreamDataProto.StreamContentId;
import com.google.search.now.feed.client.StreamDataProto.StreamSharedState;
import com.google.search.now.feed.client.StreamDataProto.StreamStructure;
import com.google.search.now.feed.client.StreamDataProto.StreamStructure.Operation;
import com.google.search.now.feed.client.StreamDataProto.StreamToken;
import com.google.search.now.wire.feed.ContentIdProto.ContentId;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import javax.annotation.Nullable;
import javax.annotation.concurrent.GuardedBy;

/** An implementation of {@link ModelProvider}. This will represent the Stream tree in memory. */
public class FeedModelProvider extends Observable<ModelProviderObserver>
    implements ModelProvider, Dumpable {

  private static final String TAG = "FeedModelProvider";
  private static final List<UpdatableModelChild> EMPTY_LIST =
      Collections.unmodifiableList(new ArrayList<>());

  private final Object lock = new Object();

  @GuardedBy("lock")
  private ModelFeature root = null;

  // The tree is model as a parent with an list of children.  A container is created for every
  // ModelChild with a child.
  @GuardedBy("lock")
  private final Map<String, ArrayList<UpdatableModelChild>> containers = new HashMap<>();

  @GuardedBy("lock")
  private final Map<String, UpdatableModelChild> contents = new HashMap<>();

  @GuardedBy("lock")
  private final Map<ByteString, TokenTracking> tokens = new HashMap<>();

  @GuardedBy("lock")
  private final List<WeakReference<ModelCursorImpl>> cursors = new ArrayList<>();

  @GuardedBy("lock")
  private State currentState = State.INITIALIZING;

  // #dump() operation counts
  private int removedChildrenCount = 0;
  private int removeScanCount = 0;
  private int commitCount = 0;
  private int commitTokenCount = 0;
  private int commitUpdateCount = 0;

  private final SessionManager sessionManager;
  private final ThreadUtils threadUtils;
  private final InternalProtocolUtils protocolUtils;
  private final UiRunnableHandler uiRunnableHandler;
  private final ModelChildFactory modelChildFactory;
  private final ModelChildBinder modelChildBinder;
  private final TimingUtils timingUtils;

  public FeedModelProvider(
      SessionManager sessionManager,
      ThreadUtils threadUtils,
      TimingUtils timingUtils,
      InternalProtocolUtils protocolUtils,
      UiRunnableHandler uiRunnableHandler) {
    this.sessionManager = sessionManager;
    this.threadUtils = threadUtils;
    this.timingUtils = timingUtils;
    this.protocolUtils = protocolUtils;
    this.uiRunnableHandler = uiRunnableHandler;

    modelChildFactory = new ModelChildFactory(protocolUtils);
    CursorProvider cursorProvider =
        parentId -> {
          synchronized (lock) {
            ArrayList<UpdatableModelChild> children =
                containers.get(protocolUtils.getContentIdKey(parentId));
            if (children == null) {
              Logger.i(TAG, "No children found for Cursor");
              ModelCursorImpl cursor = new ModelCursorImpl(EMPTY_LIST.listIterator(), threadUtils);
              cursors.add(new WeakReference<>(cursor));
              return cursor;
            }
            ModelCursorImpl cursor = new ModelCursorImpl(children.listIterator(), threadUtils);
            cursors.add(new WeakReference<>(cursor));
            return cursor;
          }
        };
    modelChildBinder =
        new ModelChildBinder(sessionManager, cursorProvider, timingUtils, protocolUtils);
  }

  @Override
  @Nullable
  public ModelFeature getRootFeature() {
    synchronized (lock) {
      if (root == null) {
        Logger.i(TAG, "Found Empty Stream");
      }
      return root;
    }
  }

  @Override
  @Nullable
  public ModelChild getModelChild(StreamContentId contentId) {
    String key = protocolUtils.getContentIdKey(contentId);
    synchronized (lock) {
      return contents.get(key);
    }
  }

  @Override
  @Nullable
  public StreamSharedState getSharedState(ContentId contentId) {
    return sessionManager.getSharedState(contentId);
  }

  @Override
  public void handleToken(ModelToken modelToken) {
    sessionManager.handleToken(modelToken.getStreamToken());
  }

  @Override
  public void registerObserver(ModelProviderObserver observer) {
    super.registerObserver(observer);
    synchronized (lock) {
      // If we are in the ready state, then call the Observer to inform it things are ready.
      if (currentState == State.READY) {
        observer.onSessionStart();
      } else if (currentState == State.INVALIDATED) {
        observer.onSessionFinished();
      }
    }
  }

  @Override
  public State getCurrentState() {
    synchronized (lock) {
      return currentState;
    }
  }

  @Override
  public ModelMutator edit() {
    return new ModelMutatorImpl(committer, modelChildFactory, modelChildBinder);
  }

  @Override
  public void invalidate() {
    Logger.i(TAG, "Invalidating the current ModelProvider");
    synchronized (lock) {
      currentState = State.INVALIDATED;
      for (WeakReference<ModelCursorImpl> cursorRef : cursors) {
        ModelCursorImpl cursor = cursorRef.get();
        if (cursor != null) {
          cursor.release();
          cursorRef.clear();
        }
      }
      cursors.clear();
      tokens.clear();
      containers.clear();
    }

    // Always run the observers on the UI Thread
    List<ModelProviderObserver> observers = getObserversToNotify();
    uiRunnableHandler.post(
        TAG + " - onSessionFinished",
        () -> {
          threadUtils.checkMainThread();
          for (ModelProviderObserver observer : observers) {
            observer.onSessionFinished();
          }
        });
  }

  @Override
  public void dump(Dumper dumper) {
    synchronized (lock) {
      dumper.title(TAG);
      dumper.forKey("currentState").value(currentState.ordinal());
      dumper.forKey("contentCount").value(contents.size()).compactPrevious();
      dumper.forKey("containers").value(containers.size()).compactPrevious();
      dumper.forKey("tokens").value(tokens.size()).compactPrevious();
      dumper.forKey("observers").value(mObservers.size()).compactPrevious();
      dumper.forKey("commitCount").value(commitCount);
      dumper.forKey("commitTokenCount").value(commitTokenCount).compactPrevious();
      dumper.forKey("commitUpdateCount").value(commitUpdateCount).compactPrevious();
      dumper.forKey("removeCount").value(removedChildrenCount);
      dumper.forKey("removeScanCount").value(removeScanCount).compactPrevious();
      if (root != null
          && root.getStreamFeature() != null
          && root.getStreamFeature().hasContentId()) {
        dumper
            .forKey("root")
            .value(protocolUtils.getContentIdKey(root.getStreamFeature().getContentId()));
      } else {
        dumper.forKey("root").value("[UNDEFINED]");
      }
      for (Entry<String, ArrayList<UpdatableModelChild>> entry : containers.entrySet()) {
        Dumper childDumper = dumper.getChildDumper();
        childDumper.title("container");
        childDumper.forKey("parent").value(entry.getKey());
        childDumper.forKey("children").value(entry.getValue().size()).compactPrevious();
      }
      dumper.forKey("cursors").value(cursors.size());
      for (WeakReference<ModelCursorImpl> cursorRef : cursors) {
        ModelCursorImpl cursor = cursorRef.get();
        if (cursor != null) {
          dumper.dump(cursor);
        }
      }
    }
  }

  @VisibleForTesting
  List<ModelProviderObserver> getObserversToNotify() {
    // Make a copy of the observers, so the observers are not mutated while invoking callbacks.
    // mObservers is locked when adding or removing observers. Also, release the lock before
    // invoking callbacks to avoid deadlocks. ([INTERNAL LINK])
    synchronized (mObservers) {
      return new ArrayList<>(mObservers);
    }
  }

  /**
   * Interface used by the {@code ModelMutatorCommitter} to modify the model state based upon the
   * current model state and the contents of the mutation. We define mutation handlers for the
   * initialization, for a mutation based upon a continuation token response, and then a standard
   * update mutation.
   */
  interface MutationHandler {

    /**
     * Called before processing the children of the mutation. This allows the model to be cleaned up
     * before new children are added.
     */
    void preMutation();

    /** Append a child to a parent */
    void appendChild(String parentKey, UpdatableModelChild child);

    /** Remove a child from a parent */
    void removeChild(String parentKey, UpdatableModelChild child);

    /**
     * This is called after the model has been updated. Typically this will notify observers of the
     * changes made during the mutation.
     */
    void postMutation();
  }

  /** This is the {@code ModelMutatorCommitter} which updates the model */
  private final ModelMutatorCommitter committer =
      new ModelMutatorCommitter() {
        @Override
        public void commit(
            List<ChildMutation> structureChanges,
            List<ChildUpdateMutation> updates,
            StreamToken mutationSourceToken) {
          Logger.i(
              TAG,
              "commit mutation - Structure changes %s, Updates %s, Token %s",
              structureChanges.size(),
              updates.size(),
              mutationSourceToken != null);

          threadUtils.checkNotMainThread();
          ElapsedTimeTracker timeTracker = timingUtils.getElapsedTimeTracker(TAG);
          commitCount++;

          synchronized (lock) {
            MutationHandler mutationHandler;
            if (currentState == State.INITIALIZING) {
              Validators.checkState(
                  mutationSourceToken == null,
                  "Initializing the Model Provider from a Continuation Token");
              mutationHandler = new InitializeModel();
            } else if (mutationSourceToken != null) {
              mutationHandler = new TokenMutation(mutationSourceToken);
              commitTokenCount++;
            } else {
              mutationHandler = new UpdateMutation(updates);
              commitUpdateCount++;
            }

            // Processing before the structural mutation
            mutationHandler.preMutation();

            // process the structure changes
            boolean rootChanged = false;
            String currentParentKey = null;
            ArrayList<UpdatableModelChild> childrenList = null;
            for (ChildMutation childMutation : structureChanges) {
              if (childMutation.operation == Operation.UPDATE_OR_APPEND) {
                UpdatableModelChild child = Validators.checkNotNull(childMutation.addChild);
                if (child.getType() == Type.UNBOUND) {
                  Logger.e(TAG, "Unbound Child Found %s", child.getContentIdAsKey());
                  continue;
                }
                if (!child.hasParentId()) {
                  rootChanged = createRoot(child);
                  contents.put(child.getContentIdAsKey(), child);
                  continue;
                }

                String parentKey = protocolUtils.getContentIdKey(child.getParentId());
                if (!parentKey.equals(currentParentKey)) {
                  childrenList = getChildList(parentKey);
                  currentParentKey = parentKey;
                }
                childrenList.add(child);
                contents.put(child.getContentIdAsKey(), child);
                mutationHandler.appendChild(parentKey, child);

                if (child.getType() == Type.TOKEN) {
                  TokenTracking tokenTracking =
                      new TokenTracking(child.getUpdatableModelToken(), childrenList);
                  tokens.put(
                      child.getModelToken().getStreamToken().getNextPageToken(), tokenTracking);
                }
              } else {
                Validators.checkNotNull(childMutation.removeChild);
                handleRemoveOperation(mutationHandler, childMutation.removeChild);
                contents.remove(
                    protocolUtils.getContentIdKey(childMutation.removeChild.getContentId()));
              }
            }

            // check if we need to notify that the root was changed
            if (rootChanged && currentState == State.READY) {
              List<ModelProviderObserver> observers = getObserversToNotify();
              uiRunnableHandler.post(
                  TAG + " - onRootSet",
                  () -> {
                    // Always run the observers on the UI Thread
                    threadUtils.checkMainThread();
                    for (ModelProviderObserver observer : observers) {
                      observer.onRootSet();
                    }
                  });
            }

            // The first mutation of an INITIALIZING model provider will cause the session to be
            // started
            mutationHandler.postMutation();
          }
          timeTracker.stop("", "modelMutation", "updates", structureChanges.size());
        }
      };

  private void handleRemoveOperation(MutationHandler mutationHandler, StreamStructure removeChild) {
    // remove everything from the remove list first
    String parentKey = protocolUtils.getContentIdKey(removeChild.getParentContentId());
    synchronized (lock) {
      List<UpdatableModelChild> childList = containers.get(parentKey);
      if (childList == null) {
        Logger.w(TAG, "Parent of removed item is not found");
        return;
      }

      // For FEATURE children, add the remove to the mutation handler to create the
      // StreamFeatureChange.  We skip this for TOKENS.
      String childKey = protocolUtils.getContentIdKey(removeChild.getContentId());
      UpdatableModelChild targetChild = contents.get(childKey);
      Validators.checkNotNull(targetChild, "Child not found in the ModelProvider contents");
      if (targetChild.getType() == Type.FEATURE) {
        Logger.i(TAG, "Removing a child");
        mutationHandler.removeChild(parentKey, targetChild);
      }

      // This walks the child list backwards because the most common removal item is a
      // token which is always the last item in the list.  removeScanCount tracks if we are
      // walking the list too much
      ListIterator<UpdatableModelChild> li = childList.listIterator(childList.size());
      UpdatableModelChild removed = null;
      while (li.hasPrevious()) {
        removeScanCount++;
        UpdatableModelChild child = li.previous();
        if (child.getContentIdAsKey().equals(childKey)) {
          removed = child;
          break;
        }
      }

      if (removed != null) {
        childList.remove(removed);
        removedChildrenCount++;
      } else {
        Logger.w(TAG, "Child to be removed was not found");
      }
    }
  }

  /**
   * This {@link MutationHandler} handles the initial mutation populating the model. No update
   * events are triggered. When the model is updated, we trigger a Session Started event.
   */
  @VisibleForTesting
  class InitializeModel implements MutationHandler {

    @Override
    public void preMutation() {
      // no-op
    }

    @Override
    public void appendChild(String parentKey, UpdatableModelChild child) {
      // During initialization, all children added are not tracked, the consumer will get
      // an initial cursor for the children.
    }

    @Override
    public void removeChild(String parentKey, UpdatableModelChild child) {
      // During initialization, all children removed are not tracked, the consumer will get
      // an initial cursor for the children.
    }

    @Override
    public void postMutation() {
      synchronized (lock) {
        currentState = State.READY;
      }
      List<ModelProviderObserver> observers = getObserversToNotify();
      uiRunnableHandler.post(
          TAG + " - onSessionStart",
          () -> {
            // Always run the observers on the UI Thread
            threadUtils.checkMainThread();
            for (ModelProviderObserver observer : observers) {
              observer.onSessionStart();
            }
          });
    }
  }

  /**
   * This {@link MutationHandler} handles a mutation based upon a continuation token. For a token we
   * will not generate changes for the parent updated by the token. Instead, the new children are
   * appended and a {@link TokenCompleted} will be triggered.
   */
  @VisibleForTesting
  class TokenMutation implements MutationHandler {
    private final StreamToken mutationSourceToken;
    TokenTracking token = null;
    int newCursorStart = -1;

    TokenMutation(StreamToken mutationSourceToken) {
      this.mutationSourceToken = mutationSourceToken;
    }

    @VisibleForTesting
    TokenTracking getTokenTrackingForTest() {
      synchronized (lock) {
        return tokens.get(mutationSourceToken.getNextPageToken());
      }
    }

    @Override
    public void preMutation() {
      synchronized (lock) {
        token = tokens.remove(mutationSourceToken.getNextPageToken());
        if (token == null) {
          Logger.e(TAG, "Token was not found, positioning to end of list");
        }
        newCursorStart = token.location.size();
      }
    }

    @Override
    public void appendChild(String parentKey, UpdatableModelChild child) {
      // The children added will be accessed through a new cursor.
      // TODO: Do we support adding children to more than the parent with the token?
    }

    @Override
    public void removeChild(String parentKey, UpdatableModelChild child) {
      // The children added will be accessed through a new cursor.
    }

    @Override
    public void postMutation() {
      ModelCursorImpl cursor =
          new ModelCursorImpl(token.location.listIterator(newCursorStart), threadUtils);
      TokenCompleted tokenCompleted = new TokenCompleted(cursor);
      List<TokenCompletedObserver> observers = token.tokenChild.getObserversToNotify();
      uiRunnableHandler.post(
          TAG + " - onTokenChange",
          () -> {
            // Always run the observers on the UI Thread
            threadUtils.checkMainThread();
            for (TokenCompletedObserver observer : observers) {
              observer.onChange(tokenCompleted);
            }
          });
    }
  }

  /**
   * {@code MutationHandler} which handles updates. All changes are tracked for the UI through
   * {@link FeatureChange}. One will be created for each {@link ModelFeature} that changed. There
   * are two types of changes, the content and changes to the children (structure).
   */
  @VisibleForTesting
  class UpdateMutation implements MutationHandler {

    private final List<ChildUpdateMutation> updates;
    private final Map<String, FeatureChangeImpl> changes = new HashMap<>();
    private final Set<String> newParents = new HashSet<>();

    UpdateMutation(List<ChildUpdateMutation> updates) {
      this.updates = updates;
    }

    @Override
    public void preMutation() {
      Logger.i(TAG, "Updating %s items", updates.size());
      // Walk all the updates and update the values, creating changes to track these
      for (ChildUpdateMutation update : updates) {
        String contentKey = protocolUtils.getContentIdKey(update.updateChild.getContentId());
        UpdatableModelChild updatableModelChild;
        synchronized (lock) {
          updatableModelChild = contents.get(contentKey);
        }
        if (updatableModelChild == null) {
          Logger.w(TAG, "Didn't find feature being updated for %s", contentKey);
          continue;
        }
        updatableModelChild.updateFeature(update.payload);
        FeatureChangeImpl change = getChange(contentKey);
        if (change != null) {
          change.setFeatureChanged(true);
        }
      }
    }

    @Override
    public void removeChild(String parentKey, UpdatableModelChild child) {
      // TODO: Need to implement this
      Logger.w(TAG, "Remove is not yet implemented", parentKey);
    }

    @Override
    public void appendChild(String parentKey, UpdatableModelChild child) {
      // Is this a child of a node that is new to the model?  We only report changes
      // to existing ModelFeatures.
      String childKey = child.getContentIdAsKey();
      if (newParents.contains(parentKey)) {
        // Don't create a change the child of a new child
        newParents.add(childKey);
        return;
      }

      newParents.add(childKey);
      FeatureChangeImpl change = getChange(parentKey);
      if (change != null) {
        Logger.i(TAG, "Add child %s to a parent %s", childKey, parentKey);
        change.getChildChangesImpl().addAppendChild(child);
      }
    }

    @Override
    public void postMutation() {
      for (FeatureChangeImpl change : changes.values()) {
        // TODO: This cast is because StreamFeatureChange is part of the API, cleanup?
        List<FeatureChangeObserver> observers =
            ((UpdatableModelFeature) change.getModelFeature()).getObserversToNotify();
        for (FeatureChangeObserver observer : observers) {
          observer.onChange(change);
        }
      }
    }

    @Nullable
    private FeatureChangeImpl getChange(String contentIdKey) {
      FeatureChangeImpl change = changes.get(contentIdKey);
      if (change == null) {
        UpdatableModelChild modelChild;
        synchronized (lock) {
          modelChild = contents.get(contentIdKey);
        }
        if (modelChild == null) {
          Logger.e(TAG, "Didn't find '%s' in content", contentIdKey);
          return null;
        }
        change = new FeatureChangeImpl(modelChild.getModelFeature());
        changes.put(contentIdKey, change);
      }
      return change;
    }
  }

  // This method will return true if it sets/updates root
  private boolean createRoot(UpdatableModelChild child) {
    synchronized (lock) {
      // this must be a root
      if (child.getType() == Type.FEATURE) {
        if (root != null) {
          // TODO: What do we do about multiple roots?
          Logger.e(
              TAG,
              "Found multiple roots, this will overwrite the previous definition: %s",
              child.getContentIdAsKey());
        }
        root = child.getModelFeature();
        return true;
      } else {
        // continuation tokens can not be roots.
        Logger.e(TAG, "Invalid Root");
      }
      return false;
    }
  }

  // Lazy creation of containers
  private ArrayList<UpdatableModelChild> getChildList(String parentKey) {
    synchronized (lock) {
      if (!containers.containsKey(parentKey)) {
        containers.put(parentKey, new ArrayList<>());
      }
      return containers.get(parentKey);
    }
  }

  /** Track the continuation token location and model */
  @VisibleForTesting
  static class TokenTracking {
    final UpdatableModelToken tokenChild;
    final ArrayList<UpdatableModelChild> location;

    TokenTracking(UpdatableModelToken tokenChild, ArrayList<UpdatableModelChild> location) {
      this.tokenChild = tokenChild;
      this.location = location;
    }
  }

  // test only method for returning a copy of the tokens map
  @VisibleForTesting
  Map<ByteString, TokenTracking> getTokensForTest() {
    synchronized (lock) {
      return new HashMap<>(tokens);
    }
  }
}
