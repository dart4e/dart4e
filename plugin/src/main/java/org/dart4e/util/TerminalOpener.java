/*
 * SPDX-FileCopyrightText: © The Dart4E authors
 * SPDX-FileContributor: Sebastian Thomschke
 * SPDX-License-Identifier: EPL-2.0
 * SPDX-ArtifactOfProjectHomePage: https://github.com/dart4e/dart4e
 */
package org.dart4e.util;

import static net.sf.jstuff.core.validation.NullAnalysisHelper.asNonNull;

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.part.IShowInTarget;
import org.osgi.framework.Bundle;

import de.sebthom.eclipse.commons.ui.Dialogs;
import de.sebthom.eclipse.commons.ui.UI;

/**
 * @author Sebastian Thomschke
 */
public final class TerminalOpener {

   // CHECKSTYLE:IGNORE .* FOR NEXT 2 LINES
   // see https://github.com/eclipse-cdt/cdt/blob/cdt_12_2/terminal/plugins/org.eclipse.tm.terminal.view.core/src/org/eclipse/tm/terminal/view/core/interfaces/constants/ITerminalsConnectorConstants.java
   // see https://github.com/eclipse-platform/eclipse.platform/blob/master/terminal/bundles/org.eclipse.terminal.view.core/src/org/eclipse/terminal/view/core/ITerminalsConnectorConstants.java
   private static final String PROP_ENCODING = "encoding";
   private static final String PROP_TITLE = "title";
   private static final String PROP_PROCESS_PATH = "process.path";
   private static final String PROP_PROCESS_ARGS = "process.args";
   private static final String PROP_PROCESS_WORKING_DIR = "process.working_dir";
   private static final String PROP_DELEGATE_ID = "delegateId";

   /* org.eclipse.tm.terminal.view.ui.interfaces.IUIConstants.ID */
   public static final String TERMINAL_VIEWS_ID_OLD = "org.eclipse.tm.terminal.view.ui.TerminalsView";
   /* org.eclipse.terminal.view.ui.IUIConstants.ID */
   public static final String TERMINAL_VIEWS_ID_NEW = "org.eclipse.terminal.view.ui.TerminalsView";

   private static final String TERMINAL_VIEW_UI_PLUGIN_ID_OLD = "org.eclipse.tm.terminal.view.ui";
   private static final String TERMINAL_VIEW_UI_PLUGIN_ID_NEW = "org.eclipse.terminal.view.ui";

   private static boolean isPluginAvailable(final String id) {
      final var bundle = Platform.getBundle(id);
      return bundle != null && (bundle.getState() & (Bundle.RESOLVED | Bundle.ACTIVE | Bundle.STARTING)) != 0;
   }

   /**
    * @return true if the old terminal view plugin (pre Eclipse 2025-09) is available
    */
   public static boolean isOldTerminalViewPluginAvailable() {
      return isPluginAvailable(TERMINAL_VIEW_UI_PLUGIN_ID_OLD);
   }

   /**
    * @return true if the new terminal view plugin (Eclipse 2025-09 or newer) is available
    */
   public static boolean isNewTerminalViewPluginAvailable() {
      return isPluginAvailable(TERMINAL_VIEW_UI_PLUGIN_ID_NEW);
   }

   /**
    * @return true if any supported terminal view plugin is available
    */
   public static boolean isTerminalViewPluginAvailable() {
      return isOldTerminalViewPluginAvailable() || isNewTerminalViewPluginAvailable();
   }

   public static String getTerminalsViewId() {
      return isOldTerminalViewPluginAvailable() ? TERMINAL_VIEWS_ID_OLD : TERMINAL_VIEWS_ID_NEW;
   }

   public static boolean showTerminalView() {
      if (!isTerminalViewPluginAvailable()) {
         Dialogs.showStatus("Cannot open Terminals view", Status.error("No terminal plugin found (" + TERMINAL_VIEW_UI_PLUGIN_ID_OLD + " / "
               + TERMINAL_VIEW_UI_PLUGIN_ID_NEW + ")"), true);
         return false;
      }
      final IViewPart terminalView = asNonNull(UI.openView(getTerminalsViewId()));
      ((IShowInTarget) terminalView).show(null);
      terminalView.setFocus();
      return true;
   }

