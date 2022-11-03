/*
 * Copyright 2022 by the Dart4E authors.
 * SPDX-License-Identifier: EPL-2.0
 */
package org.dart4e;

import java.util.regex.Pattern;

/**
 * @author Sebastian Thomschke
 */
public interface Constants {

   /** see https://dart.dev/tools/pub/pubspec#name */
   Pattern VALID_PROJECT_NAME_PATTERN = Pattern.compile("^[a-z0-9_]+$");

   String IMAGE_DART_ICON = "src/main/resources/images/logo/dart_icon.png";
   String IMAGE_NAVIGATOR_DART_PROJECT = "src/main/resources/images/navigator/dart_project.png";
   String IMAGE_DART_DEPENDENCIES = "src/main/resources/images/navigator/classpath.png";
   String IMAGE_DART_FILE = "src/main/resources/images/navigator/dart_file.png";
   String IMAGE_DART_SOURCE_FOLDER = "src/main/resources/images/navigator/packagefolder_obj.png";
   String IMAGE_DART_SOURCE_PACKAGE = "src/main/resources/images/navigator/package_obj.png";
   String IMAGE_DART_WIZARD_BANNER = "src/main/resources/images/logo/dart_wizard_banner.png";
   String IMAGE_OUTLINE_SYMBOL_ENUM_MEMBER = "src/main/resources/images/outline/enum_member.png";
   String IMAGE_OUTLINE_SYMBOL_TYPEDEF = "src/main/resources/images/outline/typedef.png";
   String IMAGE_TERMINATE_BUTTON = "src/main/resources/images/console/terminate_co.png";
   String IMAGE_TERMINATE_BUTTON_DISABLED = "src/main/resources/images/console/terminate_co_disabled.png";

   String DART_FILE_EXTENSION = "dart";

   String TEST_FOLDER_NAME = "test";
   String PUBSPEC_LOCK_FILENAME = "pubspec.lock";
   String PUBSPEC_YAML_FILENAME = "pubspec.yaml";

   /** id of <launchConfigurationType/> as specified in plugin.xml */
   String LAUNCH_DART_COMMAND_CONFIGURATION_ID = "org.dart4e.launch.dart_command";

   /** id of <launchConfigurationType/> as specified in plugin.xml */
   String LAUNCH_DART_PROGRAM_CONFIGURATION_ID = "org.dart4e.launch.dart_program";

   /** id of <launchConfigurationType/> as specified in plugin.xml */
   String LAUNCH_DART_TEST_CONFIGURATION_ID = "org.dart4e.launch.dart_test";

   /** id of <launchGroup/> as specified in plugin.xml */
   String LAUNCH_DART_GROUP = "org.dart4e.launch.dart.group";
}
