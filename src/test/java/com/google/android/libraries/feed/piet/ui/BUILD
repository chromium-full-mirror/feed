load("@gmaven_rules//:defs.bzl", "gmaven_artifact")
load("//src/main/java/com/google/android/libraries/feed/buildtools/testing:android_local_test.bzl", "DEFAULT_ANDROID_LOCAL_TEST_MANIFEST")

licenses(["notice"])  # Apache 2

android_local_test(
    name = "AspectRatioScalingImageViewTest",
    size = "small",
    timeout = "moderate",
    srcs = ["AspectRatioScalingImageViewTest.java"],
    manifest_values = DEFAULT_ANDROID_LOCAL_TEST_MANIFEST,
    deps = [
        "@com_google_protobuf_javalite//:protobuf_java_lite",
        "//src/main/java/com/google/android/libraries/feed/piet/ui",
        "@com_google_truth_truth//jar",
        "@robolectric//bazel:robolectric",
    ],
)

android_local_test(
    name = "BorderDrawableTest",
    size = "small",
    timeout = "moderate",
    srcs = ["BorderDrawableTest.java"],
    manifest_values = DEFAULT_ANDROID_LOCAL_TEST_MANIFEST,
    deps = [
        "@com_google_protobuf_javalite//:protobuf_java_lite",
        "//src/main/java/com/google/android/libraries/feed/common/ui",
        "//src/main/java/com/google/android/libraries/feed/piet/ui",
        "//src/main/proto/search/now/ui/piet:piet_java_proto_lite",
        "@com_google_truth_truth//jar",
        "@robolectric//bazel:robolectric",
    ],
)

android_local_test(
    name = "GradientDrawableTest",
    size = "small",
    timeout = "moderate",
    srcs = ["GradientDrawableTest.java"],
    manifest_values = DEFAULT_ANDROID_LOCAL_TEST_MANIFEST,
    deps = [
        "@com_google_protobuf_javalite//:protobuf_java_lite",
        "//src/main/java/com/google/android/libraries/feed/common/testing",
        "//src/main/java/com/google/android/libraries/feed/common/ui",
        "//src/main/java/com/google/android/libraries/feed/piet/ui",
        "//src/main/proto/search/now/ui/piet:piet_java_proto_lite",
        "@com_google_truth_truth//jar",
        "@robolectric//bazel:robolectric",
    ],
)

android_local_test(
    name = "GradientShaderTest",
    size = "small",
    timeout = "moderate",
    srcs = ["GradientShaderTest.java"],
    manifest_values = DEFAULT_ANDROID_LOCAL_TEST_MANIFEST,
    deps = [
        "@com_google_protobuf_javalite//:protobuf_java_lite",
        "//src/main/java/com/google/android/libraries/feed/common/testing",
        "//src/main/java/com/google/android/libraries/feed/common/ui",
        "//src/main/java/com/google/android/libraries/feed/piet/ui",
        "//src/main/proto/search/now/ui/piet:piet_java_proto_lite",
        "@com_google_truth_truth//jar",
        "@robolectric//bazel:robolectric",
    ],
)

android_local_test(
    name = "GridRowViewTest",
    size = "small",
    timeout = "moderate",
    srcs = ["GridRowViewTest.java"],
    manifest_values = DEFAULT_ANDROID_LOCAL_TEST_MANIFEST,
    deps = [
        "@com_google_protobuf_javalite//:protobuf_java_lite",
        "//src/main/java/com/google/android/libraries/feed/common/testing",
        "//src/main/java/com/google/android/libraries/feed/piet/ui",
        "@com_google_truth_truth//jar",
        "@robolectric//bazel:robolectric",
    ],
)
