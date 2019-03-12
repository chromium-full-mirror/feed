load("//src/main/java/com/google/android/libraries/feed/buildtools/testing:android_local_test.bzl", "DEFAULT_ANDROID_LOCAL_TEST_MANIFEST")

licenses(["notice"])  # Apache 2

android_local_test(
    name = "MenuMeasurerTest",
    size = "small",
    timeout = "moderate",
    srcs = ["MenuMeasurerTest.java"],
    aapt_version = "aapt2",
    manifest_values = DEFAULT_ANDROID_LOCAL_TEST_MANIFEST,
    deps = [
        "//src/main/java/com/google/android/libraries/feed/sharedstream/publicapi/menumeasurer",
        "@com_google_guava_guava_android//jar",
        "@com_google_protobuf_javalite//:protobuf_java_lite",
        "@com_google_truth_truth//jar",
        "@robolectric//bazel:robolectric",
    ],
)
