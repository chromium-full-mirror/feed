package(default_visibility = ["//visibility:public"])

licenses(["notice"])  # Apache 2

android_library(
    name = "mocknetworkclient",
    srcs = glob(["*.java"]),
    deps = [
        "//src/main/java/com/google/android/libraries/feed/api/common",
        "//src/main/java/com/google/android/libraries/feed/api/sessionmanager",
        "//src/main/java/com/google/android/libraries/feed/common",
        "//src/main/java/com/google/android/libraries/feed/common/functional",
        "//src/main/java/com/google/android/libraries/feed/common/logging",
        "//src/main/java/com/google/android/libraries/feed/feedrequestmanager",
        "//src/main/java/com/google/android/libraries/feed/host/network",
        "//src/main/java/com/google/android/libraries/feed/internalapi/protocoladapter",
        "//src/main/proto/com/google/android/libraries/feed/internalapi/proto:client_feed_java_proto_lite",
        "//src/main/proto/search/now/wire/feed:feed_java_proto_lite",
        "//src/main/proto/search/now/wire/feed/mockserver:mock_server_java_proto_lite",
        "@com_google_protobuf_javalite//:protobuf_java_lite",
        "@maven//:com_android_support_support_annotations",
    ],
)
