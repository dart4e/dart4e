/*
 * Copyright 2022 by the Dart4E authors.
 * SPDX-License-Identifier: EPL-2.0
 */
package org.dart4e.launch.test;

import static java.util.Collections.singletonList;
import static net.sf.jstuff.core.validation.NullAnalysisHelper.asNonNull;
import static net.sf.jstuff.core.validation.NullAnalysisHelper.lazyNonNull;

import java.util.Objects;

import org.dart4e.Constants;
import org.dart4e.Dart4EPlugin;
import org.dart4e.localization.Messages;
import org.dart4e.prefs.DartWorkspacePreference;
import org.dart4e.project.DartProjectNature;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.ui.AbstractLaunchConfigurationTab;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;

import de.sebthom.eclipse.commons.resources.Projects;
import de.sebthom.eclipse.commons.ui.Dialogs;

/**
 * @author Sebastian Thomschke
 */
public class LaunchTestConfigTab extends AbstractLaunchConfigurationTab {

   private LaunchTestConfigForm form = lazyNonNull();

   @Override
   public void createControl(final Composite parent) {
      form = new LaunchTestConfigForm(parent, SWT.NONE);
      setControl(form);
   }

   @Override
   public String getId() {
      return LaunchTestConfigTab.class.getName();
   }

   @Override
   public @Nullable Image getImage() {
      return Dart4EPlugin.get().getImageRegistry().get(Constants.IMAGE_ICON);
   }

   @Override
   public String getName() {
      return Messages.Label_Dart_Test_Configuration;
   }

   @Override
   public void initializeFrom(final ILaunchConfiguration config) {
      try {
         final var projectName = config.getAttribute(Constants.LAUNCH_ATTR_PROJECT, "");
         final var project = Projects.findOpenProjectWithNature(projectName, DartProjectNature.NATURE_ID);
         form.selectedProject.set(project);
         form.selectedProject.subscribe(this::updateLaunchConfigurationDialog);

         if (project != null) {
            form.selectedTestResources.set( //
               config.getAttribute(Constants.LAUNCH_ATTR_DART_TEST_RESOURCES, singletonList(Constants.TEST_FOLDER_NAME)).stream() //
                  .map(project::findMember) //
                  .filter(Objects::nonNull) //
                  .toList() //
            );
         }
         form.selectedTestResources.subscribe(this::updateLaunchConfigurationDialog);

         final var altSDK = DartWorkspacePreference.getDartSDK(config.getAttribute(Constants.LAUNCH_ATTR_DART_SDK, ""));
         form.selectedAltSDK.set(altSDK);
         form.selectedAltSDK.subscribe(this::updateLaunchConfigurationDialog);

         form.programArgs.set(config.getAttribute(Constants.LAUNCH_ATTR_PROGRAM_ARGS, ""));
         form.programArgs.subscribe(this::updateLaunchConfigurationDialog);
         form.vmArgs.set(config.getAttribute(Constants.LAUNCH_ATTR_VM_ARGS, ""));
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
      config.setAttribute(Constants.LAUNCH_ATTR_PROJECT, form.selectedProject.get() == null ? null
         : asNonNull(form.selectedProject.get()).getName());

      final var selectedTestResources = form.selectedTestResources.get();
      if (selectedTestResources.isEmpty()) {
         config.removeAttribute(Constants.LAUNCH_ATTR_DART_TEST_RESOURCES);
      } else {
         config.setAttribute(Constants.LAUNCH_ATTR_DART_TEST_RESOURCES, //
            selectedTestResources.stream().map(r -> r.getProjectRelativePath().toString()).toList());
      }

      final var altSDK = form.selectedAltSDK.get();

      config.setAttribute(Constants.LAUNCH_ATTR_PROGRAM_ARGS, form.programArgs.get());
      config.setAttribute(Constants.LAUNCH_ATTR_VM_ARGS, form.vmArgs.get());
      config.setAttribute(Constants.LAUNCH_ATTR_DART_SDK, altSDK == null ? "" : altSDK.getName());
   }

   @Override
   public void setDefaults(final ILaunchConfigurationWorkingCopy config) {
   }
}
