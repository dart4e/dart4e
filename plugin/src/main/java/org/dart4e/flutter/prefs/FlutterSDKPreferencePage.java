/*
 * SPDX-FileCopyrightText: © The Dart4E authors
 * SPDX-FileContributor: Sebastian Thomschke
 * SPDX-License-Identifier: EPL-2.0
 * SPDX-ArtifactOfProjectHomePage: https://github.com/dart4e/dart4e
 */
package org.dart4e.flutter.prefs;

import static net.sf.jstuff.core.validation.NullAnalysisHelper.*;

import java.util.Collection;
import java.util.HashSet;
import java.util.Objects;

import org.dart4e.flutter.model.FlutterSDK;
import org.dart4e.localization.Messages;
import org.dart4e.util.ui.GridDatas;
import org.dart4e.util.ui.StyledLabelProviderAdapter;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.jface.viewers.CheckboxTableViewer;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.DelegatingStyledCellLabelProvider;
import org.eclipse.jface.viewers.ICheckStateProvider;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.StyledString;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

import de.sebthom.eclipse.commons.ui.Buttons;
import de.sebthom.eclipse.commons.ui.Fonts;
import de.sebthom.eclipse.commons.ui.Tables;
import net.sf.jstuff.core.collection.ObservableSet;
import net.sf.jstuff.core.ref.MutableObservableRef;

/**
 * @author Sebastian Thomschke
 */
public class FlutterSDKPreferencePage extends PreferencePage implements IWorkbenchPreferencePage {

   private CheckboxTableViewer sdkTable = lateNonNull();
   private final ObservableSet<FlutterSDK> sdks = new ObservableSet<>(new HashSet<>());
   private final MutableObservableRef<@Nullable FlutterSDK> defaultSDK = MutableObservableRef.of(null);

   public FlutterSDKPreferencePage() {
      setDescription(Messages.Flutter_Prefs_ManageSDKsDescription);
   }

