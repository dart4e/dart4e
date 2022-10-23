/*
 * Copyright 2022 by the Dart4E authors.
 * SPDX-License-Identifier: EPL-2.0
 */
package org.dart4e.prefs;

import static net.sf.jstuff.core.validation.NullAnalysisHelper.*;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.dart4e.localization.Messages;
import org.dart4e.model.DartSDK;
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
public class DartSDKEditDialog extends TitleAreaDialog {
   public final MutableObservableRef<@Nullable String> dartSDKName = MutableObservableRef.of(null);
   public final MutableObservableRef<@Nullable Path> dartSDKPath = MutableObservableRef.of(null);

   private boolean isEditSDK;

   private Text txtName = lazyNonNull();
   private Text txtDartPath = lazyNonNull();

   /**
    * @wbp.parser.constructor
    */
   public DartSDKEditDialog(final Shell parentShell) {
      super(parentShell);
      isEditSDK = false;
   }

   public DartSDKEditDialog(final Shell parentShell, final DartSDK sdk) {
      super(parentShell);
      isEditSDK = true;
      dartSDKName.set(sdk.getName());
      dartSDKPath.set(sdk.getInstallRoot());
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

      shell.setText(isEditSDK ? "Edit Dart SDK" : "Add Dart SDK");

      return content;
   }

   @Override
   protected void createButtonsForButtonBar(final Composite parent) {
      createButton(parent, IDialogConstants.OK_ID, IDialogConstants.OK_LABEL, true);
      createButton(parent, IDialogConstants.CANCEL_ID, IDialogConstants.CANCEL_LABEL, false);
   }

   @Override
   protected Control createDialogArea(final Composite parent) {
      setTitle(Messages.Label_Dart_SDK);
      setMessage("Configure an installed Dart SDK.");

      final var area = (Composite) super.createDialogArea(parent);
      final var container = new Composite(area, SWT.NONE);
      final var containerLayout = new GridLayout(3, false);
      containerLayout.marginRight = 5;
      containerLayout.marginLeft = 5;
      containerLayout.marginTop = 5;
      container.setLayout(containerLayout);
      container.setLayoutData(new GridData(GridData.FILL_BOTH));

      /*
       * Dart SDK Name
       */
      final var lblName = new Label(container, SWT.NONE);
      lblName.setLayoutData(GridDatas.alignRight());
      lblName.setText(Messages.Label_Name + ":");
      txtName = new Text(container, SWT.BORDER);
      txtName.setLayoutData(GridDatas.fillHorizontalExcessive(2));
      Texts.bind(txtName, dartSDKName);
      Texts.onModified(txtName, () -> setErrorMessage(null));

      /*
       * Dart Path
       */
      final var lblPath = new Label(container, SWT.NONE);
      lblPath.setLayoutData(GridDatas.alignRight());
      lblPath.setText(Messages.Label_Path + " (Dart SDK):");
      txtDartPath = new Text(container, SWT.BORDER);
      txtDartPath.setEditable(false);
      txtDartPath.setLayoutData(GridDatas.fillHorizontalExcessive());
      Texts.bind(txtDartPath, dartSDKPath, Paths::get, Strings::emptyIfNull);
      Texts.onModified(txtDartPath, () -> setErrorMessage(null));

      final var btnBrowse = new Button(container, SWT.NONE);
      btnBrowse.setText(Messages.Label_Browse);
      Buttons.onSelected(btnBrowse, this::onBrowseForDartSDKButton);

      return area;
   }

   @Override
   protected void okPressed() {
      if (Strings.isBlank(dartSDKName.get())) {
         setErrorMessage(NLS.bind(Messages.Error_ValueMustBeSpecified, Messages.Label_Name));
         txtName.setFocus();
         return;
      }

      final var sdkPath = dartSDKPath.get();
      if (sdkPath == null) {
         setErrorMessage(NLS.bind(Messages.Error_ValueMustBeSpecified, Messages.Label_Path));
         txtDartPath.setFocus();
         return;
      }

      if (!Files.isDirectory(sdkPath) || !new DartSDK("whatever", sdkPath).isValid()) {
         setErrorMessage(Messages.SDKPathInvalid);
         txtDartPath.setFocus();
         return;
      }

      setErrorMessage(null);
      super.okPressed();
   }

   protected void onBrowseForDartSDKButton() {
      final var dlg = new DirectoryDialog(getShell());
      dlg.setText(Messages.Label_Path + ": Dart SDK");
      dlg.setMessage("Select a directory containing a Dart SDK");

      @Nullable
      String dir = txtDartPath.getText();
      if (Strings.isBlank(dir)) {
         final var p = DartSDK.fromPath();
         if (p != null) {
            dir = p.getInstallRoot().toString();
         }
      }
      while (true) {
         dlg.setFilterPath(dir);
         dir = dlg.open();
         if (dir == null)
            return;

         final var dartSDK = new DartSDK("whatever", Paths.get(dir));
         if (dartSDK.isValid()) {
            txtDartPath.setText(dir);
            if (Strings.isEmpty(txtName.getText())) {
               txtName.setText(new File(dir).getName());
            }
            return;
         }

         Dialogs.showError(Messages.SDKPathInvalid, NLS.bind(Messages.SDKPathInvalid_Descr, dir));
         txtDartPath.setFocus();
      }
   }
}
