package(default_visibility = ["//visibility:public"])

licenses(["notice"])  # Apache 2

android_library(
    name = "feedactionmanager",
    srcs = glob(["*.java"]),
    deps = [
        "//src/main/java/com/google/android/libraries/feed/api/common",
        "//src/main/java/com/google/android/libraries/feed/api/internal/sessionmanager",
        "//src/main/java/com/google/android/libraries/feed/common",
        "//src/main/java/com/google/android/libraries/feed/common/concurrent",
        "//src/main/java/com/google/android/libraries/feed/common/functional",
        "//src/main/java/com/google/android/libraries/feed/common/time",
        "//src/main/java/com/google/android/libraries/feed/host/logging",
        "//src/main/java/com/google/android/libraries/feed/internalapi/actionmanager",
        "//src/main/java/com/google/android/libraries/feed/internalapi/common",
        "//src/main/java/com/google/android/libraries/feed/internalapi/store",
        "//src/main/proto/com/google/android/libraries/feed/internalapi/proto:client_feed_java_proto_lite",
        "//src/main/proto/search/now/wire/feed:feed_java_proto_lite",
        "@com_google_code_findbugs_jsr305//jar",
    ],
)
