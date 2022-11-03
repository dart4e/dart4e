/*
 * Copyright 2022 by the Dart4E authors.
 * SPDX-License-Identifier: EPL-2.0
 */
package org.dart4e.launch.test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.apache.commons.lang3.ArrayUtils;
import org.dart4e.Constants;
import org.dart4e.model.DartSDK;
import org.dart4e.util.ui.GridDatas;
import org.dart4e.widget.DartFileSelectionDialog;
import org.dart4e.widget.DartProjectSelectionGroup;
import org.dart4e.widget.DartSDKSelectionGroup;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.dialogs.ContainerCheckedTreeViewer;
import org.eclipse.ui.model.BaseWorkbenchContentProvider;
import org.eclipse.ui.model.WorkbenchLabelProvider;

import de.sebthom.eclipse.commons.ui.Colors;
import de.sebthom.eclipse.commons.ui.Texts;
import net.sf.jstuff.core.ref.MutableObservableRef;

/**
 * @author Sebastian Thomschke
 */
public class TestLaunchConfigForm extends Composite {

   public final MutableObservableRef<@Nullable IProject> selectedProject;
   public final MutableObservableRef<List<IResource>> selectedTestResources = MutableObservableRef.of(Collections.emptyList());
   public final MutableObservableRef<@Nullable DartSDK> selectedAltSDK;
   public final MutableObservableRef<String> programArgs = MutableObservableRef.of("");
   public final MutableObservableRef<String> vmArgs = MutableObservableRef.of("");

   public TestLaunchConfigForm(final Composite parent, final int style) {
      super(parent, style);
      setLayout(new GridLayout(1, false));

      final var grpProject = new DartProjectSelectionGroup(this, SWT.NONE, GridDatas.fillHorizontalExcessive());
      selectedProject = grpProject.selectedProject;

      final var grpTestCandidates = new Group(this, SWT.NONE);
      grpTestCandidates.setLayout(new GridLayout(1, false));
      grpTestCandidates.setLayoutData(GridDatas.fillHorizontalExcessive(1, 150));
      grpTestCandidates.setText("Folders/files to test");

      final var treeTestFiles = new ContainerCheckedTreeViewer(grpTestCandidates, SWT.CHECK);
      treeTestFiles.getTree().setLayoutData(GridDatas.fillExcessive());
      treeTestFiles.getTree().setBackground(Colors.get(SWT.COLOR_WIDGET_BACKGROUND));
      final var treeTestFilesContentProvider = new BaseWorkbenchContentProvider();
      treeTestFiles.setContentProvider(treeTestFilesContentProvider);
      treeTestFiles.setLabelProvider(new WorkbenchLabelProvider());
      treeTestFiles.addFilter(new DartFileSelectionDialog.DartFileViewerFilter());
      selectedProject.subscribe(project -> {
         treeTestFiles.setInput(project);
         if (project != null) {
            treeTestFiles.setChecked(project.getFolder(Constants.TEST_FOLDER_NAME), true);
         }
      });
      treeTestFiles.addCheckStateListener( //
         ev -> selectedTestResources.set(Arrays.stream(treeTestFiles.getCheckedElements()) //
            .filter(e -> { //
               if (treeTestFiles.getGrayed(e)) // ignore all half-checked elements
                  return false;
               final var p = treeTestFilesContentProvider.getParent(e);
               if (p != null && treeTestFiles.getChecked(p) && !treeTestFiles.getGrayed(p))
                  return false; // ignore child elements where the parent is full-checked
               return true;
            }) //
            .map(IResource.class::cast) //
            .toList() //
         ));
      selectedTestResources.subscribe(res -> {
         // workaround for "treeTestFiles.setCheckedElements(res::toArray);"
         // which does not set the half-checked state of parent elements
         treeTestFiles.setCheckedElements(ArrayUtils.EMPTY_OBJECT_ARRAY);
         for (final var elem : res) {
            treeTestFiles.setChecked(elem, true);
         }
      });

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
}
