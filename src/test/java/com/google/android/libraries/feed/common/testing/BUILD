licenses(["notice"])  # Apache 2

java_test(
    name = "RunnableSubjectTest",
    size = "small",
    srcs = ["RunnableSubjectTest.java"],
    deps = [
        "@com_google_protobuf_javalite//:protobuf_java_lite",
        "//src/main/java/com/google/android/libraries/feed/common/testing",
        # The diffutils dependency is needed to use com.google.common.truth.ExpectFailure in the
        # open source build.
        "@com_googlecode_java_diff_utils_diffutils//jar",  # buildcleaner: keep
        "@junit_junit//jar",
        "@org_mockito_mockito_all//jar",
        "@com_google_truth_truth//jar",
    ],
)

java_test(
    name = "RequiredConsumerTest",
    size = "small",
    srcs = ["RequiredConsumerTest.java"],
    deps = [
        "//src/main/java/com/google/android/libraries/feed/common/testing",
        "@com_google_protobuf_javalite//:protobuf_java_lite",
        "@com_google_truth_truth//jar",
        "@junit_junit//jar",
    ],
)
