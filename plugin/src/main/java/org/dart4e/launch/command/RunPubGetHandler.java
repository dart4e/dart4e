/*
 * SPDX-FileCopyrightText: © The Dart4E authors
 * SPDX-FileContributor: Sebastian Thomschke
 * SPDX-License-Identifier: EPL-2.0
 * SPDX-ArtifactOfProjectHomePage: https://github.com/dart4e/dart4e
 */
package org.dart4e.launch.command;

import org.dart4e.flutter.launch.command.RunFlutterPubGetHandler;
import org.dart4e.flutter.project.FlutterProjectNature;
import org.dart4e.localization.Messages;
import org.eclipse.core.resources.IProject;

import de.sebthom.eclipse.commons.resources.Projects;

/**
 * This class is registered via the plugin.xml
 *
 * @author Sebastian Thomschke
 */
public class RunPubGetHandler extends AbstractDartCommandHandler {

   public RunPubGetHandler() {
      super(Messages.Label_Dart_Pub_Get, "pub", "get");
   }

   @Override
   public void runDartCommand(final IProject project) {
      if (Projects.hasNature(project, FlutterProjectNature.NATURE_ID)) {
         new RunFlutterPubGetHandler().runFlutterCommand(project);
      } else {
         super.runDartCommand(project);
      }
   }
}
