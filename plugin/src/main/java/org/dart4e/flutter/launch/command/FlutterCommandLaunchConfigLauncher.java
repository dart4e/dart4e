/*
 * SPDX-FileCopyrightText: © The Dart4E authors
 * SPDX-FileContributor: Sebastian Thomschke
 * SPDX-License-Identifier: EPL-2.0
 * SPDX-ArtifactOfProjectHomePage: https://github.com/dart4e/dart4e
 */
package org.dart4e.flutter.launch.command;

import java.io.IOException;

import org.dart4e.Dart4EPlugin;
import org.dart4e.flutter.prefs.FlutterProjectPreference;
import org.dart4e.launch.LaunchConfigurations;
import org.dart4e.localization.Messages;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.debug.core.RefreshUtil;
import org.eclipse.debug.core.model.LaunchConfigurationDelegate;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.jface.dialogs.MessageDialog;

import de.sebthom.eclipse.commons.ui.Consoles;
import de.sebthom.eclipse.commons.ui.Dialogs;
import de.sebthom.eclipse.commons.ui.UI;
import net.sf.jstuff.core.SystemUtils;

/**
 * This class is registered via the plugin.xml
 *
 * @author Sebastian Thomschke
 */
public class FlutterCommandLaunchConfigLauncher extends LaunchConfigurationDelegate {

   @Override
   public void launch(final ILaunchConfiguration config, final String mode, final ILaunch launch, final @Nullable IProgressMonitor monitor)
      throws CoreException {

      final var project = LaunchConfigurations.getProject(config);
      if (project == null) {
         Dialogs.showError(Messages.Launch_NoProjectSelected, Messages.Launch_NoProjectSelected_Descr);
         return;
      }

      final var prefs = FlutterProjectPreference.get(project);
      final var flutterSDK = prefs.getEffectiveFlutterSDK();
      if (flutterSDK == null || !flutterSDK.isValid()) {
         Dialogs.showError(Messages.Flutter_Prefs_NoSDKRegistered_Title, Messages.Flutter_Prefs_NoSDKRegistered_Body);
         return;
      }

      final var workdir = LaunchConfigurations.getWorkingDirectory(config);
      final var envVars = LaunchConfigurations.getEnvVars(config);
      final var appendEnvVars = LaunchConfigurations.isAppendEnvVars(config);

      final var flutterArgs = SystemUtils.splitCommandLine(LaunchConfigurations.getProgramArgs(config));
      if (!flutterArgs.isEmpty() && "pub".equals(flutterArgs.get(0)) && Consoles.isAnsiColorsSupported()) {
         flutterArgs.add("--color");
      }

      switch (mode) {

         case ILaunchManager.RUN_MODE:
            try {
               final var proc = flutterSDK.getFlutterProcessBuilder(!appendEnvVars) //
                  .withArgs(flutterArgs.toArray()) //
                  .withEnvironment(env -> env.putAll(envVars)) //
                  .withWorkingDirectory(workdir) //
                  .onExit(process -> {
                     try {
                        RefreshUtil.refreshResources(config, monitor);
                     } catch (final CoreException ex) {
                        Dart4EPlugin.log().error(ex);
                     }
                  }) //
                  .start();
               final var processHandle = DebugPlugin.newProcess(launch, proc.getProcess(), flutterSDK.getFlutterExecutable().toString());
               processHandle.setAttribute(LaunchConfigurations.PROCESS_ATTRIBUTE_PROJECT_NAME, project.getName());
               launch.addProcess(processHandle);
            } catch (final IOException ex) {
               Dialogs.showStatus(Messages.Flutter_Launch_CouldNotRunFlutter, Dart4EPlugin.status().createError(ex), true);
            }
            return;

         default:
            UI.run(() -> MessageDialog.openError(null, "Unsupported launch mode", "Launch mode [" + mode + "] is not supported."));
      }
   }
}
