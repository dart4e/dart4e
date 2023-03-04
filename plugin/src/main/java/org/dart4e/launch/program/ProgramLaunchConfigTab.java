/*
 * SPDX-FileCopyrightText: © The Dart4E authors
 * SPDX-FileContributor: Sebastian Thomschke
 * SPDX-License-Identifier: EPL-2.0
 * SPDX-ArtifactOfProjectHomePage: https://github.com/dart4e/dart4e
 */
package org.dart4e.launch.program;

import static net.sf.jstuff.core.validation.NullAnalysisHelper.lazyNonNull;

import org.dart4e.Constants;
import org.dart4e.Dart4EPlugin;
import org.dart4e.launch.LaunchConfigurations;
import org.dart4e.localization.Messages;
import org.dart4e.model.DartSDK;
import org.dart4e.widget.DartFileSelectionGroup;
import org.dart4e.widget.DartProjectSelectionGroup;
import org.dart4e.widget.DartSDKSelectionGroup;
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
import org.eclipse.swt.widgets.Composite;

import net.sf.jstuff.core.ref.MutableObservableRef;

/**
 * @author Sebastian Thomschke
 */
public class ProgramLaunchConfigTab extends AbstractLaunchConfigurationTab {

   private MutableObservableRef<@Nullable IProject> selectedProject = lazyNonNull();
   private MutableObservableRef<@Nullable IFile> selectedDartFile = lazyNonNull();
   private MutableObservableRef<@Nullable DartSDK> selectedAltSDK = lazyNonNull();
   private MutableObservableRef<String> programArgs = lazyNonNull();
   private MutableObservableRef<String> vmArgs = lazyNonNull();

   @Override
   public void createControl(final Composite parent) {
      final var form = new Composite(parent, SWT.NONE);
      form.setLayout(new GridLayout(1, false));

      selectedProject = new DartProjectSelectionGroup(form).selectedProject;

      final var grpDartFile = new DartFileSelectionGroup(form);
      selectedDartFile = grpDartFile.selectedDartFile;
      selectedProject.subscribe(grpDartFile::setProject);

      programArgs = new TextFieldGroup(form, "Program arguments").text;
      vmArgs = new TextFieldGroup(form, "Dart VM arguments").text;
      selectedAltSDK = new DartSDKSelectionGroup(form).selectedAltSDK;

      setControl(form);
   }

   @Override
   public String getId() {
      return ProgramLaunchConfigTab.class.getName();
   }

   @Override
   public @Nullable Image getImage() {
      return Dart4EPlugin.get().getImageRegistry().get(Constants.IMAGE_DART_ICON);
   }

   @Override
   public String getName() {
      return Messages.Label_Dart_Program_Configuration;
   }

   @Override
   public void initializeFrom(final ILaunchConfiguration config) {
      selectedProject.set(LaunchConfigurations.getProject(config));
      selectedProject.subscribe(this::updateLaunchConfigurationDialog);

      selectedDartFile.set(LaunchConfigurations.getDartMainFile(config));
      selectedDartFile.subscribe(this::updateLaunchConfigurationDialog);

      selectedAltSDK.set(LaunchConfigurations.getAlternativeDartSDK(config));
      selectedAltSDK.subscribe(this::updateLaunchConfigurationDialog);

      programArgs.set(LaunchConfigurations.getProgramArgs(config));
      programArgs.subscribe(this::updateLaunchConfigurationDialog);

      vmArgs.set(LaunchConfigurations.getDartVMArgs(config));
      vmArgs.subscribe(this::updateLaunchConfigurationDialog);
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
      LaunchConfigurations.setProgramArgs(config, programArgs.get());
      LaunchConfigurations.setDartVMArgs(config, vmArgs.get());
      LaunchConfigurations.setAlternativeDartSDK(config, selectedAltSDK.get());
   }

   @Override
   public void setDefaults(final ILaunchConfigurationWorkingCopy config) {
   }
}
