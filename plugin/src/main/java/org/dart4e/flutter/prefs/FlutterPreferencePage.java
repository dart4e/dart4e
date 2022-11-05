/*
 * Copyright 2022 by the Dart4E authors.
 * SPDX-License-Identifier: EPL-2.0
 */
package org.dart4e.flutter.prefs;

import org.dart4e.localization.Messages;
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

/**
 * @author Sebastian Thomschke
 */
public class FlutterPreferencePage extends PreferencePage implements IWorkbenchPreferencePage {

   public FlutterPreferencePage() {
      setDescription(Messages.Flutter_Prefs_GeneralDescription);
   }

   @Override
   public Control createContents(final Composite parent) {
      final var container = new Composite(parent, SWT.NULL);
      container.setLayout(new GridLayout(1, true));
      return container;
   }

   @Override
   public void init(final IWorkbench workbench) {
      setPreferenceStore(FlutterWorkspacePreference.PREFS);
   }

   @Override
   protected void performDefaults() {
      super.performDefaults();
   }

   @Override
   public boolean performOk() {
      if (!FlutterWorkspacePreference.save()) {
         setValid(false);
         return false;
      }

      setValid(true);
      return true;
   }

}
