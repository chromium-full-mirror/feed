licenses(["notice"])  # Apache 2

android_local_test(
    name = "ConfigManagerTest",
    size = "small",
    timeout = "moderate",
    srcs = ["ConfigManagerTest.java"],
    manifest = "AndroidManifest.xml",
    deps = [
        "@com_google_protobuf_javalite//:protobuf_java_lite",
        "//src/main/java/com/google/android/libraries/feed/host/config",
        "@bazel_android//:robolectric",
        "@com_google_code_findbugs_jsr305//jar",
        "@com_google_truth_truth//jar",
    ],
)
