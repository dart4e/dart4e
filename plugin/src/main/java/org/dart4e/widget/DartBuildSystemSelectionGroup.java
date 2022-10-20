/*
 * Copyright 2022 by the Dart4E authors.
 * SPDX-License-Identifier: EPL-2.0
 */
package org.dart4e.widget;

import org.dart4e.model.buildsystem.BuildSystem;
import org.dart4e.prefs.DartProjectPreference;
import org.dart4e.util.ui.GridDatas;
import org.eclipse.core.resources.IProject;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;

import de.sebthom.eclipse.commons.ui.widgets.ComboWrapper;
import net.sf.jstuff.core.ref.MutableObservableRef;

/**
 * @author Sebastian Thomschke
 */
public class DartBuildSystemSelectionGroup extends Composite {

   private final ComboWrapper<BuildSystem> cmbBuildSystem;
   public final MutableObservableRef<BuildSystem> selectedBuildSystem = MutableObservableRef.of(BuildSystem.DART);

   public DartBuildSystemSelectionGroup(final Composite parent, final Object layoutData) {
      this(parent, SWT.NONE, layoutData);
   }

   public DartBuildSystemSelectionGroup(final Composite parent, final int style, final Object layoutData) {
      super(parent, style);

      setLayoutData(layoutData);
      setLayout(GridLayoutFactory.fillDefaults().create());

      final var grpBuildSystem = new Group(this, SWT.NONE);
      grpBuildSystem.setLayoutData(GridDatas.fillHorizontalExcessive());
      grpBuildSystem.setLayout(GridLayoutFactory.swtDefaults().numColumns(2).create());
      grpBuildSystem.setText("Build System");

      cmbBuildSystem = new ComboWrapper<BuildSystem>(grpBuildSystem, GridDataFactory.fillDefaults().create()) //
         .setEnabled(false) //
         .setItems(BuildSystem.values()).setLabelProvider(bs -> switch (bs) {
            case DART -> "Dart";
            case FLUTTER -> "Flutter";
            default -> bs.toString();
         }) //
         .bind(selectedBuildSystem);
   }

   public void setProject(final @Nullable IProject project) {
      if (project == null) {
         cmbBuildSystem.setEnabled(false);
      } else {
         final var prefs = DartProjectPreference.get(project);
         selectedBuildSystem.set(prefs.getBuildSystem());
         cmbBuildSystem.setEnabled(true);
      }
   }
}
