/*
 * SPDX-FileCopyrightText: © The Dart4E authors
 * SPDX-FileContributor: Sebastian Thomschke
 * SPDX-License-Identifier: EPL-2.0
 * SPDX-ArtifactOfProjectHomePage: https://github.com/dart4e/dart4e
 */
package org.dart4e.flutter.launch.app;

import org.dart4e.Dart4EPlugin;
import org.dart4e.flutter.launch.FlutterLaunchConfigurations;
import org.dart4e.launch.LaunchConfigurations;
import org.dart4e.localization.Messages;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.ui.DebugUITools;
import org.eclipse.debug.ui.IDebugUIConstants;
import org.eclipse.debug.ui.ILaunchShortcut;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.window.Window;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IFileEditorInput;

import de.sebthom.eclipse.commons.ui.Dialogs;
import de.sebthom.eclipse.commons.ui.UI;

/**
 * Handler for Flutter project context menu {@code Debug/Run As -> Flutter App}
 *
 * @author Sebastian Thomschke
 */
public class RunFlutterAppShortcut implements ILaunchShortcut {

   @Override
   public void launch(final IEditorPart editor, final String mode) {
      IProject project = null;
      final var editorInput = editor.getEditorInput();
      if (editorInput instanceof final IFileEditorInput fileInput) {
         project = fileInput.getFile().getProject();
      }

      if (project == null)
         throw new IllegalArgumentException("RunFlutterAppShortcut: No project found for editor " + editor);
      launchProject(project, mode);
   }

   @Override
   public void launch(final ISelection selection, final String mode) {
      IProject project = null;
      if (selection instanceof final StructuredSelection structuredSel //
            && structuredSel.getFirstElement() instanceof final IResource res) {
         project = res.getProject();
      }

      if (project == null)
         throw new IllegalArgumentException("RunFlutterAppShortcut: No project found for selection " + selection);
      launchProject(project, mode);
   }

   private void launchProject(final IProject project, final String mode) {
      final var launchMgr = DebugPlugin.getDefault().getLaunchManager();
      final var launchConfigType = launchMgr.getLaunchConfigurationType(FlutterAppLaunchConfigurations.LAUNCH_CONFIGURATION_ID);

      try {
         // search most recently launched configs for a matching one
         for (final var launch : launchMgr.getLaunches()) {
            final var cfg = launch.getLaunchConfiguration();
            if (cfg != null && cfg.getType().equals(launchConfigType) //
                  && project.getName().equalsIgnoreCase(LaunchConfigurations.getProjectName(cfg)) //
            ) {
               DebugUITools.launch(cfg, mode);
               return;
            }
         }

         // search all created launch configs for a matching one
         for (final var cfg : launchMgr.getLaunchConfigurations(launchConfigType)) {
            if (project.getName().equalsIgnoreCase(LaunchConfigurations.getProjectName(cfg))) {
               DebugUITools.launch(cfg, mode);
               return;
            }
         }

         // create a new launch config
         final var newLaunchConfig = FlutterAppLaunchConfigurations.create(project);
         final String groupId = "debug".equals(mode) ? IDebugUIConstants.ID_DEBUG_LAUNCH_GROUP
               : FlutterLaunchConfigurations.LAUNCH_FLUTTER_GROUP;
         if (Window.OK == DebugUITools.openLaunchConfigurationDialog(UI.getShell(), newLaunchConfig, groupId, null)) {
            newLaunchConfig.doSave();
         }
      } catch (final CoreException ex) {
         Dialogs.showStatus(Messages.Launch_CreatingLaunchConfigFailed, Dart4EPlugin.status().createError(ex), true);
      }
   }
}
