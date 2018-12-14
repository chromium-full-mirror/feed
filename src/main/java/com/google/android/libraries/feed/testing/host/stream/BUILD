load("@gmaven_rules//:defs.bzl", "gmaven_artifact")

package(default_visibility = ["//visibility:public"])

licenses(["notice"])  # Apache 2

android_library(
    name = "fake_card_configuration",
    testonly = True,
    srcs = ["FakeCardConfiguration.java"],
    deps = [
        "//src/main/java/com/google/android/libraries/feed/host/stream",
        gmaven_artifact("com.android.support:support_annotations:jar:28.0.0"),
    ],
)
