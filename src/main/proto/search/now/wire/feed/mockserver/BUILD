package(default_visibility = ["//visibility:public"])

licenses(["notice"])  # Apache 2

proto_library(
    name = "mock_server_proto",
    srcs = glob(["*.proto"]),
    deps = [
        "//src/main/proto/search/now/wire/feed:feed_proto",
        "//third_party/android/nanoproto:nano_descriptor",
    ],
)

java_proto_library(
    name = "mock_server_java_proto",
    deps = [":mock_server_proto"],
)

java_lite_proto_library(
    name = "mock_server_java_proto_lite",
    deps = [":mock_server_proto"],
)
