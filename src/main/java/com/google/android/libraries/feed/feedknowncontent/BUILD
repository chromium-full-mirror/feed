package(default_visibility = ["//visibility:public"])

package_group(
    name = "feedknowncontent_pkgs",
    packages = [
        "//src/main/java/com/google/android/libraries/feed/feedknowncontent/...",
        "//src/test/java/com/google/android/libraries/feed/feedknowncontent/...",
    ],
)

licenses(["notice"])  # Apache 2

java_library(
    name = "feedknowncontent",
    srcs = glob(["*.java"]),
    deps = [
        "//src/main/java/com/google/android/libraries/feed/api/knowncontent",
        "//src/main/java/com/google/android/libraries/feed/api/sessionmanager",
        "//src/main/java/com/google/android/libraries/feed/common/functional",
    ],
)
