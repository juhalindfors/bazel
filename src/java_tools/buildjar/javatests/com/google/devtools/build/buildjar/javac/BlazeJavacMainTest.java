// Copyright 2017 The Bazel Authors. All rights reserved.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//    http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package com.google.devtools.build.buildjar.javac;

import static java.nio.charset.StandardCharsets.UTF_16;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import com.google.common.collect.ImmutableList;
import com.sun.tools.javac.file.JavacFileManager;
import com.sun.tools.javac.main.Option;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.nio.charset.CharsetDecoder;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;


/**
 * Unit testing {@link BlazeJavacMain} implementation.
 *
 * @author <a href = "http://www.github.com/juhalindfors">Juha Lindfors</a>
 */
@RunWith(JUnit4.class)
public class BlazeJavacMainTest {

  /* Copy & pasted from BazelJavaCompilerTest.
     TODO(juhalindfors/bazel-team): Share test code utility functions. */
  private static Path getTmpDir() {
    String tmpdir = System.getenv("TEST_TMPDIR");

    if (tmpdir == null) {
      // Fall back on the system temporary directory
      tmpdir = System.getProperty("java.io.tmpdir");
    }

    if (tmpdir == null) {
      fail("TEST_TMPDIR environment variable is not set!");
    }

    return new File(tmpdir).toPath();
  }


  /**
   * Test to ensure the default UTF-8 source encoding is used and that it can be overridden
   * with '-encoding' javac argument. <p>
   *
   * Test uses a temporary source files with UTF-8 and UTF-16 encodings and attempts to compile
   * using default source encoding (no explicit '-encoding' option present, should fail due
   * to UTF-8 default) and then compiles again by setting the correct UTF-16 encoding option
   * (should succeed).
   *
   * @throws Exception if test fails
   */
  @Test
  public void testDefaultEncoding() throws Exception {

    // Create temporary Java source files with different encodings...
    File fileUTF8 = File.createTempFile("bzltest", ".java");
    File fileUTF16 = File.createTempFile("bzltest", ".java");
    String source = "class Test { }";

    try (BufferedOutputStream bout8 = new BufferedOutputStream(new FileOutputStream(fileUTF8));
        BufferedOutputStream bout16 = new BufferedOutputStream(new FileOutputStream(fileUTF16))) {

      bout8.write(source.getBytes(UTF_8));
      bout16.write(source.getBytes(UTF_16));
    }

    Path utf8SourcePath = Paths.get(fileUTF8.toURI());
    Path utf16SourcePath = Paths.get(fileUTF16.toURI());

    // Run UTF-8 source through the compiler without explicit '-encoding' setting -- the decoder
    // should use the default UTF-8 charset...
    BlazeJavacResult result = BlazeJavacMain.compile(createTestArgs(utf8SourcePath));
    assertTrue(result.isOk());

    JavacFileManager mgr = (JavacFileManager) result.compiler().filemanager();
    CharsetDecoder decoder = mgr.getDecoder(null /* should fall back to default charset */, true);
    assertTrue(decoder.charset().equals(UTF_8));

    // Run UTF-16 source through the compiler without explicit '-encoding' setting -- the decoder
    // should use the default UTF-8 charset causing a failure...
    result = BlazeJavacMain.compile(createTestArgs(utf16SourcePath));
    assertFalse(result.isOk());

    mgr = (JavacFileManager) result.compiler().filemanager();
    decoder = mgr.getDecoder(UTF_16.name() /* ignored and falls back to default charset */, true);
    assertTrue(decoder.charset().equals(UTF_8));

    // Run UTF-16 source through the compiler *with* explicit '-encoding' setting -- the decoder
    // should use UTF-16 charset and succeed...
    result = BlazeJavacMain.compile(createTestArgs(
        utf16SourcePath, Option.ENCODING.getPrimaryName(), UTF_16.name()));
    assertTrue(result.isOk());

    mgr = (JavacFileManager) result.compiler().filemanager();
    assertTrue(mgr.getEncodingName().equals(UTF_16.name()));

    decoder = mgr.getDecoder(UTF_16.name(), true);
    assertTrue(decoder.charset().equals(UTF_16));
  }

  /* Helper to create Bazel javac argument instances for testing... */
  private BlazeJavacArguments createTestArgs(final Path sourceFile, final String... args) {

    return BlazeJavacArguments
        .builder()
        .sourceFiles(ImmutableList.of(sourceFile))
        .javacOptions(ImmutableList.copyOf(args))
        .classOutput(getTmpDir())
        .build();
  }

}
