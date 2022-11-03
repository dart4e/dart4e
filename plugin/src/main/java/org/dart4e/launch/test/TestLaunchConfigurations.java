/*
 * Copyright 2022 by the Dart4E authors.
 * SPDX-License-Identifier: EPL-2.0
 */
package org.dart4e.launch.test;

import static java.util.Collections.singletonList;

import org.dart4e.Constants;
import org.dart4e.launch.LaunchConfigurations;
import org.dart4e.prefs.DartProjectPreference;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.jdt.annotation.Nullable;

/**
 * @author Sebastian Thomschke
 */
public abstract class TestLaunchConfigurations {

   public static final String LAUNCH_ATTR_DART_TEST_RESOURCES = "launch.dart.dart_test_resources";

   public static ILaunchConfigurationWorkingCopy create(final IProject project) throws CoreException {
      return create(project, null);
   }

   public static ILaunchConfigurationWorkingCopy create(final IProject project, final @Nullable String testResource) throws CoreException {
      final var launchMgr = DebugPlugin.getDefault().getLaunchManager();
      final var launchConfigType = launchMgr.getLaunchConfigurationType(Constants.LAUNCH_DART_TEST_CONFIGURATION_ID);
      final var launchConfigName = testResource == null //
         ? project.getName() //
         : project.getName() + " (" + testResource + ")";
      final var newLaunchConfig = launchConfigType.newInstance(null, //
         launchMgr.generateLaunchConfigurationName(launchConfigName));
      initialize(newLaunchConfig, project, testResource);
      return newLaunchConfig;
   }

   public static void initialize(final ILaunchConfigurationWorkingCopy config) {
      LaunchConfigurations.setAutoRefreshProject(config);
      LaunchConfigurations.setFavoriteGroups(config, Constants.LAUNCH_DART_GROUP);
   }

   private static void initialize(final ILaunchConfigurationWorkingCopy config, final IProject project,
      final @Nullable String testResource) {
      initialize(config);
      LaunchConfigurations.setProject(config, project);
      LaunchConfigurations.setAlternativeDartSDK(config, DartProjectPreference.get(project).getAlternateDartSDK());
      if (testResource != null) {
         config.setAttribute(LAUNCH_ATTR_DART_TEST_RESOURCES, singletonList(testResource));
      }
   }
}
