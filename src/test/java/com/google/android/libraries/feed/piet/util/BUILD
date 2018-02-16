licenses(["notice"])  # Apache 2

android_local_test(
    name = "ParameterizedTextEvaluatorTest",
    size = "small",
    timeout = "moderate",
    srcs = ["ParameterizedTextEvaluatorTest.java"],
    manifest = "AndroidManifest.xml",
    deps = [
        "@com_google_protobuf_javalite//:protobuf_java_lite",
        "//src/main/java/com/google/android/libraries/feed/piet/host",
        "//src/main/java/com/google/android/libraries/feed/piet/util",
        "//src/main/proto/search/now/proto/ui/piet:piet_java_proto_lite",
        "@bazel_android//:robolectric",
        "@com_google_truth_truth//jar",
        "@org_mockito_mockito_all//jar",
    ],
)

android_local_test(
    name = "ViewUtilsTest",
    size = "small",
    timeout = "moderate",
    srcs = ["ViewUtilsTest.java"],
    manifest = "AndroidManifest.xml",
    deps = [
        "@com_google_protobuf_javalite//:protobuf_java_lite",
        "//src/main/java/com/google/android/libraries/feed/piet/host",
        "//src/main/java/com/google/android/libraries/feed/piet/util",
        "//src/main/proto/search/now/proto/ui/piet:piet_java_proto_lite",
        "@bazel_android//:robolectric",
        "@com_google_truth_truth//jar",
        "@org_mockito_mockito_all//jar",
    ],
)
