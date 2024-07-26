/*
 * SPDX-FileCopyrightText: © The Dart4E authors
 * SPDX-FileContributor: Sebastian Thomschke
 * SPDX-License-Identifier: EPL-2.0
 * SPDX-ArtifactOfProjectHomePage: https://github.com/dart4e/dart4e
 */
package org.dart4e.langserver;

import static net.sf.jstuff.core.validation.NullAnalysisHelper.lateNonNull;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.util.Arrays;
import java.util.Map;

import org.apache.commons.lang3.SystemUtils;
import org.dart4e.Dart4EPlugin;
import org.dart4e.model.DartSDK;
import org.dart4e.model.buildsystem.BuildSystem;
import org.dart4e.prefs.DartProjectPreference;
import org.dart4e.prefs.DartWorkspacePreference;
import org.dart4e.util.TreeBuilder;
import org.dart4e.util.io.LineTransformingOutputStream;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.lsp4e.server.ProcessStreamConnectionProvider;

import de.sebthom.eclipse.commons.resources.Projects;
import net.sf.jstuff.core.Strings;
import net.sf.jstuff.core.io.stream.LinePrefixingTeeInputStream;
import net.sf.jstuff.core.io.stream.LinePrefixingTeeOutputStream;

/**
 * Launches the Dart language server.
 *
 * @author Sebastian Thomschke
 */
public final class DartLangServerLauncher extends ProcessStreamConnectionProvider {

   private static final boolean TRACE_IO = Platform.getDebugBoolean("org.dart4e/trace/langserv/io");
   private static final boolean TRACE_INIT_OPTIONS = Platform.getDebugBoolean("org.dart4e/trace/langserv/init_options");

   public DartLangServerLauncher() {
      setWorkingDirectory(SystemUtils.getUserDir().getAbsolutePath());
   }

   @Override
   public @Nullable InputStream getErrorStream() {
      final var stream = super.getErrorStream();
      if (!TRACE_IO)
         return stream;

      if (stream == null)
         return null;
      return new LinePrefixingTeeInputStream(stream, System.out, "SRVERR >> ");
   }

   private DartSDK dartSDK = lateNonNull();

   @Override
   public @Nullable Map<String, Object> getInitializationOptions(final @Nullable URI projectRootUri) {
      final var project = Projects.findProjectOfResource(projectRootUri);
      final @Nullable DartSDK dartSDK;
      final BuildSystem buildSystem;
      if (project == null) {
         dartSDK = DartWorkspacePreference.getDefaultDartSDK(true, true);
         buildSystem = BuildSystem.DART;
      } else {
         final var projectPrefs = DartProjectPreference.get(project);
         dartSDK = projectPrefs.getEffectiveDartSDK();
         buildSystem = BuildSystem.guessBuildSystemOfProject(project);
      }

      if (dartSDK == null)
         throw new IllegalStateException("Cannot initialize Dart Language Server, no Dart SDK found.");

      this.dartSDK = dartSDK;
      setCommands(Arrays.asList( //
         dartSDK.getDartExecutable().toString(), //
         dartSDK.getInstallRoot().resolve("bin/snapshots/analysis_server.dart.snapshot").toString(), //
         "--protocol=lsp"));

      /*
       * https://github.com/dart-lang/sdk/blob/main/pkg/analysis_server/tool/lsp_spec/README.md#initialization-options
       */
      final var opts = new TreeBuilder<String>() //
         .put("outline", true) //
         .put("flutterOutline", buildSystem == BuildSystem.FLUTTER) //
         .put("suggestFromUnimportedLibraries", true) //
         .getMap();

      if (TRACE_INIT_OPTIONS) {
         Dart4EPlugin.log().info(opts);
      }
      return opts;
   }

   @Override
   public @Nullable InputStream getInputStream() {
      final var stream = super.getInputStream();
      if (stream == null)
         return null;

      return TRACE_IO //
            ? new LinePrefixingTeeInputStream(stream, System.out, "SERVER >> ")
            : stream;
   }

   @Override
   public @Nullable OutputStream getOutputStream() {
      var stream = super.getOutputStream();
      if (stream == null)
         return null;

      /*
       * workaround for https://github.com/dart-lang/sdk/issues/56311#issuecomment-2250089185
       */
      final var dartVersion = dartSDK.getVersion();
      if (dartVersion != null && (dartVersion.startsWith("3.4.") || dartVersion.startsWith("3.5."))) {
         final boolean[] isFirstInlayHintRequest = {true};
         stream = new LineTransformingOutputStream(stream, line -> {
            if (isFirstInlayHintRequest[0] && line.contains("\"method\":\"textDocument/inlayHint\"")) {
               isFirstInlayHintRequest[0] = false;
               return Strings.replace(line, "\"method\":\"textDocument/inlayHint\"", "\"method\":\"dart-lang/sdk#56311\"   ");
            }
            return line;
         });
      }

      return TRACE_IO //
            ? new LinePrefixingTeeOutputStream(stream, System.out, "CLIENT >> ")
            : stream;
   }

   @Override
   public String getTrace(final @Nullable URI rootUri) {
      // return "verbose"; // has no effect, maybe not implemented in Dart language server
      return "off";
   }
}
