load("@gmaven_rules//:defs.bzl", "gmaven_artifact")

package(default_visibility = ["//visibility:public"])

licenses(["notice"])  # Apache 2

android_library(
    name = "viewholders",
    srcs = glob(["*.java"]),
    manifest = "AndroidManifest.xml",
    resource_files = glob(["res/**"]),
    deps = [
        "//src/main/java/com/google/android/libraries/feed/api/actionparser",
        "//src/main/java/com/google/android/libraries/feed/api/stream",
        "//src/main/java/com/google/android/libraries/feed/common",
        "//src/main/java/com/google/android/libraries/feed/common/logging",
        "//src/main/java/com/google/android/libraries/feed/common/ui",
        "//src/main/java/com/google/android/libraries/feed/host/action",
        "//src/main/java/com/google/android/libraries/feed/host/config",
        "//src/main/java/com/google/android/libraries/feed/host/stream",
        "//src/main/java/com/google/android/libraries/feed/piet",
        "//src/main/java/com/google/android/libraries/feed/piet/host",
        "//src/main/java/com/google/android/libraries/feed/sharedstream:resources",  # build_cleaner: keep
        "//src/main/java/com/google/android/libraries/feed/sharedstream/logging",
        "//src/main/java/com/google/android/libraries/feed/sharedstream/scroll",
        "//src/main/proto/search/now/ui/action:feed_action_payload_java_proto_lite",
        "//src/main/proto/search/now/ui/piet:piet_java_proto_lite",
        gmaven_artifact("com.android.support:support_annotations:jar:28.0.0"),  # buildcleaner: keep
        gmaven_artifact("com.android.support:recyclerview_v7:aar:28.0.0"),
        "@com_google_code_findbugs_jsr305//jar",
    ],
)
