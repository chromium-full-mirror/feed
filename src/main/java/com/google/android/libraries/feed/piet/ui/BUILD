package(default_visibility = ["//visibility:public"])

licenses(["notice"])  # Apache 2

android_library(
    name = "ui",
    srcs = glob(["*.java"]),
    deps = [
        "//src/main/java/com/google/android/libraries/feed/common",
        "//src/main/java/com/google/android/libraries/feed/common/functional",
        "//src/main/java/com/google/android/libraries/feed/common/logging",
        "//src/main/java/com/google/android/libraries/feed/common/ui",
        "//src/main/proto/search/now/ui/piet:piet_java_proto_lite",
        "@maven//:androidx_annotation_annotation",
        "@maven//:com_android_support_support_compat",
        "@maven//:com_google_code_findbugs_jsr305",
    ],
)
