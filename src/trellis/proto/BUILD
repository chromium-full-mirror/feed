licenses(["notice"])

exports_files(["LICENSE"])

proto_library(
    name = "proto",
    srcs = glob(["*.proto"]),
    cc_api_version = 2,
    compatible_with = ["//buildenv/target:appengine"],
    deps = [
        "//google/protobuf:duration",
    ],
)
