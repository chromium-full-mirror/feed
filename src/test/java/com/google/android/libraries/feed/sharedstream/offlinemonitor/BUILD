licenses(["notice"])  # Apache 2

android_local_test(
    name = "StreamOfflineMonitorTest",
    size = "small",
    timeout = "moderate",
    srcs = ["StreamOfflineMonitorTest.java"],
    manifest = "AndroidManifest.xml",
    deps = [
        "@com_google_protobuf_javalite//:protobuf_java_lite",
        "//src/main/java/com/google/android/libraries/feed/common/functional",
        "//src/main/java/com/google/android/libraries/feed/host/offlineindicator",
        "//src/main/java/com/google/android/libraries/feed/sharedstream/offlinemonitor",
        "@bazel_android//:robolectric",
        "@com_google_truth_truth//jar",
        "@org_mockito_mockito_all//jar",
    ],
)
