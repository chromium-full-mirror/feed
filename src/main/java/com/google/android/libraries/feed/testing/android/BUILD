load("@gmaven_rules//:defs.bzl", "gmaven_artifact")

package(default_visibility = ["//visibility:public"])

licenses(["notice"])  # Apache 2

android_library(
    name = "android",
    testonly = 1,
    srcs = glob(["*.java"]),
    deps = [gmaven_artifact("com.android.support:recyclerview_v7:aar:28.0.0")],
)
