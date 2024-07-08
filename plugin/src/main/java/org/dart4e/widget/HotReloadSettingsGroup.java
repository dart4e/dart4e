/*
 * SPDX-FileCopyrightText: © The Dart4E authors
 * SPDX-FileContributor: Sebastian Thomschke
 * SPDX-License-Identifier: EPL-2.0
 * SPDX-ArtifactOfProjectHomePage: https://github.com/dart4e/dart4e
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

import de.sebthom.eclipse.commons.ui.Buttons;
import net.sf.jstuff.core.ref.MutableObservableRef;

/**
 * @author Sebastian Thomschke
 */
public class HotReloadSettingsGroup extends Composite {

   public final MutableObservableRef<Boolean> defaultHotReloadOnSave = MutableObservableRef.of(true);
   public final MutableObservableRef<Boolean> hotReloadOnSave = MutableObservableRef.of(true);

   public HotReloadSettingsGroup(final Composite parent) {
      this(parent, SWT.NONE);
   }

   public HotReloadSettingsGroup(final Composite parent, final int style) {
      super(parent, style);

      if (parent.getLayout() instanceof GridLayout) {
         setLayoutData(GridDatas.fillHorizontalExcessive());
      }
      setLayout(GridLayoutFactory.fillDefaults().create());

      final var grpDebugSettings = new Group(this, SWT.NONE);
      grpDebugSettings.setLayoutData(GridDatas.fillHorizontalExcessive());
      grpDebugSettings.setLayout(GridLayoutFactory.swtDefaults().numColumns(3).create());
      grpDebugSettings.setText("Hot Reload Settings (Debug mode only)");

      final var lblHotReloadOnSave = new Label(grpDebugSettings, SWT.NONE);
      lblHotReloadOnSave.setLayoutData(GridDatas.alignRight());
      lblHotReloadOnSave.setText("Hot Reload on Save:");

      final var checkHotReloadOnSave = new Button(grpDebugSettings, SWT.CHECK);
      checkHotReloadOnSave.setLayoutData(GridDatas.fillHorizontalExcessive());
      Buttons.bind(checkHotReloadOnSave, hotReloadOnSave);

      final var btnDefault = new Button(grpDebugSettings, SWT.NONE);
      btnDefault.setText("Default");
      Buttons.onSelected(btnDefault, () -> hotReloadOnSave.set(defaultHotReloadOnSave.get()));
   }
}
