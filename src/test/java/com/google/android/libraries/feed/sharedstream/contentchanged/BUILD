load("//src/main/java/com/google/android/libraries/feed/buildtools/testing:android_local_test.bzl", "DEFAULT_ANDROID_LOCAL_TEST_MANIFEST")

licenses(["notice"])  # Apache 2

android_local_test(
    name = "StreamContentChangedListenerTest",
    size = "small",
    timeout = "moderate",
    srcs = ["StreamContentChangedListenerTest.java"],
    manifest_values = DEFAULT_ANDROID_LOCAL_TEST_MANIFEST,
    deps = [
        "@com_google_protobuf_javalite//:protobuf_java_lite",
        "//src/main/java/com/google/android/libraries/feed/api/stream",
        "//src/main/java/com/google/android/libraries/feed/sharedstream/contentchanged",
        "@bazel_android//:robolectric",
        "@org_mockito_mockito_all//jar",
    ],
)
