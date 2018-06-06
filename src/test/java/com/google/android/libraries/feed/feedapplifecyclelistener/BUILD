licenses(["notice"])  # Apache 2

android_local_test(
    name = "FeedAppLifecycleListenerTest",
    size = "small",
    timeout = "moderate",
    srcs = ["FeedAppLifecycleListenerTest.java"],
    manifest = "AndroidManifest.xml",
    deps = [
        "@com_google_protobuf_javalite//:protobuf_java_lite",
        "//src/main/java/com/google/android/libraries/feed/api/common",
        "//src/main/java/com/google/android/libraries/feed/feedapplifecyclelistener",
        "//src/main/java/com/google/android/libraries/feed/feedapplifecyclelistener/internal",
        "@bazel_android//:robolectric",
        "@org_mockito_mockito_all//jar",
    ],
)
