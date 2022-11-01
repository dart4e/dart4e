/*
 * Copyright 2022 by the Dart4E authors.
 * SPDX-License-Identifier: EPL-2.0
 */
package org.dart4e.project;

import static net.sf.jstuff.core.validation.NullAnalysisHelper.*;

import org.dart4e.prefs.DartProjectPreference;
import org.dart4e.prefs.DartWorkspacePreference;
import org.dart4e.util.ui.GridDatas;
import org.dart4e.widget.DartSDKSelectionGroup;
import org.dart4e.widget.FormatterSettingsGroup;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;

import de.sebthom.eclipse.commons.resources.Projects;

/**
 * @author Sebastian Thomschke
 */
public final class DartProjectPropertyPage extends org.eclipse.ui.dialogs.PropertyPage {

   private DartProjectPreference prefs = lazyNonNull();

   @Override
   protected Control createContents(final Composite parent) {
      // we don't need the "Restore Defaults" button
      noDefaultButton();

      final var container = new Composite(parent, SWT.NONE);
      container.setLayout(new GridLayout(1, true));
      container.setLayoutData(GridDatas.fillHorizontal());

      final var project = asNonNullUnsafe(Projects.adapt(getElement()));
      prefs = DartProjectPreference.get(project);

      /*
       * alt SDK selection
       */
      final var grpDartSDKSelection = new DartSDKSelectionGroup(container, GridDatas.fillHorizontalExcessive());
      grpDartSDKSelection.selectedAltSDK.set(prefs.getAlternateDartSDK());
      grpDartSDKSelection.selectedAltSDK.subscribe(prefs::setAlternateDartSDK);

      /*
       * formatter
       */
      final var formatterSettings = new FormatterSettingsGroup(container, GridDatas.fillHorizontalExcessive());
      formatterSettings.defaultMaxLineLength.set(DartWorkspacePreference.getFormatterMaxLineLength());
      formatterSettings.maxLineLength.set(prefs.getFormatterMaxLineLength());
      formatterSettings.maxLineLength.subscribe(newValue -> prefs.setFormatterMaxLineLength(newValue));
      return container;
   }

   @Override
   public boolean performOk() {
      prefs.save();
      return super.performOk();
   }

   @Override
   public boolean performCancel() {
      prefs.revert();
      return true;
   }
}
