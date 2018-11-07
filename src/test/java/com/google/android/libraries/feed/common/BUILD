load("//src/main/java/com/google/android/libraries/feed/buildtools/testing:android_local_test.bzl", "DEFAULT_ANDROID_LOCAL_TEST_MANIFEST")

licenses(["notice"])  # Apache 2

android_local_test(
    name = "ValidatorsTest",
    size = "small",
    timeout = "moderate",
    srcs = ["ValidatorsTest.java"],
    manifest_values = DEFAULT_ANDROID_LOCAL_TEST_MANIFEST,
    deps = [
        "@com_google_protobuf_javalite//:protobuf_java_lite",
        "//src/main/java/com/google/android/libraries/feed/common",
        "//src/main/java/com/google/android/libraries/feed/common/testing",
        "@bazel_android//:robolectric",
        "@com_google_truth_truth//jar",
    ],
)

android_local_test(
    name = "ResultTest",
    size = "small",
    timeout = "moderate",
    srcs = ["ResultTest.java"],
    manifest_values = DEFAULT_ANDROID_LOCAL_TEST_MANIFEST,
    deps = [
        "@com_google_protobuf_javalite//:protobuf_java_lite",
        "//src/main/java/com/google/android/libraries/feed/common",
        "//src/main/java/com/google/android/libraries/feed/common/testing",
        "@bazel_android//:robolectric",
        "@com_google_truth_truth//jar",
    ],
)
