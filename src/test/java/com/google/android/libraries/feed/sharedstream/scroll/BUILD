load("//src/main/java/com/google/android/libraries/feed/buildtools/testing:android_local_test.bzl", "DEFAULT_ANDROID_LOCAL_TEST_MANIFEST")

licenses(["notice"])  # Apache 2

android_local_test(
    name = "ScrollRestoreHelperTest",
    size = "small",
    timeout = "moderate",
    srcs = ["ScrollRestoreHelperTest.java"],
    manifest_values = DEFAULT_ANDROID_LOCAL_TEST_MANIFEST,
    deps = [
        "@com_google_protobuf_javalite//:protobuf_java_lite",
        "//src/main/java/com/google/android/libraries/feed/host/config",
        "//src/main/java/com/google/android/libraries/feed/sharedstream/proto:scroll_state_java_proto_lite",
        "//src/main/java/com/google/android/libraries/feed/sharedstream/scroll",
        "//src/main/java/com/google/android/libraries/feed/testing/android",
        "@androidsdk//com.android.support:recyclerview-v7-25.3.1",
        "@bazel_android//:robolectric",
        "@com_google_truth_truth//jar",
    ],
)
