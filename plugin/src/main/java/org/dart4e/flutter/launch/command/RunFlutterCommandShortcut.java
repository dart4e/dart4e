/*
 * SPDX-FileCopyrightText: © The Dart4E authors
 * SPDX-FileContributor: Sebastian Thomschke
 * SPDX-License-Identifier: EPL-2.0
 * SPDX-ArtifactOfProjectHomePage: https://github.com/dart4e/dart4e
 */
package org.dart4e.flutter.launch.command;

import org.dart4e.Dart4EPlugin;
import org.dart4e.flutter.launch.FlutterLaunchConfigurations;
import org.dart4e.localization.Messages;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.ui.DebugUITools;
import org.eclipse.debug.ui.ILaunchShortcut;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.window.Window;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IFileEditorInput;

import de.sebthom.eclipse.commons.ui.Dialogs;
import de.sebthom.eclipse.commons.ui.UI;

/**
 * This class is registered via the plugin.xml
 *
 * @author Sebastian Thomschke
 */
public class RunFlutterCommandShortcut implements ILaunchShortcut {

   @Override
   public void launch(final IEditorPart editor, final String mode) {
      IProject project = null;
      final var editorInput = editor.getEditorInput();
      if (editorInput instanceof final IFileEditorInput fileInput) {
         project = fileInput.getFile().getProject();
      }

      if (project == null)
         throw new IllegalArgumentException("RunFlutterCommandShortcut: No project found for editor " + editor);
      runFlutter(project);
   }

   @Override
   public void launch(final ISelection selection, final String mode) {
      IProject project = null;
      if (selection instanceof final StructuredSelection structuredSel //
            && structuredSel.getFirstElement() instanceof final IResource res) {
         project = res.getProject();
      }

      if (project == null)
         throw new IllegalArgumentException("RunFlutterCommandShortcut: No project found for selection " + selection);
      runFlutter(project);
   }

   private void runFlutter(final IProject project) {
      try {
         // create a new launch config
         final var newLaunchConfig = FlutterCommandLaunchConfigurations.create(project);
         if (Window.OK == DebugUITools.openLaunchConfigurationDialog(UI.getShell(), newLaunchConfig,
            FlutterLaunchConfigurations.LAUNCH_FLUTTER_GROUP, null)) {
            newLaunchConfig.doSave();
         }
      } catch (final CoreException ex) {
         Dialogs.showStatus(Messages.Launch_CreatingLaunchConfigFailed, Dart4EPlugin.status().createError(ex), true);
      }
   }
}
