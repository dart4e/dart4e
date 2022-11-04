/*
 * Copyright 2022 by the Dart4E authors.
 * SPDX-License-Identifier: EPL-2.0
 */
package org.dart4e.launch.command;

import static net.sf.jstuff.core.validation.NullAnalysisHelper.lazyNonNull;

import org.dart4e.Constants;
import org.dart4e.Dart4EPlugin;
import org.dart4e.launch.LaunchConfigurations;
import org.dart4e.localization.Messages;
import org.dart4e.model.DartSDK;
import org.dart4e.widget.DartProjectSelectionGroup;
import org.dart4e.widget.DartSDKSelectionGroup;
import org.dart4e.widget.TextFieldGroup;
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
public class CommandLaunchConfigTab extends AbstractLaunchConfigurationTab {

   private MutableObservableRef<@Nullable IProject> selectedProject = lazyNonNull();
   private MutableObservableRef<@Nullable DartSDK> selectedAltSDK = lazyNonNull();
   private MutableObservableRef<String> dartArgs = lazyNonNull();

   @Override
   public void createControl(final Composite parent) {
      final var form = new Composite(parent, SWT.NONE);
      form.setLayout(new GridLayout(1, false));

      selectedProject = new DartProjectSelectionGroup(form).selectedProject;
      dartArgs = new TextFieldGroup(form, "Dart command and arguments").text;
      selectedAltSDK = new DartSDKSelectionGroup(form).selectedAltSDK;

      setControl(form);
   }

   @Override
   public String getId() {
      return CommandLaunchConfigTab.class.getName();
   }

   @Override
   public @Nullable Image getImage() {
      return Dart4EPlugin.get().getImageRegistry().get(Constants.IMAGE_DART_ICON);
   }

   @Override
   public String getName() {
      return Messages.Label_Dart_Command_Configuration;
   }

   @Override
   public void initializeFrom(final ILaunchConfiguration config) {
      selectedProject.set(LaunchConfigurations.getProject(config));
      selectedProject.subscribe(this::updateLaunchConfigurationDialog);

      selectedAltSDK.set(LaunchConfigurations.getAlternativeDartSDK(config));
      selectedAltSDK.subscribe(this::updateLaunchConfigurationDialog);

      dartArgs.set(LaunchConfigurations.getProgramArgs(config));
      dartArgs.subscribe(this::updateLaunchConfigurationDialog);
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
      LaunchConfigurations.setProgramArgs(config, dartArgs.get());
      LaunchConfigurations.setAlternativeDartSDK(config, selectedAltSDK.get());
   }

   @Override
   public void setDefaults(final ILaunchConfigurationWorkingCopy config) {
   }
}
