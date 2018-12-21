load("@gmaven_rules//:defs.bzl", "gmaven_artifact")
load("//src/main/java/com/google/android/libraries/feed/buildtools/testing:android_local_test.bzl", "DEFAULT_ANDROID_LOCAL_TEST_MANIFEST")

licenses(["notice"])  # Apache 2

android_local_test(
    name = "OneShotVisibilityLoggingListenerTest",
    size = "small",
    timeout = "moderate",
    srcs = ["OneShotVisibilityLoggingListenerTest.java"],
    manifest_values = DEFAULT_ANDROID_LOCAL_TEST_MANIFEST,
    deps = [
        "@com_google_protobuf_javalite//:protobuf_java_lite",
        "//src/main/java/com/google/android/libraries/feed/sharedstream/logging",
        "@org_mockito_mockito_all//jar",
        "@robolectric//bazel:robolectric",
    ],
)

android_local_test(
    name = "SpinnerLoggerTest",
    size = "small",
    timeout = "moderate",
    srcs = ["SpinnerLoggerTest.java"],
    manifest_values = DEFAULT_ANDROID_LOCAL_TEST_MANIFEST,
    deps = [
        "@com_google_protobuf_javalite//:protobuf_java_lite",
        "//src/main/java/com/google/android/libraries/feed/common/testing",
        "//src/main/java/com/google/android/libraries/feed/host/logging",
        "//src/main/java/com/google/android/libraries/feed/sharedstream/logging",
        "@com_google_truth_truth//jar",
        "@org_mockito_mockito_all//jar",
        "@robolectric//bazel:robolectric",
    ],
)

android_local_test(
    name = "VisibilityMonitorTest",
    size = "small",
    timeout = "moderate",
    srcs = ["VisibilityMonitorTest.java"],
    manifest_values = DEFAULT_ANDROID_LOCAL_TEST_MANIFEST,
    deps = [
        "@com_google_protobuf_javalite//:protobuf_java_lite",
        "//src/main/java/com/google/android/libraries/feed/host/config",
        "//src/main/java/com/google/android/libraries/feed/sharedstream/logging",
        "@org_mockito_mockito_all//jar",
        "@robolectric//bazel:robolectric",
    ],
)
