/*
 * SPDX-FileCopyrightText: © The Dart4E authors
 * SPDX-FileContributor: Sebastian Thomschke
 * SPDX-License-Identifier: EPL-2.0
 * SPDX-ArtifactOfProjectHomePage: https://github.com/dart4e/dart4e
 */
package org.dart4e.flutter.prefs;

import static net.sf.jstuff.core.validation.NullAnalysisHelper.lateNonNull;
import static org.dart4e.localization.Messages.*;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.dart4e.flutter.model.FlutterSDK;
import org.dart4e.util.ui.GridDatas;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

import de.sebthom.eclipse.commons.ui.Buttons;
import de.sebthom.eclipse.commons.ui.Dialogs;
import de.sebthom.eclipse.commons.ui.Texts;
import net.sf.jstuff.core.Strings;
import net.sf.jstuff.core.ref.MutableObservableRef;

/**
 * @author Sebastian Thomschke
 */
public class FlutterSDKEditDialog extends TitleAreaDialog {
   public final MutableObservableRef<@Nullable String> sdkName = MutableObservableRef.of(null);
   public final MutableObservableRef<@Nullable Path> sdkPath = MutableObservableRef.of(null);

   private boolean isEditSDK;

   private Text txtSDKName = lateNonNull();
   private Text txtSDKPath = lateNonNull();

   /**
    * @wbp.parser.constructor
    */
   public FlutterSDKEditDialog(final Shell parentShell) {
      super(parentShell);
      isEditSDK = false;
   }

   public FlutterSDKEditDialog(final Shell parentShell, final FlutterSDK sdk) {
      super(parentShell);
      isEditSDK = true;
      sdkName.set(sdk.getName());
      sdkPath.set(sdk.getInstallRoot());
   }

   @Override
   protected void configureShell(final Shell newShell) {
      super.configureShell(newShell);
      newShell.setMinimumSize(new Point(400, 200));
   }

   @Override
   protected Control createContents(final Composite parent) {
      final var content = super.createContents(parent);
      final var shell = parent.getShell();

      shell.setText(isEditSDK ? "Edit Flutter SDK" : "Add Flutter SDK");

      return content;
   }

   @Override
   protected void createButtonsForButtonBar(final Composite parent) {
      createButton(parent, IDialogConstants.OK_ID, IDialogConstants.OK_LABEL, true);
      createButton(parent, IDialogConstants.CANCEL_ID, IDialogConstants.CANCEL_LABEL, false);
   }

   @Override
   protected Control createDialogArea(final Composite parent) {
      setTitle(Label_Flutter_SDK);
      setMessage("Configure an installed Flutter SDK");

      final var area = (Composite) super.createDialogArea(parent);
      final var container = new Composite(area, SWT.NONE);
      final var containerLayout = new GridLayout(3, false);
      containerLayout.marginRight = 5;
      containerLayout.marginLeft = 5;
      containerLayout.marginTop = 5;
      container.setLayout(containerLayout);
      container.setLayoutData(new GridData(GridData.FILL_BOTH));

      /*
       * SDK Name
       */
      final var lblName = new Label(container, SWT.NONE);
      lblName.setLayoutData(GridDatas.alignRight());
      lblName.setText(Label_Name + ":");
      txtSDKName = new Text(container, SWT.BORDER);
      txtSDKName.setLayoutData(GridDatas.fillHorizontalExcessive(2));
      Texts.bind(txtSDKName, sdkName);
      Texts.onModified(txtSDKName, () -> setErrorMessage(null));

      /*
       * SDK Path
       */
      final var lblPath = new Label(container, SWT.NONE);
      lblPath.setLayoutData(GridDatas.alignRight());
      lblPath.setText(Label_Path + " (" + Label_Flutter_SDK + "):");
      txtSDKPath = new Text(container, SWT.BORDER);
      txtSDKPath.setEditable(false);
      txtSDKPath.setLayoutData(GridDatas.fillHorizontalExcessive());
      Texts.bind(txtSDKPath, sdkPath, Paths::get, Strings::emptyIfNull);
      Texts.onModified(txtSDKPath, () -> setErrorMessage(null));

      final var btnBrowse = new Button(container, SWT.NONE);
      btnBrowse.setText(Label_Browse);
      Buttons.onSelected(btnBrowse, this::onBrowseForSDKButton);

      return area;
   }

   @Override
   protected void okPressed() {
      if (Strings.isBlank(sdkName.get())) {
         setErrorMessage(NLS.bind(Error_ValueMustBeSpecified, Label_Name));
         txtSDKName.setFocus();
         return;
      }

      final var sdkPath = this.sdkPath.get();
      if (sdkPath == null) {
         setErrorMessage(NLS.bind(Error_ValueMustBeSpecified, Label_Path));
         txtSDKPath.setFocus();
         return;
      }

      if (!Files.isDirectory(sdkPath) || !new FlutterSDK("whatever", sdkPath).isValid()) {
         setErrorMessage(Flutter_Prefs_SDKPathInvalid);
         txtSDKPath.setFocus();
         return;
      }

      setErrorMessage(null);
      super.okPressed();
   }

   protected void onBrowseForSDKButton() {
      final var dlg = new DirectoryDialog(getShell());
      dlg.setText(Label_Path + ": " + Label_Flutter_SDK);
      dlg.setMessage("Select a directory containing the Flutter SDK");

      @Nullable
      String dir = txtSDKPath.getText();
      if (Strings.isBlank(dir)) {
         final var sdkFromPath = FlutterSDK.fromPath();
         if (sdkFromPath != null) {
            dir = sdkFromPath.getInstallRoot().toString();
         }
      }
      while (true) {
         dlg.setFilterPath(dir);
         dir = dlg.open();
         if (dir == null)
            return;

         final var sdk = new FlutterSDK("whatever", Paths.get(dir));
         if (sdk.isValid()) {
            txtSDKPath.setText(dir);
            if (Strings.isEmpty(txtSDKName.getText())) {
               txtSDKName.setText(new File(dir).getName());
            }
            return;
         }

         Dialogs.showError(Flutter_Prefs_SDKPathInvalid, NLS.bind(Flutter_Prefs_SDKPathInvalid_Descr, dir));
         txtSDKPath.setFocus();
      }
   }
}
