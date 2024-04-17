/*
 * SPDX-FileCopyrightText: © The Dart4E authors
 * SPDX-FileContributor: Sebastian Thomschke
 * SPDX-License-Identifier: EPL-2.0
 * SPDX-ArtifactOfProjectHomePage: https://github.com/dart4e/dart4e
 */
package org.dart4e.localization;

import static net.sf.jstuff.core.validation.NullAnalysisHelper.lateNonNull;

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
   public static String Label_Dart_File = lateNonNull();
   public static String Label_Dart_Project = lateNonNull();
   public static String Label_Dart_Pub_Get = lateNonNull();
   public static String Label_Dart_Pub_Downgrade = lateNonNull();
   public static String Label_Dart_Pub_Upgrade = lateNonNull();
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

   public static String Prefs_GeneralDescription = "General settings for Dart development:";
   public static String Prefs_ManageSDKsTitle = "Dart SDKs";
   public static String Prefs_ManageSDKsDescription = "Manage installed Dart SDKs. By default the checked SDK will be used for newly created Dart projects:";
   public static String Prefs_NoSDKRegistered_Title = "Dart SDK missing";
   public static String Prefs_NoSDKRegistered_Body = "No Dart SDK configured";
   public static String Prefs_SDKPath = "Dart SDK path";
   public static String Prefs_SDKPathInvalid = "Dart SDK path invalid.";
   public static String Prefs_SDKPathInvalid_Descr = "\"{0}\" does not point to a valid Dart SDK";
   public static String Prefs_SDKPathInvalid_Message = "No valid Dart SDK seems to be installed on your system.\n\nPlease ensure that the Dart SDK is installed and is on your PATH.";
   public static String Prefs_SavingPreferencesFailed = "Saving preferences failed.";

   public static String NewDartFile = "New Dart File";
   public static String NewDartFile_Descr = lateNonNull();
   public static String NewDartFile_Creating = "Creating Dart file \"{0}\"...";
   public static String NewDartFile_OpeningInEditor = "Opening Dart file in editor...";
   public static String NewDartFile_DirectoryDoesNotExist = "Directory \"{0}\" does not exist.";

   public static String NewDartProject = "New Dart Project";
   public static String NewDartProject_Descr = lateNonNull();
   public static String NewDartProject_SDKNotFound_Message = "No valid Dart SDK found! Please specify a valid SDK on the Dart preference page.";

   public static String Launch_NoProjectSelected = "No project selected";
   public static String Launch_NoProjectSelected_Descr = "Please select a project to launch";
   public static String Launch_SDKPath_Descr = "The path where the Dart SDK is installed";
   public static String Launch_CouldNotRunDart = "Could not run Dart";
   public static String Launch_InitializingLaunchConfigTabFailed = "Initializing LaunchConfigTab failed";
   public static String Launch_CreatingLaunchConfigFailed = "Creating new launch configuration failed";

   /*
    * Flutter specific
    */
   public static String Label_Flutter_SDK = "Flutter SDK";
   public static String Label_Flutter_Project = lateNonNull();
   public static String Label_Flutter_App_Configuration = "Flutter App Configuration";
   public static String Label_Flutter_Command_Configuration = "Flutter Command Configuration";
   public static String Label_Flutter_Test_Configuration = "Flutter Test Configuration";
   public static String Label_Flutter_Pub_Get = lateNonNull();
   public static String Label_Flutter_Pub_Downgrade = lateNonNull();
   public static String Label_Flutter_Pub_Upgrade = lateNonNull();

   public static String Flutter_Launch_CouldNotRunFlutter = "Could not run Flutter";

   public static String Flutter_Prefs_GeneralDescription = Prefs_GeneralDescription.replace("Dart", "Flutter");
   public static String Flutter_Prefs_ManageSDKsTitle = "Flutter SDKs";
   public static String Flutter_Prefs_ManageSDKsDescription = Prefs_ManageSDKsDescription.replace("Dart", "Flutter");
   public static String Flutter_Prefs_NoSDKRegistered_Title = Prefs_NoSDKRegistered_Title.replace("Dart", "Flutter");
   public static String Flutter_Prefs_NoSDKRegistered_Body = Prefs_NoSDKRegistered_Body.replace("Dart", "Flutter");
   public static String Flutter_Prefs_SDKPath = Prefs_SDKPath.replace("Dart", "Flutter");
   public static String Flutter_Prefs_SDKPathInvalid = Prefs_SDKPathInvalid.replace("Dart", "Flutter");
   public static String Flutter_Prefs_SDKPathInvalid_Descr = Prefs_SDKPathInvalid_Descr.replace("Dart", "Flutter");
   public static String Flutter_Prefs_SDKPathInvalid_Message = Prefs_SDKPathInvalid_Message.replace("Dart", "Flutter");

   public static String Flutter_NewProject = "New Flutter Project";
   public static String Flutter_NewProject_Descr = lateNonNull();
   public static String Flutter_NewProject_SDKNotFound_Message = "No valid Flutter SDK found! Please specify a valid SDK on the Flutter preference page.";

   static {
      MessagesInitializer.initializeMessages(BUNDLE_NAME, Messages.class);
   }

   private Messages() {
   }
}