   /**
    * Opens an Eclipse Terminal view and runs the given executable inside it.
    * <p>
    * This method automatically supports both the legacy TM Terminal API ({@code org.eclipse.tm.terminal.*}) and the new renamed Terminal
    * API ({@code org.eclipse.terminal.*}) introduced in Eclipse 2025-09.
    * It selects the correct implementation at runtime via reflection.
    * </p>
    *
    * <p>
    * Typical usage:
    * <pre>{@code
    * TerminalOpener.runInTerminal("Dart Shell", Path.of("/usr/bin/dart"), "--version", null);
    * }</pre>
    * </p>
    *
    * @param title the title shown in the terminal tab
    * @param executable the absolute path of the executable to run
    * @param args optional command-line arguments passed to the executable
    * @param workingDir optional working directory for the launched process
    *
    * @throws IllegalStateException if no supported terminal plugin is available or if the terminal service cannot be obtained
    * @throws RuntimeException if an unexpected reflection or invocation error occurs
    *
    * @see <a href=
    *      "https://github.com/eclipse-cdt/cdt/blob/cdt_12_2/terminal/plugins/org.eclipse.tm.terminal.view.core/src/org/eclipse/tm/terminal/view/core/">
    *      Legacy TM Terminal API</a>
    * @see <a href=
    *      "https://github.com/eclipse-platform/eclipse.platform/blob/master/terminal/bundles/org.eclipse.terminal.view.core/src/org/eclipse/terminal/view/core/">New
    *      Terminal API</a>
    */
   public static void runInTerminal(final String title, final Path executable, @Nullable final String args,
         @Nullable final Path workingDir) {

      final var properties = new HashMap<String, Object>();
      properties.put(PROP_ENCODING, StandardCharsets.UTF_8.name());
      properties.put(PROP_TITLE, title);
      properties.put(PROP_PROCESS_PATH, executable.toString());
      if (args != null && !args.isBlank()) {
         properties.put(PROP_PROCESS_ARGS, args);
      }
      if (workingDir != null) {
         properties.put(PROP_PROCESS_WORKING_DIR, workingDir.toString());
      }

      try {
         // Check old (before Eclipse 2025-09) -> then new (Eclipse 2025-09+)
         // Workaround for https://github.com/eclipse-platform/eclipse.platform/issues/2206
         if (isOldTerminalViewPluginAvailable()) {
            openTerminalViaOldAPI(properties);
         } else if (isNewTerminalViewPluginAvailable()) {
            openTerminalViaNewAPI(properties);
         } else {
            Dialogs.showStatus("Cannot open: " + title, Status.error("No terminal plugin found (" + TERMINAL_VIEW_UI_PLUGIN_ID_OLD + " / "
                  + TERMINAL_VIEW_UI_PLUGIN_ID_NEW + ")"), true);
         }
      } catch (final Exception t) {
         Dialogs.showStatus("Cannot open: " + title, Status.error(t.getClass().getSimpleName() + ": " + t.getMessage(), t), true);
      }
   }

   private static void openTerminalViaNewAPI(final Map<String, Object> properties) throws Exception {
      properties.put(PROP_DELEGATE_ID, "org.eclipse.terminal.connector.local.launcher.local");

      final Bundle bundle = Platform.getBundle(TERMINAL_VIEW_UI_PLUGIN_ID_NEW);
      if (bundle == null)
         throw new IllegalStateException("New terminal UI bundle not resolved");

      // org.eclipse.terminal.view.ui.internal.UIPlugin.getTerminalService()
      final /*ITerminalService*/ Object terminalService = bundle.loadClass("org.eclipse.terminal.view.ui.internal.UIPlugin") //
         .getMethod("getTerminalService").invoke(null);
      // https://github.com/eclipse-platform/eclipse.platform/blob/master/terminal/bundles/org.eclipse.terminal.view.core/src/org/eclipse/terminal/view/core/ITerminalService.java
      if (terminalService == null)
         throw new IllegalStateException("TerminalService not available");

      // public CompletableFuture<?> openConsole(Map<String, Object> properties);
      final Method openConsole = terminalService.getClass().getMethod("openConsole", Map.class);
      if (openConsole.invoke(terminalService, properties) instanceof final CompletableFuture<?> future) {
         future.whenComplete((result, ex) -> {
            if (ex != null) {
               Dialogs.showStatus("Cannot open: " + properties.get(PROP_TITLE), Status.error(ex.getMessage() != null ? ex.getMessage()
                     : ex.toString(), ex), true);
            }
         });
      }
   }

   private static void openTerminalViaOldAPI(final Map<String, Object> properties) throws Exception {
      properties.put(PROP_DELEGATE_ID, "org.eclipse.tm.terminal.connector.local.launcher.local");

      final Bundle bundle = Platform.getBundle("org.eclipse.tm.terminal.view.core");
      if (bundle == null)
         throw new IllegalStateException("TM Terminal Core bundle not resolved");

      // org.eclipse.tm.terminal.view.core.TerminalServiceFactory.getService()
      final /*ITerminalService*/ Object terminalService = bundle.loadClass("org.eclipse.tm.terminal.view.core.TerminalServiceFactory") //
         .getMethod("getService").invoke(null);
      // https://github.com/eclipse-cdt/cdt/blob/cdt_12_2/terminal/plugins/org.eclipse.tm.terminal.view.core/src/org/eclipse/tm/terminal/view/core/interfaces/ITerminalService.java
      if (terminalService == null)
         throw new IllegalStateException("TerminalService not available");

      // Load Done interface
      final Class<?> doneIface = bundle.loadClass("org.eclipse.tm.terminal.view.core.interfaces.ITerminalService$Done");
      final Method doneMethod = doneIface.getMethod("done", IStatus.class);

      final Object doneCallback = Proxy.newProxyInstance(doneIface.getClassLoader(), new Class<?>[] {doneIface}, (proxy, method, args) -> {
         if (method.equals(doneMethod)) {
            final var status = asNonNull((IStatus) asNonNull(args)[0]);
            if (!status.isOK()) {
               Dialogs.showStatus("Cannot open: " + properties.get(PROP_TITLE), status, true);
            }
         }
         return null;
      });

      // public void openConsole(Map<String, Object> properties, Done done);
      final Method openConsole = terminalService.getClass().getMethod("openConsole", Map.class, doneIface);
      openConsole.invoke(terminalService, properties, doneCallback);
   }

   private TerminalOpener() {
   }
}
