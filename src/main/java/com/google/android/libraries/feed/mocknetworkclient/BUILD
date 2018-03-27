package(default_visibility = ["//visibility:public"])

licenses(["notice"])  # Apache 2

android_library(
    name = "mocknetworkclient",
    srcs = glob(["*.java"]),
    deps = [
        "//src/main/java/com/google/android/libraries/feed/api/requestmanager",
        "//src/main/java/com/google/android/libraries/feed/common",
        "//src/main/java/com/google/android/libraries/feed/common/protoextensions",
        "//src/main/java/com/google/android/libraries/feed/host/network",
        "//src/main/proto/search/now/proto/wire/feed:feed_java_proto_lite",
        "//src/main/proto/search/now/proto/wire/feed/mockserver:mock_server_java_proto_lite",
        "@androidsdk//com.android.support:support-annotations-25.3.1",
        "@com_google_code_findbugs_jsr305//jar",
        "@com_google_protobuf_javalite//:protobuf_java_lite",
    ],
)
