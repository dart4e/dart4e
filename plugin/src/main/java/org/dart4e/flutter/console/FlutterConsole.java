/*
 * SPDX-FileCopyrightText: © The Dart4E authors
 * SPDX-FileContributor: Sebastian Thomschke
 * SPDX-License-Identifier: EPL-2.0
 * SPDX-ArtifactOfProjectHomePage: https://github.com/dart4e/dart4e
 */
package org.dart4e.flutter.console;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.commons.lang3.time.DurationFormatUtils;
import org.dart4e.Dart4EPlugin;
import org.dart4e.flutter.model.FlutterSDK;
import org.dart4e.flutter.prefs.FlutterProjectPreference;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.debug.internal.ui.DebugUIPlugin;
import org.eclipse.debug.internal.ui.preferences.IDebugPreferenceConstants;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.ui.console.IConsoleFactory;
import org.eclipse.ui.console.MessageConsole;

import de.sebthom.eclipse.commons.resources.Resources;
import de.sebthom.eclipse.commons.ui.Consoles;
import de.sebthom.eclipse.commons.ui.UI;
import net.sf.jstuff.core.Strings;
import net.sf.jstuff.core.concurrent.Threads;

/**
 * @author Sebastian Thomschke
 */
@SuppressWarnings("restriction")
public final class FlutterConsole extends MessageConsole {

   /**
    * Adds an entry to the console view's "Display Selected Console" entry drop down button.
    */
   public static class Factory implements IConsoleFactory {
      @Override
      public void openConsole() {
         Consoles.showConsole(c -> CONSOLE_TYPE.equals(c.getType()));
      }
   }

   /**
    * This value is configured in plugin.xml
    */
   public static final String CONSOLE_TYPE = FlutterConsole.class.getName();

   public static void runWithConsole(final IProgressMonitor monitor, final String headLine, final FlutterSDK flutterSDK,
      final @Nullable IProject project, final @Nullable Path workdir, final String... args) throws CoreException {

      final var processBuilder = flutterSDK.getFlutterProcessBuilder(false).withArgs(args);

      final var onTerminated = new CompletableFuture<@Nullable Void>();
      final var console = new FlutterConsole(project, onTerminated, monitor);
      Consoles.closeConsoles(c -> c instanceof final FlutterConsole flutterConsole //
         && flutterConsole.project == project // CHECKSTYLE:IGNORE .*
         && flutterConsole.onTerminated.toCompletableFuture().isDone());
      Consoles.showConsole(console);

      try (var out = console.newMessageStream();
           var err = console.newMessageStream()) {

         UI.run(() -> {
            out.setColor(DebugUIPlugin.getPreferenceColor(IDebugPreferenceConstants.CONSOLE_SYS_OUT_COLOR));
            err.setColor(DebugUIPlugin.getPreferenceColor(IDebugPreferenceConstants.CONSOLE_SYS_ERR_COLOR));
         });

         final var startAt = LocalTime.now();
         final var startAtStr = startAt.truncatedTo(ChronoUnit.SECONDS).format(DateTimeFormatter.ISO_TIME);

         out.println(headLine);
         out.println();

         final var hasOutput = new AtomicBoolean(false);
         final var proc = processBuilder //
            .withWorkingDirectory(workdir) //
            .withRedirectOutput(line -> {
               out.println(line);
               hasOutput.set(true);
            }) //
            .withRedirectError(line -> {
               err.println(line);
               hasOutput.set(true);
            }) //
            .start();

         final var exe = proc.getProcess().info().command().orElse("<unknown>");

         console.setTitle("<running> " + exe + " (" + startAtStr + ")");

         while (proc.isAlive()) {
            // kill process if job was aborted by user
            if (monitor.isCanceled()) {
               proc.terminate() //
                  .waitForExit(2, TimeUnit.SECONDS) //
                  .kill();
               proc.getProcess().descendants().forEach(ProcessHandle::destroy);
               Threads.sleep(1000);
               proc.getProcess().descendants().forEach(ProcessHandle::destroyForcibly);
               err.println("Aborted on user request.");
               break;
            }
            Threads.sleep(500);
         }

         final var endAt = LocalTime.now();
         final var endAtStr = endAt.truncatedTo(ChronoUnit.SECONDS).format(DateTimeFormatter.ISO_TIME);
         console.setTitle("<terminated> " + exe + " (" + startAtStr + " - " + endAtStr + ") [" + proc.getProcess().pid() + "]");
         if (monitor.isCanceled())
            return;

         if (hasOutput.get()) {
            out.println();
         }
         if (proc.exitStatus() == 0) {
            out.write("Execution successful in ");
         } else {
            out.write("Execution");
            out.flush();
            err.write(" failed ");
            out.write("in ");
         }

         var elapsed = ChronoUnit.MILLIS.between(startAt, endAt);
         if (elapsed < 1_000) { // prevent 'Build successful in 0 seconds'
            elapsed = 1_000;
         }
         out.write(DurationFormatUtils.formatDurationWords(elapsed, true, true));
         if (proc.exitStatus() != 0) {
            out.write(" (exit code: " + proc.exitStatus() + ")");
         }
         out.println();

      } catch (final IOException ex) {
         throw new CoreException(Dart4EPlugin.status().createError(ex, "Failed to run Flutter."));
      } catch (final InterruptedException ex) {
         Thread.currentThread().interrupt();
         throw new CoreException(Dart4EPlugin.status().createError(ex, "Aborted."));
      } finally {
         onTerminated.complete(null);
      }
   }

   /**
    * Runs the flutter command in the {@link FlutterConsole}.
    */
   public static void runWithConsole(final IProgressMonitor monitor, final String headLine, final FlutterSDK flutterSDK,
      final @Nullable Path workdir, final String... args) throws CoreException {
      runWithConsole(monitor, headLine, flutterSDK, null, workdir, args);
   }

   /**
    * Runs the flutter command in the {@link FlutterConsole}.
    */
   public static void runWithConsole(final IProgressMonitor monitor, final String headLine, final IProject project, final String... args)
      throws CoreException {
      final var prefs = FlutterProjectPreference.get(project);
      final var flutterSDK = prefs.getEffectiveFlutterSDK();
      if (flutterSDK == null)
         throw new IllegalStateException("No Flutter SDK found!");

      var workdir = Resources.toAbsolutePath(project);
      if (!Files.exists(workdir)) {
         workdir = workdir.getParent();
      }

      runWithConsole(monitor, headLine, flutterSDK, workdir, args);
   }

   public final @Nullable IProject project;
   public final CompletionStage<@Nullable Void> onTerminated;
   public final IProgressMonitor monitor;

   private FlutterConsole(final @Nullable IProject project, final CompletionStage<@Nullable Void> onTerminated,
      final IProgressMonitor monitor) {
      super("Flutter", CONSOLE_TYPE, null, true);
      this.project = project;
      this.onTerminated = onTerminated;
      this.monitor = monitor;
   }

   public void setTitle(final String title) {
      UI.run(() -> {
         if (Strings.isEmpty(title)) {
            setName("Flutter");
         } else {
            setName("Flutter: " + title);
         }
      });
   }
}
