/*
 * Copyright 2022 by the Dart4E authors.
 * SPDX-License-Identifier: EPL-2.0
 */
package org.dart4e.launch.program;

import static net.sf.jstuff.core.validation.NullAnalysisHelper.asNonNull;

import org.dart4e.Dart4EPlugin;
import org.dart4e.launch.LaunchConfigurations;
import org.dart4e.localization.Messages;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.ui.DebugUITools;
import org.eclipse.debug.ui.IDebugUIConstants;
import org.eclipse.debug.ui.ILaunchShortcut;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.window.Window;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.part.FileEditorInput;

import de.sebthom.eclipse.commons.ui.Dialogs;
import de.sebthom.eclipse.commons.ui.UI;

/**
 * @author Sebastian Thomschke
 */
public class RunDartFileShortcut implements ILaunchShortcut {

   @Override
   public void launch(final IEditorPart editor, final String mode) {
      final var editorInput = editor.getEditorInput();
      if (editorInput instanceof final FileEditorInput fileInput) {
         launchDartFile(fileInput.getFile(), mode);
      }
   }

   @Override
   public void launch(final ISelection selection, final String mode) {
      if (selection instanceof final IStructuredSelection structuredSelection) {
         final var firstElement = structuredSelection.getFirstElement();
         if (firstElement instanceof @NonNull final IFile file) {
            launchDartFile(file, mode);
         }
      }
   }

   private void launchDartFile(final IFile dartFile, final String mode) {
      final var launchMgr = DebugPlugin.getDefault().getLaunchManager();
      final var launchConfigType = launchMgr.getLaunchConfigurationType(ProgramLaunchConfigurations.LAUNCH_CONFIGURATION_ID);

      final var project = asNonNull(dartFile.getProject());
      try {
         // use an existing launch config if available
         for (final var cfg : launchMgr.getLaunchConfigurations(launchConfigType)) {
            if (project.getName().equalsIgnoreCase(LaunchConfigurations.getProjectName(cfg)) //
               && dartFile.getProjectRelativePath().toString().equals(LaunchConfigurations.getDartMainFilePath(cfg)) //
            ) {
               DebugUITools.launch(cfg, mode);
               return;
            }
         }

         // create a new launch config
         final var newLaunchConfig = ProgramLaunchConfigurations.create(dartFile);
         final String groupId = "debug".equals(mode) ? IDebugUIConstants.ID_DEBUG_LAUNCH_GROUP : LaunchConfigurations.LAUNCH_DART_GROUP;
         if (Window.OK == DebugUITools.openLaunchConfigurationDialog(UI.getShell(), newLaunchConfig, groupId, null)) {
            newLaunchConfig.doSave();
         }
      } catch (final CoreException ex) {
         Dialogs.showStatus(Messages.Launch_CreatingLaunchConfigFailed, Dart4EPlugin.status().createError(ex), true);
      }
   }
}
