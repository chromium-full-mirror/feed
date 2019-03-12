load("//src/main/java/com/google/android/libraries/feed/buildtools/testing:android_local_test.bzl", "DEFAULT_ANDROID_LOCAL_TEST_MANIFEST")

licenses(["notice"])  # Apache 2

android_local_test(
    name = "FeedActionParserTest",
    size = "small",
    timeout = "moderate",
    srcs = ["FeedActionParserTest.java"],
    aapt_version = "aapt2",
    manifest_values = DEFAULT_ANDROID_LOCAL_TEST_MANIFEST,
    deps = [
        "//src/main/java/com/google/android/libraries/feed/api/actionparser",
        "//src/main/java/com/google/android/libraries/feed/api/knowncontent",
        "//src/main/java/com/google/android/libraries/feed/api/protocoladapter",
        "//src/main/java/com/google/android/libraries/feed/common",
        "//src/main/java/com/google/android/libraries/feed/feedactionparser",
        "//src/main/java/com/google/android/libraries/feed/feedactionparser/internal",
        "//src/main/java/com/google/android/libraries/feed/host/action",
        "//src/main/java/com/google/android/libraries/feed/host/logging",
        "//src/main/java/com/google/android/libraries/feed/host/stream",
        "//src/main/proto/com/google/android/libraries/feed/api/proto:client_feed_java_proto_lite",
        "//src/main/proto/search/now/ui/action:feed_action_java_proto_lite",
        "//src/main/proto/search/now/ui/action:feed_action_payload_java_proto_lite",
        "//src/main/proto/search/now/ui/action:piet_extensions_java_proto_lite",
        "//src/main/proto/search/now/ui/piet:piet_java_proto_lite",
        "//src/main/proto/search/now/wire/feed:feed_java_proto_lite",
        "@com_google_guava_guava_android//jar",
        "@com_google_protobuf_javalite//:protobuf_java_lite",
        "@com_google_truth_truth//jar",
        "@org_mockito_mockito_all//jar",
        "@robolectric//bazel:robolectric",
    ],
)
