/*
 * Copyright 2022 by the Dart4E authors.
 * SPDX-License-Identifier: EPL-2.0
 */
package org.dart4e.launch.test;

import static java.util.Collections.singletonList;
import static net.sf.jstuff.core.validation.NullAnalysisHelper.lazyNonNull;

import java.util.Objects;

import org.dart4e.Constants;
import org.dart4e.Dart4EPlugin;
import org.dart4e.launch.LaunchConfigurations;
import org.dart4e.localization.Messages;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.ui.AbstractLaunchConfigurationTab;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;

import de.sebthom.eclipse.commons.ui.Dialogs;

/**
 * @author Sebastian Thomschke
 */
public class TestLaunchConfigTab extends AbstractLaunchConfigurationTab {

   private TestLaunchConfigForm form = lazyNonNull();

   @Override
   public void createControl(final Composite parent) {
      form = new TestLaunchConfigForm(parent, SWT.NONE);
      setControl(form);
   }

   @Override
   public String getId() {
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
         form.selectedProject.set(LaunchConfigurations.getProject(config));
         form.selectedProject.subscribe(this::updateLaunchConfigurationDialog);

         final var project = LaunchConfigurations.getProject(config);
         if (project != null) {
            form.selectedTestResources.set( //
               config.getAttribute(TestLaunchConfigurations.LAUNCH_ATTR_DART_TEST_RESOURCES, singletonList(Constants.TEST_FOLDER_NAME))
                  .stream() //
                  .map(project::findMember) //
                  .filter(Objects::nonNull) //
                  .toList() //
            );
         }
         form.selectedTestResources.subscribe(this::updateLaunchConfigurationDialog);

         form.selectedAltSDK.set(LaunchConfigurations.getAlternativeDartSDK(config));
         form.selectedAltSDK.subscribe(this::updateLaunchConfigurationDialog);

         form.programArgs.set(LaunchConfigurations.getProgramArgs(config));
         form.programArgs.subscribe(this::updateLaunchConfigurationDialog);

         form.vmArgs.set(LaunchConfigurations.getDartVMArgs(config));
         form.vmArgs.subscribe(this::updateLaunchConfigurationDialog);
      } catch (final CoreException ex) {
         Dialogs.showStatus(Messages.Launch_InitializingLaunchConfigTabFailed, Dart4EPlugin.status().createError(ex), true);
      }
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

      final var selectedTestResources = form.selectedTestResources.get();
      if (selectedTestResources.isEmpty()) {
         config.removeAttribute(TestLaunchConfigurations.LAUNCH_ATTR_DART_TEST_RESOURCES);
      } else {
         config.setAttribute(TestLaunchConfigurations.LAUNCH_ATTR_DART_TEST_RESOURCES, //
            selectedTestResources.stream().map(r -> r.getProjectRelativePath().toString()).toList());
      }

      LaunchConfigurations.setProgramArgs(config, form.programArgs.get());
      LaunchConfigurations.setDartVMArgs(config, form.vmArgs.get());
      LaunchConfigurations.setAlternativeDartSDK(config, form.selectedAltSDK.get());
   }

   @Override
   public void setDefaults(final ILaunchConfigurationWorkingCopy config) {
   }
}
