load("//src/main/java/com/google/android/libraries/feed/buildtools/testing:android_local_test.bzl", "DEFAULT_ANDROID_LOCAL_TEST_MANIFEST")

licenses(["notice"])  # Apache 2

android_local_test(
    name = "UtilsTest",
    size = "small",
    timeout = "moderate",
    srcs = ["UtilsTest.java"],
    aapt_version = "aapt2",
    manifest_values = DEFAULT_ANDROID_LOCAL_TEST_MANIFEST,
    deps = [
        "//src/main/java/com/google/android/libraries/feed/feedrequestmanager/internal",
        "//src/main/proto/search/now/wire/feed:feed_java_proto_lite",
        "@com_google_protobuf_javalite//:protobuf_java_lite",
        "@com_google_truth_liteproto_extension//jar",
        "@com_google_truth_truth//jar",  # buildclean: keep
        "@robolectric//bazel:robolectric",
    ],
)
