/*
 * SPDX-FileCopyrightText: © The Dart4E authors
 * SPDX-FileContributor: Sebastian Thomschke
 * SPDX-License-Identifier: EPL-2.0
 * SPDX-ArtifactOfProjectHomePage: https://github.com/dart4e/dart4e
 */
package org.dart4e.launch.command;

import org.dart4e.localization.Messages;

/**
 * This class is registered via the plugin.xml
 *
 * @author Sebastian Thomschke
 */
public class RunPubUpgradeMajorVersionsHandler extends AbstractDartCommandHandler {

   public RunPubUpgradeMajorVersionsHandler() {
      super(Messages.Label_Dart_Pub_UpgradeMajorVersions, "pub", "upgrade", "--major-versions");
   }
}
