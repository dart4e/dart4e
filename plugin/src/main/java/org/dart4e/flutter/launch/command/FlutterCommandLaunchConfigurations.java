/*
 * SPDX-FileCopyrightText: © The Dart4E authors
 * SPDX-FileContributor: Sebastian Thomschke
 * SPDX-License-Identifier: EPL-2.0
 * SPDX-ArtifactOfProjectHomePage: https://github.com/dart4e/dart4e
 */
package org.dart4e.flutter.launch.command;

import org.dart4e.flutter.launch.FlutterLaunchConfigurations;
import org.dart4e.flutter.prefs.FlutterProjectPreference;
import org.dart4e.launch.LaunchConfigurations;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;

/**
 * @author Sebastian Thomschke
 */
public abstract class FlutterCommandLaunchConfigurations {

   /** id of <launchConfigurationType/> as specified in plugin.xml */
   public static final String LAUNCH_CONFIGURATION_ID = "org.dart4e.flutter.launch.flutter_command";

   public static ILaunchConfigurationWorkingCopy create(final IProject project) throws CoreException {
      final var launchMgr = DebugPlugin.getDefault().getLaunchManager();
      final var launchConfigType = launchMgr.getLaunchConfigurationType(LAUNCH_CONFIGURATION_ID);
      final var newLaunchConfig = launchConfigType.newInstance(null, launchMgr.generateLaunchConfigurationName(project.getName()));
      initialize(newLaunchConfig, project);
      return newLaunchConfig;
   }

   public static void initialize(final ILaunchConfigurationWorkingCopy config) {
      LaunchConfigurations.setAutoRefreshProject(config);
      LaunchConfigurations.setFavoriteGroups(config, FlutterLaunchConfigurations.LAUNCH_FLUTTER_GROUP);
   }

   private static void initialize(final ILaunchConfigurationWorkingCopy config, final IProject project) {
      initialize(config);
      LaunchConfigurations.setProject(config, project);
      FlutterLaunchConfigurations.setAlternativeFlutterSDK(config, FlutterProjectPreference.get(project).getAlternateFlutterSDK());
   }
}
