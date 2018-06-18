package(default_visibility = ["//visibility:public"])

licenses(["notice"])  # Apache 2

android_library(
    name = "scroll",
    srcs = glob(["*.java"]),
    deps = [
        "//src/main/java/com/google/android/libraries/feed/common",
        "//src/main/java/com/google/android/libraries/feed/common/logging",
        "@androidsdk//com.android.support:support-annotations-25.3.1",
        # NOTE: For unknown reasons this dep is required even though this code doesn't
        # reference this library. When this is removed the following error is given:
        # [INTERNAL LINK]
        "@androidsdk//com.android.support:support-core-ui-25.3.1",  # buildcleaner: keep
        "@androidsdk//com.android.support:recyclerview-v7-25.3.1",
        "@com_google_code_findbugs_jsr305//jar",
    ],
)
