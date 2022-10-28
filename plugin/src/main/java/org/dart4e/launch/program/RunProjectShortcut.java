/*
 * Copyright 2022 by the Dart4E authors.
 * SPDX-License-Identifier: EPL-2.0
 */
package org.dart4e.launch.program;

import org.dart4e.Constants;
import org.dart4e.Dart4EPlugin;
import org.dart4e.localization.Messages;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.DebugPlugin;
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
 * @author Sebastian Thomschke
 */
public class RunProjectShortcut implements ILaunchShortcut {

   @Override
   public void launch(final IEditorPart editor, final String mode) {
      IProject project = null;
      final var editorInput = editor.getEditorInput();
      if (editorInput instanceof final IFileEditorInput fileInput) {
         project = fileInput.getFile().getProject();
      }

      if (project == null)
         throw new IllegalArgumentException("RunProjectShortcut: No project found for editor " + editor);
      launchProject(project, mode);
   }

   @Override
   public void launch(final ISelection selection, final String mode) {
      IProject project = null;
      if (selection instanceof final StructuredSelection structuredSel) {
         final var firstElement = structuredSel.getFirstElement();
         if (firstElement instanceof final IResource res) {
            project = res.getProject();
         }
      }

      if (project == null)
         throw new IllegalArgumentException("RunProjectShortcut: No project found for selection " + selection);
      launchProject(project, mode);
   }

   private void launchProject(final IProject project, final String mode) {
      final var launchMgr = DebugPlugin.getDefault().getLaunchManager();
      final var launchConfigType = launchMgr.getLaunchConfigurationType(Constants.LAUNCH_DART_CONFIGURATION_ID);

      try {
         // search most recently launched configs for a matching one
         for (final var launch : launchMgr.getLaunches()) {
            final var cfg = launch.getLaunchConfiguration();
            if (cfg != null && cfg.getType().equals(launchConfigType) //
               && cfg.getAttribute(Constants.LAUNCH_ATTR_PROJECT, "").equalsIgnoreCase(project.getName()) //
            ) {
               DebugUITools.launch(cfg, mode);
               return;
            }
         }

         // search all created launch configs for a matching one
         for (final var cfg : launchMgr.getLaunchConfigurations(launchConfigType)) {
            if (cfg.getAttribute(Constants.LAUNCH_ATTR_PROJECT, "").equalsIgnoreCase(project.getName())) {
               DebugUITools.launch(cfg, mode);
               return;
            }
         }

         // create a new launch config
         final var newLaunchConfig = LaunchConfigurations.create(project);

         if (Window.OK == DebugUITools.openLaunchConfigurationDialog(UI.getShell(), newLaunchConfig, Constants.LAUNCH_DART_GROUP, null)) {
            newLaunchConfig.doSave();
         }
      } catch (final CoreException ex) {
         Dialogs.showStatus(Messages.Launch_CreatingLaunchConfigFailed, Dart4EPlugin.status().createError(ex), true);
      }
   }
}
