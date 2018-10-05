licenses(["notice"])  # Apache 2

android_local_test(
    name = "LocaleUtilsTest",
    size = "small",
    timeout = "moderate",
    srcs = ["LocaleUtilsTest.java"],
    manifest = "AndroidManifest.xml",
    deps = [
        "@com_google_protobuf_javalite//:protobuf_java_lite",
        "//src/main/java/com/google/android/libraries/feed/common/locale",
        "@bazel_android//:robolectric",
        "@com_google_truth_truth//jar",
    ],
)
