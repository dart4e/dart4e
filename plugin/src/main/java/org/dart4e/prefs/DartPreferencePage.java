/*
 * SPDX-FileCopyrightText: © The Dart4E authors
 * SPDX-FileContributor: Sebastian Thomschke
 * SPDX-License-Identifier: EPL-2.0
 * SPDX-ArtifactOfProjectHomePage: https://github.com/dart4e/dart4e
 */
package org.dart4e.prefs;

import java.util.List;

import org.dart4e.localization.Messages;
import org.eclipse.jface.preference.BooleanFieldEditor;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Scale;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

import de.sebthom.eclipse.commons.prefs.fieldeditor.GroupFieldEditor;
import de.sebthom.eclipse.commons.prefs.fieldeditor.ScaleFieldEditor;

/**
 * @author Sebastian Thomschke
 */
public class DartPreferencePage extends FieldEditorPreferencePage implements IWorkbenchPreferencePage {

   public DartPreferencePage() {
      super(GRID);
      setDescription(Messages.Prefs_GeneralDescription);
   }

   @Override
   public void init(final IWorkbench workbench) {
      setPreferenceStore(DartWorkspacePreference.STORE);
   }

   @Override
   protected void createFieldEditors() {
      final var parent = getFieldEditorParent();

      addField(new GroupFieldEditor("Dart Language Server - Troubleshooting", parent, group -> List.of( //
         new BooleanFieldEditor(DartWorkspacePreference.PREFKEY_LSP_TRACE_INITOPTS, "Log Init Options", group), //
         new BooleanFieldEditor(DartWorkspacePreference.PREFKEY_LSP_TRACE_IO, "Log Language Server Protocol communication", group), //
         new BooleanFieldEditor(DartWorkspacePreference.PREFKEY_LSP_TRACE_IO_VERBOSE,
            "Log Language Server Protocol communication (verbose)", group) //
      )));

      addField(new GroupFieldEditor("Dart Debug Adapter - Troubleshooting", parent, group -> List.of( //
         new BooleanFieldEditor(DartWorkspacePreference.PREFKEY_DAP_TRACE_IO, "Log Debug Adatper Protocol communication", group), //
         new BooleanFieldEditor(DartWorkspacePreference.PREFKEY_DAP_TRACE_IO_VERBOSE, "Log Debug Adatper Protocol communication (verbose)",
            group) //
      )));

      addField(new GroupFieldEditor("Dart Formatter Settings", parent, group -> List.of( //
         new ScaleFieldEditor(DartWorkspacePreference.PREFKEY_FORMATTER_MAX_LINE_LENGTH, "Maximal Line Length", group) {
            @Override
            protected Scale createScale(final Composite parent) {
               final var scale = new Scale(parent, SWT.HORIZONTAL);
               scale.setMinimum(40);
               scale.setMaximum(300);
               scale.setIncrement(1);
               scale.setPageIncrement(10);
               return scale;
            }
         })));
   }
}
