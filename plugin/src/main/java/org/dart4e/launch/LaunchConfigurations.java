/*
 * SPDX-FileCopyrightText: © The Dart4E authors
 * SPDX-FileContributor: Sebastian Thomschke
 * SPDX-License-Identifier: EPL-2.0
 * SPDX-ArtifactOfProjectHomePage: https://github.com/dart4e/dart4e
 */
package org.dart4e.launch;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.dart4e.Dart4EPlugin;
import org.dart4e.model.DartSDK;
import org.dart4e.prefs.DartWorkspacePreference;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.debug.ui.IDebugUIConstants;
import org.eclipse.debug.ui.RefreshTab;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.lsp4e.debug.DSPPlugin;

import de.sebthom.eclipse.commons.resources.Projects;
import de.sebthom.eclipse.commons.resources.Resources;
import net.sf.jstuff.core.io.MoreFiles;

/**
 * @author Sebastian Thomschke
 */
@SuppressWarnings("restriction")
public abstract class LaunchConfigurations {

   /** id of <launchGroup/> as specified in plugin.xml */
   public static final String LAUNCH_DART_GROUP = "org.dart4e.launch.dart.group";

   private static final String LAUNCH_ATTR_PROJECT = "launch.dart.project";
   private static final String LAUNCH_ATTR_DART_MAIN_FILE = "launch.dart.dart_main_file";
   private static final String LAUNCH_ATTR_DART_SDK = "launch.dart.sdk";
   private static final String LAUNCH_ATTR_HOT_RELOAD_ON_SAVE = "launch.dart.hot_reload_on_save";
   private static final String LAUNCH_ATTR_PROGRAM_ARGS = "launch.dart.program_args";
   private static final String LAUNCH_ATTR_VM_ARGS = "launch.dart.vm_args";

   /**
    * Used to get/set the associated project name from a Debug Console's process, e.g.
    *
    * <pre>
    * debugConsole.getProcess().getAttribute(LaunchConfigurations.PROCESS_ATTRIBUTE_PROJECT_NAME);
    * </pre>
    */
   public static final String PROCESS_ATTRIBUTE_PROJECT_NAME = "project_name";

   public static boolean isAppendEnvVars(final ILaunchConfiguration config) throws CoreException {
      return config.getAttribute(ILaunchManager.ATTR_APPEND_ENVIRONMENT_VARIABLES, true);
   }

   public static void setOrRemoveAttribute(final ILaunchConfigurationWorkingCopy config, final String attrName,
         @Nullable final String value) {
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
      setOrRemoveAttribute(config, LAUNCH_ATTR_DART_SDK, altSDK == null ? null : altSDK.getName());
   }

   public static Map<String, String> getEnvVars(final ILaunchConfiguration config) throws CoreException {
      return config.getAttribute(ILaunchManager.ATTR_ENVIRONMENT_VARIABLES, Collections.emptyMap());
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
      setOrRemoveAttribute(config, LAUNCH_ATTR_VM_ARGS, args);
   }

   public static void setDartMainFile(final ILaunchConfigurationWorkingCopy config, @Nullable final IFile dartFile) {
      setOrRemoveAttribute(config, LAUNCH_ATTR_DART_MAIN_FILE, dartFile == null ? null : dartFile.getProjectRelativePath().toString());
   }

   public static boolean isMonitorDebugAdapter(final ILaunchConfiguration config) throws CoreException {
      return config.getAttribute(DSPPlugin.ATTR_DSP_MONITOR_DEBUG_ADAPTER, true);
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
      setOrRemoveAttribute(config, LAUNCH_ATTR_PROGRAM_ARGS, args);
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
      setOrRemoveAttribute(config, LAUNCH_ATTR_PROJECT, project == null ? null : project.getName());
   }

   public static Path getWorkingDirectory(final ILaunchConfiguration config) throws CoreException {
      final var workdir = config.getAttribute(DebugPlugin.ATTR_WORKING_DIRECTORY, "");
      if (!workdir.isEmpty())
         return Paths.get(workdir);

      final var project = getProject(config);
      return project == null //
            ? MoreFiles.getWorkingDirectory()
            : Resources.toAbsolutePath(project);
   }

   public static boolean isHotReloadOnSave(final ILaunchConfiguration config) {
      try {
         return config.getAttribute(LAUNCH_ATTR_HOT_RELOAD_ON_SAVE, true);
      } catch (final Exception ex) {
         Dart4EPlugin.log().error(ex);
         return true;
      }
   }

   public static void setHotReloadOnSave(final ILaunchConfigurationWorkingCopy config, final boolean hotReloadOnSave) {
      config.setAttribute(LAUNCH_ATTR_HOT_RELOAD_ON_SAVE, hotReloadOnSave);
   }
}
