/*
 * Copyright 2022 by the Dart4E authors.
 * SPDX-License-Identifier: EPL-2.0
 */
package org.dart4e.launch.command;

import java.util.List;

import org.dart4e.Constants;
import org.dart4e.prefs.DartProjectPreference;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.ui.IDebugUIConstants;
import org.eclipse.debug.ui.RefreshTab;

/**
 * @author Sebastian Thomschke
 */
public abstract class CommandLaunchConfigurations {

   public static ILaunchConfigurationWorkingCopy create(final IProject project) throws CoreException {
      final var launchMgr = DebugPlugin.getDefault().getLaunchManager();
      final var launchConfigType = launchMgr.getLaunchConfigurationType(Constants.LAUNCH_DART_COMMAND_CONFIGURATION_ID);
      final var launchConfigName = project.getName();
      final var newLaunchConfig = launchConfigType.newInstance(null, //
         launchMgr.generateLaunchConfigurationName(launchConfigName));
      initialize(newLaunchConfig, project);
      return newLaunchConfig;
   }

   public static void initialize(final ILaunchConfigurationWorkingCopy config) {
      config.setAttribute(RefreshTab.ATTR_REFRESH_SCOPE, "${project}");
      config.setAttribute(RefreshTab.ATTR_REFRESH_RECURSIVE, true);
      config.setAttribute(IDebugUIConstants.ATTR_FAVORITE_GROUPS, List.of(Constants.LAUNCH_DART_GROUP));
   }

   public static void initialize(final ILaunchConfigurationWorkingCopy config, final IProject project) {
      initialize(config);
      config.setAttribute(Constants.LAUNCH_ATTR_PROJECT, project.getName());
      final var altSDK = DartProjectPreference.get(project).getAlternateDartSDK();
      if (altSDK != null) {
         config.setAttribute(Constants.LAUNCH_ATTR_DART_SDK, altSDK.getName());
      }
      config.setAttribute(Constants.LAUNCH_ATTR_PROGRAM_ARGS, "pub get");
   }
}
