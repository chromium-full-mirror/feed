licenses(["notice"])  # Apache 2

android_local_test(
    name = "SimpleSettableFutureTest",
    size = "small",
    timeout = "moderate",
    srcs = ["SimpleSettableFutureTest.java"],
    deps = [
        "@com_google_protobuf_javalite//:protobuf_java_lite",
        "//src/main/java/com/google/android/libraries/feed/common/concurrent",
        "//src/main/java/com/google/android/libraries/feed/common/testing",
        "@bazel_android//:robolectric",
        "@com_google_truth_truth//jar",
    ],
)

android_local_test(
    name = "TaskQueueTest",
    size = "small",
    timeout = "moderate",
    srcs = ["TaskQueueTest.java"],
    manifest = "AndroidManifest.xml",
    deps = [
        "@com_google_protobuf_javalite//:protobuf_java_lite",
        "//src/main/java/com/google/android/libraries/feed/common/concurrent",
        "//src/main/java/com/google/android/libraries/feed/common/time",
        "@bazel_android//:robolectric",
        "@com_google_guava_guava//jar",
        "@com_google_truth_truth//jar",
        "@org_mockito_mockito_all//jar",
    ],
)
