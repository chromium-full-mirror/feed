load("@gmaven_rules//:defs.bzl", "gmaven_artifact")

package(default_visibility = ["//visibility:public"])

licenses(["notice"])  # Apache 2

package_group(
    name = "test_framework",
    packages = [
        "//src/test/java/com/google/android/libraries/feed/...",
    ],
)

android_library(
    name = "shadows",
    testonly = True,
    srcs = glob(["*.java"]),
    deps = [
        gmaven_artifact("com.android.support:recyclerview_v7:aar:28.0.0"),
        "@robolectric//bazel:robolectric",
    ],
)
