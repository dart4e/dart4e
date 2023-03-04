/*
 * SPDX-FileCopyrightText: © The Dart4E authors
 * SPDX-FileContributor: Sebastian Thomschke
 * SPDX-License-Identifier: EPL-2.0
 * SPDX-ArtifactOfProjectHomePage: https://github.com/dart4e/dart4e
 */
package org.dart4e.navigation;

import org.eclipse.ui.IPartListener2;
import org.eclipse.ui.IWorkbenchPartReference;

import de.sebthom.eclipse.commons.ui.UI;

/**
 * @author Sebastian Thomschke
 */
public final class ActiveEditorChangeListener implements IPartListener2 {
   public static final ActiveEditorChangeListener INSTANCE = new ActiveEditorChangeListener();

   public void attach() {
      for (final var window : UI.getWorkbench().getWorkbenchWindows()) {
         window.getPartService().addPartListener(this);
      }
   }

   public void detach() {
      for (final var window : UI.getWorkbench().getWorkbenchWindows()) {
         window.getPartService().removePartListener(this);
      }
   }

   @Override
   public void partBroughtToTop(final IWorkbenchPartReference partRef) {
      // do interesting stuff
   }
}
