load("//src/main/java/com/google/android/libraries/feed/buildtools/testing:android_local_test.bzl", "DEFAULT_ANDROID_LOCAL_TEST_MANIFEST")

licenses(["notice"])  # Apache 2

android_local_test(
    name = "StreamActionApiImplTest",
    size = "small",
    timeout = "moderate",
    srcs = ["StreamActionApiImplTest.java"],
    aapt_version = "aapt2",
    manifest_values = DEFAULT_ANDROID_LOCAL_TEST_MANIFEST,
    deps = [
        "//src/main/java/com/google/android/libraries/feed/api/actionparser",
        "//src/main/java/com/google/android/libraries/feed/api/knowncontent",
        "//src/main/java/com/google/android/libraries/feed/basicstream/internal/actions",
        "//src/main/java/com/google/android/libraries/feed/basicstream/internal/pendingdismiss",
        "//src/main/java/com/google/android/libraries/feed/common/functional",
        "//src/main/java/com/google/android/libraries/feed/host/action",
        "//src/main/java/com/google/android/libraries/feed/host/logging",
        "//src/main/java/com/google/android/libraries/feed/host/stream",
        "//src/main/java/com/google/android/libraries/feed/internalapi/actionmanager",
        "//src/main/java/com/google/android/libraries/feed/sharedstream/contextmenumanager",
        "//src/main/java/com/google/android/libraries/feed/sharedstream/pendingdismiss",
        "//src/main/java/com/google/android/libraries/feed/sharedstream/publicapi/menumeasurer",
        "//src/main/java/com/google/android/libraries/feed/testing/sharedstream/contextmenumanager",
        "//src/main/proto/com/google/android/libraries/feed/api/proto:client_feed_java_proto_lite",
        "//src/main/proto/search/now/ui/action:feed_action_java_proto_lite",
        "//src/main/proto/search/now/ui/action:feed_action_payload_java_proto_lite",
        "//src/main/proto/search/now/wire/feed:feed_java_proto_lite",
        "@com_google_protobuf_javalite//:protobuf_java_lite",
        "@maven//:com_google_guava_guava",
        "@maven//:com_google_truth_truth",
        "@maven//:org_mockito_mockito_core",
        "@robolectric//bazel:robolectric",
    ],
)
