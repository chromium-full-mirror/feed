licenses(["notice"])  # Apache 2

android_local_test(
    name = "ContextMenuHelperTest",
    size = "small",
    timeout = "moderate",
    srcs = ["ContextMenuHelperTest.java"],
    manifest = "AndroidManifest.xml",
    deps = [
        "@com_google_protobuf_javalite//:protobuf_java_lite",
        "//src/main/java/com/google/android/libraries/feed/basicstream/internal/util",
        "@bazel_android//:robolectric",
        "@com_google_truth_truth//jar",
    ],
)
