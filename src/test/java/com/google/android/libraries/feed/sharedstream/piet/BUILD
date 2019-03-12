load("//src/main/java/com/google/android/libraries/feed/buildtools/testing:android_local_test.bzl", "DEFAULT_ANDROID_LOCAL_TEST_MANIFEST")

licenses(["notice"])  # Apache 2

android_local_test(
    name = "PietEventLoggerTest",
    size = "small",
    timeout = "moderate",
    srcs = ["PietEventLoggerTest.java"],
    aapt_version = "aapt2",
    manifest_values = DEFAULT_ANDROID_LOCAL_TEST_MANIFEST,
    deps = [
        "//src/main/java/com/google/android/libraries/feed/host/logging",
        "//src/main/java/com/google/android/libraries/feed/sharedstream/piet",
        "//src/main/proto/search/now/ui/piet:piet_errors_java_proto_lite",
        "//src/main/proto/search/now/ui/piet:piet_java_proto_lite",
        "@com_google_protobuf_javalite//:protobuf_java_lite",
        "@com_google_truth_truth//jar",
        "@org_mockito_mockito_all//jar",
        "@robolectric//bazel:robolectric",
    ],
)

android_local_test(
    name = "PietImageLoaderTest",
    size = "small",
    timeout = "moderate",
    srcs = ["PietImageLoaderTest.java"],
    aapt_version = "aapt2",
    manifest_values = DEFAULT_ANDROID_LOCAL_TEST_MANIFEST,
    deps = [
        "//src/main/java/com/google/android/libraries/feed/common/functional",
        "//src/main/java/com/google/android/libraries/feed/common/time/testing",
        "//src/main/java/com/google/android/libraries/feed/host/imageloader",
        "//src/main/java/com/google/android/libraries/feed/host/stream",
        "//src/main/java/com/google/android/libraries/feed/sharedstream/piet",
        "//src/main/proto/search/now/ui/piet:piet_java_proto_lite",
        "@com_google_protobuf_javalite//:protobuf_java_lite",
        "@org_mockito_mockito_all//jar",
        "@robolectric//bazel:robolectric",
    ],
)

android_local_test(
    name = "PietHostBindingProviderTest",
    size = "small",
    timeout = "moderate",
    srcs = ["PietHostBindingProviderTest.java"],
    aapt_version = "aapt2",
    manifest_values = DEFAULT_ANDROID_LOCAL_TEST_MANIFEST,
    deps = [
        "//src/main/java/com/google/android/libraries/feed/piet/host",
        "//src/main/java/com/google/android/libraries/feed/sharedstream/offlinemonitor",
        "//src/main/java/com/google/android/libraries/feed/sharedstream/piet",
        "//src/main/proto/search/now/ui/piet:piet_java_proto_lite",
        "//src/main/proto/search/now/ui/stream:stream_offline_extension_java_proto_lite",
        "@com_google_protobuf_javalite//:protobuf_java_lite",
        "@com_google_truth_truth//jar",
        "@org_mockito_mockito_all//jar",
        "@robolectric//bazel:robolectric",
    ],
)

android_local_test(
    name = "PietCustomElementProviderTest",
    size = "small",
    timeout = "moderate",
    srcs = ["PietCustomElementProviderTest.java"],
    aapt_version = "aapt2",
    manifest_values = DEFAULT_ANDROID_LOCAL_TEST_MANIFEST,
    deps = [
        "//src/main/java/com/google/android/libraries/feed/piet/host",
        "//src/main/java/com/google/android/libraries/feed/sharedstream/piet",
        "//src/main/proto/search/now/ui/piet:piet_java_proto_lite",
        "@com_google_protobuf_javalite//:protobuf_java_lite",
        "@com_google_truth_truth//jar",
        "@org_mockito_mockito_all//jar",
        "@robolectric//bazel:robolectric",
    ],
)
