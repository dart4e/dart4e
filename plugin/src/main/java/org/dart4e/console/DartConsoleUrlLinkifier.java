/*
 * SPDX-FileCopyrightText: © The Dart4E authors
 * SPDX-FileContributor: Sebastian Thomschke
 * SPDX-License-Identifier: EPL-2.0
 * SPDX-ArtifactOfProjectHomePage: https://github.com/dart4e/dart4e
 */
package org.dart4e.console;

import java.net.MalformedURLException;
import java.net.URL;

import org.dart4e.Dart4EPlugin;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.console.IHyperlink;
import org.eclipse.ui.console.IPatternMatchListenerDelegate;
import org.eclipse.ui.console.PatternMatchEvent;
import org.eclipse.ui.console.TextConsole;

import de.sebthom.eclipse.commons.ui.UI;

/**
 * @author Sebastian Thomschke
 */
public final class DartConsoleUrlLinkifier implements IPatternMatchListenerDelegate {

   private @Nullable TextConsole console;

   @Override
   public void connect(final TextConsole console) {
      this.console = console;
   }

   @Override
   public void disconnect() {
      console = null;
   }

   @Override
   public void matchFound(final PatternMatchEvent event) {
      final var console = this.console;
      if (console == null)
         return;

      final var offset = event.getOffset();
      final var length = event.getLength();
      final var doc = console.getDocument();
      try {
         final var sourceLoc = doc.get(offset, length);

         console.addHyperlink(new IHyperlink() {
            @Override
            public void linkActivated() {
               try {
                  UI.getWorkbench().getBrowserSupport().getExternalBrowser().openURL(new URL(sourceLoc));
               } catch (PartInitException | MalformedURLException ex) {
                  Dart4EPlugin.log().debug(ex);
               }
            }

            @Override
            public void linkEntered() {
            }

            @Override
            public void linkExited() {
            }
         }, offset, length);
      } catch (final Exception ex) {
         Dart4EPlugin.log().debug(ex);
      }
   }
}
