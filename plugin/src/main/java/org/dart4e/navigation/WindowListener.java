/*
 * Copyright 2022 by the Dart4E authors.
 * SPDX-License-Identifier: EPL-2.0
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
