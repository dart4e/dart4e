/*
 * SPDX-FileCopyrightText: © The Dart4E authors
 * SPDX-FileContributor: Sebastian Thomschke
 * SPDX-License-Identifier: EPL-2.0
 * SPDX-ArtifactOfProjectHomePage: https://github.com/dart4e/dart4e
 */
package org.dart4e.prefs;

import static net.sf.jstuff.core.validation.NullAnalysisHelper.lateNonNull;

import java.util.List;

import org.eclipse.jface.preference.BooleanFieldEditor;
import org.eclipse.jface.preference.ComboFieldEditor;
import org.eclipse.jface.preference.FieldEditor;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

import de.sebthom.eclipse.commons.prefs.fieldeditor.GroupFieldEditor;

/**
 * Preference page for configuring Dart inlay hints.
 *
 * @author Sebastian Thomschke
 */
public final class DartInlayHintsPreferencePage extends FieldEditorPreferencePage implements IWorkbenchPreferencePage {

   private BooleanFieldEditor inlayHintsEnabledEditor = lateNonNull();
   private GroupFieldEditor inlayHintsGroupEditor = lateNonNull();

   public DartInlayHintsPreferencePage() {
      super(GRID);
      setDescription("Configure Dart inlay hints. (requires Dart 3.10 or newer)");
   }

   @Override
   public void init(final IWorkbench workbench) {
      setPreferenceStore(DartWorkspacePreference.STORE);
   }

   @Override
   protected void createFieldEditors() {
      final Composite parent = getFieldEditorParent();

      inlayHintsEnabledEditor = new BooleanFieldEditor( //
         DartWorkspacePreference.PREFKEY_INLAY_HINTS_ENABLED, //
         "Enable inlay hints", //
         parent);
      addField(inlayHintsEnabledEditor);

      inlayHintsGroupEditor = new GroupFieldEditor("Show inlay hints for", parent, group -> List.of( //
         new BooleanFieldEditor(DartWorkspacePreference.PREFKEY_INLAY_HINTS_DOT_SHORTHAND_TYPES_ENABLED, "Dot shorthand types", group), //
         new ComboFieldEditor(DartWorkspacePreference.PREFKEY_INLAY_HINTS_PARAMETER_NAMES_MODE, "Parameter names", //
            new String[][] { //
               {"None", "none"}, //
               {"Only for literal arguments", "literal"}, //
               {"For all arguments", "all"} //
            }, group), //
         new BooleanFieldEditor(DartWorkspacePreference.PREFKEY_INLAY_HINTS_PARAMETER_TYPES_ENABLED, "Parameter types", group), //
         new BooleanFieldEditor(DartWorkspacePreference.PREFKEY_INLAY_HINTS_RETURN_TYPES_ENABLED, "Return types", group), //
         new BooleanFieldEditor(DartWorkspacePreference.PREFKEY_INLAY_HINTS_TYPE_ARGUMENTS_ENABLED, "Type arguments", group), //
         new BooleanFieldEditor(DartWorkspacePreference.PREFKEY_INLAY_HINTS_VARIABLE_TYPES_ENABLED, "Variable types", group) //
      ));
      addField(inlayHintsGroupEditor);

      setInlayHintsEditorsEnabled(DartWorkspacePreference.isInlayHintsEnabled());
   }

   @Override
   public void propertyChange(final PropertyChangeEvent event) {
      super.propertyChange(event);

      if (!FieldEditor.VALUE.equals(event.getProperty()) || event.getSource() != inlayHintsEnabledEditor)
         return;

      if (event.getNewValue() instanceof final Boolean enabled) {
         setInlayHintsEditorsEnabled(enabled);
      }
   }

   private void setInlayHintsEditorsEnabled(final boolean enabled) {
      inlayHintsGroupEditor.setEnabled(enabled, getFieldEditorParent());
   }
}
