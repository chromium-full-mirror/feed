licenses(["notice"])  # Apache 2

android_local_test(
    name = "DeepestContentTrackerTest",
    size = "small",
    timeout = "moderate",
    srcs = ["DeepestContentTrackerTest.java"],
    manifest = "AndroidManifest.xml",
    deps = [
        "@com_google_protobuf_javalite//:protobuf_java_lite",
        "//src/main/java/com/google/android/libraries/feed/basicstream/internal",
        "@bazel_android//:robolectric",
        "@com_google_truth_truth//jar",
    ],
)

android_local_test(
    name = "StreamContentChangedListenerTest",
    size = "small",
    timeout = "moderate",
    srcs = ["StreamContentChangedListenerTest.java"],
    manifest = "AndroidManifest.xml",
    deps = [
        "@com_google_protobuf_javalite//:protobuf_java_lite",
        "//src/main/java/com/google/android/libraries/feed/api/stream",
        "//src/main/java/com/google/android/libraries/feed/basicstream/internal",
        "@bazel_android//:robolectric",
        "@org_mockito_mockito_all//jar",
    ],
)

android_local_test(
    name = "StreamItemAnimatorTest",
    size = "small",
    timeout = "moderate",
    srcs = ["StreamItemAnimatorTest.java"],
    manifest = "AndroidManifest.xml",
    deps = [
        "@com_google_protobuf_javalite//:protobuf_java_lite",
        "//src/main/java/com/google/android/libraries/feed/api/stream",
        "//src/main/java/com/google/android/libraries/feed/basicstream/internal",
        "//src/main/java/com/google/android/libraries/feed/basicstream/internal/viewholders",
        "@bazel_android//:robolectric",
        "@org_mockito_mockito_all//jar",
    ],
)

android_local_test(
    name = "StreamItemTouchCallbacksTest",
    size = "small",
    timeout = "moderate",
    srcs = ["StreamItemTouchCallbacksTest.java"],
    manifest = "AndroidManifest.xml",
    deps = [
        "@com_google_protobuf_javalite//:protobuf_java_lite",
        "//src/main/java/com/google/android/libraries/feed/basicstream/internal",
        "//src/main/java/com/google/android/libraries/feed/basicstream/internal/viewholders",
        "@androidsdk//com.android.support:recyclerview-v7-25.3.1",
        "@bazel_android//:robolectric",
        "@com_google_truth_truth//jar",
        "@org_mockito_mockito_all//jar",
    ],
)

android_local_test(
    name = "StreamRecyclerViewAdapterTest",
    size = "small",
    timeout = "moderate",
    srcs = ["StreamRecyclerViewAdapterTest.java"],
    manifest = "AndroidManifest.xml",
    deps = [
        "@com_google_protobuf_javalite//:protobuf_java_lite",
        "//src/main/java/com/google/android/libraries/feed/api/actionparser",
        "//src/main/java/com/google/android/libraries/feed/basicstream/internal",
        "//src/main/java/com/google/android/libraries/feed/basicstream/internal/drivers",
        "//src/main/java/com/google/android/libraries/feed/basicstream/internal/viewholders",
        "//src/main/java/com/google/android/libraries/feed/common/functional",
        "//src/main/java/com/google/android/libraries/feed/host/config",
        "//src/main/java/com/google/android/libraries/feed/host/stream",
        "//src/main/java/com/google/android/libraries/feed/piet",
        "//src/main/java/com/google/android/libraries/feed/piet/host",
        "@androidsdk//com.android.support:recyclerview-v7-25.3.1",
        "@bazel_android//:robolectric",
        "@com_google_guava_guava//jar",
        "@com_google_truth_truth//jar",
        "@org_mockito_mockito_all//jar",
    ],
)

android_local_test(
    name = "StreamScrollMonitorTest",
    size = "small",
    timeout = "moderate",
    srcs = ["StreamScrollMonitorTest.java"],
    manifest = "AndroidManifest.xml",
    deps = [
        "@com_google_protobuf_javalite//:protobuf_java_lite",
        "//src/main/java/com/google/android/libraries/feed/api/stream",
        "//src/main/java/com/google/android/libraries/feed/basicstream/internal",
        "//src/main/java/com/google/android/libraries/feed/common/testing",
        "@androidsdk//com.android.support:recyclerview-v7-25.3.1",
        "@bazel_android//:robolectric",
        "@org_mockito_mockito_all//jar",
    ],
)
