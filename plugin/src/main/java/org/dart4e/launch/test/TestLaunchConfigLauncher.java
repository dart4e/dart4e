/*
 * SPDX-FileCopyrightText: © The Dart4E authors
 * SPDX-FileContributor: Sebastian Thomschke
 * SPDX-License-Identifier: EPL-2.0
 * SPDX-ArtifactOfProjectHomePage: https://github.com/dart4e/dart4e
 */
package org.dart4e.launch.test;

import static java.util.Collections.singletonList;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.dart4e.Constants;
import org.dart4e.Dart4EPlugin;
import org.dart4e.launch.LaunchConfigurations;
import org.dart4e.launch.LaunchDebugConfig;
import org.dart4e.localization.Messages;
import org.dart4e.prefs.DartProjectPreference;
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
public class TestLaunchConfigLauncher extends LaunchConfigurationDelegate {

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

      final var testResources = config.getAttribute(TestLaunchConfigurations.LAUNCH_ATTR_DART_TEST_RESOURCES, singletonList(
         Constants.PROJECT_TEST_DIRNAME));

      final var workdir = LaunchConfigurations.getWorkingDirectory(config);
      final var envVars = LaunchConfigurations.getEnvVars(config);
      final var appendEnvVars = LaunchConfigurations.isAppendEnvVars(config);

      final var programArgs = SystemUtils.splitCommandLine(LaunchConfigurations.getProgramArgs(config));
      final var vmArgs = SystemUtils.splitCommandLine(LaunchConfigurations.getDartVMArgs(config));

      switch (mode) {

         case ILaunchManager.DEBUG_MODE:
            // https://github.com/dart-lang/sdk/blob/main/third_party/pkg/dap/tool/README.md#launchattach-arguments
            final var debuggerOpts = new TreeBuilder<String>() //
               .put("cwd", workdir.toString()) //
               .put("program", testResources.get(0))
               // TODO appendEnvVars handling, i.e. running with a clean env is not supported (yet) by Dart Debug Adapter
               .put("env", envVars) //
               // .put("sendLogsToClient", true) // has performance implications, intended for troubleshooting
               .put("args", programArgs) //
               .put("toolArgs", testResources) //
               .put("vmAdditionalArgs", vmArgs) //
               .getMap();

            try {
               final var builder = new DSPLaunchDelegateLaunchBuilder(config, ILaunchManager.DEBUG_MODE, launch, monitor) //
                  .setLaunchDebugAdapter( //
                     dartSDK.getDartExecutable().toString(), //
                     List.of("debug_adapter", "--test")) //
                  .setMonitorDebugAdapter(LaunchConfigurations.isMonitorDebugAdapter(config)) //
                  .setDspParameters(debuggerOpts);
               new LaunchDebugConfig(project, LaunchConfigurations.isHotReloadOnSave(config)).launch(builder);
            } catch (final CoreException ex) {
               Dialogs.showStatus("Failed to start debug session", Dart4EPlugin.status().createError(ex), true);
            }
            return;

         case ILaunchManager.RUN_MODE:
            final var args = new ArrayList<Object>(vmArgs);
            args.add("test");
            args.add("--chain-stack-traces");
            args.addAll(testResources);
            args.addAll(programArgs);
            try {
               final var proc = dartSDK.getDartProcessBuilder(!appendEnvVars) //
                  .withArgs(args.toArray()) //
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
               final var processHandle = DebugPlugin.newProcess(launch, proc.getProcess(), dartSDK.getDartExecutable().toString());
               processHandle.setAttribute(LaunchConfigurations.PROCESS_ATTRIBUTE_PROJECT_NAME, project.getName());
               launch.addProcess(processHandle);
            } catch (final IOException ex) {
               Dialogs.showStatus(Messages.Launch_CouldNotRunDart, Dart4EPlugin.status().createError(ex), true);
            }
            return;

         default:
            UI.run(() -> MessageDialog.openError(null, "Unsupported launch mode", "Launch mode [" + mode + "] is not supported."));
      }
   }
}
