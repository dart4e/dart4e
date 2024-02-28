/*
 * SPDX-FileCopyrightText: © The Dart4E authors
 * SPDX-FileContributor: Sebastian Thomschke
 * SPDX-License-Identifier: EPL-2.0
 * SPDX-ArtifactOfProjectHomePage: https://github.com/dart4e/dart4e
 */
package org.dart4e.launch.command;

import org.dart4e.flutter.launch.command.RunFlutterPubDowngradeHandler;
import org.dart4e.flutter.project.FlutterProjectNature;
import org.dart4e.localization.Messages;
import org.eclipse.core.resources.IProject;

import de.sebthom.eclipse.commons.resources.Projects;

/**
 * This class is registered via the plugin.xml
 *
 * @author Sebastian Thomschke
 */
public class RunPubDowngradeHandler extends AbstractDartCommandHandler {

   public RunPubDowngradeHandler() {
      super(Messages.Label_Dart_Pub_Downgrade, "pub", "downgrade");
   }

   @Override
   public void runDartCommand(final IProject project) {
      if (Projects.hasNature(project, FlutterProjectNature.NATURE_ID)) {
         new RunFlutterPubDowngradeHandler().runFlutterCommand(project);
      } else {
         super.runDartCommand(project);
      }
   }
}
