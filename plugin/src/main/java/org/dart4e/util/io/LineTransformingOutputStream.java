/*
 * SPDX-FileCopyrightText: © The Dart4E authors
 * SPDX-FileContributor: Sebastian Thomschke
 * SPDX-License-Identifier: EPL-2.0
 * SPDX-ArtifactOfProjectHomePage: https://github.com/dart4e/dart4e
 */
package org.dart4e.util.io;

import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.function.Function;

/**
 * @author Sebastian Thomschke
 */
public class LineTransformingOutputStream extends FilterOutputStream {

   private final Function<String, String> lineTransformer;
   private final StringBuilder lineBuffer = new StringBuilder();

   public LineTransformingOutputStream(final OutputStream out, final Function<String, String> lineTransformer) {
      super(out);
      this.lineTransformer = lineTransformer;
   }

   @Override
   public void write(final int b) throws IOException {
      if (b == '\n') {
         lineBuffer.append('\n');
         out.write(lineTransformer.apply(lineBuffer.toString()).getBytes(StandardCharsets.UTF_8));
         lineBuffer.setLength(0);
      } else {
         lineBuffer.append((char) b);
      }
   }

   @Override
   public void flush() throws IOException {
      if (lineBuffer.length() > 0) {
         out.write(lineTransformer.apply(lineBuffer.toString()).getBytes(StandardCharsets.UTF_8));
         lineBuffer.setLength(0);
      }
      super.flush();
   }
}
