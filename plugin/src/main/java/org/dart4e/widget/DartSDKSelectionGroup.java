/*
 * Copyright 2022 by the Dart4E authors.
 * SPDX-License-Identifier: EPL-2.0
 */
package org.dart4e.widget;

import org.dart4e.localization.Messages;
import org.dart4e.model.DartSDK;
import org.dart4e.prefs.DartWorkspacePreference;
import org.dart4e.util.ui.GridDatas;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Text;

import de.sebthom.eclipse.commons.ui.Buttons;
import de.sebthom.eclipse.commons.ui.UI;
import de.sebthom.eclipse.commons.ui.widgets.ComboWrapper;
import net.sf.jstuff.core.ref.MutableObservableRef;

/**
 * @author Sebastian Thomschke
 */
public class DartSDKSelectionGroup extends Composite {

   public final MutableObservableRef<@Nullable DartSDK> selectedAltSDK = MutableObservableRef.of(null);

   public DartSDKSelectionGroup(final Composite parent) {
      this(parent, SWT.NONE);
   }

   public DartSDKSelectionGroup(final Composite parent, final int style) {
      super(parent, style);

      if (parent.getLayout() instanceof GridLayout) {
         setLayoutData(GridDatas.fillHorizontalExcessive());
      }
      setLayout(GridLayoutFactory.fillDefaults().create());

      final var grpSdk = new Group(this, SWT.NONE);
      grpSdk.setLayoutData(GridDatas.fillHorizontalExcessive());
      grpSdk.setLayout(GridLayoutFactory.swtDefaults().numColumns(2).create());
      grpSdk.setText(Messages.Label_Dart_SDK);

      final var radioDefaultSDK = new Button(grpSdk, SWT.RADIO);
      radioDefaultSDK.setText(Messages.Label_Default);
      radioDefaultSDK.setSelection(true);
      Buttons.onSelected(radioDefaultSDK, () -> selectedAltSDK.set(null));

      final var txtDefaultSdk = new Text(grpSdk, SWT.BORDER);
      txtDefaultSdk.setEditable(false);
      txtDefaultSdk.setLayoutData(GridDatas.fillHorizontalExcessive());

      final var defaultSDK = DartWorkspacePreference.getDefaultDartSDK(false, true);
      if (defaultSDK != null) {
         txtDefaultSdk.setText(defaultSDK.toShortString());
      }

      final var radioAltSDK = new Button(grpSdk, SWT.RADIO);
      radioAltSDK.setText(Messages.Label_Alternative);

      final var cmbAltSDK = new ComboWrapper<DartSDK>(grpSdk, GridDataFactory.fillDefaults().create()) //
         .setLabelProvider(DartSDK::toShortString) //
         .onItemsChanged((widget, oldItems, newItems) -> {
            if (newItems.isEmpty()) {
               Buttons.selectRadio(radioDefaultSDK);
               radioAltSDK.setEnabled(false);
               widget.setEnabled(false);
            } else {
               radioAltSDK.setEnabled(true);
               widget.setEnabled(true);
            }
         });

      final var registeredSDKs = DartWorkspacePreference.getDartSDKs();
      cmbAltSDK.setItems(registeredSDKs);
      if (!registeredSDKs.isEmpty()) {
         cmbAltSDK.setSelection(registeredSDKs.first(), true);
      }

      Buttons.onSelected(radioAltSDK, () -> selectedAltSDK.set(cmbAltSDK.getSelection()));
      cmbAltSDK.onSelectionChanged(selectedAltSDK::set);

      selectedAltSDK.subscribe(sdk -> UI.run(() -> {
         if (sdk == null) {
            Buttons.selectRadio(radioDefaultSDK);
         } else {
            Buttons.selectRadio(radioAltSDK);
            cmbAltSDK.setSelection(sdk);
         }
      }));
   }
}
