/*
 * Copyright 2022 by the Dart4E authors.
 * SPDX-License-Identifier: EPL-2.0
 */
package org.dart4e.project;

import static net.sf.jstuff.core.validation.NullAnalysisHelper.*;

import java.lang.reflect.InvocationTargetException;

import org.dart4e.Dart4EPlugin;
import org.dart4e.console.DartConsole;
import org.dart4e.localization.Messages;
import org.dart4e.navigation.DartDependenciesUpdater;
import org.dart4e.prefs.DartProjectPreference;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceStatus;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.ui.INewWizard;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.ide.undo.CreateProjectOperation;
import org.eclipse.ui.ide.undo.WorkspaceUndoUtil;
import org.eclipse.ui.wizards.newresource.BasicNewResourceWizard;

import de.sebthom.eclipse.commons.ui.Dialogs;
import de.sebthom.eclipse.commons.ui.UI;
import net.sf.jstuff.core.concurrent.Threads;

/**
 * @author Sebastian Thomschke
 */
public final class NewDartProjectWizard extends Wizard implements INewWizard {

   private NewDartProjectPage newDartProjectPage = lazyNonNull();
   private @Nullable IProject newProject;

   @Override
   public void addPages() {
      newDartProjectPage = new NewDartProjectPage(NewDartProjectPage.class.getSimpleName());
      newDartProjectPage.setTitle(Messages.Label_Dart_Project);
      newDartProjectPage.setDescription(Messages.NewDartProject_Descr);
      addPage(newDartProjectPage);
   }

   @Override
   public void init(final IWorkbench workbench, final IStructuredSelection selection) {
      setWindowTitle(Messages.NewDartProject);
      setNeedsProgressMonitor(true);
   }

   @Override
   public synchronized boolean performFinish() {
      if (newProject != null)
         return true;

      final var projHandle = newDartProjectPage.getProjectHandle();
      final var projConfig = ResourcesPlugin.getWorkspace().newProjectDescription(projHandle.getName());

      // configure target root folder
      if (!newDartProjectPage.useDefaults()) {
         projConfig.setLocationURI(newDartProjectPage.getLocationURI());
      }

      try {
         getContainer().run(true, true, monitor -> {
            try {
               // create the eclipse project
               final var create = new CreateProjectOperation(projConfig, Messages.NewDartProject);
               create.execute(monitor, WorkspaceUndoUtil.getUIInfoAdapter(getShell()));
               final var newProject = this.newProject = (IProject) asNonNullUnsafe(create.getAffectedObjects())[0];

               // generate the Dart Template files
               DartConsole.runWithConsole(monitor, "Creating new project layout...", newProject, "create", "--force", "-t",
                  newDartProjectPage.selectedTemplate.get(), ".");

               DartProjectNature.addToProject(newProject);
               final var prefs = DartProjectPreference.get(newProject);
               prefs.setAlternateDartSDK(newDartProjectPage.selectedAltSDK.get());
               prefs.save();

               newProject.open(monitor);
               newProject.refreshLocal(IResource.DEPTH_INFINITE, monitor);

               DartDependenciesUpdater.INSTANCE.onProjectConfigChanged(newProject); // TODO this should not be required
            } catch (final Exception ex) {
               throw new InvocationTargetException(ex);
            }
         });
      } catch (final InterruptedException ex) {
         Threads.handleInterruptedException(ex);
         return false;
      } catch (final InvocationTargetException ex) {
         final var exUnwrapped = ex.getTargetException();
         final IStatus status;
         if (exUnwrapped.getCause() instanceof final CoreException exCore) {
            if (exCore.getStatus().getCode() == IResourceStatus.CASE_VARIANT_EXISTS) {
               status = Dart4EPlugin.status().createError(exCore, Messages.NewDartProject_CaseVariantExistsError, projHandle.getName());
            } else {
               status = Dart4EPlugin.status().createStatus(exCore.getStatus().getSeverity(), exCore,
                  Messages.NewDartProject_UnexpectedError, exCore.getMessage());
               Dart4EPlugin.log().log(status);
            }
         } else {
            status = Dart4EPlugin.status().createError(exUnwrapped, Messages.NewDartProject_UnexpectedError, exUnwrapped.getMessage());
            Dart4EPlugin.log().log(status);
         }

         Dialogs.showStatus(Messages.NewDartProject_ErrorTitle, status, false);
         return false;
      }

      BasicNewResourceWizard.selectAndReveal(newProject, UI.getActiveWorkbenchWindow());
      return true;
   }
}
