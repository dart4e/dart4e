/*
 * Copyright 2022 by the Dart4E authors.
 * SPDX-License-Identifier: EPL-2.0
 */
package org.dart4e.console;

import static net.sf.jstuff.core.validation.NullAnalysisHelper.*;

import java.io.IOException;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.commons.lang3.time.DurationFormatUtils;
import org.dart4e.Dart4EPlugin;
import org.dart4e.prefs.DartProjectPreference;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.debug.internal.ui.DebugUIPlugin;
import org.eclipse.debug.internal.ui.preferences.IDebugPreferenceConstants;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.ui.console.IConsoleFactory;
import org.eclipse.ui.console.MessageConsole;

import de.sebthom.eclipse.commons.ui.Consoles;
import de.sebthom.eclipse.commons.ui.UI;
import net.sf.jstuff.core.Strings;
import net.sf.jstuff.core.concurrent.Threads;
import net.sf.jstuff.core.io.Processes;

/**
 * @author Sebastian Thomschke
 */
@SuppressWarnings("restriction")
public final class DartConsole extends MessageConsole {

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
   public static final String CONSOLE_TYPE = DartConsole.class.getName();

   /**
    * Runs the dart command in the DartConsole.
    */
   public static void runWithConsole(final IProject project, final List<String> args, final IProgressMonitor monitor) throws CoreException {
      final var prefs = DartProjectPreference.get(project);
      final var dartSDK = prefs.getEffectiveDartSDK();
      if (dartSDK == null)
         throw new IllegalStateException("No Dart SDK found!");

      monitor.setTaskName("Running dart with '" + project.getName() + "'");
      System.err.println(args);
      final Processes.Builder processBuilder = Processes.builder(dartSDK.getDartExecutable()).withArgs(args.toArray(String[]::new));

      final var onTerminated = new CompletableFuture<@Nullable Void>();
      final var console = new DartConsole(project, onTerminated, monitor);
      Consoles.closeConsoles(c -> c instanceof final DartConsole dartConsole //
         && dartConsole.project.equals(project) //
         && dartConsole.onTerminated.toCompletableFuture().isDone());
      Consoles.showConsole(console);

      try (var out = console.newMessageStream();
           var err = console.newMessageStream()) {

         UI.run(() -> {
            out.setColor(DebugUIPlugin.getPreferenceColor(IDebugPreferenceConstants.CONSOLE_SYS_OUT_COLOR));
            err.setColor(DebugUIPlugin.getPreferenceColor(IDebugPreferenceConstants.CONSOLE_SYS_ERR_COLOR));
         });

         final var startAt = LocalTime.now();
         final var startAtStr = startAt.truncatedTo(ChronoUnit.SECONDS).format(DateTimeFormatter.ISO_TIME);

         out.println("Building project '" + project.getName() + "'...");
         out.println();

         var workdir = asNonNull(project.getLocation()).toFile();
         if (!workdir.exists()) {
            workdir = asNonNull(workdir.getParentFile());
         }
         final var hasOutput = new AtomicBoolean(false);
         final var proc = processBuilder //
            .withWorkingDirectory(workdir) //
            .withEnvironment(env -> {
               if (Consoles.isAnsiColorsSupported()) {
                  env.put("ANSICON", "1");
               }
            }) //
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
            /*
             * kill process if job was aborted by user
             */
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
         throw new CoreException(Dart4EPlugin.status().createError(ex, "Failed to run Dart."));
      } catch (final InterruptedException ex) {
         Thread.currentThread().interrupt();
         throw new CoreException(Dart4EPlugin.status().createError(ex, "Aborted."));
      } finally {
         onTerminated.complete(null);
      }
   }

   public final IProject project;
   public final CompletionStage<@Nullable Void> onTerminated;
   public final IProgressMonitor monitor;

   private DartConsole(final IProject project, final CompletionStage<@Nullable Void> onTerminated, final IProgressMonitor monitor) {
      super("Dart", CONSOLE_TYPE, null, true);
      this.project = project;
      this.onTerminated = onTerminated;
      this.monitor = monitor;
   }

   public void setTitle(final String title) {
      UI.run(() -> {
         if (Strings.isEmpty(title)) {
            setName("Dart");
         } else {
            setName("Dart: " + title);
         }
      });
   }
}
