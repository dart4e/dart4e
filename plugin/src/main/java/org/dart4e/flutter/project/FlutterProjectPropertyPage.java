/*
 * SPDX-FileCopyrightText: © The Dart4E authors
 * SPDX-FileContributor: Sebastian Thomschke
 * SPDX-License-Identifier: EPL-2.0
 * SPDX-ArtifactOfProjectHomePage: https://github.com/dart4e/dart4e
 */
package org.dart4e.flutter.project;

import static net.sf.jstuff.core.validation.NullAnalysisHelper.*;

import org.dart4e.flutter.prefs.FlutterProjectPreference;
import org.dart4e.flutter.widget.FlutterSDKSelectionGroup;
import org.dart4e.util.ui.GridDatas;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;

import de.sebthom.eclipse.commons.resources.Projects;

/**
 * @author Sebastian Thomschke
 */
public final class FlutterProjectPropertyPage extends org.eclipse.ui.dialogs.PropertyPage {

   private FlutterProjectPreference prefs = lateNonNull();

   @Override
   protected Control createContents(final Composite parent) {
      // we don't need the "Restore Defaults" button
      noDefaultButton();

      final var container = new Composite(parent, SWT.NONE);
      container.setLayout(new GridLayout(1, true));
      container.setLayoutData(GridDatas.fillHorizontal());

      final var project = asNonNullUnsafe(Projects.adapt(getElement()));
      prefs = FlutterProjectPreference.get(project);

      /*
       * alt SDK selection
       */
      final var selectedAltSDK = new FlutterSDKSelectionGroup(container).selectedAltSDK;
      selectedAltSDK.set(prefs.getAlternateFlutterSDK());
      selectedAltSDK.subscribe(prefs::setAlternateFlutterSDK);

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
