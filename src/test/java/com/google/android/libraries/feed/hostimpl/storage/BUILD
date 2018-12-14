load("@gmaven_rules//:defs.bzl", "gmaven_artifact")
load("//src/main/java/com/google/android/libraries/feed/buildtools/testing:android_local_test.bzl", "DEFAULT_ANDROID_LOCAL_TEST_MANIFEST")

licenses(["notice"])  # Apache 2

android_local_test(
    name = "InMemoryJournalStorageTest",
    size = "small",
    timeout = "moderate",
    srcs = ["InMemoryJournalStorageTest.java"],
    manifest_values = DEFAULT_ANDROID_LOCAL_TEST_MANIFEST,
    deps = [
        "@com_google_protobuf_javalite//:protobuf_java_lite",
        "//src/main/java/com/google/android/libraries/feed/api/common",
        "//src/main/java/com/google/android/libraries/feed/common",
        "//src/main/java/com/google/android/libraries/feed/common/functional",
        "//src/main/java/com/google/android/libraries/feed/host/storage",
        "//src/main/java/com/google/android/libraries/feed/hostimpl/storage",
        "//src/main/java/com/google/android/libraries/feed/testing/conformance/storage",
        "@bazel_android//:robolectric",
        "@org_mockito_mockito_all//jar",
    ],
)

android_local_test(
    name = "InMemoryJournalStorageDirectTest",
    size = "small",
    timeout = "moderate",
    srcs = ["InMemoryJournalStorageDirectTest.java"],
    manifest_values = DEFAULT_ANDROID_LOCAL_TEST_MANIFEST,
    deps = [
        "@com_google_protobuf_javalite//:protobuf_java_lite",
        "//src/main/java/com/google/android/libraries/feed/hostimpl/storage",
        "//src/main/java/com/google/android/libraries/feed/testing/conformance/storage",
        "@bazel_android//:robolectric",
    ],
)

android_local_test(
    name = "InMemoryContentStorageTest",
    size = "small",
    timeout = "moderate",
    srcs = ["InMemoryContentStorageTest.java"],
    manifest_values = DEFAULT_ANDROID_LOCAL_TEST_MANIFEST,
    deps = [
        "@com_google_protobuf_javalite//:protobuf_java_lite",
        "//src/main/java/com/google/android/libraries/feed/common",
        "//src/main/java/com/google/android/libraries/feed/common/concurrent",
        "//src/main/java/com/google/android/libraries/feed/common/functional",
        "//src/main/java/com/google/android/libraries/feed/common/testing",
        "//src/main/java/com/google/android/libraries/feed/feedstore",
        "//src/main/java/com/google/android/libraries/feed/host/storage",
        "//src/main/java/com/google/android/libraries/feed/hostimpl/storage",
        "//src/main/java/com/google/android/libraries/feed/testing/conformance/storage",
        "@bazel_android//:robolectric",
        "@com_google_code_findbugs_jsr305//jar",
        "@com_google_truth_truth//jar",
        "@org_mockito_mockito_all//jar",
    ],
)

android_local_test(
    name = "InMemoryContentStorageDirectTest",
    size = "small",
    timeout = "moderate",
    srcs = ["InMemoryContentStorageDirectTest.java"],
    manifest_values = DEFAULT_ANDROID_LOCAL_TEST_MANIFEST,
    deps = [
        "@com_google_protobuf_javalite//:protobuf_java_lite",
        "//src/main/java/com/google/android/libraries/feed/hostimpl/storage",
        "//src/main/java/com/google/android/libraries/feed/testing/conformance/storage",
        "@bazel_android//:robolectric",
    ],
)

android_local_test(
    name = "PersistentContentStorageTest",
    size = "small",
    timeout = "moderate",
    srcs = ["PersistentContentStorageTest.java"],
    manifest_values = DEFAULT_ANDROID_LOCAL_TEST_MANIFEST,
    deps = [
        "@com_google_protobuf_javalite//:protobuf_java_lite",
        "//src/main/java/com/google/android/libraries/feed/api/common",
        "//src/main/java/com/google/android/libraries/feed/common",
        "//src/main/java/com/google/android/libraries/feed/common/testing",
        "//src/main/java/com/google/android/libraries/feed/host/storage",
        "//src/main/java/com/google/android/libraries/feed/hostimpl/storage",
        "//src/main/java/com/google/android/libraries/feed/testing/conformance/storage",
        "@bazel_android//:robolectric",
        "@com_google_code_findbugs_jsr305//jar",
        "@com_google_guava_guava//jar",
        "@com_google_truth_truth//jar",
        "@org_mockito_mockito_all//jar",
    ],
)

android_local_test(
    name = "PersistentContentStorageDirectTest",
    size = "small",
    timeout = "moderate",
    srcs = ["PersistentContentStorageDirectTest.java"],
    manifest_values = DEFAULT_ANDROID_LOCAL_TEST_MANIFEST,
    deps = [
        "@com_google_protobuf_javalite//:protobuf_java_lite",
        "//src/main/java/com/google/android/libraries/feed/api/common",
        "//src/main/java/com/google/android/libraries/feed/hostimpl/storage",
        "//src/main/java/com/google/android/libraries/feed/testing/conformance/storage",
        "@bazel_android//:robolectric",
        "@com_google_guava_guava//jar",
        "@org_mockito_mockito_all//jar",
    ],
)

android_local_test(
    name = "PersistentJournalStorageTest",
    size = "small",
    timeout = "moderate",
    srcs = ["PersistentJournalStorageTest.java"],
    manifest_values = DEFAULT_ANDROID_LOCAL_TEST_MANIFEST,
    deps = [
        "@com_google_protobuf_javalite//:protobuf_java_lite",
        "//src/main/java/com/google/android/libraries/feed/api/common",
        "//src/main/java/com/google/android/libraries/feed/common",
        "//src/main/java/com/google/android/libraries/feed/common/testing",
        "//src/main/java/com/google/android/libraries/feed/host/storage",
        "//src/main/java/com/google/android/libraries/feed/hostimpl/storage",
        "//src/main/java/com/google/android/libraries/feed/testing/conformance/storage",
        "@bazel_android//:robolectric",
        "@com_google_code_findbugs_jsr305//jar",
        "@com_google_guava_guava//jar",
        "@com_google_truth_truth//jar",
        "@org_mockito_mockito_all//jar",
    ],
)

android_local_test(
    name = "PersistentJournalStorageDirectTest",
    size = "small",
    timeout = "moderate",
    srcs = ["PersistentJournalStorageDirectTest.java"],
    manifest_values = DEFAULT_ANDROID_LOCAL_TEST_MANIFEST,
    deps = [
        "@com_google_protobuf_javalite//:protobuf_java_lite",
        "//src/main/java/com/google/android/libraries/feed/api/common",
        "//src/main/java/com/google/android/libraries/feed/hostimpl/storage",
        "//src/main/java/com/google/android/libraries/feed/testing/conformance/storage",
        "@bazel_android//:robolectric",
        "@com_google_guava_guava//jar",
        "@com_google_truth_truth//jar",
        "@org_mockito_mockito_all//jar",
    ],
)
