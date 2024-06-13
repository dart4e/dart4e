/*
 * SPDX-FileCopyrightText: © The Dart4E authors
 * SPDX-FileContributor: Sebastian Thomschke
 * SPDX-License-Identifier: EPL-2.0
 * SPDX-ArtifactOfProjectHomePage: https://github.com/dart4e/dart4e
 */
package org.dart4e.flutter.console;

import static net.sf.jstuff.core.validation.NullAnalysisHelper.lateNonNull;

import org.dart4e.Constants;
import org.dart4e.Dart4EPlugin;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.Separator;
import org.eclipse.ui.console.IConsole;
import org.eclipse.ui.console.IConsoleConstants;
import org.eclipse.ui.console.IConsolePageParticipant;
import org.eclipse.ui.part.IPageBookViewPage;

/**
 * @author Sebastian Thomschke
 */
public final class FlutterConsoleActions implements IConsolePageParticipant {

   private Action terminate = lateNonNull();

   @Override
   public void activated() {
   }

   @Override
   public void deactivated() {
   }

   @Override
   public void dispose() {
   }

   @Override
   public <T> @Nullable T getAdapter(final @Nullable Class<T> adapter) {
      return null;
   }

   @Override
   public void init(final IPageBookViewPage page, final IConsole console) {
      final var flutterConsole = (FlutterConsole) console;

      flutterConsole.onTerminated.thenRun(() -> terminate.setEnabled(false));

      terminate = new Action("Terminate") {
         @Override
         public void run() {
            flutterConsole.monitor.setCanceled(true);
            terminate.setEnabled(false);
         }
      };
      terminate.setImageDescriptor(Dart4EPlugin.get().getSharedImageDescriptor(Constants.IMAGE_TERMINATE_BUTTON));
      terminate.setDisabledImageDescriptor(Dart4EPlugin.get().getSharedImageDescriptor(Constants.IMAGE_TERMINATE_BUTTON_DISABLED));

      final var bars = page.getSite().getActionBars();
      bars.getMenuManager().add(new Separator());
      bars.getMenuManager().add(terminate);

      final var toolbarManager = bars.getToolBarManager();
      toolbarManager.appendToGroup(IConsoleConstants.LAUNCH_GROUP, terminate);

      bars.updateActionBars();
   }
}
