load("@gmaven_rules//:defs.bzl", "gmaven_artifact")
load("//src/main/java/com/google/android/libraries/feed/buildtools/testing:android_local_test.bzl", "DEFAULT_ANDROID_LOCAL_TEST_MANIFEST")

licenses(["notice"])  # Apache 2

android_local_test(
    name = "ResettableOneShotVisibilityLoggingListenerTest",
    size = "small",
    timeout = "moderate",
    srcs = ["ResettableOneShotVisibilityLoggingListenerTest.java"],
    aapt_version = "aapt2",
    manifest_values = DEFAULT_ANDROID_LOCAL_TEST_MANIFEST,
    deps = [
        "//src/main/java/com/google/android/libraries/feed/basicstream/internal/viewloggingupdater",
        "//src/main/java/com/google/android/libraries/feed/sharedstream/logging",
        "@com_google_protobuf_javalite//:protobuf_java_lite",
        "@org_mockito_mockito_all//jar",
        "@robolectric//bazel:robolectric",
    ],
)

android_local_test(
    name = "ViewLoggingUpdaterTest",
    size = "small",
    timeout = "moderate",
    srcs = ["ViewLoggingUpdaterTest.java"],
    aapt_version = "aapt2",
    manifest_values = DEFAULT_ANDROID_LOCAL_TEST_MANIFEST,
    deps = [
        "//src/main/java/com/google/android/libraries/feed/basicstream/internal/viewloggingupdater",
        "@com_google_protobuf_javalite//:protobuf_java_lite",
        "@org_mockito_mockito_all//jar",
        "@robolectric//bazel:robolectric",
    ],
)
