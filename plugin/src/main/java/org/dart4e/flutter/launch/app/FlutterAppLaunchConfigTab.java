/*
 * SPDX-FileCopyrightText: © The Dart4E authors
 * SPDX-FileContributor: Sebastian Thomschke
 * SPDX-License-Identifier: EPL-2.0
 * SPDX-ArtifactOfProjectHomePage: https://github.com/dart4e/dart4e
 */
package org.dart4e.flutter.launch.app;

import static net.sf.jstuff.core.validation.NullAnalysisHelper.lazyNonNull;

import org.dart4e.Constants;
import org.dart4e.Dart4EPlugin;
import org.dart4e.flutter.launch.FlutterLaunchConfigurations;
import org.dart4e.flutter.model.Device;
import org.dart4e.flutter.model.FlutterSDK;
import org.dart4e.flutter.prefs.FlutterProjectPreference;
import org.dart4e.flutter.widget.FlutterProjectSelectionGroup;
import org.dart4e.flutter.widget.FlutterSDKSelectionGroup;
import org.dart4e.launch.LaunchConfigurations;
import org.dart4e.localization.Messages;
import org.dart4e.util.ui.GridDatas;
import org.dart4e.widget.DartFileSelectionGroup;
import org.dart4e.widget.TextFieldGroup;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
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

import de.sebthom.eclipse.commons.ui.Buttons;
import de.sebthom.eclipse.commons.ui.UI;
import de.sebthom.eclipse.commons.ui.widgets.ComboWrapper;
import net.sf.jstuff.core.ref.MutableObservableRef;

/**
 * @author Sebastian Thomschke
 */
public class FlutterAppLaunchConfigTab extends AbstractLaunchConfigurationTab {

   private MutableObservableRef<@Nullable IProject> selectedProject = lazyNonNull();
   private MutableObservableRef<@Nullable IFile> selectedDartFile = lazyNonNull();
   private MutableObservableRef<@Nullable FlutterSDK> selectedAltSDK = lazyNonNull();
   private final MutableObservableRef<@Nullable Device> selectedDevice = MutableObservableRef.of(null);
   private MutableObservableRef<String> flutterArgs = lazyNonNull();

   @Override
   public void createControl(final Composite parent) {
      final var form = new Composite(parent, SWT.NONE);
      form.setLayout(new GridLayout(1, false));

      selectedProject = new FlutterProjectSelectionGroup(form).selectedProject;

      final var grpDartFile = new DartFileSelectionGroup(form);
      selectedDartFile = grpDartFile.selectedDartFile;
      selectedProject.subscribe(grpDartFile::setProject);

      flutterArgs = new TextFieldGroup(form, "'flutter run' arguments").text;

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
      return FlutterAppLaunchConfigTab.class.getName();
   }

   @Override
   public @Nullable Image getImage() {
      return Dart4EPlugin.get().getImageRegistry().get(Constants.IMAGE_FLUTTER_ICON);
   }

   @Override
   public String getName() {
      return Messages.Label_Flutter_Command_Configuration;
   }

   @Override
   public void initializeFrom(final ILaunchConfiguration config) {
      selectedProject.set(LaunchConfigurations.getProject(config));
      selectedProject.subscribe(this::updateLaunchConfigurationDialog);

      selectedDartFile.set(LaunchConfigurations.getDartMainFile(config));
      selectedDartFile.subscribe(this::updateLaunchConfigurationDialog);

      selectedAltSDK.set(FlutterLaunchConfigurations.getAlternativeFlutterSDK(config));
      selectedAltSDK.subscribe(this::updateLaunchConfigurationDialog);

      selectedDevice.set(FlutterLaunchConfigurations.getFlutterDevice(config));
      selectedDevice.subscribe(this::updateLaunchConfigurationDialog);

      flutterArgs.set(LaunchConfigurations.getProgramArgs(config));
      flutterArgs.subscribe(this::updateLaunchConfigurationDialog);
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

      final var dartFile = selectedDartFile.get();
      if (dartFile == null) {
         setErrorMessage("No Dart file selected");
         return false;
      }
      if (!dartFile.exists()) {
         setErrorMessage("Dart file \"" + project.getName() + "/" + dartFile.getProjectRelativePath() + "\" does not exist.");
         return false;
      }

      setErrorMessage(null);

      return super.isValid(launchConfig);
   }

   @Override
   public void performApply(final ILaunchConfigurationWorkingCopy config) {
      LaunchConfigurations.setProject(config, selectedProject.get());
      LaunchConfigurations.setDartMainFile(config, selectedDartFile.get());
      LaunchConfigurations.setProgramArgs(config, flutterArgs.get());
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
