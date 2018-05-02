package(default_visibility = ["//visibility:public"])

licenses(["notice"])  # Apache 2

java_library(
    name = "fakemodelchild",
    testonly = True,
    srcs = ["FakeModelChild.java"],
    deps = [
        ":fakemodelfeature",
        ":fakemodeltoken",
        "//src/main/java/com/google/android/libraries/feed/api/modelprovider",
        "@com_google_code_findbugs_jsr305//jar",
    ],
)

java_library(
    name = "fakemodelcursor",
    testonly = True,
    srcs = ["FakeModelCursor.java"],
    deps = [
        "//src/main/java/com/google/android/libraries/feed/api/modelprovider",
        "@com_google_code_findbugs_jsr305//jar",
    ],
)

java_library(
    name = "fakemodelfeature",
    testonly = True,
    srcs = ["FakeModelFeature.java"],
    deps = [
        ":fakemodelcursor",
        "//src/main/java/com/google/android/libraries/feed/api/modelprovider",
        "//src/main/proto/com/google/android/libraries/feed/api/proto:client_feed_java_proto_lite",
        "@com_google_code_findbugs_jsr305//jar",
    ],
)

java_library(
    name = "fakemodeltoken",
    testonly = True,
    srcs = ["FakeModelToken.java"],
    deps = [
        "//src/main/java/com/google/android/libraries/feed/api/modelprovider",
        "//src/main/proto/com/google/android/libraries/feed/api/proto:client_feed_java_proto_lite",
    ],
)
