/*
 * SPDX-FileCopyrightText: © The Dart4E authors
 * SPDX-FileContributor: Sebastian Thomschke
 * SPDX-License-Identifier: EPL-2.0
 * SPDX-ArtifactOfProjectHomePage: https://github.com/dart4e/dart4e
 */
package org.dart4e.widget;

import org.dart4e.Constants;
import org.dart4e.Dart4EPlugin;
import org.dart4e.navigation.DartDependenciesUpdater;
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.dialogs.ElementTreeSelectionDialog;
import org.eclipse.ui.model.BaseWorkbenchContentProvider;
import org.eclipse.ui.model.WorkbenchLabelProvider;

/**
 * @author Sebastian Thomschke
 */
public class DartFileSelectionDialog extends ElementTreeSelectionDialog {

   public static class DartFileViewerFilter extends ViewerFilter {
      @Override
      public boolean select(final Viewer viewer, final @Nullable Object parent, final Object element) {
         if (element instanceof final IFile file)
            return Constants.DART_FILE_EXTENSION.equals(file.getFileExtension());

         if (element instanceof final IContainer container && container.isAccessible()) {
            if (container.getName().startsWith(".") //
               || DartDependenciesUpdater.DEPS_MAGIC_FOLDER_NAME.equals(container.getName()) //
               || DartDependenciesUpdater.STDLIB_MAGIC_FOLDER_NAME.equals(container.getName()))
               return false;

            try {
               for (final var child : container.members()) {
                  if (select(viewer, parent, child))
                     return true;
               }
            } catch (final CoreException ex) {
               Dart4EPlugin.log().error(ex);
            }
         }

         return false;
      }
   }

   public DartFileSelectionDialog(final Shell parentShell, final String title, final IContainer parentContainer) {
      super(parentShell, new WorkbenchLabelProvider(), new BaseWorkbenchContentProvider());
      setAllowMultiple(true);
      setEmptyListMessage("No Dart files in [" + parentContainer.getName() + "]!");
      setTitle(title);
      setMessage("Select a Dart file (*." + Constants.DART_FILE_EXTENSION + ")");
      setInput(parentContainer);
      addFilter(new DartFileViewerFilter());

      setValidator(selection -> {
         if (selection.length > 0 && selection[0] instanceof final IFile file)
            return Dart4EPlugin.status().createStatus(IStatus.OK, "File [{0}] selected.", file.getProjectRelativePath());
         return Dart4EPlugin.status().createError("No Dart file selected.");
      });
   }
}
