android_sdk_repository(
    name = "androidsdk"
)

load("@bazel_tools//tools/build_defs/repo:http.bzl", "http_archive")

# Google Maven Repository
GMAVEN_TAG = "20181206-1"

http_archive(
    name = "gmaven_rules",
    strip_prefix = "gmaven_rules-%s" % GMAVEN_TAG,
    url = "https://github.com/bazelbuild/gmaven_rules/archive/%s.tar.gz" % GMAVEN_TAG,
)

load("@gmaven_rules//:gmaven.bzl", "gmaven_rules")
gmaven_rules()

http_archive(
    name = "com_google_protobuf",
    sha256 = "73fdad358857e120fd0fa19e071a96e15c0f23bb25f85d3f7009abfd4f264a2a",
    strip_prefix = "protobuf-3.6.1.3",
    urls = ["https://github.com/protocolbuffers/protobuf/archive/v3.6.1.3.tar.gz"],
)

http_archive(
    name = "com_google_protobuf_javalite",
    sha256 = "79d102c61e2a479a0b7e5fc167bcfaa4832a0c6aad4a75fa7da0480564931bcc",
    strip_prefix = "protobuf-384989534b2246d413dbcd750744faab2607b516",
    urls = ["https://github.com/google/protobuf/archive/384989534b2246d413dbcd750744faab2607b516.zip"],
)

http_archive(
    name = "robolectric",
    urls = ["https://github.com/robolectric/robolectric-bazel/archive/7020b9a7a4796d2dc5392d6385d83e384a01f9fd.tar.gz"],
    strip_prefix = "robolectric-bazel-7020b9a7a4796d2dc5392d6385d83e384a01f9fd",
)

load("@robolectric//bazel:robolectric.bzl", "robolectric_repositories")
robolectric_repositories()

maven_jar(
    name = "com_google_android_android",
    artifact = "com.google.android:android:4.1.1.4",
)

maven_jar(
    name = "com_google_code_findbugs_jsr305",
    artifact = "com.google.code.findbugs:jsr305:3.0.2",
)

maven_jar(
    name = "com_google_dagger_dagger",
    artifact = "com.google.dagger:dagger:2.0",
)

maven_jar(
    name = "com_google_guava_guava_android",
    artifact = "com.google.guava:guava:27.0.1-android",
)

maven_jar(
    name = "com_google_guava_guava_testlib",
    artifact = "com.google.guava:guava-testlib:27.0.1-android",
)

maven_jar(
    name = "com_google_inject_guice",
    artifact = "com.google.inject:guice:4.2.2",
)

maven_jar(
    name = "com_google_testing_compile_compile_testing",
    artifact = "com.google.testing.compile:compile-testing:4.2.2",
)

maven_jar(
    name = "com_google_truth_truth",
    artifact = "com.google.truth:truth:0.39",
)

maven_jar(
    name = "com_google_truth_liteproto_extension",
    artifact = "com.google.truth.extensions:truth-liteproto-extension:0.39"
)

maven_jar(
    name = "org_mockito_mockito_all",
    artifact = "org.mockito:mockito-all:1.9.5",
)

maven_jar(
    name = "com_squareup_javapoet",
    artifact = "com.squareup:javapoet:1.8.0",
)

maven_jar(
    name = "javax_annotation_jsr250_api",
    artifact = "javax.annotation:jsr250-api:1.0",
)

maven_jar(
    name = "javax_inject_javax_inject",
    artifact = "javax.inject:javax.inject:1",
)
