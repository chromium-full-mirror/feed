package(default_visibility = ["//visibility:public"])

licenses(["notice"])  # Apache 2

java_library(
    name = "util",
    srcs = glob(["*.java"]),
    constraints = ["android"],
    deps = [
        "//src/main/java/com/google/android/libraries/feed/common",
        "//src/main/java/com/google/android/libraries/feed/piet/host",
        "//src/main/proto/search/now/proto/ui/piet:piet_java_proto_lite",
        "@androidsdk//com.android.support:support-annotations-25.3.1",
        "@androidsdk//com.android.support:support-compat-25.3.1",
        "@com_google_code_findbugs_jsr305//jar",
    ],
)
