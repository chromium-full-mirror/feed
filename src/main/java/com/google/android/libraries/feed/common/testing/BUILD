package(default_visibility = ["//visibility:public"])

licenses(["notice"])  # Apache 2

android_library(
    name = "testing",
    testonly = 1,
    srcs = glob(["*.java"]),
    deps = [
        "@maven//:com_google_guava_guava",
        "@maven//:com_google_truth_truth",
    ],
)
