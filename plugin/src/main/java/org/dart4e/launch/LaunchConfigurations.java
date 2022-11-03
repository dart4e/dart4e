/*
 * Copyright 2022 by the Dart4E authors.
 * SPDX-License-Identifier: EPL-2.0
 */
package org.dart4e.launch;

import java.util.List;

import org.dart4e.Dart4EPlugin;
import org.dart4e.model.DartSDK;
import org.dart4e.prefs.DartWorkspacePreference;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.ui.IDebugUIConstants;
import org.eclipse.debug.ui.RefreshTab;
import org.eclipse.jdt.annotation.Nullable;

import de.sebthom.eclipse.commons.resources.Projects;

/**
 * @author Sebastian Thomschke
 */
public abstract class LaunchConfigurations {

   private static final String LAUNCH_ATTR_PROJECT = "launch.dart.project";
   private static final String LAUNCH_ATTR_DART_MAIN_FILE = "launch.dart.dart_main_file";
   private static final String LAUNCH_ATTR_DART_SDK = "launch.dart.sdk";
   private static final String LAUNCH_ATTR_PROGRAM_ARGS = "launch.dart.program_args";
   private static final String LAUNCH_ATTR_VM_ARGS = "launch.dart.vm_args";

   public static void setAttribute(final ILaunchConfigurationWorkingCopy config, final String attrName, @Nullable final String value) {
      if (value == null) {
         config.removeAttribute(attrName);
      } else {
         config.setAttribute(attrName, value);
      }
   }

   public static void setAutoRefreshProject(final ILaunchConfigurationWorkingCopy config) {
      config.setAttribute(RefreshTab.ATTR_REFRESH_SCOPE, "${project}");
      config.setAttribute(RefreshTab.ATTR_REFRESH_RECURSIVE, true);
   }

   public static @Nullable DartSDK getAlternativeDartSDK(final ILaunchConfiguration config) {
      try {
         return DartWorkspacePreference.getDartSDK(config.getAttribute(LAUNCH_ATTR_DART_SDK, ""));
      } catch (final Exception ex) {
         Dart4EPlugin.log().error(ex);
         return null;
      }
   }

   public static void setAlternativeDartSDK(final ILaunchConfigurationWorkingCopy config, @Nullable final DartSDK altSDK) {
      setAttribute(config, LAUNCH_ATTR_DART_SDK, altSDK == null ? null : altSDK.getName());
   }

   public static void setFavoriteGroups(final ILaunchConfigurationWorkingCopy config, final String... groupIDs) {
      config.setAttribute(IDebugUIConstants.ATTR_FAVORITE_GROUPS, List.of(groupIDs));
   }

   public static @Nullable IFile getDartMainFile(final ILaunchConfiguration config) {
      final var project = getProject(config);
      if (project == null)
         return null;
      final var dartMainFilePath = getDartMainFilePath(config);
      if (dartMainFilePath == null)
         return null;
      final var dartMainFile = project.getFile(dartMainFilePath);
      return dartMainFile;
   }

   public static @Nullable String getDartMainFilePath(final ILaunchConfiguration config) {
      try {
         final var dartMainFilePath = config.getAttribute(LAUNCH_ATTR_DART_MAIN_FILE, "");
         return dartMainFilePath.isEmpty() ? null : dartMainFilePath;
      } catch (final Exception ex) {
         Dart4EPlugin.log().error(ex);
         return null;
      }
   }

   public static String getDartVMArgs(final ILaunchConfiguration config) {
      try {
         return config.getAttribute(LAUNCH_ATTR_VM_ARGS, "");
      } catch (final Exception ex) {
         Dart4EPlugin.log().error(ex);
         return "";
      }
   }

   public static void setDartVMArgs(final ILaunchConfigurationWorkingCopy config, @Nullable final String args) {
      setAttribute(config, LAUNCH_ATTR_VM_ARGS, args);
   }

   public static void setDartMainFile(final ILaunchConfigurationWorkingCopy config, @Nullable final IFile dartFile) {
      setAttribute(config, LAUNCH_ATTR_DART_MAIN_FILE, dartFile == null ? null : dartFile.getProjectRelativePath().toString());
   }

   public static String getProgramArgs(final ILaunchConfiguration config) {
      try {
         return config.getAttribute(LAUNCH_ATTR_PROGRAM_ARGS, "");
      } catch (final Exception ex) {
         Dart4EPlugin.log().error(ex);
         return "";
      }
   }

   public static void setProgramArgs(final ILaunchConfigurationWorkingCopy config, @Nullable final String args) {
      setAttribute(config, LAUNCH_ATTR_PROGRAM_ARGS, args);
   }

   public static @Nullable IProject getProject(final ILaunchConfiguration config) {
      return Projects.getProject(getProjectName(config));
   }

   public static @Nullable String getProjectName(final ILaunchConfiguration config) {
      try {
         final var projectName = config.getAttribute(LAUNCH_ATTR_PROJECT, "");
         return projectName.isEmpty() ? null : projectName;
      } catch (final Exception ex) {
         Dart4EPlugin.log().error(ex);
         return null;
      }
   }

   public static void setProject(final ILaunchConfigurationWorkingCopy config, @Nullable final IProject project) {
      setAttribute(config, LAUNCH_ATTR_PROJECT, project == null ? null : project.getName());
   }
}
