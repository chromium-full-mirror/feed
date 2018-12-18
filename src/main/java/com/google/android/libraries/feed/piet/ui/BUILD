load("@gmaven_rules//:defs.bzl", "gmaven_artifact")

package(default_visibility = ["//visibility:public"])

licenses(["notice"])  # Apache 2

android_library(
    name = "ui",
    srcs = glob(["*.java"]),
    deps = [
        "//src/main/java/com/google/android/libraries/feed/common/logging",
        "//src/main/java/com/google/android/libraries/feed/common/ui",
        "//src/main/proto/search/now/ui/piet:piet_java_proto_lite",
        gmaven_artifact("com.android.support:support_annotations:jar:28.0.0"),
        "@com_google_code_findbugs_jsr305//jar",
    ],
)
