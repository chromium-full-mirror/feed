load("//src/main/java/com/google/android/libraries/feed/buildtools/testing:android_local_test.bzl", "DEFAULT_ANDROID_LOCAL_TEST_MANIFEST")

licenses(["notice"])  # Apache 2

android_local_test(
    name = "ScrollRestoreHelperTest",
    size = "small",
    timeout = "moderate",
    srcs = ["ScrollRestoreHelperTest.java"],
    aapt_version = "aapt2",
    manifest_values = DEFAULT_ANDROID_LOCAL_TEST_MANIFEST,
    deps = [
        "//src/main/java/com/google/android/libraries/feed/host/config",
        "//src/main/java/com/google/android/libraries/feed/sharedstream/scroll",
        "//src/main/java/com/google/android/libraries/feed/testing/android",
        "//src/main/proto/com/google/android/libraries/feed/sharedstream/proto:scroll_state_java_proto_lite",
        "@com_google_protobuf_javalite//:protobuf_java_lite",
        "@com_google_truth_truth//jar",
        "@maven//:com_android_support_recyclerview_v7",
        "@robolectric//bazel:robolectric",
    ],
)

android_local_test(
    name = "StreamScrollMonitorTest",
    size = "small",
    timeout = "moderate",
    srcs = ["StreamScrollMonitorTest.java"],
    aapt_version = "aapt2",
    manifest_values = DEFAULT_ANDROID_LOCAL_TEST_MANIFEST,
    deps = [
        "//src/main/java/com/google/android/libraries/feed/api/stream",
        "//src/main/java/com/google/android/libraries/feed/common/concurrent/testing",
        "//src/main/java/com/google/android/libraries/feed/common/testing",
        "//src/main/java/com/google/android/libraries/feed/sharedstream/scroll",
        "@com_google_protobuf_javalite//:protobuf_java_lite",
        "@maven//:com_android_support_recyclerview_v7",
        "@org_mockito_mockito_all//jar",
        "@robolectric//bazel:robolectric",
    ],
)
