/*
 * SPDX-FileCopyrightText: © The Dart4E authors
 * SPDX-FileContributor: Sebastian Thomschke
 * SPDX-License-Identifier: EPL-2.0
 * SPDX-ArtifactOfProjectHomePage: https://github.com/dart4e/dart4e
 */
package org.dart4e.flutter.project;

import static net.sf.jstuff.core.validation.NullAnalysisHelper.*;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;

import org.dart4e.Dart4EPlugin;
import org.dart4e.flutter.console.FlutterConsole;
import org.dart4e.flutter.prefs.FlutterProjectPreference;
import org.dart4e.localization.Messages;
import org.dart4e.navigation.DartDependenciesUpdater;
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
import net.sf.jstuff.core.Strings;
import net.sf.jstuff.core.concurrent.Threads;

/**
 * @author Sebastian Thomschke
 */
public final class NewFlutterProjectWizard extends Wizard implements INewWizard {

   private NewFlutterProjectPage newProjectPage = lateNonNull();
   private @Nullable IProject newProject;

   @Override
   public void addPages() {
      newProjectPage = new NewFlutterProjectPage(NewFlutterProjectPage.class.getSimpleName());
      newProjectPage.setTitle(Messages.Label_Flutter_Project);
      newProjectPage.setDescription(Messages.Flutter_NewProject_Descr);
      addPage(newProjectPage);
   }

   @Override
   public void init(final IWorkbench workbench, final IStructuredSelection selection) {
      setWindowTitle(Messages.Flutter_NewProject);
      setNeedsProgressMonitor(true);
   }

   @Override
   public synchronized boolean performFinish() {
      if (newProject != null)
         return true;

      final var projHandle = newProjectPage.getProjectHandle();
      final var projConfig = ResourcesPlugin.getWorkspace().newProjectDescription(projHandle.getName());

      // configure target root folder
      if (!newProjectPage.useDefaults()) {
         projConfig.setLocationURI(newProjectPage.getLocationURI());
      }

      try {
         getContainer().run(true, true, monitor -> {
            try {
               // create the eclipse project
               final var create = new CreateProjectOperation(projConfig, Messages.Flutter_NewProject);
               create.execute(monitor, WorkspaceUndoUtil.getUIInfoAdapter(getShell()));
               final var newProject = this.newProject = (IProject) asNonNullUnsafe(create.getAffectedObjects())[0];

               final var selectedTemplate = newProjectPage.template.get();
               // generate the Flutter Template files
               final var args = new ArrayList<String>();
               args.add("create");
               args.add("--template");
               args.add(newProjectPage.template.get());
               args.add("--description");
               args.add(newProjectPage.description.get());
               if (selectedTemplate.contains("android")) {
                  args.add("--android-language");
                  args.add(newProjectPage.androidLanguage.get());
               }

               if (Strings.equalsAny(selectedTemplate, "app", "plugin")) {
                  args.add("--platforms");
                  args.add(Strings.join(newProjectPage.platforms.get(), ","));
               }
               if ("app".equals(selectedTemplate) && !newProjectPage.appSampleId.get().isBlank()) {
                  args.add("--sample");
                  args.add(newProjectPage.appSampleId.get());
               }
               args.add(".");

               FlutterConsole.runWithConsole(monitor, "Creating new project layout...", newProject, args.toArray(String[]::new));

               FlutterProjectNature.addToProject(newProject);
               final var prefs = FlutterProjectPreference.get(newProject);
               prefs.setAlternateFlutterSDK(newProjectPage.altSDK.get());
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
               status = Dart4EPlugin.status().createError(exCore, Messages.Error_CaseVariantExistsError, projHandle.getName());
            } else {
               status = Dart4EPlugin.status().createStatus(exCore.getStatus().getSeverity(), exCore, Messages.Error_UnexpectedError, exCore
                  .getMessage());
               Dart4EPlugin.log().log(status);
            }
         } else {
            status = Dart4EPlugin.status().createError(exUnwrapped, Messages.Error_UnexpectedError, exUnwrapped.getMessage());
            Dart4EPlugin.log().log(status);
         }

         Dialogs.showStatus(Messages.Error_ProjectCreationProblem, status, false);
         return false;
      }

      BasicNewResourceWizard.selectAndReveal(newProject, UI.getActiveWorkbenchWindow());
      return true;
   }
}
