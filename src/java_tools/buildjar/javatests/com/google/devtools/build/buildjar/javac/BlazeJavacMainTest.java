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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import com.google.common.collect.ImmutableList;
import com.google.devtools.build.buildjar.javac.plugins.BlazeJavaCompilerPlugin;
import com.sun.tools.javac.main.Option;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Locale;
import javax.annotation.processing.Processor;
import javax.tools.Diagnostic;
import org.junit.Test;


/**
 * Unit testing {@link BlazeJavacMain} implementation.
 *
 * @author <a href = "http://www.github.com/juhalindfors">Juha Lindfors</a>
 */
public class BlazeJavacMainTest {

  /* Copy & pasted from BazelJavaCompilerTest.
     There should be a place to share test code utility functions? */
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
   * Tests passing -encoding argument from Bazel javac wrapper into JDK JavacTool implementation.
   * This test should prevent regression on issue #2926 where encoding argument was ignored by
   * the file manager implementation. <p>
   *
   * Test uses a temporary source file with invalid UTF-8 encoding (the source is valid ISO-8859-1
   * byte encoding though) and checks the compiler result on incorrect and correct encoding setting.
   *
   * @throws Exception if test fails
   */
  @Test
  public void testNonUTF8EncodedCompile() throws Exception {

    final int INVALID_UTF8_BYTEVALUE = 0xF6;                // maps to character 'ö' in ISO-8859-1

    // create temp Java source file...

    File file = File.createTempFile("bzltest", ".java");
    BufferedOutputStream bout = new BufferedOutputStream(new FileOutputStream(file));
    bout.write(createInvalidUTF8Encoding(INVALID_UTF8_BYTEVALUE));
    bout.close();

    // run source through the compiler with UTF-8 encoding -- should result in an error...

    String decoderCharsetName = StandardCharsets.UTF_8.name();

    BlazeJavacResult result = BlazeJavacMain.compile(
        createTestArgs(Paths.get(file.toURI()), Option.ENCODING.getPrimaryName(), decoderCharsetName));

    assertFalse(result.isOk());

    // Our source is very simple so assume first diagnostic contains the error...

    FormattedDiagnostic firstDiagnostic = result.diagnostics().get(0);
    String formattedMessage = firstDiagnostic.getFormatted()
        .toLowerCase(Locale.ENGLISH);
    String expectedMsgContent = ("error: unmappable character (0x" +
        Integer.toHexString(INVALID_UTF8_BYTEVALUE) +
        ") for encoding " + decoderCharsetName)
        .toLowerCase(Locale.ENGLISH);

    assertTrue(
        "Received: " + firstDiagnostic,
        firstDiagnostic.getKind() == Diagnostic.Kind.ERROR);

    // testing on the error message produced by tool -- brittle (and will it be localized?)...
    assertTrue(
        "Received: " + formattedMessage,
        formattedMessage.contains(expectedMsgContent));

    // Compile again, this time with the correct character encoding...

    result = BlazeJavacMain.compile(createTestArgs(
        Paths.get(file.toURI()), Option.ENCODING.getPrimaryName(), StandardCharsets.ISO_8859_1.name()));

    assertTrue(result.isOk());
  }


  /* Helper to create Bazel javac argument instances for testing... */
  private BlazeJavacArguments createTestArgs(final Path sourceFile, final String... args) {

    return new BlazeJavacArguments() {
      @Override
      public ImmutableList<Path> sourceFiles() {
        return ImmutableList.of(sourceFile);
      }

      @Override
      public ImmutableList<String> javacOptions() {
        return ImmutableList.copyOf(args);
      }

      @Override
      public ImmutableList<Path> classPath() {
        return ImmutableList.of();
      }

      @Override
      public ImmutableList<Path> bootClassPath() {
        return ImmutableList.of();
      }

      @Override
      public ImmutableList<Path> sourcePath() {
        return ImmutableList.of();
      }

      @Override
      public ImmutableList<Path> processorPath() {
        return ImmutableList.of();
      }

      @Override
      public ImmutableList<BlazeJavaCompilerPlugin> plugins() {
        return ImmutableList.of();
      }

      @Override
      public ImmutableList<Processor> processors() {
        return null;
      }

      @Override
      public Path classOutput() {
        return getTmpDir();
      }

      @Override
      public Path sourceOutput() {
        return null;
      }
    };
  }

  /* Creates a byte representation of a test Java source file... */
  private byte[] createInvalidUTF8Encoding(int... invalidBytes) {

    String sourceStart = "class Test { /* ";
    String sourceEnd = " */ }";

    byte[] textBytes = new byte[sourceStart.length() + sourceEnd.length() + 1];
    System.arraycopy(sourceStart.getBytes(), 0, textBytes, 0, sourceStart.length());

    for (int index = 0; index < invalidBytes.length; ++index) {
      textBytes[sourceStart.length() + index] = (byte) invalidBytes[index];
    }

    System.arraycopy(sourceEnd.getBytes(), 0, textBytes,
        sourceStart.length() + invalidBytes.length, sourceEnd.length());

    return textBytes;
  }

}
