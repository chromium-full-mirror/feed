android_sdk_repository(
    name = "androidsdk"
)

load("@bazel_tools//tools/build_defs/repo:http.bzl", "http_archive")

# Google Maven Repository
RULES_JVM_EXTERNAL_TAG = "3.0"
RULES_JVM_EXTERNAL_SHA = "62133c125bf4109dfd9d2af64830208356ce4ef8b165a6ef15bbff7460b35c3a"

# Current version of Android support library which Android code depends on
ANDROID_SUPPORT_LIBRARY_VERSION = "28.0.0"

http_archive(
    name = "rules_jvm_external",
    strip_prefix = "rules_jvm_external-%s" % RULES_JVM_EXTERNAL_TAG,
    sha256 = RULES_JVM_EXTERNAL_SHA,
    url = "https://github.com/bazelbuild/rules_jvm_external/archive/%s.zip" % RULES_JVM_EXTERNAL_TAG,
)

load("@rules_jvm_external//:defs.bzl", "maven_install")
load("//third_party:dependencies.bzl", "get_robolectric_dependency")

maven_install(
    artifacts = [
        "androidx.test:core:1.1.0",
        "androidx.test:monitor:1.1.0",
        "com.android.support:appcompat-v7:" + ANDROID_SUPPORT_LIBRARY_VERSION,
        "com.android.support:collections:" + ANDROID_SUPPORT_LIBRARY_VERSION,
        "com.android.support:interpolator:" + ANDROID_SUPPORT_LIBRARY_VERSION,
        "com.android.support:recyclerview-v7:" + ANDROID_SUPPORT_LIBRARY_VERSION,
        "com.android.support:support-annotations:" + ANDROID_SUPPORT_LIBRARY_VERSION,
        "com.android.support:support-compat:" + ANDROID_SUPPORT_LIBRARY_VERSION,
        "com.android.support:support-core-ui:" + ANDROID_SUPPORT_LIBRARY_VERSION,
        "com.android.support:swiperefreshlayout:" + ANDROID_SUPPORT_LIBRARY_VERSION,
        "com.google.android:android:4.1.1.4",
        "com.google.code.findbugs:jsr305:3.0.2",
        "com.google.dagger:dagger:2.17",
        "com.google.dagger:dagger-compiler:2.17",
        "com.google.dagger:dagger-producers:2.17",
        "com.google.guava:guava:27.0.1-android",
        "com.google.guava:guava-testlib:27.0.1-android",
        "com.google.inject:guice:4.2.2",
        "com.google.testing.compile:compile-testing:0.15",
        "com.google.truth:truth:0.45",
        "com.google.truth.extensions:truth-liteproto-extension:0.45",
        "com.squareup:javapoet:1.8.0",
        "javax.annotation:jsr250-api:1.0",
        "javax.inject:javax.inject:1",
        "junit:junit:4.12",
        "org.mockito:mockito-core:2.24.0",
        get_robolectric_dependency("annotations"),
        get_robolectric_dependency("robolectric"),
        get_robolectric_dependency("shadowapi"),
        get_robolectric_dependency("shadows-framework"),
    ],
    repositories = [
        "https://repo1.maven.org/maven2/",
        "https://maven.google.com",
    ],
)

# Loading android_library rules for Bazel scripts.
http_archive(
    name = "build_bazel_rules_android",
    urls = ["https://github.com/bazelbuild/rules_android/archive/v0.1.1.zip"],
    sha256 = "cd06d15dd8bb59926e4d65f9003bfc20f9da4b2519985c27e190cddc8b7a7806",
    strip_prefix = "rules_android-0.1.1",
)

http_archive(
    name = "com_google_protobuf_javalite",
    sha256 = "e60211a40473f6be95b53f64559f82a3b2971672b11710db2fc9081708e25699",
    strip_prefix = "protobuf-0425fa932ce95a32bb9f88b2c09b995e9ff8207b",
    urls = ["https://github.com/google/protobuf/archive/0425fa932ce95a32bb9f88b2c09b995e9ff8207b.zip"],
)

# rules_cc defines rules for generating C++ code from Protocol Buffers.
http_archive(
    name = "rules_cc",
    sha256 = "35f2fb4ea0b3e61ad64a369de284e4fbbdcdba71836a5555abb5e194cf119509",
    strip_prefix = "rules_cc-624b5d59dfb45672d4239422fa1e3de1822ee110",
    urls = [
        "https://mirror.bazel.build/github.com/bazelbuild/rules_cc/archive/624b5d59dfb45672d4239422fa1e3de1822ee110.tar.gz",
        "https://github.com/bazelbuild/rules_cc/archive/624b5d59dfb45672d4239422fa1e3de1822ee110.tar.gz",
    ],
)

# rules_java defines rules for generating Java code from Protocol Buffers.
http_archive(
    name = "rules_java",
    sha256 = "ccf00372878d141f7d5568cedc4c42ad4811ba367ea3e26bc7c43445bbc52895",
    strip_prefix = "rules_java-d7bf804c8731edd232cb061cb2a9fe003a85d8ee",
    urls = [
        "https://mirror.bazel.build/github.com/bazelbuild/rules_java/archive/d7bf804c8731edd232cb061cb2a9fe003a85d8ee.tar.gz",
        "https://github.com/bazelbuild/rules_java/archive/d7bf804c8731edd232cb061cb2a9fe003a85d8ee.tar.gz",
    ],
)

# rules_proto defines abstract rules for building Protocol Buffers.
http_archive(
    name = "rules_proto",
    sha256 = "2490dca4f249b8a9a3ab07bd1ba6eca085aaf8e45a734af92aad0c42d9dc7aaf",
    strip_prefix = "rules_proto-218ffa7dfa5408492dc86c01ee637614f8695c45",
    urls = [
        "https://mirror.bazel.build/github.com/bazelbuild/rules_proto/archive/218ffa7dfa5408492dc86c01ee637614f8695c45.tar.gz",
        "https://github.com/bazelbuild/rules_proto/archive/218ffa7dfa5408492dc86c01ee637614f8695c45.tar.gz",
    ],
)

http_archive(
    name = "robolectric",
    urls = ["https://github.com/robolectric/robolectric-bazel/archive/4.1.tar.gz"],
    sha256 = "2ee850ca521288db72b0dedb9ecbda55b64d11c470435a882f8daf615091253d",
    strip_prefix = "robolectric-bazel-4.1",
)

load("@rules_cc//cc:repositories.bzl", "rules_cc_dependencies")
rules_cc_dependencies()

load("@rules_java//java:repositories.bzl", "rules_java_dependencies", "rules_java_toolchains")
rules_java_dependencies()
rules_java_toolchains()

load("@rules_proto//proto:repositories.bzl", "rules_proto_dependencies", "rules_proto_toolchains")
rules_proto_dependencies()
rules_proto_toolchains()

load("@robolectric//bazel:robolectric.bzl", "robolectric_repositories")
robolectric_repositories()
