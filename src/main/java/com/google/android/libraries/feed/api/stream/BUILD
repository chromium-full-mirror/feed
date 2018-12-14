load("@gmaven_rules//:defs.bzl", "gmaven_artifact")

package(default_visibility = ["//visibility:public"])

licenses(["notice"])  # Apache 2

android_library(
    name = "stream",
    srcs = glob(["*.java"]),
    deps = [
        gmaven_artifact("com.android.support:support_annotations:jar:28.0.0"),
        "@com_google_code_findbugs_jsr305//jar",
    ],
)
