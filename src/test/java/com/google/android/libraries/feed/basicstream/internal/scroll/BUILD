load("//src/main/java/com/google/android/libraries/feed/buildtools/testing:android_local_test.bzl", "DEFAULT_ANDROID_LOCAL_TEST_MANIFEST")

licenses(["notice"])  # Apache 2

android_local_test(
    name = "ScrollRestorerTest",
    size = "small",
    timeout = "moderate",
    srcs = ["ScrollRestorerTest.java"],
    manifest_values = DEFAULT_ANDROID_LOCAL_TEST_MANIFEST,
    deps = [
        "@com_google_protobuf_javalite//:protobuf_java_lite",
        "//src/main/java/com/google/android/libraries/feed/basicstream/internal/scroll",
        "//src/main/java/com/google/android/libraries/feed/host/config",
        "//src/main/java/com/google/android/libraries/feed/sharedstream/proto:scroll_state_java_proto_lite",
        "//src/main/java/com/google/android/libraries/feed/testing/android",
        "@androidsdk//com.android.support:recyclerview-v7-25.3.1",
        "@bazel_android//:robolectric",
        "@com_google_truth_truth//jar",
        "@org_mockito_mockito_all//jar",
    ],
)

android_local_test(
    name = "StreamScrollMonitorTest",
    size = "small",
    timeout = "moderate",
    srcs = ["StreamScrollMonitorTest.java"],
    manifest_values = DEFAULT_ANDROID_LOCAL_TEST_MANIFEST,
    deps = [
        "@com_google_protobuf_javalite//:protobuf_java_lite",
        "//src/main/java/com/google/android/libraries/feed/api/stream",
        "//src/main/java/com/google/android/libraries/feed/basicstream/internal/scroll",
        "//src/main/java/com/google/android/libraries/feed/common/concurrent/testing",
        "//src/main/java/com/google/android/libraries/feed/common/testing",
        "@androidsdk//com.android.support:recyclerview-v7-25.3.1",
        "@bazel_android//:robolectric",
        "@org_mockito_mockito_all//jar",
    ],
)
