/*
 * Copyright 2022 by the Dart4E authors.
 * SPDX-License-Identifier: EPL-2.0
 */
package org.dart4e;

import org.dart4e.navigation.ActiveEditorChangeListener;
import org.dart4e.navigation.DartDependenciesUpdater;
import org.dart4e.project.DartProjectNature;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.ui.IStartup;

import de.sebthom.eclipse.commons.resources.Projects;

/**
 * @author Sebastian Thomschke
 */
public class Dart4EStartupListener implements IStartup {

   @Override
   public void earlyStartup() {
      // workaround for "IResourceChangeListener adding at IDE startup" https://www.eclipse.org/forums/index.php/t/87906/
      ResourcesPlugin.getWorkspace().addResourceChangeListener(DartDependenciesUpdater.INSTANCE, IResourceChangeEvent.POST_CHANGE);

      // refresh dependencies when workbench first starts
      DartDependenciesUpdater.INSTANCE.onProjectsConfigChanged(Projects.getOpenProjectsWithNature(DartProjectNature.NATURE_ID).toList());

      ActiveEditorChangeListener.INSTANCE.attach();
   }
}
