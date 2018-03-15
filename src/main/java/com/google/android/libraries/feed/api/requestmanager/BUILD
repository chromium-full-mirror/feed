package(default_visibility = ["//visibility:public"])

licenses(["notice"])  # Apache 2

android_library(
    name = "requestmanager",
    srcs = glob(["*.java"]),
    deps = [
        "//src/main/proto/search/now/proto/wire/feed:feed_java_proto_lite",
        "@com_google_protobuf_javalite//:protobuf_java_lite",
    ],
)
