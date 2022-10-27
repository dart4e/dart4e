/*
 * Copyright 2022 by the Dart4E authors.
 * SPDX-License-Identifier: EPL-2.0
 */
package org.dart4e.launch.test;

import static java.util.Collections.singletonList;

import java.util.List;

import org.dart4e.Constants;
import org.dart4e.prefs.DartProjectPreference;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.ui.IDebugUIConstants;
import org.eclipse.debug.ui.RefreshTab;
import org.eclipse.jdt.annotation.Nullable;

/**
 * @author Sebastian Thomschke
 */
public abstract class LaunchTestConfigurations {

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
      config.setAttribute(RefreshTab.ATTR_REFRESH_SCOPE, "${project}");
      config.setAttribute(RefreshTab.ATTR_REFRESH_RECURSIVE, true);
      config.setAttribute(IDebugUIConstants.ATTR_FAVORITE_GROUPS, List.of(Constants.LAUNCH_DART_GROUP));
   }

   public static void initialize(final ILaunchConfigurationWorkingCopy config, final IProject project) {
      initialize(config, project, null);
   }

   public static void initialize(final ILaunchConfigurationWorkingCopy config, final IProject project,
      final @Nullable String testResource) {
      initialize(config);
      config.setAttribute(Constants.LAUNCH_ATTR_PROJECT, project.getName());
      final var altSDK = DartProjectPreference.get(project).getAlternateDartSDK();
      if (altSDK != null) {
         config.setAttribute(Constants.LAUNCH_ATTR_DART_SDK, altSDK.getName());
      }
      if (testResource != null) {
         config.setAttribute(Constants.LAUNCH_ATTR_DART_TEST_RESOURCES, singletonList(testResource));
      }
   }
}
