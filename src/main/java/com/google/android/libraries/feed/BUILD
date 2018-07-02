package(default_visibility = ["//visibility:public"])

package_group(
    name = "feed_pkgs",
    packages = ["//src/main/java/com/google/android/libraries/feed/..."],
)

licenses(["notice"])  # Apache 2

package_group(
    name = "feed_pkgs_and_users",
    includes = [
        ":feed_pkgs",
    ],
    packages = ["//..."],
)

filegroup(
    name = "all_messages",
    srcs = [
        "//src/main/java/com/google/android/libraries/feed/basicstream/internal/viewholders:translatable_messages",
    ],
)
