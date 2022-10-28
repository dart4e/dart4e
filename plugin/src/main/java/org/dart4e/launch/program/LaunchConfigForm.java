/*
 * Copyright 2022 by the Dart4E authors.
 * SPDX-License-Identifier: EPL-2.0
 */
package org.dart4e.launch.program;

import org.dart4e.localization.Messages;
import org.dart4e.model.DartSDK;
import org.dart4e.project.DartProjectNature;
import org.dart4e.project.DartProjectSelectionDialog;
import org.dart4e.util.ui.GridDatas;
import org.dart4e.widget.DartFileSelectionGroup;
import org.dart4e.widget.DartSDKSelectionGroup;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Text;

import de.sebthom.eclipse.commons.resources.Projects;
import de.sebthom.eclipse.commons.ui.Buttons;
import de.sebthom.eclipse.commons.ui.Texts;
import net.sf.jstuff.core.ref.MutableObservableRef;

/**
 * @author Sebastian Thomschke
 */
public class LaunchConfigForm extends Composite {

   public final MutableObservableRef<@Nullable IProject> selectedProject = MutableObservableRef.of(null);
   public final MutableObservableRef<@Nullable IFile> selectedDartFile;
   public final MutableObservableRef<@Nullable DartSDK> selectedAltSDK;
   public final MutableObservableRef<String> programArgs = MutableObservableRef.of("");
   public final MutableObservableRef<String> vmArgs = MutableObservableRef.of("");

   public LaunchConfigForm(final Composite parent, final int style) {
      super(parent, style);
      setLayout(new GridLayout(1, false));

      final var grpProject = new Group(this, SWT.NONE);
      grpProject.setLayout(new GridLayout(2, false));
      grpProject.setLayoutData(GridDatas.fillHorizontalExcessive());
      grpProject.setText(Messages.Label_Project);

      final var txtSelectedProject = new Text(grpProject, SWT.BORDER);
      txtSelectedProject.setEditable(false);
      txtSelectedProject.setLayoutData(GridDatas.fillHorizontalExcessive());
      Texts.bind(txtSelectedProject, selectedProject, //
         projectName -> Projects.findOpenProjectWithNature(projectName, DartProjectNature.NATURE_ID), //
         project -> project == null ? "" : project.getName() //
      );

      final var btnBrowseProject = new Button(grpProject, SWT.NONE);
      btnBrowseProject.setText(Messages.Label_Browse);
      Buttons.onSelected(btnBrowseProject, this::onSelectProject);

      final var grpDartFile = new DartFileSelectionGroup(this, GridDatas.fillHorizontalExcessive());
      selectedDartFile = grpDartFile.selectedDartFile;
      selectedProject.subscribe(grpDartFile::setProject);

      final var grpDartProgramArgs = new Group(this, SWT.NONE);
      grpDartProgramArgs.setLayout(new GridLayout(2, false));
      grpDartProgramArgs.setLayoutData(GridDatas.fillHorizontalExcessive());
      grpDartProgramArgs.setText("Program Arguments");
      final var txtDartProgramArgs = new Text(grpDartProgramArgs, SWT.BORDER);
      txtDartProgramArgs.setLayoutData(GridDatas.fillHorizontalExcessive());
      Texts.bind(txtDartProgramArgs, programArgs);

      final var grpDartVMArgs = new Group(this, SWT.NONE);
      grpDartVMArgs.setLayout(new GridLayout(2, false));
      grpDartVMArgs.setLayoutData(GridDatas.fillHorizontalExcessive());
      grpDartVMArgs.setText("Dart VM Arguments");
      final var txtDartVMArgs = new Text(grpDartVMArgs, SWT.BORDER);
      txtDartVMArgs.setLayoutData(GridDatas.fillHorizontalExcessive());
      Texts.bind(txtDartVMArgs, vmArgs);

      final var grpDartSDKSelection = new DartSDKSelectionGroup(this, GridDatas.fillHorizontalExcessive());
      selectedAltSDK = grpDartSDKSelection.selectedAltSDK;
   }

   private void onSelectProject() {
      var project = selectedProject.get(); //
      project = new DartProjectSelectionDialog(getShell()) //
         .setSelectedProject(project) //
         .show();

      if (project != null) {
         selectedProject.set(project);
      }
   }
}
