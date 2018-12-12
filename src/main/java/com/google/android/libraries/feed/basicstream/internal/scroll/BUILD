package(default_visibility = ["//visibility:public"])

licenses(["notice"])  # Apache 2

android_library(
    name = "scroll",
    srcs = glob(["*.java"]),
    deps = [
        "//src/main/java/com/google/android/libraries/feed/api/stream",
        "//src/main/java/com/google/android/libraries/feed/common",
        "//src/main/java/com/google/android/libraries/feed/common/concurrent",
        "//src/main/java/com/google/android/libraries/feed/common/logging",
        "//src/main/java/com/google/android/libraries/feed/host/config",
        "//src/main/java/com/google/android/libraries/feed/sharedstream/proto:scroll_state_java_proto_lite",
        "//src/main/java/com/google/android/libraries/feed/sharedstream/scroll",
        "@androidsdk//com.android.support:recyclerview-v7-25.3.1",
        "@com_google_code_findbugs_jsr305//jar",
    ],
)
