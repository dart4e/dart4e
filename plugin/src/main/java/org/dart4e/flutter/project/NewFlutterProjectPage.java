/*
 * SPDX-FileCopyrightText: © The Dart4E authors
 * SPDX-FileContributor: Sebastian Thomschke
 * SPDX-License-Identifier: EPL-2.0
 * SPDX-ArtifactOfProjectHomePage: https://github.com/dart4e/dart4e
 */
package org.dart4e.flutter.project;

import static net.sf.jstuff.core.validation.NullAnalysisHelper.*;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.dart4e.Constants;
import org.dart4e.Dart4EPlugin;
import org.dart4e.flutter.model.FlutterSDK;
import org.dart4e.flutter.widget.FlutterSDKSelectionGroup;
import org.dart4e.localization.Messages;
import org.dart4e.prefs.DartWorkspacePreference;
import org.dart4e.util.io.JSON;
import org.dart4e.util.ui.GridDatas;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.dialogs.WizardNewProjectCreationPage;

import com.fasterxml.jackson.core.type.TypeReference;

import de.sebthom.eclipse.commons.ui.Texts;
import de.sebthom.eclipse.commons.ui.widgets.CheckboxTableWrapper;
import de.sebthom.eclipse.commons.ui.widgets.ComboWrapper;
import net.sf.jstuff.core.Strings;
import net.sf.jstuff.core.ref.MutableObservableRef;
import net.sf.jstuff.core.ref.ObservableRef;

/**
 * @author Sebastian Thomschke
 */
public final class NewFlutterProjectPage extends WizardNewProjectCreationPage {

   private static final List<String> TARGET_PLATFORMS = List.of("android", "ios", "linux", "macos", "web", "windows");

   public ObservableRef<@Nullable FlutterSDK> altSDK = lazyNonNull();
   public MutableObservableRef<String> template = MutableObservableRef.of("app");
   public MutableObservableRef<String> description = MutableObservableRef.of("A new Flutter project.");
   public MutableObservableRef<String> orgName = MutableObservableRef.of("com.example");
   public MutableObservableRef<List<String>> platforms = MutableObservableRef.of(TARGET_PLATFORMS);
   public MutableObservableRef<String> androidLanguage = MutableObservableRef.of("kotlin");
   public MutableObservableRef<String> iosLanguage = MutableObservableRef.of("swift");
   public MutableObservableRef<String> appSampleId = MutableObservableRef.of("");

   public NewFlutterProjectPage(final String pageName) {
      super(pageName);
      setImageDescriptor(Dart4EPlugin.get().getSharedImageDescriptor(Constants.IMAGE_FLUTTER_WIZARD_BANNER));
   }

