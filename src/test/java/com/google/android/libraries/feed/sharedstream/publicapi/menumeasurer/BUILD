load("@gmaven_rules//:defs.bzl", "gmaven_artifact")
load("//src/main/java/com/google/android/libraries/feed/buildtools/testing:android_local_test.bzl", "DEFAULT_ANDROID_LOCAL_TEST_MANIFEST")

licenses(["notice"])  # Apache 2

android_local_test(
    name = "MenuMeasurerTest",
    size = "small",
    timeout = "moderate",
    srcs = ["MenuMeasurerTest.java"],
    aapt_version = "aapt",
    manifest_values = DEFAULT_ANDROID_LOCAL_TEST_MANIFEST,
    deps = [
        "@com_google_protobuf_javalite//:protobuf_java_lite",
        "//src/main/java/com/google/android/libraries/feed/sharedstream/publicapi/menumeasurer",
        "@com_google_guava_guava//jar",
        "@com_google_truth_truth//jar",
        "@robolectric//bazel:robolectric",
    ],
)
