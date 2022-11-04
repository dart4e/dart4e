/*
 * Copyright 2022 by the Dart4E authors.
 * SPDX-License-Identifier: EPL-2.0
 */
package org.dart4e.widget;

import org.dart4e.util.ui.GridDatas;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Spinner;

import de.sebthom.eclipse.commons.ui.Buttons;
import de.sebthom.eclipse.commons.ui.Spinners;
import net.sf.jstuff.core.ref.MutableObservableRef;

/**
 * @author Sebastian Thomschke
 */
public class FormatterSettingsGroup extends Composite {

   public final MutableObservableRef<Integer> defaultMaxLineLength = MutableObservableRef.of(80);
   public final MutableObservableRef<Integer> maxLineLength = MutableObservableRef.of(80);

   public FormatterSettingsGroup(final Composite parent) {
      this(parent, SWT.NONE);
   }

   public FormatterSettingsGroup(final Composite parent, final int style) {
      super(parent, style);

      if (parent.getLayout() instanceof GridLayout) {
         setLayoutData(GridDatas.fillHorizontalExcessive());
      }
      setLayout(GridLayoutFactory.fillDefaults().create());

      final var grpFormatter = new Group(this, SWT.NONE);
      grpFormatter.setLayoutData(GridDatas.fillHorizontalExcessive());
      grpFormatter.setLayout(GridLayoutFactory.swtDefaults().numColumns(3).create());
      grpFormatter.setText("Formatter Settings");

      final var lblLineLength = new Label(grpFormatter, SWT.NONE);
      lblLineLength.setLayoutData(GridDatas.alignRight());
      lblLineLength.setText("Maximal Line Length:");
      final var inputLineLength = new Spinner(grpFormatter, SWT.BORDER);
      inputLineLength.setValues(0, 40, 200, 0, 1, 10);
      inputLineLength.setLayoutData(GridDatas.fillHorizontalExcessive());
      Spinners.bind(inputLineLength, maxLineLength);

      final var btnDefault = new Button(grpFormatter, SWT.NONE);
      btnDefault.setText("Default");
      Buttons.onSelected(btnDefault, () -> maxLineLength.set(defaultMaxLineLength.get()));
   }
}