   @Override
   public Control createContents(final Composite parent) {
      setTitle(Messages.Flutter_Prefs_ManageSDKsTitle);

      final var container = new Composite(parent, SWT.NULL);
      container.setLayout(new GridLayout(2, false));

      sdkTable = CheckboxTableViewer.newCheckList(container, SWT.BORDER | SWT.FULL_SELECTION);
      sdkTable.addCheckStateListener(event -> {
         if (event.getChecked()) {
            final var sdk = (FlutterSDK) event.getElement();
            defaultSDK.set(sdk);
         } else {
            defaultSDK.set(null);
         }
      });
      sdkTable.setCheckStateProvider(new ICheckStateProvider() {
         @Override
         public boolean isChecked(final @Nullable Object element) {
            return element != null && isDefaultFlutterSDK((FlutterSDK) element);
         }

         @Override
         public boolean isGrayed(final @Nullable Object element) {
            return false;
         }
      });
      final var table = sdkTable.getTable();
      table.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 1, 4));
      table.setHeaderVisible(true);
      table.setLinesVisible(true);

      sdkTable.setContentProvider(new IStructuredContentProvider() {
         @Override
         public Object[] getElements(final @Nullable Object input) {
            if (input == null)
               return new Object[0];
            @SuppressWarnings("unchecked")
            final var items = (Collection<FlutterSDK>) input;
            return items.toArray(new FlutterSDK[items.size()]);
         }

         @Override
         public void inputChanged(final Viewer viewer, final @Nullable Object oldInput, final @Nullable Object newInput) {
            refreshTable();
         }
      });

      final var colName = new TableViewerColumn(sdkTable, SWT.NONE);
      colName.setLabelProvider(new DelegatingStyledCellLabelProvider(new StyledLabelProviderAdapter()) {
         @Override
         public StyledString getStyledText(final @Nullable Object element) {
            if (element == null)
               return new StyledString("");
            final var sdk = (FlutterSDK) element;
            return isDefaultFlutterSDK(sdk) //
                  ? new StyledString(sdk.getName(), Fonts.DEFAULT_FONT_BOLD_STYLER)
                  : new StyledString(sdk.getName());
         }
      });
      colName.getColumn().setWidth(100);
      colName.getColumn().setText(Messages.Label_Name);

      final var colVer = new TableViewerColumn(sdkTable, SWT.NONE);
      colVer.setLabelProvider(new ColumnLabelProvider() {
         @Override
         public @Nullable String getText(final @Nullable Object element) {
            return element == null ? null : ((FlutterSDK) element).getVersion();
         }
      });
      colVer.getColumn().setWidth(100);
      colVer.getColumn().setText(Messages.Label_Version);

      final var colPath = new TableViewerColumn(sdkTable, SWT.NONE);
      colPath.setLabelProvider(new ColumnLabelProvider() {
         @Override
         public @Nullable String getText(final @Nullable Object element) {
            return element == null ? null : ((FlutterSDK) element).getInstallRoot().toString();
         }
      });
      colPath.getColumn().setWidth(100);
      colPath.getColumn().setText(Messages.Label_Path);

      final var btnAdd = new Button(container, SWT.NONE);
      btnAdd.setLayoutData(GridDatas.fillHorizontal());
      btnAdd.setText(Messages.Label_Add + "...");
      Buttons.onSelected(btnAdd, this::onButton_Add);

      final var btnEdit = new Button(container, SWT.NONE);
      btnEdit.setLayoutData(GridDatas.fillHorizontal());
      btnEdit.setText(Messages.Label_Edit + "...");
      btnEdit.setEnabled(false);
      Buttons.onSelected(btnEdit, this::onButton_Edit);

      final var btnRemove = new Button(container, SWT.NONE);
      btnRemove.setLayoutData(GridDatas.fillHorizontal());
      btnRemove.setText(Messages.Label_Remove);
      btnRemove.setEnabled(false);
      Buttons.onSelected(btnRemove, this::onButton_Remove);

      sdkTable.addSelectionChangedListener(event -> {
         if (event.getSelection().isEmpty()) {
            btnEdit.setEnabled(false);
            btnRemove.setEnabled(false);
         } else {
            btnEdit.setEnabled(true);
            btnRemove.setEnabled(true);
         }
      });

      sdkTable.setInput(sdks);
      Tables.autoResizeColumns(sdkTable);

      sdks.subscribe(e -> refreshTable());
      defaultSDK.subscribe(this::refreshTable);

      return container;
   }

   @Override
   public void init(final IWorkbench workbench) {
      setPreferenceStore(FlutterWorkspacePreference.PREFS);
      sdks.addAll(FlutterWorkspacePreference.getFlutterSDKs());
      defaultSDK.set(FlutterWorkspacePreference.getDefaultFlutterSDK(false, false));
   }

   private boolean isDefaultFlutterSDK(final FlutterSDK sdk) {
      return Objects.equals(sdk, defaultSDK.get());
   }

   private void onButton_Add() {
      final var dialog = new FlutterSDKEditDialog(getShell());
      if (dialog.open() == Window.OK) {
         sdks.add(new FlutterSDK( //
            asNonNullUnsafe(dialog.sdkName.get()), //
            asNonNullUnsafe(dialog.sdkPath.get()) //
         ));
      }
   }

   private void onButton_Edit() {
      if (((StructuredSelection) sdkTable.getSelection()).getFirstElement() instanceof final FlutterSDK sdk) {
         final var dialog = new FlutterSDKEditDialog(getShell(), sdk);
         if (dialog.open() == Window.OK) {
            sdks.remove(sdk);
            sdks.add(new FlutterSDK( //
               asNonNullUnsafe(dialog.sdkName.get()), //
               asNonNullUnsafe(dialog.sdkPath.get()) //
            ));
         }
      }
   }

   private void onButton_Remove() {
      if (((StructuredSelection) sdkTable.getSelection()).getFirstElement() instanceof final FlutterSDK sdk) {
         sdks.remove(sdk);
         sdkTable.refresh();
      }
   }

   @Override
   protected void performDefaults() {
      super.performDefaults();
   }

   @Override
   public boolean performOk() {
      FlutterWorkspacePreference.setFlutterSDKs(sdks);
      final var sdk = defaultSDK.get();
      if (sdk != null) {
         FlutterWorkspacePreference.setDefaultFlutterSDK(sdk.getName());
      }
      if (!FlutterWorkspacePreference.save()) {
         setValid(false);
         return false;
      }

      setValid(true);
      return true;
   }

   private void refreshTable() {
      sdkTable.refresh();
      Tables.autoResizeColumns(sdkTable);
   }
}
