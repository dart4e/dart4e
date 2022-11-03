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
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.ui.AbstractLaunchConfigurationTab;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;

/**
 * @author Sebastian Thomschke
 */
public class CommandLaunchConfigTab extends AbstractLaunchConfigurationTab {

   private CommandLaunchConfigForm form = lazyNonNull();

   @Override
   public void createControl(final Composite parent) {
      form = new CommandLaunchConfigForm(parent, SWT.NONE);
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
      form.selectedProject.set(LaunchConfigurations.getProject(config));
      form.selectedProject.subscribe(this::updateLaunchConfigurationDialog);

      form.selectedAltSDK.set(LaunchConfigurations.getAlternativeDartSDK(config));
      form.selectedAltSDK.subscribe(this::updateLaunchConfigurationDialog);

      form.dartArgs.set(LaunchConfigurations.getProgramArgs(config));
      form.dartArgs.subscribe(this::updateLaunchConfigurationDialog);
   }

   @Override
   public boolean isValid(final ILaunchConfiguration launchConfig) {
      final var project = form.selectedProject.get();
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
      LaunchConfigurations.setProject(config, form.selectedProject.get());
      LaunchConfigurations.setProgramArgs(config, form.dartArgs.get());
      LaunchConfigurations.setAlternativeDartSDK(config, form.selectedAltSDK.get());
   }

   @Override
   public void setDefaults(final ILaunchConfigurationWorkingCopy config) {
   }
}
