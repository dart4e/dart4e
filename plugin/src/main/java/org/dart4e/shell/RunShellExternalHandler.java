/*
 * SPDX-FileCopyrightText: © The Dart4E authors
 * SPDX-FileContributor: Sebastian Thomschke
 * SPDX-License-Identifier: EPL-2.0
 * SPDX-ArtifactOfProjectHomePage: https://github.com/dart4e/dart4e
 */
package org.dart4e.shell;

import java.io.IOException;

import org.dart4e.Dart4EPlugin;
import org.dart4e.localization.Messages;
import org.dart4e.model.DartSDK;
import org.dart4e.prefs.DartProjectPreference;
import org.dart4e.prefs.DartWorkspacePreference;
import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.handlers.HandlerUtil;

import de.sebthom.eclipse.commons.resources.Projects;
import de.sebthom.eclipse.commons.resources.Resources;
import de.sebthom.eclipse.commons.ui.Dialogs;
import net.sf.jstuff.core.Strings;
import net.sf.jstuff.core.SystemUtils;
import net.sf.jstuff.core.collection.CollectionUtils;
import net.sf.jstuff.core.concurrent.Threads;
import net.sf.jstuff.core.io.Processes;
import net.sf.jstuff.core.net.NetUtils;

/**
 * @author Sebastian Thomschke
 */
public class RunShellExternalHandler extends AbstractHandler {

   @Override
   public @Nullable Object execute(final ExecutionEvent event) throws ExecutionException {
      if (HandlerUtil.getCurrentSelection(event) instanceof final IStructuredSelection currentSelection) {
         final var project = Projects.adapt(currentSelection.getFirstElement());
         launchShell(project);
         return Status.OK_STATUS;
      }
      return Status.CANCEL_STATUS;
   }

   private void launchShell(@Nullable final IProject project) {

      final var job = Job.create("Preparing Dart Shell", jobMonitor -> {
         final DartSDK dartSDK;
         if (project == null) {
            dartSDK = DartWorkspacePreference.getDefaultDartSDK(true, true);
         } else {
            final var prefs = DartProjectPreference.get(project);
            dartSDK = prefs.getEffectiveDartSDK();
         }
         if (dartSDK == null || !dartSDK.isValid()) {
            Dialogs.showError(Messages.Prefs_NoSDKRegistered_Title, Messages.Prefs_NoSDKRegistered_Body);
            return;
         }

         dartSDK.installInteractiveShell(jobMonitor);

         final var args = CollectionUtils.newArrayList(dartSDK.getDartExecutable(), //
            "pub", "global", "run", //
            "--enable-vm-service=" + NetUtils.getAvailableLocalPort(), //
            "interactive");
         if (project != null) {
            args.add("--directory");
            args.add(Strings.replace(Resources.toAbsolutePath(project).toString(), '\\', '/'));
         }

         try {
            if (SystemUtils.IS_OS_WINDOWS) {
               Processes.builder("cmd") //
                  .withArgs("/K", "start", "Dart Interactive Shell") //
                  .withArgs(args) //
                  .start();
            } else if (SystemUtils.IS_OS_LINUX) {
               Processes.builder("cmd") //
                  .withArgs("gnome-terminal", "--") //
                  .withArgs(args) //
                  .start();
            } else if (SystemUtils.IS_OS_MAC) {
               Processes.builder("osascript") //
                  .withArgs( //
                     "-e", "tell application \"Terminal\" to activate", //
                     "-e", "tell application \"Terminal\" to do script \"" + Strings.join(args, ' ') + "\"") //
                  .start();
            } else {
               Dialogs.showError("Unsupported Operating System", "Cannot launch Dart Shell. {0} is not supported", SystemUtils.OS_NAME);
            }

            if (project != null) {
               jobMonitor.setTaskName("Cleaning up Dart Shell");
               Threads.sleep(5_000);
               final var launchFile = project.getFile("lib/auto_generated.dart");
               launchFile.refreshLocal(0, jobMonitor);
               launchFile.delete(true, jobMonitor);
            }
         } catch (final IOException ex) {
            Dart4EPlugin.log().error(ex);
         }
      });
      job.schedule();
   }
}
