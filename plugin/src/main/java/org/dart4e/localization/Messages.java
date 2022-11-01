/*
 * Copyright 2022 by the Dart4E authors.
 * SPDX-License-Identifier: EPL-2.0
 */
package org.dart4e.localization;

import static net.sf.jstuff.core.validation.NullAnalysisHelper.lazyNonNull;

import org.eclipse.osgi.util.NLS;

import de.sebthom.eclipse.commons.localization.MessagesInitializer;

/**
 * @author Sebastian Thomschke
 */
public final class Messages extends NLS {

   private static final String BUNDLE_NAME = Messages.class.getPackageName() + ".messages";

   // Keys with default values directly assigned in this class are only used by Java classes.
   // Keys without default values are loaded from messages.properties, because they are also referenced in plugin.xml

   // CHECKSTYLE:IGNORE .* FOR NEXT 100 LINES

   public static String Label_Dart_Command_Configuration = "Dart Command Configuration";
   public static String Label_Dart_Program_Configuration = "Dart Program Configuration";
   public static String Label_Dart_Test_Configuration = "Dart Test Configuration";
   public static String Label_Dart_SDK = "Dart SDK";
   public static String Label_Dart_File = lazyNonNull();
   public static String Label_Dart_Project = lazyNonNull();
   public static String Label_Dart_Pub_Get = lazyNonNull();
   public static String Label_Dart_Pub_Downgrade = lazyNonNull();
   public static String Label_Dart_Pub_Upgrade = lazyNonNull();
   public static String Label_Name = "Name";
   public static String Label_Version = "Version";
   public static String Label_Path = "Path";
   public static String Label_Project = "Project";
   public static String Label_Add = "Add";
   public static String Label_Browse = "Browse...";
   public static String Label_Edit = "Edit";
   public static String Label_Remove = "Remove";
   public static String Label_Default = "Default";
   public static String Label_Alternative = "Alternative";

   public static String Error_CaseVariantExistsError = "The file system is not case sensitive. An existing file or directory conflicts with \"{0}\".";
   public static String Error_ValueMustBeSpecified = "\"{0}\" must be specified.";
   public static String Error_UnexpectedError = "Unexpected error: {0}";
   public static String Error_InvalidProjectName = "Invalid project name. See https://dart.dev/tools/pub/pubspec#name for more information.";
   public static String Error_ProjectCreationProblem = "Project creation problem";

   public static String SDKPathInvalid = "{0} Path invalid.";
   public static String SDKPathInvalid_Descr = "\"{0}\" does not point to a valid {1}";

   public static String Prefs_GeneralDescription = "General settings for Dart development:";
   public static String Prefs_ManageSDKsDescription = "Manage installed Dart SDKs. By default the checked SDK will be used for newly created Dart projects:";
   public static String Prefs_NoSDKRegistered_Title = "Dart SDK missing";
   public static String Prefs_NoSDKRegistered_Body = "No Dart SDK configured";
   public static String Prefs_SDKPath = "Dart SDK path";
   public static String Prefs_SDKPathInvalid_Message = "No valid Dart SDK seems to be installed on your system.\n\nPlease ensure that the Dart SDK is installed and is on your PATH.";
   public static String Prefs_SavingPreferencesFailed = "Saving preferences failed.";

   public static String NewDartFile = "New Dart File";
   public static String NewDartFile_Descr = lazyNonNull();
   public static String NewDartFile_Creating = "Creating Dart file \"{0}\"...";
   public static String NewDartFile_OpeningInEditor = "Opening Dart file in editor...";
   public static String NewDartFile_DirectoryDoesNotExist = "Directory \"{0}\" does not exist.";

   public static String NewDartProject = "New Dart Project";
   public static String NewDartProject_Descr = lazyNonNull();
   public static String NewDartProject_SDKNotFound_Message = "No valid Dart SDK found! Please specify a valid SDK on the Dart preference page.";

   public static String Launch_NoProjectSelected = "No project selected";
   public static String Launch_NoProjectSelected_Descr = "Please select a project to launch";
   public static String Launch_SDKPath_Descr = "The path where the Dart SDK is installed";
   public static String Launch_CouldNotRunDart = "Could not run Dart";
   public static String Launch_InitializingLaunchConfigTabFailed = "Initializing LaunchConfigTab failed";
   public static String Launch_CreatingLaunchConfigFailed = "Creating new launch configuration failed";

   static {
      MessagesInitializer.initializeMessages(BUNDLE_NAME, Messages.class);
   }

   private Messages() {
   }
}
