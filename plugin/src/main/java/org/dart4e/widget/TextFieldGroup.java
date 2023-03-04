/*
 * SPDX-FileCopyrightText: © The Dart4E authors
 * SPDX-FileContributor: Sebastian Thomschke
 * SPDX-License-Identifier: EPL-2.0
 * SPDX-ArtifactOfProjectHomePage: https://github.com/dart4e/dart4e
 */
package org.dart4e.widget;

import org.dart4e.util.ui.GridDatas;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Text;

import de.sebthom.eclipse.commons.ui.Texts;
import net.sf.jstuff.core.ref.MutableObservableRef;

/**
 * @author Sebastian Thomschke
 */
public class TextFieldGroup extends Composite {

   public final MutableObservableRef<String> text = MutableObservableRef.of("");

   private final Group group;

   public TextFieldGroup(final Composite parent, final String label) {
      this(parent, label, SWT.NONE);
   }

   public TextFieldGroup(final Composite parent, final String label, final int style) {
      super(parent, style);

      if (parent.getLayout() instanceof GridLayout) {
         setLayoutData(GridDatas.fillHorizontalExcessive());
      }
      setLayout(new FillLayout());

      group = new Group(this, SWT.NONE);
      group.setLayout(new GridLayout(2, false));
      group.setText(label);
      final var txtDartVMArgs = new Text(group, SWT.BORDER);
      txtDartVMArgs.setLayoutData(GridDatas.fillHorizontalExcessive());
      Texts.bind(txtDartVMArgs, text);
   }

   public TextFieldGroup setLabel(final String label) {
      group.setText(label);
      return this;
   }
}
