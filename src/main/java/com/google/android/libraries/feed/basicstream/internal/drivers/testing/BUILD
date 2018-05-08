package(default_visibility = ["//visibility:public"])

licenses(["notice"])  # Apache 2

android_library(
    name = "fake_feature_driver",
    testonly = True,
    srcs = ["FakeFeatureDriver.java"],
    deps = [
        "//src/main/java/com/google/android/libraries/feed/api/modelprovider",
        "//src/main/java/com/google/android/libraries/feed/basicstream/internal/contentmodels",
        "//src/main/java/com/google/android/libraries/feed/basicstream/internal/contentmodels/testing:fake_content_model",
        "//src/main/java/com/google/android/libraries/feed/basicstream/internal/drivers",
        "//src/main/java/com/google/android/libraries/feed/testing/modelprovider:fake_model_feature",
        "@com_google_code_findbugs_jsr305//jar",
    ],
)
