/*
 * SPDX-FileCopyrightText: © The Dart4E authors
 * SPDX-FileContributor: Sebastian Thomschke
 * SPDX-License-Identifier: EPL-2.0
 * SPDX-ArtifactOfProjectHomePage: https://github.com/dart4e/dart4e
 */
package org.dart4e.navigation;

import org.eclipse.ui.IWindowListener;
import org.eclipse.ui.IWorkbenchWindow;

import de.sebthom.eclipse.commons.ui.UI;

/**
 * @author Sebastian Thomschke
 */
public class WindowListener implements IWindowListener {
   public static final WindowListener INSTANCE = new WindowListener();

   @Override
   public void windowActivated(final IWorkbenchWindow window) {
      // do interesting stuff
   }

   @Override
   public void windowDeactivated(final IWorkbenchWindow window) {
   }

   @Override
   public void windowClosed(final IWorkbenchWindow window) {
   }

   @Override
   public void windowOpened(final IWorkbenchWindow window) {
   }

   public void attach() {
      UI.getWorkbench().addWindowListener(this);
   }

   public void detatch() {
      UI.getWorkbench().removeWindowListener(this);
   }
}
