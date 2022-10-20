/*
 * Copyright 2022 by the Dart4E authors.
 * SPDX-License-Identifier: EPL-2.0
 */
package org.dart4e.tests.project;

import static org.assertj.core.api.Assertions.*;

import org.dart4e.localization.Messages;
import org.eclipse.reddeer.junit.runner.RedDeerSuite;
import org.eclipse.reddeer.requirements.cleanworkspace.CleanWorkspaceRequirement;
import org.eclipse.reddeer.swt.impl.menu.ShellMenu;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * @author Sebastian Thomschke
 */
@RunWith(RedDeerSuite.class)
public class NewDartProjectTest {

   private CleanWorkspaceRequirement cwr = new CleanWorkspaceRequirement();

   @Before
   public void setup() {
      cwr.fulfill();
   }

   @After
   public void teardown() {
      cwr.fulfill();
   }

   @Test
   public void testMenuEntriesAvailable() {
      assertThat(new ShellMenu().hasItem("File", "New", Messages.Label_Dart_Project)).isTrue();
      assertThat(new ShellMenu().hasItem("File", "New", Messages.Label_Dart_File)).isTrue();
   }
}
