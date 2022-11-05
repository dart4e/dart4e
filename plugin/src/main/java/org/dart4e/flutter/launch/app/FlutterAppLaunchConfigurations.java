/*
 * Copyright 2022 by the Dart4E authors.
 * SPDX-License-Identifier: EPL-2.0
 */
package org.dart4e.flutter.launch.app;

import static net.sf.jstuff.core.validation.NullAnalysisHelper.asNonNull;

import org.dart4e.flutter.launch.FlutterLaunchConfigurations;
import org.dart4e.flutter.prefs.FlutterProjectPreference;
import org.dart4e.launch.LaunchConfigurations;
import org.dart4e.model.buildsystem.BuildFile;
import org.dart4e.model.buildsystem.DartBuildFile;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;

/**
 * @author Sebastian Thomschke
 */
public abstract class FlutterAppLaunchConfigurations {

   /** id of <launchConfigurationType/> as specified in plugin.xml */
   public static final String LAUNCH_CONFIGURATION_ID = "org.dart4e.flutter.launch.flutter_app";

   public static ILaunchConfigurationWorkingCopy create(final IFile dartFile) throws CoreException {
      final var project = asNonNull(dartFile.getProject());

      final var launchMgr = DebugPlugin.getDefault().getLaunchManager();
      final var launchConfigType = launchMgr.getLaunchConfigurationType(LAUNCH_CONFIGURATION_ID);
      final var newLaunchConfig = launchConfigType.newInstance(null, launchMgr.generateLaunchConfigurationName(project.getName() + " ("
         + dartFile.getName() + ")"));
      initialize(newLaunchConfig, dartFile);
      return newLaunchConfig;
   }

   public static ILaunchConfigurationWorkingCopy create(final IProject project) throws CoreException {
      final var buildFile = BuildFile.of(project);
      if (buildFile instanceof final DartBuildFile dartBuildFile && !dartBuildFile.getExecutables().isEmpty())
         return create(project.getFile(dartBuildFile.getExecutables().get(0)));

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

   public static void initialize(final ILaunchConfigurationWorkingCopy config, final IFile dartFile) {
      initialize(config, asNonNull(dartFile.getProject()));
      LaunchConfigurations.setDartMainFile(config, dartFile);
   }

   public static void initialize(final ILaunchConfigurationWorkingCopy config, final IProject project) {
      initialize(config);
      LaunchConfigurations.setProject(config, project);
      FlutterLaunchConfigurations.setAlternativeFlutterSDK(config, FlutterProjectPreference.get(project).getAlternateFlutterSDK());
   }

}
