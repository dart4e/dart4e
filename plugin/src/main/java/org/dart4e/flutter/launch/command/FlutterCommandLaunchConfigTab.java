/*
 * SPDX-FileCopyrightText: © The Dart4E authors
 * SPDX-FileContributor: Sebastian Thomschke
 * SPDX-License-Identifier: EPL-2.0
 * SPDX-ArtifactOfProjectHomePage: https://github.com/dart4e/dart4e
 */
package org.dart4e.flutter.launch.command;

import static net.sf.jstuff.core.validation.NullAnalysisHelper.lazyNonNull;

import org.dart4e.Constants;
import org.dart4e.Dart4EPlugin;
import org.dart4e.flutter.launch.FlutterLaunchConfigurations;
import org.dart4e.flutter.model.FlutterSDK;
import org.dart4e.flutter.widget.FlutterProjectSelectionGroup;
import org.dart4e.flutter.widget.FlutterSDKSelectionGroup;
import org.dart4e.launch.LaunchConfigurations;
import org.dart4e.localization.Messages;
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
public class FlutterCommandLaunchConfigTab extends AbstractLaunchConfigurationTab {

   private MutableObservableRef<@Nullable IProject> selectedProject = lazyNonNull();
   private MutableObservableRef<@Nullable FlutterSDK> selectedAltSDK = lazyNonNull();
   private MutableObservableRef<String> flutterArgs = lazyNonNull();

   @Override
   public void createControl(final Composite parent) {
      final var form = new Composite(parent, SWT.NONE);
      form.setLayout(new GridLayout(1, false));

      selectedProject = new FlutterProjectSelectionGroup(form).selectedProject;
      flutterArgs = new TextFieldGroup(form, "Flutter command and arguments").text;
      selectedAltSDK = new FlutterSDKSelectionGroup(form).selectedAltSDK;

      setControl(form);
   }

   @Override
   public String getId() {
      return FlutterCommandLaunchConfigTab.class.getName();
   }

   @Override
   public @Nullable Image getImage() {
      return Dart4EPlugin.get().getImageRegistry().get(Constants.IMAGE_FLUTTER_ICON);
   }

   @Override
   public String getName() {
      return Messages.Label_Flutter_App_Configuration;
   }

   @Override
   public void initializeFrom(final ILaunchConfiguration config) {
      selectedProject.set(LaunchConfigurations.getProject(config));
      selectedProject.subscribe(this::updateLaunchConfigurationDialog);

      selectedAltSDK.set(FlutterLaunchConfigurations.getAlternativeFlutterSDK(config));
      selectedAltSDK.subscribe(this::updateLaunchConfigurationDialog);

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

      setErrorMessage(null);

      return super.isValid(launchConfig);
   }

   @Override
   public void performApply(final ILaunchConfigurationWorkingCopy config) {
      LaunchConfigurations.setProject(config, selectedProject.get());
      LaunchConfigurations.setProgramArgs(config, flutterArgs.get());
      FlutterLaunchConfigurations.setAlternativeFlutterSDK(config, selectedAltSDK.get());
   }

   @Override
   public void setDefaults(final ILaunchConfigurationWorkingCopy config) {
   }
}
