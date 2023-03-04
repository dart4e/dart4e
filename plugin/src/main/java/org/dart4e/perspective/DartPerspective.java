/*
 * SPDX-FileCopyrightText: © The Dart4E authors
 * SPDX-FileContributor: Sebastian Thomschke
 * SPDX-License-Identifier: EPL-2.0
 * SPDX-ArtifactOfProjectHomePage: https://github.com/dart4e/dart4e
 */
package org.dart4e.perspective;

import org.eclipse.ui.IPageLayout;
import org.eclipse.ui.IPerspectiveFactory;

/**
 * @author Sebastian Thomschke
 */
public class DartPerspective implements IPerspectiveFactory {

   @Override
   public void createInitialLayout(final IPageLayout layout) {
      defineLayout(layout);
      defineToolbarActions(layout);
      defineMenuActions(layout);
   }

   public void defineLayout(final IPageLayout layout) {
      final var editorArea = layout.getEditorArea();

      final var left = layout.createFolder("left", IPageLayout.LEFT, (float) 0.20, editorArea);
      left.addView(IPageLayout.ID_PROJECT_EXPLORER);

      final var bottom = layout.createFolder("bottom", IPageLayout.BOTTOM, (float) 0.80, editorArea);
      bottom.addView("org.eclipse.ui.console.ConsoleView");
      bottom.addView(IPageLayout.ID_BOOKMARKS);
      bottom.addView(IPageLayout.ID_TASK_LIST);
      bottom.addView(IPageLayout.ID_PROBLEM_VIEW);
      bottom.addView("de.sebthom.eclipse.findview.ui.FindView");
      bottom.addView("org.eclipse.team.ui.GenericHistoryView");
      bottom.addView("org.eclipse.egit.ui.StagingView");
      bottom.addView("org.eclipse.tm.terminal.view.ui.TerminalsView");

      final var bottom_right = layout.createFolder("bottom_right", IPageLayout.RIGHT, (float) 0.80, "bottom");
      bottom_right.addView(IPageLayout.ID_PROGRESS_VIEW);

      final var right = layout.createFolder("right", IPageLayout.RIGHT, (float) 0.80, editorArea);
      right.addView(IPageLayout.ID_OUTLINE);
      right.addView(IPageLayout.ID_PROP_SHEET);
   }

   public void defineToolbarActions(final IPageLayout layout) {
      layout.addActionSet("org.eclipse.debug.ui.launchActionSet");
   }

   /**
    * Add entries to "Window > Show View > ..."
    */
   public void defineMenuActions(final IPageLayout layout) {
      layout.addShowViewShortcut(IPageLayout.ID_PROJECT_EXPLORER);
      layout.addShowViewShortcut("org.eclipse.ui.console.ConsoleView");
      layout.addShowViewShortcut(IPageLayout.ID_BOOKMARKS);
      layout.addShowViewShortcut(IPageLayout.ID_TASK_LIST);
      layout.addShowViewShortcut(IPageLayout.ID_PROBLEM_VIEW);
      layout.addShowViewShortcut(IPageLayout.ID_PROGRESS_VIEW);
      layout.addShowViewShortcut("org.eclipse.team.ui.GenericHistoryView");
      layout.addShowViewShortcut("org.eclipse.egit.ui.StagingView");
      layout.addShowViewShortcut(IPageLayout.ID_OUTLINE);
      layout.addShowViewShortcut(IPageLayout.ID_PROP_SHEET);
      layout.addShowViewShortcut("org.eclipse.pde.runtime.LogView");
   }
}
