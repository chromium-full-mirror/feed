licenses(["notice"])  # Apache 2

android_local_test(
    name = "MutationContextTest",
    size = "small",
    timeout = "moderate",
    srcs = ["MutationContextTest.java"],
    manifest = "AndroidManifest.xml",
    deps = [
        "@com_google_protobuf_javalite//:protobuf_java_lite",
        "//src/main/java/com/google/android/libraries/feed/api/common",
        "//src/main/java/com/google/android/libraries/feed/common/functional",
        "//src/main/proto/com/google/android/libraries/feed/api/proto:client_feed_java_proto_lite",
        "@bazel_android//:robolectric",
        "@com_google_truth_truth//jar",
    ],
)

android_local_test(
    name = "ThreadUtilsTest",
    size = "small",
    timeout = "moderate",
    srcs = ["ThreadUtilsTest.java"],
    manifest = "AndroidManifest.xml",
    deps = [
        "@com_google_protobuf_javalite//:protobuf_java_lite",
        "//src/main/java/com/google/android/libraries/feed/api/common",
        "//src/main/java/com/google/android/libraries/feed/common/testing",
        "@bazel_android//:robolectric",
        "@com_google_truth_truth//jar",
    ],
)
