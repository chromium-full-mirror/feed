licenses(["notice"])  # Apache 2

android_local_test(
    name = "FeedKnownContentApiTest",
    size = "small",
    timeout = "moderate",
    srcs = ["FeedKnownContentApiTest.java"],
    manifest = "AndroidManifest.xml",
    deps = [
        "@com_google_protobuf_javalite//:protobuf_java_lite",
        "//src/main/java/com/google/android/libraries/feed/api/knowncontent",
        "//src/main/java/com/google/android/libraries/feed/api/sessionmanager",
        "//src/main/java/com/google/android/libraries/feed/feedknowncontent",
        "@bazel_android//:robolectric",
        "@org_mockito_mockito_all//jar",
    ],
)
