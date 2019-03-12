load("//src/main/java/com/google/android/libraries/feed/buildtools/testing:android_local_test.bzl", "DEFAULT_ANDROID_LOCAL_TEST_MANIFEST")

licenses(["notice"])  # Apache 2

android_local_test(
    name = "StreamContentChangedListenerTest",
    size = "small",
    timeout = "moderate",
    srcs = ["StreamContentChangedListenerTest.java"],
    aapt_version = "aapt2",
    manifest_values = DEFAULT_ANDROID_LOCAL_TEST_MANIFEST,
    deps = [
        "//src/main/java/com/google/android/libraries/feed/api/stream",
        "//src/main/java/com/google/android/libraries/feed/sharedstream/contentchanged",
        "@com_google_protobuf_javalite//:protobuf_java_lite",
        "@org_mockito_mockito_all//jar",
        "@robolectric//bazel:robolectric",
    ],
)
