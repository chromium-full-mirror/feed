package(default_visibility = ["//visibility:public"])

licenses(["notice"])  # Apache 2

java_library(
    name = "actionmanager",
    testonly = True,
    srcs = glob(["*.java"]),
    deps = [
        "//src/main/java/com/google/android/libraries/feed/api/common",
        "//src/main/java/com/google/android/libraries/feed/common",
        "//src/main/java/com/google/android/libraries/feed/internalapi/actionmanager",
        "//src/main/proto/search/now/wire/feed:feed_java_proto_lite",
    ],
)
