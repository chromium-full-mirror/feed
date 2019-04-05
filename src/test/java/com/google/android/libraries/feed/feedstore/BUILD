load("//src/main/java/com/google/android/libraries/feed/buildtools/testing:android_local_test.bzl", "DEFAULT_ANDROID_LOCAL_TEST_MANIFEST")

licenses(["notice"])  # Apache 2

android_local_test(
    name = "FeedStoreEphemeralModeTest",
    size = "small",
    timeout = "moderate",
    srcs = ["FeedStoreEphemeralModeTest.java"],
    aapt_version = "aapt2",
    manifest_values = DEFAULT_ANDROID_LOCAL_TEST_MANIFEST,
    deps = [
        "//src/main/java/com/google/android/libraries/feed/api/store",
        "//src/main/java/com/google/android/libraries/feed/common/concurrent",
        "//src/main/java/com/google/android/libraries/feed/common/concurrent/testing",
        "//src/main/java/com/google/android/libraries/feed/common/protoextensions",
        "//src/main/java/com/google/android/libraries/feed/feedstore",
        "//src/main/java/com/google/android/libraries/feed/feedstore/testing",
        "//src/main/java/com/google/android/libraries/feed/host/config",
        "//src/main/java/com/google/android/libraries/feed/host/storage",
        "//src/main/java/com/google/android/libraries/feed/hostimpl/storage",
        "//src/main/java/com/google/android/libraries/feed/testing/host/logging",
        "@com_google_protobuf_javalite//:protobuf_java_lite",
        "@maven//:com_google_guava_guava",
        "@maven//:org_mockito_mockito_core",
        "@robolectric//bazel:robolectric",
    ],
)

android_local_test(
    name = "FeedStoreTest",
    size = "small",
    timeout = "moderate",
    srcs = ["FeedStoreTest.java"],
    aapt_version = "aapt2",
    manifest_values = DEFAULT_ANDROID_LOCAL_TEST_MANIFEST,
    deps = [
        "//src/main/java/com/google/android/libraries/feed/api/store",
        "//src/main/java/com/google/android/libraries/feed/common/concurrent",
        "//src/main/java/com/google/android/libraries/feed/common/concurrent/testing",
        "//src/main/java/com/google/android/libraries/feed/common/protoextensions",
        "//src/main/java/com/google/android/libraries/feed/feedapplifecyclelistener",
        "//src/main/java/com/google/android/libraries/feed/feedstore",
        "//src/main/java/com/google/android/libraries/feed/feedstore/internal",
        "//src/main/java/com/google/android/libraries/feed/feedstore/testing",
        "//src/main/java/com/google/android/libraries/feed/host/config",
        "//src/main/java/com/google/android/libraries/feed/host/logging",
        "//src/main/java/com/google/android/libraries/feed/host/storage",
        "//src/main/java/com/google/android/libraries/feed/hostimpl/storage",
        "//src/main/proto/com/google/android/libraries/feed/api/proto:client_feed_java_proto_lite",
        "@com_google_protobuf_javalite//:protobuf_java_lite",
        "@maven//:com_google_guava_guava",
        "@maven//:com_google_truth_truth",
        "@maven//:org_mockito_mockito_core",
        "@robolectric//bazel:robolectric",
    ],
)
