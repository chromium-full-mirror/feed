# Contributing to the Feed

## Contributor License Agreement

Contributions to this project must be accompanied by a Contributor License
Agreement. You (or your employer) retain the copyright to your contribution,
this simply gives us permission to use and redistribute your contributions as
part of the project. Head over to <https://cla.developers.google.com/> to see
your current agreements on file or to sign a new one.

You generally only need to submit a CLA once, so if you've already submitted
one (even if it was for a different project), you probably don't need to do it
again.

## Patch Acceptance Process

1.  Prepare a git commit that implements the feature. Don't forget to add
    tests.
1.  Ensure you've signed a [Contributor License Agreement]
    [contributor_agreement].
1.  Create a new code review on Gerrit.
    1.  Have an automatically generated "Change Id" line in your commit
        message. If you haven't used Gerrit before, it will print a bash
        command to create the git hook and then you will need to run git `commit
        --amend` to add the line.

        See the [Gerrit documentation][gerrit_docs] for more information about
        uploading changes.
1.   Wait for Feed team member to assign you a reviewer.
1.   Complete a code review.  Amend your existing commit and re-push to make
     changes to your patch.
1.   An engineer at Google applies the patch to our internal version control
     system. The patch is exported as a Git commit, at which point the Gerrit
     code review is closed.

## Compiling and Testing

Feed libraries use Bazel in order to build.  Bazel must first be setup to build
Android apps.  See [here][bazel_android_instructions] for instructions on how
to setup Bazel for Android.

After setting up Bazel the following command will build all Feed libraries:
```
bazel build src/main/...
```
The following can then be used to run all Feed library tests:
```
bazel test src/test/...
```

[bazel_android_instructions]:  https://docs.bazel.build/versions/master/tutorial/android-app.html
[contributor_agreement]: https://cla.developers.google.com/
[gerrit_docs]: https://gerrit-review.googlesource.com/Documentation/user-upload.
