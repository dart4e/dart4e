/*
 * Copyright 2022 by the Dart4E authors.
 * SPDX-License-Identifier: EPL-2.0
 */
package org.dart4e.launch.program;

import static net.sf.jstuff.core.validation.NullAnalysisHelper.asNonNull;

import java.util.List;

import org.dart4e.Constants;
import org.dart4e.model.buildsystem.DartBuildFile;
import org.dart4e.prefs.DartProjectPreference;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.ui.IDebugUIConstants;
import org.eclipse.debug.ui.RefreshTab;

/**
 * @author Sebastian Thomschke
 */
public abstract class ProgramLaunchConfigurations {

   public static ILaunchConfigurationWorkingCopy create(final IFile dartFile) throws CoreException {
      final var project = asNonNull(dartFile.getProject());

      final var launchMgr = DebugPlugin.getDefault().getLaunchManager();
      final var launchConfigType = launchMgr.getLaunchConfigurationType(Constants.LAUNCH_DART_PROGRAM_CONFIGURATION_ID);
      final var newLaunchConfig = launchConfigType.newInstance(null, launchMgr.generateLaunchConfigurationName(project.getName() + " ("
         + dartFile.getName() + ")"));
      initialize(newLaunchConfig, dartFile);
      return newLaunchConfig;
   }

   public static ILaunchConfigurationWorkingCopy create(final IProject project) throws CoreException {
      final var prefs = DartProjectPreference.get(project);
      final var buildFile = prefs.getBuildSystem().findBuildFile(project);
      if (buildFile instanceof final DartBuildFile dartBuildFile && !dartBuildFile.getExecutables().isEmpty())
         return create(project.getFile(dartBuildFile.getExecutables().get(0)));

      final var launchMgr = DebugPlugin.getDefault().getLaunchManager();
      final var launchConfigType = launchMgr.getLaunchConfigurationType(Constants.LAUNCH_DART_PROGRAM_CONFIGURATION_ID);
      final var newLaunchConfig = launchConfigType.newInstance(null, launchMgr.generateLaunchConfigurationName(project.getName()));
      initialize(newLaunchConfig, project);
      return newLaunchConfig;
   }

   public static void initialize(final ILaunchConfigurationWorkingCopy config) {
      config.setAttribute(RefreshTab.ATTR_REFRESH_SCOPE, "${project}");
      config.setAttribute(RefreshTab.ATTR_REFRESH_RECURSIVE, true);
      config.setAttribute(IDebugUIConstants.ATTR_FAVORITE_GROUPS, List.of(Constants.LAUNCH_DART_GROUP));
   }

   public static void initialize(final ILaunchConfigurationWorkingCopy config, final IFile dartFile) {
      initialize(config, asNonNull(dartFile.getProject()));
      config.setAttribute(Constants.LAUNCH_ATTR_DART_MAIN_FILE, dartFile.getProjectRelativePath().toString());
   }

   public static void initialize(final ILaunchConfigurationWorkingCopy config, final IProject project) {
      initialize(config);
      config.setAttribute(Constants.LAUNCH_ATTR_PROJECT, project.getName());
      final var altSDK = DartProjectPreference.get(project).getAlternateDartSDK();
      if (altSDK != null) {
         config.setAttribute(Constants.LAUNCH_ATTR_DART_SDK, altSDK.getName());
      }
   }
}
