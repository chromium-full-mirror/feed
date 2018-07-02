package(default_visibility = ["//visibility:public"])

licenses(["notice"])  # Apache 2

android_library(
    name = "logging",
    srcs = glob(["*.java"]),
    deps = [
        "//src/main/java/com/google/android/libraries/feed/common/logging",
        "//src/main/java/com/google/android/libraries/feed/host/logging",
        "@androidsdk//com.android.support:support-core-ui-25.3.1",
        "@com_google_code_findbugs_jsr305//jar",
    ],
)
