/*
 * SPDX-FileCopyrightText: © The Dart4E authors
 * SPDX-FileContributor: Sebastian Thomschke
 * SPDX-License-Identifier: EPL-2.0
 * SPDX-ArtifactOfProjectHomePage: https://github.com/dart4e/dart4e
 */
package org.dart4e.flutter.project;

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
public class FlutterProjectSelectionDialog {
   private final ElementListSelectionDialog dialog;

   public FlutterProjectSelectionDialog(final Shell parent) {
      final var flutterProjectIcon = Dart4EPlugin.get().getSharedImage(Constants.IMAGE_FLUTTER_PROJECT);
      dialog = new ElementListSelectionDialog(parent, new LabelProvider() {
         @Override
         public @Nullable Image getImage(final @Nullable Object element) {
            return flutterProjectIcon;
         }

         @Override
         public @Nullable String getText(final @Nullable Object item) {
            return item == null ? "" : " " + ((IProject) item).getName();
         }
      });
      dialog.setImage(flutterProjectIcon);
      dialog.setTitle("Select a Flutter project");
      dialog.setMessage("Enter a string to filter the project list:");
      dialog.setEmptyListMessage("No Flutter projects found in workspace.");
      setProjects(Projects.getOpenProjectsWithNature(FlutterProjectNature.NATURE_ID).toArray(IProject[]::new));
   }

   public @Nullable IProject getSelectedProject() {
      return (IProject) dialog.getFirstResult();
   }

   public FlutterProjectSelectionDialog setProjects(final IProject... projects) {
      dialog.setElements(projects);
      return this;
   }

   public FlutterProjectSelectionDialog setProjects(final List<IProject> projects) {
      dialog.setElements(projects.toArray(new IProject[projects.size()]));
      return this;
   }

   public FlutterProjectSelectionDialog setSelectedProject(final @Nullable IProject project) {
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
