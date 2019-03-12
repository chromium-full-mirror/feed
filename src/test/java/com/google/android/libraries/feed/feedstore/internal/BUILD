load("//src/main/java/com/google/android/libraries/feed/buildtools/testing:android_local_test.bzl", "DEFAULT_ANDROID_LOCAL_TEST_MANIFEST")

licenses(["notice"])  # Apache 2

android_local_test(
    name = "LocalActionGcTest",
    size = "small",
    timeout = "moderate",
    srcs = ["LocalActionGcTest.java"],
    aapt_version = "aapt2",
    manifest_values = DEFAULT_ANDROID_LOCAL_TEST_MANIFEST,
    deps = [
        "//src/main/java/com/google/android/libraries/feed/api/store",
        "//src/main/java/com/google/android/libraries/feed/common/time",
        "//src/main/java/com/google/android/libraries/feed/feedstore/internal",
        "//src/main/java/com/google/android/libraries/feed/host/storage",
        "//src/main/proto/com/google/android/libraries/feed/api/proto:client_feed_java_proto_lite",
        "@com_google_protobuf_javalite//:protobuf_java_lite",
        "@com_google_truth_truth//jar",
        "@org_mockito_mockito_all//jar",
        "@robolectric//bazel:robolectric",
    ],
)

android_local_test(
    name = "ContentGcTest",
    size = "small",
    timeout = "moderate",
    srcs = ["ContentGcTest.java"],
    aapt_version = "aapt2",
    manifest_values = DEFAULT_ANDROID_LOCAL_TEST_MANIFEST,
    deps = [
        "//src/main/java/com/google/android/libraries/feed/api/store",
        "//src/main/java/com/google/android/libraries/feed/common",
        "//src/main/java/com/google/android/libraries/feed/common/time",
        "//src/main/java/com/google/android/libraries/feed/feedstore/internal",
        "//src/main/java/com/google/android/libraries/feed/host/storage",
        "//src/main/proto/com/google/android/libraries/feed/api/proto:client_feed_java_proto_lite",
        "@com_google_protobuf_javalite//:protobuf_java_lite",
        "@com_google_truth_truth//jar",
        "@org_mockito_mockito_all//jar",
        "@robolectric//bazel:robolectric",
    ],
)

android_local_test(
    name = "EphemeralFeedStoreTest",
    size = "small",
    timeout = "moderate",
    srcs = ["EphemeralFeedStoreTest.java"],
    aapt_version = "aapt2",
    manifest_values = DEFAULT_ANDROID_LOCAL_TEST_MANIFEST,
    deps = [
        "//src/main/java/com/google/android/libraries/feed/api/store",
        "//src/main/java/com/google/android/libraries/feed/common/concurrent",
        "//src/main/java/com/google/android/libraries/feed/feedstore/internal",
        "//src/main/java/com/google/android/libraries/feed/feedstore/testing",
        "@com_google_protobuf_javalite//:protobuf_java_lite",
        "@org_mockito_mockito_all//jar",
        "@robolectric//bazel:robolectric",
    ],
)

android_local_test(
    name = "PersistentFeedStoreTest",
    size = "small",
    timeout = "moderate",
    srcs = ["PersistentFeedStoreTest.java"],
    aapt_version = "aapt2",
    manifest_values = DEFAULT_ANDROID_LOCAL_TEST_MANIFEST,
    deps = [
        "//src/main/java/com/google/android/libraries/feed/api/common",
        "//src/main/java/com/google/android/libraries/feed/api/store",
        "//src/main/java/com/google/android/libraries/feed/common",
        "//src/main/java/com/google/android/libraries/feed/common/concurrent",
        "//src/main/java/com/google/android/libraries/feed/common/concurrent/testing",
        "//src/main/java/com/google/android/libraries/feed/common/protoextensions",
        "//src/main/java/com/google/android/libraries/feed/feedstore/internal",
        "//src/main/java/com/google/android/libraries/feed/feedstore/testing",
        "//src/main/java/com/google/android/libraries/feed/host/storage",
        "//src/main/java/com/google/android/libraries/feed/hostimpl/storage",
        "//src/main/proto/com/google/android/libraries/feed/api/proto:client_feed_java_proto_lite",
        "//src/main/proto/search/now/wire/feed:feed_java_proto_lite",
        "//src/test/proto/search/now/wire/feed:feed_test_java_proto_lite",
        "@com_google_protobuf_javalite//:protobuf_java_lite",
        "@com_google_truth_truth//jar",
        "@org_mockito_mockito_all//jar",
        "@robolectric//bazel:robolectric",
    ],
)
