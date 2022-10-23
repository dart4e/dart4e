/*
 * Copyright 2022 by the Dart4E authors.
 * SPDX-License-Identifier: EPL-2.0
 */
package org.dart4e.util.io;

import static org.apache.commons.io.IOUtils.*;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.apache.commons.io.input.ProxyInputStream;

/**
 * @author Sebastian Thomschke
 */
public final class LinePrefixingTeeInputStream extends ProxyInputStream {

   private final OutputStream branch;
   private byte[] line = new byte[32 * 1024];
   private int lineLength;
   private int prefixLength;

   public LinePrefixingTeeInputStream(final InputStream input, final OutputStream branch, final String prefix) {
      super(input);
      this.branch = branch;
      prefixLength = prefix.length();
      lineLength = prefixLength;
      System.arraycopy(prefix.getBytes(), 0, line, 0, prefixLength);
   }

   private void onByteRead(final int byteRead) throws IOException {
      switch (byteRead) {
         case EOF:
         case '\n':
            if (lineLength > prefixLength) {
               branch.write(line, 0, lineLength);
               branch.write('\n');
               lineLength = prefixLength;
               branch.flush();
            }
            return;
         case '\r':
            return;
         default:
            line[lineLength] = (byte) byteRead;
            lineLength++;
      }
   }

   @Override
   public int read() throws IOException {
      final var byteRead = super.read();
      onByteRead(byteRead);
      return byteRead;
   }

   @Override
   public int read(final byte[] buffer, final int off, final int len) throws IOException {
      final var bytesRead = super.read(buffer, off, len);
      if (bytesRead != EOF) {
         for (var i = 0; i < bytesRead; i++) {
            final var byteRead = buffer[off + i];
            onByteRead(byteRead);
         }
      }
      return bytesRead;
   }

   @Override
   public int read(final byte[] buffer) throws IOException {
      return read(buffer, 0, buffer.length);
   }
}
