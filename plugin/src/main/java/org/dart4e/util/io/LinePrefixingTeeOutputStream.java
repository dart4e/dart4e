/*
 * SPDX-FileCopyrightText: © The Dart4E authors
 * SPDX-FileContributor: Sebastian Thomschke
 * SPDX-License-Identifier: EPL-2.0
 * SPDX-ArtifactOfProjectHomePage: https://github.com/dart4e/dart4e
 */
package org.dart4e.util.io;

import static org.apache.commons.io.IOUtils.EOF;

import java.io.IOException;
import java.io.OutputStream;

import org.apache.commons.io.output.ProxyOutputStream;

/**
 * @author Sebastian Thomschke
 */
public final class LinePrefixingTeeOutputStream extends ProxyOutputStream {

   private OutputStream branch;
   private byte[] line = new byte[64 * 1024];
   private int lineLength;
   private int prefixLength;

   public LinePrefixingTeeOutputStream(final OutputStream out, final OutputStream branch, final String prefix) {
      super(out);
      this.branch = branch;
      prefixLength = prefix.length();
      lineLength = prefixLength;
      System.arraycopy(prefix.getBytes(), 0, line, 0, prefixLength);
   }

   private void onByteWritten(final byte byteWritten) throws IOException {
      switch (byteWritten) {
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
            line[lineLength] = byteWritten;
            lineLength++;
      }
   }

   @Override
   public void write(final byte[] b) throws IOException {
      write(b, 0, b.length);
   }

   @Override
   public void write(final byte[] b, final int off, final int len) throws IOException {
      super.write(b, off, len);
      for (int i = off, l = off + len; i < l; i++) {
         onByteWritten(b[i]);
      }
   }

   @Override
   public void write(final int b) throws IOException {
      super.write(b);
      onByteWritten((byte) b);
   }

   @Override
   public void flush() throws IOException {
      super.flush();
      branch.flush();
   }
}
