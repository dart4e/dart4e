/*
 * SPDX-FileCopyrightText: © The Dart4E authors
 * SPDX-FileContributor: Sebastian Thomschke
 * SPDX-License-Identifier: EPL-2.0
 * SPDX-ArtifactOfProjectHomePage: https://github.com/dart4e/dart4e
 */
package org.dart4e.tests.project;

import static org.assertj.core.api.Assertions.assertThat;

import org.dart4e.localization.Messages;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.swtbot.eclipse.finder.SWTWorkbenchBot;
import org.eclipse.swtbot.swt.finder.junit5.SWTBotJunit5Extension;
import org.eclipse.swtbot.swt.finder.utils.SWTBotPreferences;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * @author Sebastian Thomschke
 */
@ExtendWith({SWTBotJunit5Extension.class})
@TestInstance(TestInstance.Lifecycle.PER_CLASS) // so @BeforeAll can be non-static
class NewDartProjectTest {

   private SWTWorkbenchBot bot;

   @BeforeAll
   void initWorkbench() {
      try {
         SWTBotPreferences.MAX_ERROR_SCREENSHOT_COUNT = 0;
         bot = new SWTWorkbenchBot();
         // close intro view
         bot.viewByTitle("Welcome").close();
      } catch (final Exception ignored) {
         /* ignore, view absent */
      }
   }

   @BeforeEach
   @AfterEach
   void cleanWorkspace() throws CoreException {
      for (final IProject p : ResourcesPlugin.getWorkspace().getRoot().getProjects())
         if (p.exists()) {
            p.delete(true, true, null);
         }
   }

   @Test
   void testMenuEntriesAvailable() {
      assertThat(bot.menu("File").menu("New").menu(Messages.Label_Dart_Project).isEnabled()).isTrue();
      assertThat(bot.menu("File").menu("New").menu(Messages.Label_Dart_File).isEnabled()).isTrue();
   }
}
