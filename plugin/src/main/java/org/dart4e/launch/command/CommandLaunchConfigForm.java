/*
 * Copyright 2022 by the Dart4E authors.
 * SPDX-License-Identifier: EPL-2.0
 */
package org.dart4e.launch.command;

import org.dart4e.model.DartSDK;
import org.dart4e.util.ui.GridDatas;
import org.dart4e.widget.DartProjectSelectionGroup;
import org.dart4e.widget.DartSDKSelectionGroup;
import org.eclipse.core.resources.IProject;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Text;

import de.sebthom.eclipse.commons.ui.Texts;
import net.sf.jstuff.core.ref.MutableObservableRef;

/**
 * @author Sebastian Thomschke
 */
public class CommandLaunchConfigForm extends Composite {

   public final MutableObservableRef<@Nullable IProject> selectedProject;
   public final MutableObservableRef<@Nullable DartSDK> selectedAltSDK;
   public final MutableObservableRef<String> dartArgs = MutableObservableRef.of("");

   public CommandLaunchConfigForm(final Composite parent, final int style) {
      super(parent, style);
      setLayout(new GridLayout(1, false));

      final var grpProject = new DartProjectSelectionGroup(this, SWT.NONE, GridDatas.fillHorizontalExcessive());
      selectedProject = grpProject.selectedProject;

      final var grpDartArgs = new Group(this, SWT.NONE);
      grpDartArgs.setLayout(new GridLayout(2, false));
      grpDartArgs.setLayoutData(GridDatas.fillHorizontalExcessive());
      grpDartArgs.setText("Dart Arguments");
      final var txtDartArgs = new Text(grpDartArgs, SWT.BORDER);
      txtDartArgs.setLayoutData(GridDatas.fillHorizontalExcessive());
      Texts.bind(txtDartArgs, dartArgs);

      final var grpDartSDKSelection = new DartSDKSelectionGroup(this, GridDatas.fillHorizontalExcessive());
      selectedAltSDK = grpDartSDKSelection.selectedAltSDK;
   }
}
