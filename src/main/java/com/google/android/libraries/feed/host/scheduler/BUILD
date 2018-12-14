load("@gmaven_rules//:defs.bzl", "gmaven_artifact")

package(default_visibility = ["//visibility:public"])

licenses(["notice"])  # Apache 2

java_library(
    name = "scheduler",
    srcs = glob(["*.java"]),
    deps = [
        gmaven_artifact("com.android.support:support_annotations:jar:28.0.0"),
    ],
)
