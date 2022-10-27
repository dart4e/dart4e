/*
 * Copyright 2022 by the Dart4E authors.
 * SPDX-License-Identifier: EPL-2.0
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
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.debug.ui.ILaunchShortcut;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.tm.terminal.view.core.TerminalServiceFactory;
import org.eclipse.tm.terminal.view.core.interfaces.constants.ITerminalsConnectorConstants;
import org.eclipse.tm.terminal.view.ui.interfaces.IUIConstants;
import org.eclipse.tm.terminal.view.ui.view.TerminalsView;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IFileEditorInput;

import de.sebthom.eclipse.commons.resources.Resources;
import de.sebthom.eclipse.commons.ui.Dialogs;
import de.sebthom.eclipse.commons.ui.UI;
import net.sf.jstuff.core.Strings;
import net.sf.jstuff.core.net.NetUtils;

/**
 * @author Sebastian Thomschke
 */
public class RunShellEmbeddedShortcut implements ILaunchShortcut {

   @Override
   public void launch(final IEditorPart editor, final String mode) {
      IProject project = null;
      final var editorInput = editor.getEditorInput();
      if (editorInput instanceof final IFileEditorInput fileInput) {
         project = fileInput.getFile().getProject();
      }

      launchShell(project);
   }

   @Override
   public void launch(final ISelection selection, final String mode) {
      IProject project = null;
      if (selection instanceof final StructuredSelection structuredSel) {
         final var firstElement = structuredSel.getFirstElement();
         if (firstElement instanceof final IResource res) {
            project = res.getProject();
         }
      }

      launchShell(project);
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

         UI.run(() -> {
            try {
               final TerminalsView terminalView = asNonNull(UI.openView(IUIConstants.ID));
               terminalView.show(null);
               terminalView.setFocus();

               // CHECKSTYLE:IGNORE .* FOR NEXT LINE
               // see https://github.com/eclipse-cdt/cdt/blob/main/terminal/plugins/org.eclipse.tm.terminal.view.core/src/org/eclipse/tm/terminal/view/core/interfaces/constants/ITerminalsConnectorConstants.java
               final var properties = new HashMap<String, Object>();
               properties.put(ITerminalsConnectorConstants.PROP_ENCODING, StandardCharsets.UTF_8.name());
               properties.put(ITerminalsConnectorConstants.PROP_DELEGATE_ID, "org.eclipse.tm.terminal.connector.local.launcher.local");
               properties.put(ITerminalsConnectorConstants.PROP_PROCESS_PATH, dartSDK.getDartExecutable().toString());
               if (project != null) {
                  properties.put(ITerminalsConnectorConstants.PROP_TITLE, "Dart Shell (" + project.getName() + ")");
                  properties.put(ITerminalsConnectorConstants.PROP_PROCESS_WORKING_DIR, Resources.toAbsolutePath(project).toString());
                  properties.put(ITerminalsConnectorConstants.PROP_PROCESS_ARGS, "pub global run interactive --directory \"" + Strings
                     .replace(Resources.toAbsolutePath(project).toString(), '\\', '/') + "\"");
               } else {
                  properties.put(ITerminalsConnectorConstants.PROP_TITLE, "Dart Shell");
                  properties.put(ITerminalsConnectorConstants.PROP_PROCESS_ARGS, "pub global run --enable-vm-service=" + NetUtils
                     .getAvailableLocalPort() + " interactive ");
               }
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
      });
      job.schedule();
   }
}
