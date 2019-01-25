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
    sha256 = "cef7f1b5a7c5fba672bec2a319246e8feba471f04dcebfe362d55930ee7c1c30",
    strip_prefix = "protobuf-3.5.0",
    urls = ["https://github.com/google/protobuf/archive/v3.5.0.zip"],
)

http_archive(
    name = "com_google_protobuf_javalite",
    sha256 = "d8a2fed3708781196f92e1e7e7e713cf66804bd2944894401057214aff4f468e",
    strip_prefix = "protobuf-5e8916e881c573c5d83980197a6f783c132d4276",
    urls = ["https://github.com/google/protobuf/archive/5e8916e881c573c5d83980197a6f783c132d4276.zip"],
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
