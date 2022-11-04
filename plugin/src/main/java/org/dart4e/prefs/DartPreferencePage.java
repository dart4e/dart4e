/*
 * Copyright 2022 by the Dart4E authors.
 * SPDX-License-Identifier: EPL-2.0
 */
package org.dart4e.prefs;

import static net.sf.jstuff.core.validation.NullAnalysisHelper.lazyNonNull;

import org.dart4e.localization.Messages;
import org.dart4e.widget.FormatterSettingsGroup;
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

import net.sf.jstuff.core.ref.MutableObservableRef;

/**
 * @author Sebastian Thomschke
 */
public class DartPreferencePage extends PreferencePage implements IWorkbenchPreferencePage {

   private MutableObservableRef<Integer> formatterMaxLineLength = lazyNonNull();

   public DartPreferencePage() {
      setDescription(Messages.Prefs_GeneralDescription);
   }

   @Override
   public Control createContents(final Composite parent) {
      final var container = new Composite(parent, SWT.NULL);
      container.setLayout(new GridLayout(1, true));

      final var formatterSettings = new FormatterSettingsGroup(container);
      formatterSettings.defaultMaxLineLength.set(80);
      formatterMaxLineLength = formatterSettings.maxLineLength;
      formatterMaxLineLength.set(DartWorkspacePreference.getFormatterMaxLineLength());
      return container;
   }

   @Override
   public void init(final IWorkbench workbench) {
      setPreferenceStore(DartWorkspacePreference.PREFS);
   }

   @Override
   protected void performDefaults() {
      super.performDefaults();
   }

   @Override
   public boolean performOk() {
      DartWorkspacePreference.setFormatterMaxLineLength(formatterMaxLineLength.get());
      if (!DartWorkspacePreference.save()) {
         setValid(false);
         return false;
      }

      setValid(true);
      return true;
   }

}
