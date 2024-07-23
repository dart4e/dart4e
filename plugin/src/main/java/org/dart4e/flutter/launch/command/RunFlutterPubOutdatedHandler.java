/*
 * SPDX-FileCopyrightText: © The Dart4E authors
 * SPDX-FileContributor: Sebastian Thomschke
 * SPDX-License-Identifier: EPL-2.0
 * SPDX-ArtifactOfProjectHomePage: https://github.com/dart4e/dart4e
 */
package org.dart4e.flutter.launch.command;

import org.dart4e.localization.Messages;

/**
 * This class is registered via the plugin.xml
 *
 * @author Sebastian Thomschke
 */
public class RunFlutterPubOutdatedHandler extends AbstractFlutterCommandHandler {

   public RunFlutterPubOutdatedHandler() {
      super(Messages.Label_Flutter_Pub_Outdated, "pub", "outdated");
   }
}
