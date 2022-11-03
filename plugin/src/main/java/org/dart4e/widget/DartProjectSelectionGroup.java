/*
 * Copyright 2022 by the Dart4E authors.
 * SPDX-License-Identifier: EPL-2.0
 */
package org.dart4e.widget;

import org.dart4e.localization.Messages;
import org.dart4e.project.DartProjectNature;
import org.dart4e.project.DartProjectSelectionDialog;
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
public class DartProjectSelectionGroup extends Composite {

   public final MutableObservableRef<@Nullable IProject> selectedProject = MutableObservableRef.of(null);

   public DartProjectSelectionGroup(final Composite parent, final Object layoutData) {
      this(parent, SWT.NONE, layoutData);
   }

   public DartProjectSelectionGroup(final Composite parent, final int style, final Object layoutData) {
      super(parent, style);

      setLayoutData(layoutData);
      setLayout(GridLayoutFactory.fillDefaults().create());

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
