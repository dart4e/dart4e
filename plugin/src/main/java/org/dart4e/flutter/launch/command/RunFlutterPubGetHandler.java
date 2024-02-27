/*
 * SPDX-FileCopyrightText: © The Dart4E authors
 * SPDX-FileContributor: Sebastian Thomschke
 * SPDX-License-Identifier: EPL-2.0
 * SPDX-ArtifactOfProjectHomePage: https://github.com/dart4e/dart4e
 */
package org.dart4e.flutter.launch.command;

import org.dart4e.localization.Messages;

/**
 * @author Sebastian Thomschke
 */
public class RunFlutterPubGetHandler extends AbstractFlutterCommandHandler {

   public RunFlutterPubGetHandler() {
      super(Messages.Label_Flutter_Pub_Get, "pub", "get");
   }
}
