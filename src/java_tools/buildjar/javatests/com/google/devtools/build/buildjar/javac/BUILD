"""
Description:
  Unit tests for BlazeJavacMain implementation.

  Author: github.com/juhalindfors
"""

# Collects the sources of this test subpackage and makes them visible to the parent buildjar test
# package which collects all test sources and propagates them up to the main workspace BUILD
# definition.
filegroup(
    name = "srcs",
    srcs = glob(["**"]),
    visibility = ["//src/java_tools/buildjar/javatests/com/google/devtools/build/buildjar:__pkg__"],
)

java_test(
    name = "BlazeJavacMainTest",
    size = "small",
    srcs = ["BlazeJavacMainTest.java"],
    data = [
        "//third_party/java/jdk/langtools:javac_jar",
    ],
    jvm_flags = [
        # Simulates how Bazel invokes JavaBuilder
        "-Xbootclasspath/p:$(location //third_party/java/jdk/langtools:javac_jar)",
    ],
    test_class = "com.google.devtools.build.buildjar.javac.BlazeJavacMainTest",
    deps = [
        "//src/java_tools/buildjar/java/com/google/devtools/build/buildjar:javac",
        "//src/java_tools/buildjar/java/com/google/devtools/build/buildjar/javac/plugins",
        "//third_party:guava",
        "//third_party:junit4",
        "//third_party/java/jdk/langtools:javac",
    ],
)
