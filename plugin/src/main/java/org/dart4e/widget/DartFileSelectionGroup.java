/*
 * Copyright 2022 by the Dart4E authors.
 * SPDX-License-Identifier: EPL-2.0
 */
package org.dart4e.widget;

import static net.sf.jstuff.core.validation.NullAnalysisHelper.asNonNull;

import org.dart4e.localization.Messages;
import org.dart4e.model.buildsystem.BuildSystem;
import org.dart4e.model.buildsystem.DartBuildFile;
import org.dart4e.prefs.DartProjectPreference;
import org.dart4e.util.ui.GridDatas;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Text;

import de.sebthom.eclipse.commons.ui.Buttons;
import de.sebthom.eclipse.commons.ui.Texts;
import net.sf.jstuff.core.Strings;
import net.sf.jstuff.core.ref.MutableObservableRef;

/**
 * @author Sebastian Thomschke
 */
public class DartFileSelectionGroup extends Composite {

   private @Nullable DartProjectPreference projectPrefs;
   private Button btnBrowseForDartFile;
   public final MutableObservableRef<@Nullable IFile> selectedDartFile = MutableObservableRef.of(null);

   public DartFileSelectionGroup(final Composite parent) {
      this(parent, SWT.NONE);
   }

   public DartFileSelectionGroup(final Composite parent, final int style) {
      super(parent, style);

      if (parent.getLayout() instanceof GridLayout) {
         setLayoutData(GridDatas.fillHorizontalExcessive());
      }
      setLayout(GridLayoutFactory.fillDefaults().create());

      final var grpDartFile = new Group(this, SWT.NONE);
      grpDartFile.setLayoutData(GridDatas.fillHorizontalExcessive());
      grpDartFile.setText("Dart File");
      grpDartFile.setLayout(new GridLayout(2, false));

      final var txtSelectedDartFile = new Text(grpDartFile, SWT.BORDER);
      txtSelectedDartFile.setEditable(false);
      txtSelectedDartFile.setLayoutData(GridDatas.fillHorizontalExcessive());
      Texts.bind(txtSelectedDartFile, selectedDartFile, this::getDartFile, dartFile -> dartFile == null ? ""
         : dartFile.getProjectRelativePath().toString());

      btnBrowseForDartFile = new Button(grpDartFile, SWT.NONE);
      btnBrowseForDartFile.setText(Messages.Label_Browse);
      btnBrowseForDartFile.setEnabled(false);
      Buttons.onSelected(btnBrowseForDartFile, this::onBrowseForDartFile);
   }

   private @Nullable IFile getDartFile(final String path) {
      if (Strings.isBlank(path))
         return null;
      if (projectPrefs != null)
         return projectPrefs.getProject().getFile(path);
      return null;
   }

   private DartFileSelectionDialog createSelectDartFileDialog() {
      final var project = asNonNull(projectPrefs).getProject();
      final var dlg = new DartFileSelectionDialog(getShell(), "Dart file selection", project);
      dlg.setInitialSelections(selectedDartFile.get());
      return dlg;
   }

   private void onBrowseForDartFile() {
      final var dialog = createSelectDartFileDialog();

      if (dialog.open() == Window.OK) {
         selectedDartFile.set((IFile) dialog.getResult()[0]);
      }
   }

   public void setProject(final @Nullable IProject project) {
      if (projectPrefs != null && projectPrefs.getProject().equals(project))
         // nothing to do
         return;

      if (project == null) {
         projectPrefs = null;
         btnBrowseForDartFile.setEnabled(false);

         selectedDartFile.set(null);
      } else {
         projectPrefs = DartProjectPreference.get(project);
         btnBrowseForDartFile.setEnabled(true);

         var dartFile = selectedDartFile.get();
         if (dartFile == null) {
            if (BuildSystem.guessBuildSystemOfProject(project).findBuildFile(project) instanceof final DartBuildFile buildFile) {
               final var exes = buildFile.getExecutables();
               if (exes.isEmpty()) {
                  selectedDartFile.set(null);
               } else {
                  selectedDartFile.set(project.getFile(exes.get(0)));
               }
            } else {
               selectedDartFile.set(null);
            }
         } else {
            dartFile = project.getFile(dartFile.getProjectRelativePath());
            selectedDartFile.set(dartFile.exists() ? dartFile : null);
         }
      }
   }
}
