/*
 * Copyright 2022 by the Dart4E authors.
 * SPDX-License-Identifier: EPL-2.0
 */
package org.dart4e.launch.command;

import java.io.IOException;

import org.dart4e.Dart4EPlugin;
import org.dart4e.launch.LaunchConfigurations;
import org.dart4e.localization.Messages;
import org.dart4e.prefs.DartProjectPreference;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.debug.core.RefreshUtil;
import org.eclipse.debug.core.model.LaunchConfigurationDelegate;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.osgi.util.NLS;

import de.sebthom.eclipse.commons.ui.Consoles;
import de.sebthom.eclipse.commons.ui.Dialogs;
import de.sebthom.eclipse.commons.ui.UI;
import net.sf.jstuff.core.SystemUtils;

/**
 * This class is registered via the plugin.xml
 *
 * @author Sebastian Thomschke
 */
public class CommandLaunchConfigLauncher extends LaunchConfigurationDelegate {

   @Override
   public void launch(final ILaunchConfiguration config, final String mode, final ILaunch launch, final @Nullable IProgressMonitor monitor)
      throws CoreException {

      final var project = LaunchConfigurations.getProject(config);
      if (project == null) {
         Dialogs.showError(Messages.Launch_NoProjectSelected, Messages.Launch_NoProjectSelected_Descr);
         return;
      }

      final var prefs = DartProjectPreference.get(project);
      final var dartSDK = prefs.getEffectiveDartSDK();
      if (dartSDK == null || !dartSDK.isValid()) {
         Dialogs.showError(Messages.Prefs_NoSDKRegistered_Title, Messages.Prefs_NoSDKRegistered_Body);
         return;
      }

      final var workdir = LaunchConfigurations.getWorkingDirectory(config);
      final var envVars = LaunchConfigurations.getEnvVars(config);
      final var appendEnvVars = LaunchConfigurations.isAppendEnvVars(config);

      final var dartArgs = SystemUtils.splitCommandLine(LaunchConfigurations.getProgramArgs(config));
      if (!dartArgs.isEmpty() && "pub".equals(dartArgs.get(0)) && Consoles.isAnsiColorsSupported()) {
         dartArgs.add("--color");
      }

      switch (mode) {

         case ILaunchManager.RUN_MODE:
            final var job = Job.create(NLS.bind("Running Dart command...", project.getName()), jobMonitor -> {
               try {
                  final var proc = dartSDK.getDartProcessBuilder(!appendEnvVars) //
                     .withArgs(dartArgs) //
                     .withEnvironment(env -> env.putAll(envVars)) //
                     .withWorkingDirectory(workdir) //
                     .onExit(process -> {
                        try {
                           RefreshUtil.refreshResources(config, jobMonitor);
                        } catch (final CoreException ex) {
                           Dart4EPlugin.log().error(ex);
                        }
                     }) //
                     .start();
                  final var processHandle = DebugPlugin.newProcess(launch, proc.getProcess(), dartSDK.getDartExecutable().toString());
                  processHandle.setAttribute(LaunchConfigurations.PROCESS_ATTRIBUTE_PROJECT_NAME, project.getName());
                  launch.addProcess(processHandle);
               } catch (final IOException ex) {
                  Dialogs.showStatus(Messages.Launch_CouldNotRunDart, Dart4EPlugin.status().createError(ex), true);
               }
            });
            job.schedule();
            return;

         default:
            UI.run(() -> MessageDialog.openError(null, "Unsupported launch mode", "Launch mode [" + mode + "] is not supported."));
      }
   }
}
