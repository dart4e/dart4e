/*
 * SPDX-FileCopyrightText: © The Dart4E authors
 * SPDX-FileContributor: Sebastian Thomschke
 * SPDX-License-Identifier: EPL-2.0
 * SPDX-ArtifactOfProjectHomePage: https://github.com/dart4e/dart4e
 */
package org.dart4e.flutter.launch.test;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.dart4e.Dart4EPlugin;
import org.dart4e.flutter.launch.FlutterLaunchConfigurations;
import org.dart4e.flutter.prefs.FlutterProjectPreference;
import org.dart4e.launch.LaunchConfigurations;
import org.dart4e.launch.LaunchDebugConfig;
import org.dart4e.localization.Messages;
import org.dart4e.util.TreeBuilder;
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
import org.eclipse.lsp4e.debug.launcher.DSPLaunchDelegate.DSPLaunchDelegateLaunchBuilder;

import de.sebthom.eclipse.commons.ui.Dialogs;
import de.sebthom.eclipse.commons.ui.UI;
import net.sf.jstuff.core.SystemUtils;

/**
 * This class is registered via the plugin.xml
 *
 * @author Sebastian Thomschke
 */
public class FlutterTestLaunchConfigLauncher extends LaunchConfigurationDelegate {

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

      final var dartMainFile = LaunchConfigurations.getDartMainFile(config);
      final @Nullable Path dartMainFilePath;
      if (dartMainFile == null) {
         dartMainFilePath = null;
      } else {
         if (!dartMainFile.exists()) {
            Dialogs.showError("Dart file  does not exist", "The configured Dart file \"" + dartMainFile + "\" does not exist.");
            return;
         }
         dartMainFilePath = dartMainFile.getProjectRelativePath().toFile().toPath();
      }

      final var device = FlutterLaunchConfigurations.getFlutterDevice(config);

      final var workdir = LaunchConfigurations.getWorkingDirectory(config);
      final var envVars = LaunchConfigurations.getEnvVars(config);
      final var appendEnvVars = LaunchConfigurations.isAppendEnvVars(config);

      final var flutterArgs = new ArrayList<String>();
      flutterArgs.add("test");
      if (dartMainFilePath != null) {
         flutterArgs.add("--target");
         flutterArgs.add(dartMainFilePath.toString());
      }
      if (device != null) {
         flutterArgs.add("--device-id");
         flutterArgs.add(device.id);
      }

      flutterArgs.addAll(SystemUtils.splitCommandLine(LaunchConfigurations.getProgramArgs(config)));

      switch (mode) {

         case ILaunchManager.DEBUG_MODE:
            // https://github.com/flutter/flutter/blob/master/packages/flutter_tools/lib/src/debug_adapters/README.md
            final var debuggerOpts = new TreeBuilder<String>() //
               .put("cwd", workdir.toString()) //
               // TODO appendEnvVars handling, i.e. running with a clean env is not supported (yet) by Dart Debug Adapter
               .put("evaluateGettersInDebugViews", true) //
               .put("env", envVars) //
               .put("toolArgs", flutterArgs) //
               .getMap();

            try {
               final var builder = new DSPLaunchDelegateLaunchBuilder(config, ILaunchManager.DEBUG_MODE, launch, monitor) //
                  .setLaunchDebugAdapter( //
                     flutterSDK.getFlutterExecutable().toString(), //
                     List.of("debug_adapter")) //
                  .setMonitorDebugAdapter(LaunchConfigurations.isMonitorDebugAdapter(config)) //
                  .setDspParameters(debuggerOpts);
               new LaunchDebugConfig().launch(builder);
            } catch (final CoreException ex) {
               Dialogs.showStatus("Failed to start debug session", Dart4EPlugin.status().createError(ex), true);
            }
            return;

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
