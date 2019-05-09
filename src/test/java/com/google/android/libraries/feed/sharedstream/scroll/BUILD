load("//src/main/java/com/google/android/libraries/feed/buildtools/testing:android_local_test.bzl", "DEFAULT_ANDROID_LOCAL_TEST_MANIFEST")

licenses(["notice"])  # Apache 2

android_local_test(
    name = "ScrollListenerNotifierTest",
    size = "small",
    timeout = "moderate",
    srcs = ["ScrollListenerNotifierTest.java"],
    aapt_version = "aapt2",
    manifest_values = DEFAULT_ANDROID_LOCAL_TEST_MANIFEST,
    deps = [
        "//src/main/java/com/google/android/libraries/feed/api/stream",
        "//src/main/java/com/google/android/libraries/feed/common/concurrent/testing",
        "//src/main/java/com/google/android/libraries/feed/common/testing",
        "//src/main/java/com/google/android/libraries/feed/common/time/testing",
        "//src/main/java/com/google/android/libraries/feed/sharedstream/publicapi/scroll",
        "//src/main/java/com/google/android/libraries/feed/sharedstream/scroll",
        "//third_party:robolectric",
        "@com_google_protobuf_javalite//:protobuf_java_lite",
        "@maven//:com_android_support_recyclerview_v7",
        "@maven//:org_mockito_mockito_core",
        "@robolectric//bazel:android-all",
    ],
)

android_local_test(
    name = "ScrollLoggerTest",
    size = "small",
    timeout = "moderate",
    srcs = ["ScrollLoggerTest.java"],
    aapt_version = "aapt2",
    manifest_values = DEFAULT_ANDROID_LOCAL_TEST_MANIFEST,
    deps = [
        "//src/main/java/com/google/android/libraries/feed/api/host/logging",
        "//src/main/java/com/google/android/libraries/feed/sharedstream/scroll",
        "//third_party:robolectric",
        "@com_google_protobuf_javalite//:protobuf_java_lite",
        "@maven//:org_mockito_mockito_core",
        "@robolectric//bazel:android-all",
    ],
)

android_local_test(
    name = "ScrollRestoreHelperTest",
    size = "small",
    timeout = "moderate",
    srcs = ["ScrollRestoreHelperTest.java"],
    aapt_version = "aapt2",
    manifest_values = DEFAULT_ANDROID_LOCAL_TEST_MANIFEST,
    deps = [
        "//src/main/java/com/google/android/libraries/feed/api/host/config",
        "//src/main/java/com/google/android/libraries/feed/sharedstream/scroll",
        "//src/main/java/com/google/android/libraries/feed/testing/android",
        "//src/main/proto/com/google/android/libraries/feed/sharedstream/proto:scroll_state_java_proto_lite",
        "//third_party:robolectric",
        "@com_google_protobuf_javalite//:protobuf_java_lite",
        "@maven//:com_android_support_recyclerview_v7",
        "@maven//:com_google_truth_truth",
        "@robolectric//bazel:android-all",
    ],
)

android_local_test(
    name = "ScrollTrackerTest",
    size = "small",
    timeout = "moderate",
    srcs = ["ScrollTrackerTest.java"],
    aapt_version = "aapt2",
    manifest_values = DEFAULT_ANDROID_LOCAL_TEST_MANIFEST,
    deps = [
        "//src/main/java/com/google/android/libraries/feed/common/concurrent/testing",
        "//src/main/java/com/google/android/libraries/feed/common/time/testing",
        "//src/main/java/com/google/android/libraries/feed/sharedstream/scroll",
        "//third_party:robolectric",
        "@com_google_protobuf_javalite//:protobuf_java_lite",
        "@maven//:com_google_truth_truth",
        "@maven//:org_mockito_mockito_core",
        "@robolectric//bazel:android-all",
    ],
)
