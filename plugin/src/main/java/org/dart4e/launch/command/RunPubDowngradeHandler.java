/*
 * Copyright 2022 by the Dart4E authors.
 * SPDX-License-Identifier: EPL-2.0
 */
package org.dart4e.launch.command;

import org.dart4e.console.DartConsole;
import org.dart4e.localization.Messages;
import org.dart4e.prefs.DartProjectPreference;
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
import de.sebthom.eclipse.commons.ui.Dialogs;

/**
 * @author Sebastian Thomschke
 */
public class RunPubDowngradeHandler extends AbstractHandler {

   @Override
   public @Nullable Object execute(final ExecutionEvent event) throws ExecutionException {
      if (HandlerUtil.getCurrentSelection(event) instanceof final IStructuredSelection currentSelection) {
         final var project = Projects.adapt(currentSelection.getFirstElement());
         if (project == null || !project.exists()) {
            Dialogs.showError(Messages.Launch_NoProjectSelected, Messages.Launch_NoProjectSelected_Descr);
            return Status.CANCEL_STATUS;
         }
         runPubDowngrade(project);
         return Status.OK_STATUS;
      }
      return Status.CANCEL_STATUS;
   }

   private void runPubDowngrade(final IProject project) {
      final var job = Job.create(Messages.Label_Dart_Pub_Downgrade, jobMonitor -> {
         final var prefs = DartProjectPreference.get(project);
         final var dartSDK = prefs.getEffectiveDartSDK();

         if (dartSDK == null || !dartSDK.isValid()) {
            Dialogs.showError(Messages.Prefs_NoSDKRegistered_Title, Messages.Prefs_NoSDKRegistered_Body);
            return;
         }

         DartConsole.runWithConsole(jobMonitor, Messages.Label_Dart_Pub_Downgrade, project, "pub", "downgrade");
         project.refreshLocal(1, jobMonitor);
      });
      job.schedule();
   }
}
