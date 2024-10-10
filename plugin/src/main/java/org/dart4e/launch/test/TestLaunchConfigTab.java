/*
 * SPDX-FileCopyrightText: © The Dart4E authors
 * SPDX-FileContributor: Sebastian Thomschke
 * SPDX-License-Identifier: EPL-2.0
 * SPDX-ArtifactOfProjectHomePage: https://github.com/dart4e/dart4e
 */
package org.dart4e.launch.test;

import static java.util.Collections.singletonList;
import static net.sf.jstuff.core.validation.NullAnalysisHelper.lateNonNull;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import org.apache.commons.lang3.ArrayUtils;
import org.dart4e.Constants;
import org.dart4e.Dart4EPlugin;
import org.dart4e.launch.LaunchConfigurations;
import org.dart4e.localization.Messages;
import org.dart4e.model.DartSDK;
import org.dart4e.util.ui.GridDatas;
import org.dart4e.widget.DartFileSelectionDialog;
import org.dart4e.widget.DartProjectSelectionGroup;
import org.dart4e.widget.DartSDKSelectionGroup;
import org.dart4e.widget.TextFieldGroup;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.ui.AbstractLaunchConfigurationTab;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.ui.dialogs.ContainerCheckedTreeViewer;
import org.eclipse.ui.model.BaseWorkbenchContentProvider;
import org.eclipse.ui.model.WorkbenchLabelProvider;

import de.sebthom.eclipse.commons.ui.Colors;
import de.sebthom.eclipse.commons.ui.Dialogs;
import net.sf.jstuff.core.ref.MutableObservableRef;

/**
 * @author Sebastian Thomschke
 */
public class TestLaunchConfigTab extends AbstractLaunchConfigurationTab {

   private MutableObservableRef<@Nullable IProject> selectedProject = lateNonNull();
   private MutableObservableRef<List<IResource>> selectedTestResources = MutableObservableRef.of(Collections.emptyList());
   private MutableObservableRef<@Nullable DartSDK> selectedAltSDK = lateNonNull();
   private MutableObservableRef<String> testCommandArgs = lateNonNull();
   private MutableObservableRef<String> vmArgs = lateNonNull();

   @Override
   public void createControl(final Composite parent) {
      final var form = new Composite(parent, SWT.NONE);
      form.setLayout(new GridLayout(1, false));

      selectedProject = new DartProjectSelectionGroup(form).selectedProject;

      final var grpTestCandidates = new Group(form, SWT.NONE);
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
            treeTestFiles.setChecked(project.getFolder(Constants.PROJECT_TEST_DIRNAME), true);
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

      testCommandArgs = new TextFieldGroup(form,
         "Test command arguments (.e.g --name <regex>, --tags <name,...>, --exclude-tags <name,...>)").text;
      vmArgs = new TextFieldGroup(form, "Dart VM arguments").text;
      selectedAltSDK = new DartSDKSelectionGroup(form).selectedAltSDK;

      setControl(form);
   }

   @Override
   public @Nullable String getId() {
      return TestLaunchConfigTab.class.getName();
   }

   @Override
   public @Nullable Image getImage() {
      return Dart4EPlugin.get().getImageRegistry().get(Constants.IMAGE_DART_ICON);
   }

   @Override
   public String getName() {
      return Messages.Label_Dart_Test_Configuration;
   }

   @Override
   public void initializeFrom(final ILaunchConfiguration config) {
      try {
         selectedProject.set(LaunchConfigurations.getProject(config));
         selectedProject.subscribe(this::updateLaunchConfigurationDialog);

         final var project = LaunchConfigurations.getProject(config);
         if (project != null) {
            selectedTestResources.set( //
               config.getAttribute(TestLaunchConfigurations.LAUNCH_ATTR_DART_TEST_RESOURCES, singletonList(Constants.PROJECT_TEST_DIRNAME))
                  .stream() //
                  .map(project::findMember) //
                  .filter(Objects::nonNull) //
                  .toList() //
            );
         }
         selectedTestResources.subscribe(this::updateLaunchConfigurationDialog);

         selectedAltSDK.set(LaunchConfigurations.getAlternativeDartSDK(config));
         selectedAltSDK.subscribe(this::updateLaunchConfigurationDialog);

         testCommandArgs.set(LaunchConfigurations.getProgramArgs(config));
         testCommandArgs.subscribe(this::updateLaunchConfigurationDialog);

         vmArgs.set(LaunchConfigurations.getDartVMArgs(config));
         vmArgs.subscribe(this::updateLaunchConfigurationDialog);
      } catch (final CoreException ex) {
         Dialogs.showStatus(Messages.Launch_InitializingLaunchConfigTabFailed, Dart4EPlugin.status().createError(ex), true);
      }
   }

   @Override
   public boolean isValid(final ILaunchConfiguration launchConfig) {
      final var project = selectedProject.get();
      if (project == null) {
         setErrorMessage("No project selected");
         return false;
      }
      if (!project.exists()) {
         setErrorMessage("Project [" + project.getName() + "] does not exist.");
         return false;
      }

      setErrorMessage(null);

      return super.isValid(launchConfig);
   }

   @Override
   public void performApply(final ILaunchConfigurationWorkingCopy config) {
      LaunchConfigurations.setProject(config, selectedProject.get());

      final var selectedTestResources = this.selectedTestResources.get();
      if (selectedTestResources.isEmpty()) {
         config.removeAttribute(TestLaunchConfigurations.LAUNCH_ATTR_DART_TEST_RESOURCES);
      } else {
         config.setAttribute(TestLaunchConfigurations.LAUNCH_ATTR_DART_TEST_RESOURCES, //
            selectedTestResources.stream().map(r -> r.getProjectRelativePath().toString()).toList());
      }

      LaunchConfigurations.setProgramArgs(config, testCommandArgs.get());
      LaunchConfigurations.setDartVMArgs(config, vmArgs.get());
      LaunchConfigurations.setAlternativeDartSDK(config, selectedAltSDK.get());
   }

   @Override
   public void setDefaults(final ILaunchConfigurationWorkingCopy config) {
   }
}
