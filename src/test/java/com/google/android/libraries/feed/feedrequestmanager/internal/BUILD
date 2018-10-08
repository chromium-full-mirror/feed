licenses(["notice"])  # Apache 2

android_local_test(
    name = "UtilsTest",
    size = "small",
    timeout = "moderate",
    srcs = ["UtilsTest.java"],
    manifest = "AndroidManifest.xml",
    deps = [
        "@com_google_protobuf_javalite//:protobuf_java_lite",
        "//src/main/java/com/google/android/libraries/feed/feedrequestmanager/internal",
        "//src/main/proto/search/now/wire/feed:feed_java_proto_lite",
        "@bazel_android//:robolectric",
        "@com_google_truth_liteproto_extension//jar",
        "@com_google_truth_truth//jar",  # buildclean: keep
    ],
)
