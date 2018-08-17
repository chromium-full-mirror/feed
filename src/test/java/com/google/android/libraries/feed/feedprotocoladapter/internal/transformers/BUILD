licenses(["notice"])  # Apache 2

android_local_test(
    name = "ContentDataOperationTransformerTest",
    size = "small",
    timeout = "moderate",
    srcs = ["ContentDataOperationTransformerTest.java"],
    manifest = "AndroidManifest.xml",
    deps = [
        "@com_google_protobuf_javalite//:protobuf_java_lite",
        "//src/main/java/com/google/android/libraries/feed/feedprotocoladapter/internal/transformers",
        "//src/main/proto/com/google/android/libraries/feed/api/proto:client_feed_java_proto_lite",
        "//src/main/proto/search/now/ui/stream:stream_java_proto_lite",
        "//src/main/proto/search/now/wire/feed:feed_java_proto_lite",
        "@bazel_android//:robolectric",
        "@com_google_truth_truth//jar",
    ],
)

android_local_test(
    name = "FeatureDataOperationTransformerTest",
    size = "small",
    timeout = "moderate",
    srcs = ["FeatureDataOperationTransformerTest.java"],
    manifest = "AndroidManifest.xml",
    deps = [
        "@com_google_protobuf_javalite//:protobuf_java_lite",
        "//src/main/java/com/google/android/libraries/feed/feedprotocoladapter/internal/transformers",
        "//src/main/proto/com/google/android/libraries/feed/api/proto:client_feed_java_proto_lite",
        "//src/main/proto/search/now/ui/stream:stream_java_proto_lite",
        "//src/main/proto/search/now/wire/feed:feed_java_proto_lite",
        "@bazel_android//:robolectric",
        "@com_google_truth_truth//jar",
    ],
)
