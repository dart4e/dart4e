/*
 * SPDX-FileCopyrightText: © The Dart4E authors
 * SPDX-FileContributor: Sebastian Thomschke
 * SPDX-License-Identifier: EPL-2.0
 * SPDX-ArtifactOfProjectHomePage: https://github.com/dart4e/dart4e
 */
package org.dart4e.launch.command;

import org.dart4e.console.DartConsole;
import org.dart4e.localization.Messages;
import org.dart4e.prefs.DartProjectPreference;
import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.handlers.HandlerUtil;

import de.sebthom.eclipse.commons.resources.Projects;
import de.sebthom.eclipse.commons.ui.Dialogs;

/**
 * @author Sebastian Thomschke
 */
public class AbstractDartCommandHandler extends AbstractHandler {

   final String title;
   final String[] dartArgs;

   public AbstractDartCommandHandler(final String title, final String... dartArgs) {
      this.title = title;
      this.dartArgs = dartArgs;
   }

   @Override
   public @Nullable Object execute(final ExecutionEvent event) throws ExecutionException {
      if (HandlerUtil.getCurrentSelection(event) instanceof final IStructuredSelection currentSelection) {
         final var project = Projects.adapt(currentSelection.getFirstElement());
         if (project == null || !project.exists()) {
            Dialogs.showError(Messages.Launch_NoProjectSelected, Messages.Launch_NoProjectSelected_Descr);
            return null;
         }

         runDartCommand(project);
      }
      return null;
   }

   public void runDartCommand(final IProject project) {
      final var job = Job.create(title, jobMonitor -> {
         if (jobMonitor == null) {
            jobMonitor = new NullProgressMonitor();
         }

         final var prefs = DartProjectPreference.get(project);
         final var dartSDK = prefs.getEffectiveDartSDK();

         if (dartSDK == null || !dartSDK.isValid()) {
            Dialogs.showError(Messages.Prefs_NoSDKRegistered_Title, Messages.Prefs_NoSDKRegistered_Body);
            return;
         }

         DartConsole.runWithConsole(jobMonitor, title, project, dartArgs);
         project.refreshLocal(1, jobMonitor);
      });
      job.schedule();
   }
}
