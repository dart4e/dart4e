/*
 * Copyright 2022 by the Dart4E authors.
 * SPDX-License-Identifier: EPL-2.0
 */
package org.dart4e.project;

import org.dart4e.Constants;
import org.dart4e.Dart4EPlugin;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.dialogs.WizardNewFileCreationPage;

/**
 * @author Sebastian Thomschke
 */
public final class NewDartFilePage extends WizardNewFileCreationPage {

   public NewDartFilePage(final String pageName, final IStructuredSelection selection) {
      super(pageName, selection);
      setAllowExistingResources(false);
      setFileExtension("dart");
      setImageDescriptor(Dart4EPlugin.get().getSharedImageDescriptor(Constants.IMAGE_DART_WIZARD_BANNER));
   }
}
