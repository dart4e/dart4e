/*
 * Copyright 2022 by the Dart4E authors.
 * SPDX-License-Identifier: EPL-2.0
 */
package org.dart4e.project;

import static net.sf.jstuff.core.validation.NullAnalysisHelper.*;

import org.dart4e.prefs.DartProjectPreference;
import org.dart4e.util.ui.GridDatas;
import org.dart4e.widget.DartBuildSystemSelectionGroup;
import org.dart4e.widget.DartSDKSelectionGroup;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;

import de.sebthom.eclipse.commons.resources.Projects;
import de.sebthom.eclipse.commons.ui.Buttons;

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

      /*
       * alt SDK selection
       */
      final var project = asNonNullUnsafe(Projects.adapt(getElement()));
      prefs = DartProjectPreference.get(project);
      final var grpDartSDKSelection = new DartSDKSelectionGroup(container, GridDataFactory.fillDefaults().create());
      grpDartSDKSelection.selectedAltSDK.set(prefs.getAlternateDartSDK());
      grpDartSDKSelection.selectedAltSDK.subscribe(prefs::setAlternateDartSDK);

      /*
       * build system selection
       */
      final var grpBuildSystem = new DartBuildSystemSelectionGroup(container, GridDatas.fillHorizontalExcessive());
      grpBuildSystem.setProject(project);

      /*
       * auto build check box
       */
      final var btnAutoBuild = new Button(container, SWT.CHECK);
      btnAutoBuild.setText("Enable auto build");
      btnAutoBuild.setSelection(prefs.isAutoBuild());
      Buttons.onSelected(btnAutoBuild, () -> prefs.setAutoBuild(btnAutoBuild.getSelection()));
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
