/*
 * SPDX-FileCopyrightText: © The Dart4E authors
 * SPDX-FileContributor: Sebastian Thomschke
 * SPDX-License-Identifier: EPL-2.0
 * SPDX-ArtifactOfProjectHomePage: https://github.com/dart4e/dart4e
 */
package org.dart4e.launch.command;

import org.dart4e.launch.LaunchConfigurations;
import org.dart4e.prefs.DartProjectPreference;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;

/**
 * @author Sebastian Thomschke
 */
public abstract class CommandLaunchConfigurations {

   /** id of <launchConfigurationType/> as specified in plugin.xml */
   public static final String LAUNCH_CONFIGURATION_ID = "org.dart4e.launch.dart_command";

   public static ILaunchConfigurationWorkingCopy create(final IProject project) throws CoreException {
      final var launchMgr = DebugPlugin.getDefault().getLaunchManager();
      final var launchConfigType = launchMgr.getLaunchConfigurationType(LAUNCH_CONFIGURATION_ID);
      final var launchConfigName = project.getName();
      final var newLaunchConfig = launchConfigType.newInstance(null, launchMgr.generateLaunchConfigurationName(launchConfigName));
      initialize(newLaunchConfig, project);
      return newLaunchConfig;
   }

   public static void initialize(final ILaunchConfigurationWorkingCopy config) {
      LaunchConfigurations.setAutoRefreshProject(config);
      LaunchConfigurations.setFavoriteGroups(config, LaunchConfigurations.LAUNCH_DART_GROUP);
   }

   private static void initialize(final ILaunchConfigurationWorkingCopy config, final IProject project) {
      initialize(config);
      LaunchConfigurations.setProject(config, project);
      LaunchConfigurations.setAlternativeDartSDK(config, DartProjectPreference.get(project).getAlternateDartSDK());
      LaunchConfigurations.setProgramArgs(config, "pub get");
   }
}
