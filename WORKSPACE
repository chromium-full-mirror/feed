android_sdk_repository(
    name = "androidsdk",
    api_level = 28,
)


load("@bazel_tools//tools/build_defs/repo:http.bzl", "http_archive")

# Current version of Android support library which Android code depends on
ANDROID_SUPPORT_LIBRARY_VERSION = "28.0.0"

http_archive(
    name = "rules_jvm_external",
    strip_prefix = "rules_jvm_external-3.1",
    url = "https://github.com/bazelbuild/rules_jvm_external/archive/3.1.zip",
)

load("@rules_jvm_external//:defs.bzl", "maven_install")
load("//third_party:dependencies.bzl", "get_robolectric_dependency")

maven_install(
    artifacts = [
        "androidx.test:core:1.1.0",
        "androidx.test:monitor:1.1.0",
        "androidx.annotation:annotation:1.1.0",
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
    sha256 = "d8992e6eeec276d49f1d4e63cfa05bbed6d4a26cfe6ca63c972827a0d141ea3b",
    strip_prefix = "rules_proto-cfdc2fa31879c0aebe31ce7702b1a9c8a4be02d2",
    urls = [
        "https://mirror.bazel.build/github.com/bazelbuild/rules_proto/archive/cfdc2fa31879c0aebe31ce7702b1a9c8a4be02d2.tar.gz",
        "https://github.com/bazelbuild/rules_proto/archive/cfdc2fa31879c0aebe31ce7702b1a9c8a4be02d2.tar.gz",
    ],
)

http_archive(
    name = "com_google_protobuf",
    strip_prefix = "protobuf-3.13.0",
    urls = ["https://github.com/protocolbuffers/protobuf/archive/v3.13.0.tar.gz"],
)

load("@com_google_protobuf//:protobuf_deps.bzl", "protobuf_deps")
protobuf_deps()

http_archive(
    name = "robolectric",
    urls = ["https://github.com/robolectric/robolectric-bazel/archive/4.4.tar.gz"],
    strip_prefix = "robolectric-bazel-4.4",
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
