load("//src/main/java/com/google/android/libraries/feed/buildtools/testing:android_local_test.bzl", "DEFAULT_ANDROID_LOCAL_TEST_MANIFEST")

licenses(["notice"])  # Apache 2

android_local_test(
    name = "HashPoolInternerTest",
    size = "small",
    timeout = "moderate",
    srcs = ["HashPoolInternerTest.java"],
    aapt_version = "aapt2",
    manifest_values = DEFAULT_ANDROID_LOCAL_TEST_MANIFEST,
    deps = [
        "//src/main/java/com/google/android/libraries/feed/common/intern",
        "//src/main/proto/search/now/ui/piet:piet_java_proto_lite",
        "@com_google_protobuf_javalite//:protobuf_java_lite",
        "@maven//:com_google_truth_truth",
        "@robolectric//bazel:robolectric",
    ],
)

android_local_test(
    name = "InternedMapTest",
    size = "small",
    timeout = "moderate",
    srcs = ["InternedMapTest.java"],
    aapt_version = "aapt2",
    manifest_values = DEFAULT_ANDROID_LOCAL_TEST_MANIFEST,
    deps = [
        "//src/main/java/com/google/android/libraries/feed/common/intern",
        "//src/main/proto/com/google/android/libraries/feed/api/proto:client_feed_java_proto_lite",
        "@com_google_protobuf_javalite//:protobuf_java_lite",
        "@maven//:com_google_truth_truth",
        "@robolectric//bazel:robolectric",
    ],
)

android_local_test(
    name = "InternerWithStatsTest",
    size = "small",
    timeout = "moderate",
    srcs = ["InternerWithStatsTest.java"],
    aapt_version = "aapt2",
    manifest_values = DEFAULT_ANDROID_LOCAL_TEST_MANIFEST,
    deps = [
        "//src/main/java/com/google/android/libraries/feed/common/intern",
        "//src/main/proto/com/google/android/libraries/feed/api/proto:client_feed_java_proto_lite",
        "@com_google_protobuf_javalite//:protobuf_java_lite",
        "@maven//:com_google_truth_truth",
        "@robolectric//bazel:robolectric",
    ],
)

android_local_test(
    name = "WeakPoolInternerTest",
    size = "small",
    timeout = "moderate",
    srcs = ["WeakPoolInternerTest.java"],
    aapt_version = "aapt2",
    manifest_values = DEFAULT_ANDROID_LOCAL_TEST_MANIFEST,
    deps = [
        "//src/main/java/com/google/android/libraries/feed/common/intern",
        "//src/main/proto/com/google/android/libraries/feed/api/proto:client_feed_java_proto_lite",
        "@com_google_protobuf_javalite//:protobuf_java_lite",
        "@maven//:com_google_truth_truth",
        "@robolectric//bazel:robolectric",
    ],
)
