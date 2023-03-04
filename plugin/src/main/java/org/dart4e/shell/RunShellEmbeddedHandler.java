/*
 * SPDX-FileCopyrightText: © The Dart4E authors
 * SPDX-FileContributor: Sebastian Thomschke
 * SPDX-License-Identifier: EPL-2.0
 * SPDX-ArtifactOfProjectHomePage: https://github.com/dart4e/dart4e
 */
package org.dart4e.shell;

import static net.sf.jstuff.core.validation.NullAnalysisHelper.asNonNull;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;

import org.dart4e.Dart4EPlugin;
import org.dart4e.localization.Messages;
import org.dart4e.model.DartSDK;
import org.dart4e.prefs.DartProjectPreference;
import org.dart4e.prefs.DartWorkspacePreference;
import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.tm.terminal.view.core.TerminalServiceFactory;
import org.eclipse.tm.terminal.view.core.interfaces.constants.ITerminalsConnectorConstants;
import org.eclipse.tm.terminal.view.ui.interfaces.IUIConstants;
import org.eclipse.tm.terminal.view.ui.view.TerminalsView;
import org.eclipse.ui.handlers.HandlerUtil;

import de.sebthom.eclipse.commons.resources.Projects;
import de.sebthom.eclipse.commons.resources.Resources;
import de.sebthom.eclipse.commons.ui.Dialogs;
import de.sebthom.eclipse.commons.ui.UI;
import net.sf.jstuff.core.Strings;
import net.sf.jstuff.core.concurrent.Threads;
import net.sf.jstuff.core.net.NetUtils;

/**
 * @author Sebastian Thomschke
 */
public class RunShellEmbeddedHandler extends AbstractHandler {

   @Override
   public @Nullable Object execute(final ExecutionEvent event) throws ExecutionException {
      if (HandlerUtil.getCurrentSelection(event) instanceof final IStructuredSelection currentSelection) {
         final var project = Projects.adapt(currentSelection.getFirstElement());
         launchShell(project);
         return Status.OK_STATUS;
      }
      return Status.CANCEL_STATUS;
   }

   private void launchShell(@Nullable final IProject project) {
      final var job = Job.create("Preparing Dart Shell", jobMonitor -> {
         final DartSDK dartSDK;
         if (project == null) {
            dartSDK = DartWorkspacePreference.getDefaultDartSDK(true, true);
         } else {
            final var prefs = DartProjectPreference.get(project);
            dartSDK = prefs.getEffectiveDartSDK();
         }
         if (dartSDK == null || !dartSDK.isValid()) {
            Dialogs.showError(Messages.Prefs_NoSDKRegistered_Title, Messages.Prefs_NoSDKRegistered_Body);
            return;
         }

         dartSDK.installInteractiveShell(jobMonitor);

         // CHECKSTYLE:IGNORE .* FOR NEXT LINE
         // see https://github.com/eclipse-cdt/cdt/blob/main/terminal/plugins/org.eclipse.tm.terminal.view.core/src/org/eclipse/tm/terminal/view/core/interfaces/constants/ITerminalsConnectorConstants.java
         final var properties = new HashMap<String, Object>();
         properties.put(ITerminalsConnectorConstants.PROP_ENCODING, StandardCharsets.UTF_8.name());
         properties.put(ITerminalsConnectorConstants.PROP_DELEGATE_ID, "org.eclipse.tm.terminal.connector.local.launcher.local");
         properties.put(ITerminalsConnectorConstants.PROP_PROCESS_PATH, dartSDK.getDartExecutable().toString());
         if (project != null) {
            properties.put(ITerminalsConnectorConstants.PROP_TITLE, "Dart Shell (" + project.getName() + ")");
            properties.put(ITerminalsConnectorConstants.PROP_PROCESS_WORKING_DIR, Resources.toAbsolutePath(project).toString());
            properties.put(ITerminalsConnectorConstants.PROP_PROCESS_ARGS, "pub global run interactive --directory \"" + Strings.replace(
               Resources.toAbsolutePath(project).toString(), '\\', '/') + "\"");
         } else {
            properties.put(ITerminalsConnectorConstants.PROP_TITLE, "Dart Shell");
            properties.put(ITerminalsConnectorConstants.PROP_PROCESS_ARGS, "pub global run --enable-vm-service=" + NetUtils
               .getAvailableLocalPort() + " interactive ");
         }

         UI.run(() -> {
            try {
               final TerminalsView terminalView = asNonNull(UI.openView(IUIConstants.ID));
               terminalView.show(null);
               terminalView.setFocus();

               final var terminal = TerminalServiceFactory.getService();
               terminal.openConsole(properties, status -> {
                  if (!status.isOK()) {
                     Dialogs.showStatus("Cannot open Dart shell", status, true);
                  }
               });
            } catch (final Exception ex) {
               final var status = Dart4EPlugin.status().createError(ex, "Failed to launch Dart shell");
               Dialogs.showStatus("Cannot open Dart shell", status, true);
            }
         });

         if (project != null) {
            jobMonitor.setTaskName("Cleaning up Dart Shell");
            Threads.sleep(5_000);
            final var launchFile = project.getFile("lib/auto_generated.dart");
            launchFile.refreshLocal(0, jobMonitor);
            launchFile.delete(true, jobMonitor);
         }
      });
      job.schedule();
   }
}
