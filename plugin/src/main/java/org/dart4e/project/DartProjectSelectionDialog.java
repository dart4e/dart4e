/*
 * SPDX-FileCopyrightText: © The Dart4E authors
 * SPDX-FileContributor: Sebastian Thomschke
 * SPDX-License-Identifier: EPL-2.0
 * SPDX-ArtifactOfProjectHomePage: https://github.com/dart4e/dart4e
 */
package org.dart4e.project;

import java.util.List;

import org.dart4e.Constants;
import org.dart4e.Dart4EPlugin;
import org.eclipse.core.resources.IProject;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.dialogs.ElementListSelectionDialog;

import de.sebthom.eclipse.commons.resources.Projects;

/**
 * @author Sebastian Thomschke
 */
public class DartProjectSelectionDialog {
   private final ElementListSelectionDialog dialog;

   public DartProjectSelectionDialog(final Shell parent) {
      final var dartProjectIcon = Dart4EPlugin.get().getSharedImage(Constants.IMAGE_DART_PROJECT);
      dialog = new ElementListSelectionDialog(parent, new LabelProvider() {
         @Override
         public @Nullable Image getImage(final @Nullable Object element) {
            return dartProjectIcon;
         }

         @Override
         public @Nullable String getText(final @Nullable Object item) {
            return item == null ? "" : " " + ((IProject) item).getName();
         }
      });
      dialog.setImage(dartProjectIcon);
      dialog.setTitle("Select a Dart project");
      dialog.setMessage("Enter a string to filter the project list:");
      dialog.setEmptyListMessage("No Dart projects found in workspace.");
      setProjects(Projects.getOpenProjectsWithNature(DartProjectNature.NATURE_ID).toArray(IProject[]::new));
   }

   public @Nullable IProject getSelectedProject() {
      return (IProject) dialog.getFirstResult();
   }

   public DartProjectSelectionDialog setProjects(final IProject... projects) {
      dialog.setElements(projects);
      return this;
   }

   public DartProjectSelectionDialog setProjects(final List<IProject> projects) {
      dialog.setElements(projects.toArray(new IProject[projects.size()]));
      return this;
   }

   public DartProjectSelectionDialog setSelectedProject(final @Nullable IProject project) {
      dialog.setInitialSelections(project);
      return this;
   }

   /**
    * @return null if cancelled
    */
   public @Nullable IProject show() {
      if (dialog.open() == Window.OK)
         return getSelectedProject();
      return null;
   }
}
