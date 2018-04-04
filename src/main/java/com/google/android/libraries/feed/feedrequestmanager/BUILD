package(default_visibility = ["//visibility:public"])

licenses(["notice"])  # Apache 2

android_library(
    name = "feedrequestmanager",
    srcs = glob(["*.java"]),
    deps = [
        "//src/main/java/com/google/android/libraries/feed/api/common",
        "//src/main/java/com/google/android/libraries/feed/api/protocoladapter",
        "//src/main/java/com/google/android/libraries/feed/api/requestmanager",
        "//src/main/java/com/google/android/libraries/feed/common",
        "//src/main/java/com/google/android/libraries/feed/common/protoextensions",
        "//src/main/java/com/google/android/libraries/feed/host/config",
        "//src/main/java/com/google/android/libraries/feed/host/network",
        "//src/main/java/com/google/android/libraries/feed/host/scheduler",
        "//src/main/proto/search/now/proto/wire/feed:feed_java_proto_lite",
        "@com_google_code_findbugs_jsr305//jar",
        "@com_google_protobuf_javalite//:protobuf_java_lite",
    ],
)
