package(default_visibility = ["//visibility:public"])

licenses(["notice"])  # Apache 2

android_library(
    name = "actions",
    srcs = glob(["*.java"]),
    deps = [
        "//src/main/java/com/google/android/libraries/feed/api/knowncontent",
        "//src/main/java/com/google/android/libraries/feed/basicstream/internal/pendingdismiss",
        "//src/main/java/com/google/android/libraries/feed/host/action",
        "//src/main/java/com/google/android/libraries/feed/host/logging",
        "//src/main/java/com/google/android/libraries/feed/host/stream",
        "//src/main/java/com/google/android/libraries/feed/internalapi/actionmanager",
        "//src/main/java/com/google/android/libraries/feed/internalapi/actionparser",
        "//src/main/java/com/google/android/libraries/feed/sharedstream/contextmenumanager",
        "//src/main/java/com/google/android/libraries/feed/sharedstream/pendingdismiss",
        "//src/main/proto/com/google/android/libraries/feed/api/proto:client_feed_java_proto_lite",
        "//src/main/proto/search/now/ui/action:feed_action_java_proto_lite",
        "//src/main/proto/search/now/wire/feed:feed_java_proto_lite",
        "@com_google_code_findbugs_jsr305//jar",
    ],
)
