/*
 * SPDX-FileCopyrightText: © The Dart4E authors
 * SPDX-FileContributor: Sebastian Thomschke
 * SPDX-License-Identifier: EPL-2.0
 * SPDX-ArtifactOfProjectHomePage: https://github.com/dart4e/dart4e
 */
package org.dart4e.flutter.launch.command;

import org.dart4e.flutter.console.FlutterConsole;
import org.dart4e.flutter.prefs.FlutterProjectPreference;
import org.dart4e.localization.Messages;
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
public class RunFlutterPubUpgradeHandler extends AbstractHandler {

   @Override
   public @Nullable Object execute(final ExecutionEvent event) throws ExecutionException {
      if (HandlerUtil.getCurrentSelection(event) instanceof final IStructuredSelection currentSelection) {
         final var project = Projects.adapt(currentSelection.getFirstElement());
         if (project == null || !project.exists()) {
            Dialogs.showError(Messages.Launch_NoProjectSelected, Messages.Launch_NoProjectSelected_Descr);
            return Status.CANCEL_STATUS;
         }
         runPubUpgrade(project);
         return Status.OK_STATUS;
      }
      return Status.CANCEL_STATUS;
   }

   private void runPubUpgrade(final IProject project) {
      final var job = Job.create(Messages.Label_Flutter_Pub_Upgrade, jobMonitor -> {
         final var prefs = FlutterProjectPreference.get(project);
         final var flutterSDK = prefs.getEffectiveFlutterSDK();

         if (flutterSDK == null || !flutterSDK.isValid()) {
            Dialogs.showError(Messages.Flutter_Prefs_NoSDKRegistered_Title, Messages.Flutter_Prefs_NoSDKRegistered_Body);
            return;
         }

         FlutterConsole.runWithConsole(jobMonitor, Messages.Label_Flutter_Pub_Upgrade, project, "pub", "upgrade");
         project.refreshLocal(1, jobMonitor);
      });
      job.schedule();
   }
}
