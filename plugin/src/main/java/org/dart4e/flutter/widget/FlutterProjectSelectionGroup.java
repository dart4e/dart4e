/*
 * Copyright 2022 by the Dart4E authors.
 * SPDX-License-Identifier: EPL-2.0
 */
package org.dart4e.flutter.widget;

import org.dart4e.flutter.project.FlutterProjectNature;
import org.dart4e.flutter.project.FlutterProjectSelectionDialog;
import org.dart4e.localization.Messages;
import org.dart4e.util.ui.GridDatas;
import org.eclipse.core.resources.IProject;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.jface.layout.GridLayoutFactory;
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
public class FlutterProjectSelectionGroup extends Composite {

   public final MutableObservableRef<@Nullable IProject> selectedProject = MutableObservableRef.of(null);

   public FlutterProjectSelectionGroup(final Composite parent) {
      this(parent, SWT.NONE);
   }

   public FlutterProjectSelectionGroup(final Composite parent, final int style) {
      super(parent, style);

      if (parent.getLayout() instanceof GridLayout) {
         setLayoutData(GridDatas.fillHorizontalExcessive());
      }
      setLayout(GridLayoutFactory.fillDefaults().create());

      final var grpProject = new Group(this, SWT.NONE);
      grpProject.setLayout(new GridLayout(2, false));
      grpProject.setLayoutData(GridDatas.fillHorizontalExcessive());
      grpProject.setText(Messages.Label_Project);

      final var txtSelectedProject = new Text(grpProject, SWT.BORDER);
      txtSelectedProject.setEditable(false);
      txtSelectedProject.setLayoutData(GridDatas.fillHorizontalExcessive());
      Texts.bind(txtSelectedProject, selectedProject, //
         projectName -> Projects.findOpenProjectWithNature(projectName, FlutterProjectNature.NATURE_ID), //
         project -> project == null ? "" : project.getName() //
      );

      final var btnBrowseProject = new Button(grpProject, SWT.NONE);
      btnBrowseProject.setText(Messages.Label_Browse);
      Buttons.onSelected(btnBrowseProject, this::onSelectProject);
   }

   private void onSelectProject() {
      var project = selectedProject.get(); //
      project = new FlutterProjectSelectionDialog(getShell()) //
         .setSelectedProject(project) //
         .show();

      if (project != null) {
         selectedProject.set(project);
      }
   }
}