   @Override
   public void createControl(final Composite parent) {
      super.createControl(parent);

      final var control = (Composite) getControl();

      final var grpSDKSelection = new FlutterSDKSelectionGroup(control);
      altSDK = grpSDKSelection.selectedAltSDK;

      final var containerColumns = 2;
      final var container = new Composite(control, SWT.NONE);
      final var containerLayout = new GridLayout(containerColumns, false);
      container.setLayout(containerLayout);
      final var gd = new GridData(GridData.FILL_BOTH);
      gd.widthHint = 800;
      container.setLayoutData(gd);

      final var lblTemplate = new Label(container, SWT.NONE);
      lblTemplate.setLayoutData(GridDatas.alignRight());
      lblTemplate.setText("Project Template:");
      new ComboWrapper<String>(container, GridDatas.fillHorizontalExcessive()) //
         .setItems("app", "module", "package", "plugin", "plugin_ffi", "skeleton") //
         .setLabelProvider(item -> item + " - A " + switch (item) {
            case "app" -> "Flutter application";
            case "module" -> "project to add a Flutter module to an existing Android or iOS app";
            case "package" -> "shareable Flutter project with modular Dart code";
            case "plugin" -> "shareable Flutter project with an API in Dart code with platform-specific "
               + "implementations for supported platforms";
            case "plugin_ffi" -> "shareable Flutter project with an API in Dart code with a platform-specific "
               + "implementations for supported platforms";
            case "skeleton" -> "List View / Detail View Flutter app that follows community best practices.";
            default -> item;
         }) //
         .bind(template, template.get());

      /*
       * Description
       */
      final var lblDescr = new Label(container, SWT.NONE);
      lblDescr.setLayoutData(GridDatas.alignRight());
      lblDescr.setText("Description:");
      final var txtDescr = new Text(container, SWT.BORDER);
      txtDescr.setLayoutData(GridDatas.fillHorizontalExcessive(1));
      Texts.bind(txtDescr, description);
      Texts.onModified(txtDescr, () -> setErrorMessage(null));

      /*
       * Org Name
       */
      final var lblName = new Label(container, SWT.NONE);
      lblName.setLayoutData(GridDatas.alignRight());
      lblName.setText("Organization:");
      final var txtOrgName = new Text(container, SWT.BORDER);
      txtOrgName.setLayoutData(GridDatas.fillHorizontalExcessive(1));
      Texts.bind(txtOrgName, orgName);
      Texts.onModified(txtOrgName, () -> setErrorMessage(null));

      /*
       * Selected Platforms
       */
      final var lblPlatforms = new Label(container, SWT.NONE);
      lblPlatforms.setLayoutData(GridDatas.alignRight());
      lblPlatforms.setText("Target Platforms:");
      final var tablePlatforms = new CheckboxTableWrapper<String>(container, SWT.BORDER, GridDatas.fillHorizontalExcessive()) //
         .setItems(TARGET_PLATFORMS) //
         .bindCheckedItems(platforms);
      template.subscribe(template -> tablePlatforms.setEnabled("app".equals(template) || "plugin".equals(template)));

      /*
       * Android Language
       */
      final var lblAndroidLang = new Label(container, SWT.NONE);
      lblAndroidLang.setLayoutData(GridDatas.alignRight());
      lblAndroidLang.setText("Android Language:");
      final var cmbAndroidLang = new ComboWrapper<String>(container, GridDatas.fillHorizontalExcessive()) //
         .setItems("kotlin", "java") //
         .setLabelProvider(item -> item + " - " + switch (item) {
            case "kotlin" -> "(default)";
            case "java" -> "(legacy)";
            default -> item;
         }) //
         .bind(androidLanguage, androidLanguage.get());
      platforms.subscribe(platforms -> cmbAndroidLang.setEnabled(platforms.contains("android")));

      /*
       * iOS Language
       */
      final var lblIOSLang = new Label(container, SWT.NONE);
      lblIOSLang.setLayoutData(GridDatas.alignRight());
      lblIOSLang.setText("iOS Language:");
      final var cmbIOSLang = new ComboWrapper<String>(container, GridDatas.fillHorizontalExcessive()) //
         .setItems("swift", "objc") //
         .setLabelProvider(item -> item + " - " + switch (item) {
            case "swift" -> "(default)";
            case "objc" -> "(legacy)";
            default -> item;
         }) //
         .bind(iosLanguage, iosLanguage.get());
      platforms.subscribe(platforms -> cmbIOSLang.setEnabled(platforms.contains("ios")));

      /*
       * App Sample https://api.flutter.dev/snippets/index.json
       */
      try {
         final var samplesJSON = JSON.deserialize(asNonNull(getClass().getResource("/src/main/resources/flutter/snippets.json")),
            new TypeReference<List<Map<String, Object>>>() {});

         final var samples = new TreeMap<String, String>();
         samples.put(" ", " ");
         for (final var sample : samplesJSON) {
            var descr = asNonNull((String) sample.get("description"));
            descr = Strings.remove(descr, "A sample code");
            descr = Strings.remove(descr, "Here is an example of showing");
            descr = Strings.remove(descr, "Here is an example of ");
            descr = Strings.remove(descr, "Here is an example that ");
            descr = Strings.remove(descr, "Here is an example ");
            descr = Strings.remove(descr, "Here is a simple example of showing");
            descr = Strings.remove(descr, "Here is a simple example of");
            descr = Strings.remove(descr, "In this example,");
            descr = Strings.remove(descr, "In this example");
            descr = Strings.remove(descr, "In this sample");
            descr = Strings.remove(descr, "The following code");
            descr = Strings.remove(descr, "The following example");
            descr = Strings.remove(descr, "The following sample");
            descr = Strings.remove(descr, "This code");
            descr = Strings.remove(descr, "This example");
            descr = Strings.remove(descr, "This sample application");
            descr = Strings.remove(descr, "This sample");
            descr = Strings.remove(descr, "This simple example");
            descr = Strings.replace(descr, '\n', ' ');
            descr = descr.strip();
            descr = Strings.lowerCaseFirstChar(descr);

            samples.put(asNonNull((String) sample.get("id")), sample.get("id") + "  -  " + descr);
         }

         final var lblAppSample = new Label(container, SWT.NONE);
         lblAppSample.setLayoutData(GridDatas.alignRight());
         lblAppSample.setText("App Sample");

         final var cmbSample = new ComboWrapper<String>(container, GridDatas.fillHorizontalExcessive()) //
            .setItems(samples.keySet()) //
            .setLabelProvider(item -> asNonNull(samples.get(item))) //
            .bind(appSampleId, appSampleId.get());
         template.subscribe(template -> cmbSample.setEnabled("app".equals(template)));
      } catch (final Exception ex) {
         Dart4EPlugin.log().error(ex);
      }
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
         setMessage(Messages.Flutter_NewProject_SDKNotFound_Message, IMessageProvider.ERROR);
         return false;
      }
      return true;
   }
}
