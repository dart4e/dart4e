/*
 * SPDX-FileCopyrightText: © The Dart4E authors
 * SPDX-FileContributor: Sebastian Thomschke
 * SPDX-License-Identifier: EPL-2.0
 * SPDX-ArtifactOfProjectHomePage: https://github.com/dart4e/dart4e
 */
package org.dart4e.util.io;

import java.io.IOException;
import java.io.OutputStream;

import org.apache.commons.lang3.StringUtils;
import org.dart4e.Dart4EPlugin;
import org.dart4e.util.Ansi;

import net.sf.jstuff.core.Strings;

/**
 * @author Sebastian Thomschke
 */
public final class VSCodeJsonRpcLineTracing {

   public enum Source {
      CLIENT_OUT,
      SERVER_ERR,
      SERVER_OUT
   }

   public static void traceLine(final Source source, final String line, final boolean verbose) {
      traceLine(source, line, System.out, verbose, true);
   }

   public static void traceLine(final Source source, String line, final OutputStream out, final boolean verbose, final boolean colorize) {
      if (!verbose) {
         if (line.isBlank() || "Content-Type: application/vscode-jsonrpc; charset=utf-8".equals(line))
            return;
         line = Strings.substringBefore(line, "Content-Length: ");
         if (line.isBlank())
            return;
         line = StringUtils.replace(line, "\"jsonrpc\":\"2.0\",", "");
      }

      line += (colorize ? Ansi.RESET : "") + System.lineSeparator();

      try {
         switch (source) {
            case CLIENT_OUT -> out.write(((colorize ? Ansi.BLUE : "") + "CLIENT >> " + line).getBytes());
            case SERVER_OUT -> out.write(((colorize ? Ansi.MAGENTA : "") + "SERVER << " + line).getBytes());
            case SERVER_ERR -> out.write(((colorize ? Ansi.RED : "") + "SRVERR << " + line).getBytes());
         }
         out.flush();
      } catch (final IOException ex) {
         Dart4EPlugin.log().error(ex);
      }
   }

   private VSCodeJsonRpcLineTracing() {
   }
}
