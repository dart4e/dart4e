/*
 * SPDX-FileCopyrightText: © The Dart4E authors
 * SPDX-FileContributor: Sebastian Thomschke
 * SPDX-License-Identifier: EPL-2.0
 * SPDX-ArtifactOfProjectHomePage: https://github.com/dart4e/dart4e
 */
package org.dart4e.flutter.launch.test;

import static java.util.Collections.singletonList;
import static net.sf.jstuff.core.validation.NullAnalysisHelper.lazyNonNull;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import org.apache.commons.lang3.ArrayUtils;
import org.dart4e.Constants;
import org.dart4e.Dart4EPlugin;
import org.dart4e.flutter.launch.FlutterLaunchConfigurations;
import org.dart4e.flutter.model.Device;
import org.dart4e.flutter.model.FlutterSDK;
import org.dart4e.flutter.prefs.FlutterProjectPreference;
import org.dart4e.flutter.widget.FlutterProjectSelectionGroup;
import org.dart4e.flutter.widget.FlutterSDKSelectionGroup;
import org.dart4e.launch.LaunchConfigurations;
import org.dart4e.launch.test.TestLaunchConfigurations;
import org.dart4e.localization.Messages;
import org.dart4e.util.ui.GridDatas;
import org.dart4e.widget.DartFileSelectionDialog;
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
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.ui.dialogs.ContainerCheckedTreeViewer;
import org.eclipse.ui.model.BaseWorkbenchContentProvider;
import org.eclipse.ui.model.WorkbenchLabelProvider;

import de.sebthom.eclipse.commons.ui.Buttons;
import de.sebthom.eclipse.commons.ui.Colors;
import de.sebthom.eclipse.commons.ui.Dialogs;
import de.sebthom.eclipse.commons.ui.UI;
import de.sebthom.eclipse.commons.ui.widgets.ComboWrapper;
import net.sf.jstuff.core.ref.MutableObservableRef;

/**
 * @author Sebastian Thomschke
 */
public class FlutterTestLaunchConfigTab extends AbstractLaunchConfigurationTab {

   private MutableObservableRef<@Nullable IProject> selectedProject = lazyNonNull();
   private final MutableObservableRef<List<IResource>> selectedTestResources = MutableObservableRef.of(Collections.emptyList());
   private MutableObservableRef<@Nullable FlutterSDK> selectedAltSDK = lazyNonNull();
   private MutableObservableRef<String> testCommandArgs = lazyNonNull();
   private final MutableObservableRef<@Nullable Device> selectedDevice = MutableObservableRef.of(null);

   @Override
   public void createControl(final Composite parent) {
      final var form = new Composite(parent, SWT.NONE);
      form.setLayout(new GridLayout(1, false));

      selectedProject = new FlutterProjectSelectionGroup(form).selectedProject;

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

      final var grpDevice = new Group(form, SWT.NONE);
      grpDevice.setLayout(new GridLayout(2, false));
      grpDevice.setLayoutData(GridDatas.fillHorizontalExcessive());
      grpDevice.setText("Device");
      final var cmbDevice = new ComboWrapper<Device>(grpDevice, GridDatas.fillHorizontalExcessive()) //
         .setLabelProvider(device -> device.name + " [" + device.id + "]" + (device.isEmulator ? " (emulator)" : "")) //
         .setLabelComparator(String::compareTo) //
         .bind(selectedDevice);
      cmbDevice.setEnabled(false);
      final var btnRefresh = new Button(grpDevice, SWT.PUSH);
      btnRefresh.setImage(Dart4EPlugin.get().getSharedImage("platform:/plugin/org.eclipse.debug.ui/icons/full/obj16/refresh_tab.png"));
      btnRefresh.setEnabled(false);
      Buttons.onSelected(btnRefresh, () -> {
         cmbDevice.setEnabled(false);
         btnRefresh.setEnabled(false);
         refreshDeviceList(cmbDevice, btnRefresh);
      });
      selectedProject.subscribe(p -> refreshDeviceList(cmbDevice, btnRefresh));

      selectedAltSDK = new FlutterSDKSelectionGroup(form).selectedAltSDK;
      selectedAltSDK.subscribe(s -> refreshDeviceList(cmbDevice, btnRefresh));

      setControl(form);
   }

   @Override
   public String getId() {
      return FlutterTestLaunchConfigTab.class.getName();
   }

   @Override
   public @Nullable Image getImage() {
      return Dart4EPlugin.get().getImageRegistry().get(Constants.IMAGE_FLUTTER_ICON);
   }

   @Override
   public String getName() {
      return Messages.Label_Flutter_Test_Configuration;
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

         selectedAltSDK.set(FlutterLaunchConfigurations.getAlternativeFlutterSDK(config));
         selectedAltSDK.subscribe(this::updateLaunchConfigurationDialog);

         selectedDevice.set(FlutterLaunchConfigurations.getFlutterDevice(config));
         selectedDevice.subscribe(this::updateLaunchConfigurationDialog);

         testCommandArgs.set(LaunchConfigurations.getProgramArgs(config));
         testCommandArgs.subscribe(this::updateLaunchConfigurationDialog);
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
      FlutterLaunchConfigurations.setAlternativeFlutterSDK(config, selectedAltSDK.get());
      FlutterLaunchConfigurations.setFlutterDevice(config, selectedDevice.get());
   }

   private void refreshDeviceList(final ComboWrapper<Device> combo, final Button btnRefresh) {
      var sdk = selectedAltSDK.get();
      if (sdk == null) {
         final var project = selectedProject.get();
         if (project != null) {
            sdk = FlutterProjectPreference.get(project).getEffectiveFlutterSDK();
         }
      }
      if (sdk == null)
         return;
      sdk.getSupportedDevices().thenAccept(items -> UI.run(() -> {
         if (!combo.getCombo().isDisposed()) {
            combo.setItems(items);
            combo.setEnabled(true);
            btnRefresh.setEnabled(true);
         }
      })).exceptionally(th -> {
         UI.run(() -> btnRefresh.setEnabled(true));
         return null;
      });
   }

   @Override
   public void setDefaults(final ILaunchConfigurationWorkingCopy config) {
   }
}
