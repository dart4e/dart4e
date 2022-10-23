/*
 * Copyright 2022 by the Dart4E authors.
 * SPDX-License-Identifier: EPL-2.0
 */
package org.dart4e.launch;

import static net.sf.jstuff.core.validation.NullAnalysisHelper.*;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.dart4e.Constants;
import org.dart4e.Dart4EPlugin;
import org.dart4e.localization.Messages;
import org.dart4e.model.buildsystem.BuildSystem;
import org.dart4e.prefs.DartProjectPreference;
import org.dart4e.util.TreeBuilder;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
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
import org.eclipse.lsp4e.debug.DSPPlugin;
import org.eclipse.lsp4e.debug.launcher.DSPLaunchDelegate.DSPLaunchDelegateLaunchBuilder;
import org.eclipse.osgi.util.NLS;

import de.sebthom.eclipse.commons.resources.Projects;
import de.sebthom.eclipse.commons.ui.Dialogs;
import de.sebthom.eclipse.commons.ui.UI;
import net.sf.jstuff.core.Strings;

/**
 * This class is registered via the plugin.xml
 *
 * @author Sebastian Thomschke
 */
@SuppressWarnings("restriction")
public class LaunchConfigLauncher extends LaunchConfigurationDelegate {

   /**
    * Used to get/set the associated project name from a Debug Console's process, e.g.
    *
    * <pre>
    * debugConsole.getProcess().getAttribute(LaunchConfigLauncher.PROCESS_ATTRIBUTE_PROJECT_NAME);
    * </pre>
    */
   public static final String PROCESS_ATTRIBUTE_PROJECT_NAME = "project_name";

   @Override
   public void launch(final ILaunchConfiguration config, final String mode, final ILaunch launch, final @Nullable IProgressMonitor monitor)
      throws CoreException {

      final var projectName = config.getAttribute(Constants.LAUNCH_ATTR_PROJECT, "");
      final @Nullable IProject project = Strings.isBlank(projectName) ? null : Projects.getProject(projectName);
      if (project == null || !project.exists()) {
         Dialogs.showError(Messages.Launch_NoProjectSelected, Messages.Launch_NoProjectSelected_Descr);
         return;
      }
      final var projectLoc = asNonNull(project.getLocation());

      final var prefs = DartProjectPreference.get(project);
      final var dartSDK = prefs.getEffectiveDartSDK();
      if (dartSDK == null || !dartSDK.isValid()) {
         Dialogs.showError(Messages.Prefs_NoSDKRegistered_Title, Messages.Prefs_NoSDKRegistered_Body);
         return;
      }

      final var buildSystem = prefs.getBuildSystem();
      if (buildSystem != BuildSystem.DART && buildSystem != BuildSystem.FLUTTER) {
         Dialogs.showError("Unsupported Build System", "Running code via " + buildSystem + " is not yet supported.");
         return;
      }

      final var dartMainFileProjectRelativePath = config.getAttribute(Constants.LAUNCH_ATTR_DART_MAIN_FILE, "");
      final IFile dartMainFile;
      if (Strings.isBlank(dartMainFileProjectRelativePath)) {
         Dialogs.showError("No Dart file specified", "The Dart file is configured for the launch configuration.");
         return;
      }
      dartMainFile = project.getFile(dartMainFileProjectRelativePath);
      if (!dartMainFile.exists()) {
         Dialogs.showError("Dart file  does not exist", "The configured Dart file \"" + project.getName() + "/"
            + dartMainFileProjectRelativePath + "\" does not exist.");
         return;
      }

      final var dartMainFilePath = asNonNull(dartMainFile.getLocation()).toFile().toPath().normalize().toAbsolutePath();

      final var workdir = Paths.get(config.getAttribute(DebugPlugin.ATTR_WORKING_DIRECTORY, projectLoc.toOSString()));
      final var envVars = config.getAttribute(ILaunchManager.ATTR_ENVIRONMENT_VARIABLES, Collections.emptyMap());
      final var appendEnvVars = config.getAttribute(ILaunchManager.ATTR_APPEND_ENVIRONMENT_VARIABLES, true);

      final var programArgs = Strings.splitToList(config.getAttribute(Constants.LAUNCH_ATTR_PROGRAM_ARGS, "").strip(), ' ');
      final var vmArgs = Strings.splitToList(config.getAttribute(Constants.LAUNCH_ATTR_VM_ARGS, "").strip(), ' ');

      switch (mode) {

         case ILaunchManager.DEBUG_MODE:
            // https://github.com/dart-lang/sdk/blob/main/pkg/dds/tool/dap/README.md
            final var debuggerOpts = new TreeBuilder<String>() //
               .put("cwd", workdir.toString()) //
               .put("program", dartMainFilePath.toString()) //
               // TODO appendEnvVars handling, i.e. running with a clean env is not supported (yet) by Dart Debug Adapter
               .put("env", envVars) //
               .put("args", programArgs) //
               .put("vmAdditionalArgs", vmArgs) //
               .getMap();

            try {
               final var builder = new DSPLaunchDelegateLaunchBuilder(config, ILaunchManager.DEBUG_MODE, launch, monitor);
               builder.setLaunchDebugAdapter( //
                  dartSDK.getDartExecutable().toString(), //
                  List.of("debug_adapter"));
               builder.setMonitorDebugAdapter(config.getAttribute(DSPPlugin.ATTR_DSP_MONITOR_DEBUG_ADAPTER, true));
               builder.setDspParameters(debuggerOpts);
               new LaunchDebugConfig().launch(builder);
            } catch (final CoreException ex) {
               Dialogs.showStatus("Failed to start debug session", Dart4EPlugin.status().createError(ex), true);
            }
            return;

         case ILaunchManager.RUN_MODE:
            final var args = new ArrayList<Object>(vmArgs);
            args.add("run");
            args.add(dartMainFilePath);
            args.addAll(programArgs);
            final var job = Job.create(NLS.bind(Messages.Launch_RunningFile, dartMainFile.getProjectRelativePath()), jobMonitor -> {
               try {
                  final var proc = dartSDK.getDartProcessBuilder(!appendEnvVars) //
                     .withArgs(args.toArray()) //
                     .withEnvironment(env -> env.putAll(envVars)) //
                     .withWorkingDirectory(workdir) //
                     .onExit(process -> {
                        try {
                           RefreshUtil.refreshResources(config, jobMonitor);
                        } catch (final CoreException e) {
                           Dart4EPlugin.log().error(e);
                        }
                     }) //
                     .start();
                  final var processHandle = DebugPlugin.newProcess(launch, proc.getProcess(), dartSDK.getDartExecutable().toString());
                  processHandle.setAttribute(PROCESS_ATTRIBUTE_PROJECT_NAME, project.getName());
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
