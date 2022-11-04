/*
 * Copyright 2022 by the Dart4E authors.
 * SPDX-License-Identifier: EPL-2.0
 */
package org.dart4e.project;

import static net.sf.jstuff.core.validation.NullAnalysisHelper.lazyNonNull;

import org.dart4e.Constants;
import org.dart4e.Dart4EPlugin;
import org.dart4e.localization.Messages;
import org.dart4e.model.DartSDK;
import org.dart4e.prefs.DartWorkspacePreference;
import org.dart4e.util.ui.GridDatas;
import org.dart4e.widget.DartSDKSelectionGroup;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.ui.dialogs.WizardNewProjectCreationPage;

import de.sebthom.eclipse.commons.ui.widgets.ComboWrapper;
import net.sf.jstuff.core.ref.MutableObservableRef;
import net.sf.jstuff.core.ref.ObservableRef;

/**
 * @author Sebastian Thomschke
 */
public final class NewDartProjectPage extends WizardNewProjectCreationPage {

   public ObservableRef<@Nullable DartSDK> altSDK = lazyNonNull();
   public MutableObservableRef<String> template = MutableObservableRef.of("console");

   public NewDartProjectPage(final String pageName) {
      super(pageName);
      setImageDescriptor(Dart4EPlugin.get().getSharedImageDescriptor(Constants.IMAGE_DART_WIZARD_BANNER));
   }

   @Override
   public void createControl(final Composite parent) {
      super.createControl(parent);

      final var control = (Composite) getControl();
      altSDK = new DartSDKSelectionGroup(control).selectedAltSDK;

      final var grpTemplate = new Group(control, SWT.NONE);
      grpTemplate.setLayoutData(GridDatas.fillHorizontalExcessive());
      grpTemplate.setLayout(GridLayoutFactory.swtDefaults().numColumns(1).create());
      grpTemplate.setText("Project Template");
      new ComboWrapper<String>(grpTemplate, GridDatas.fillHorizontalExcessive()) //
         .setItems("console", "package", "server-shelf", "web") //
         .setLabelProvider(item -> item + " - A " + switch (item) {
            case "console" -> "command-line application";
            case "package" -> "package containing shared Dart libraries";
            case "server-shelf" -> "server app using package:shelf";
            case "web" -> "web app that uses only core Dart libraries";
            default -> item;
         }) //
         .bind(template, template.get());
   }

   @Override
   protected boolean validatePage() {
      if (!super.validatePage())
         return false;

      if (!Constants.VALID_PROJECT_NAME_PATTERN.matcher(getProjectName()).matches()) {
         setMessage(Messages.Error_InvalidProjectName, IMessageProvider.ERROR);
         return false;
      }

      if (altSDK.get() == null && DartWorkspacePreference.getDefaultDartSDK(false, true) == null) {
         setMessage(Messages.NewDartProject_SDKNotFound_Message, IMessageProvider.ERROR);
         return false;
      }
      return true;
   }
}
