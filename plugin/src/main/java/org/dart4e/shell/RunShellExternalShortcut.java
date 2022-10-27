/*
 * Copyright 2022 by the Dart4E authors.
 * SPDX-License-Identifier: EPL-2.0
 */
package org.dart4e.shell;

import java.io.IOException;

import org.dart4e.Dart4EPlugin;
import org.dart4e.localization.Messages;
import org.dart4e.model.DartSDK;
import org.dart4e.prefs.DartProjectPreference;
import org.dart4e.prefs.DartWorkspacePreference;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.debug.ui.ILaunchShortcut;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IFileEditorInput;

import de.sebthom.eclipse.commons.ui.Dialogs;
import net.sf.jstuff.core.SystemUtils;
import net.sf.jstuff.core.io.Processes;

/**
 * @author Sebastian Thomschke
 */
public class RunShellExternalShortcut implements ILaunchShortcut {

   @Override
   public void launch(final IEditorPart editor, final String mode) {
      IProject project = null;
      final var editorInput = editor.getEditorInput();
      if (editorInput instanceof final IFileEditorInput fileInput) {
         project = fileInput.getFile().getProject();
      }
      launchShell(project);
   }

   @Override
   public void launch(final ISelection selection, final String mode) {
      IProject project = null;
      if (selection instanceof final StructuredSelection structuredSel) {
         final var firstElement = structuredSel.getFirstElement();
         if (firstElement instanceof final IResource res) {
            project = res.getProject();
         }
      }
      launchShell(project);
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

         try {
            if (SystemUtils.IS_OS_WINDOWS) {
               Processes.builder("cmd") //
                  .withArgs("/K", "start", "Dart Interactive Shell", dartSDK.getDartExecutable(), "pub", "global", "run", "interactive") //
                  .start();
            } else if (SystemUtils.IS_OS_LINUX) {
               Processes.builder("cmd") //
                  .withArgs("gnome-terminal", "--", dartSDK.getDartExecutable(), "pub", "global", "run", "interactive") //
                  .start();
            } else if (SystemUtils.IS_OS_MAC) {
               Processes.builder("osascript") //
                  .withArgs( //
                     "-e", "tell application \"Terminal\" to activate", //
                     "-e", "tell application \"Terminal\" to do script \"" + dartSDK.getDartExecutable() + " pub global run interactive") //
                  .start();
            } else {
               Dialogs.showError("Unsupported Operating System", "Cannot launch Dart Shell. {0} is not supported", SystemUtils.OS_NAME);
            }
         } catch (final IOException ex) {
            Dart4EPlugin.log().error(ex);
         }

      });
      job.schedule();
   }
}
