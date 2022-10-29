/*
 * Copyright 2022 by the Dart4E authors.
 * SPDX-License-Identifier: EPL-2.0
 */
package org.dart4e.launch.command;

import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.ui.AbstractLaunchConfigurationTabGroup;
import org.eclipse.debug.ui.CommonTab;
import org.eclipse.debug.ui.EnvironmentTab;
import org.eclipse.debug.ui.ILaunchConfigurationDialog;
import org.eclipse.debug.ui.RefreshTab;

/**
 * This class is registered via the plugin.xml
 *
 * @author Sebastian Thomschke
 */
public class CommandLaunchTabGroup extends AbstractLaunchConfigurationTabGroup {

   @Override
   public void createTabs(final ILaunchConfigurationDialog dialog, final String mode) {
      setTabs(new CommandLaunchConfigTab(), new RefreshTab(), new EnvironmentTab(), new CommonTab());
   }

   @Override
   public void setDefaults(final ILaunchConfigurationWorkingCopy configuration) {
      super.setDefaults(configuration);

      CommandLaunchConfigurations.initialize(configuration);
   }
}
