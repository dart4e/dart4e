/*
 * SPDX-FileCopyrightText: © The Dart4E authors
 * SPDX-FileContributor: Sebastian Thomschke
 * SPDX-License-Identifier: EPL-2.0
 * SPDX-ArtifactOfProjectHomePage: https://github.com/dart4e/dart4e
 */
package org.dart4e.prefs;

import java.io.IOException;
import java.util.Objects;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import org.dart4e.Dart4EPlugin;
import org.dart4e.localization.Messages;
import org.dart4e.model.DartSDK;
import org.dart4e.util.io.JSON;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.jface.preference.IPersistentPreferenceStore;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.ui.dialogs.PreferencesUtil;
import org.eclipse.ui.preferences.ScopedPreferenceStore;

import de.sebthom.eclipse.commons.ui.Dialogs;
import de.sebthom.eclipse.commons.ui.UI;
import net.sf.jstuff.core.Strings;

/**
 * @author Sebastian Thomschke
 */
public final class DartWorkspacePreference {

   public static final class Initializer extends AbstractPreferenceInitializer {

      @Override
      public void initializeDefaultPreferences() {
         STORE.setDefault(PREFKEY_FORMATTER_MAX_LINE_LENGTH, 80);

         STORE.setDefault(PREFKEY_INLAY_HINTS_ENABLED, true);
         STORE.setDefault(PREFKEY_INLAY_HINTS_DOT_SHORTHAND_TYPES_ENABLED, true);
         STORE.setDefault(PREFKEY_INLAY_HINTS_PARAMETER_NAMES_MODE, "all");
         STORE.setDefault(PREFKEY_INLAY_HINTS_PARAMETER_TYPES_ENABLED, true);
         STORE.setDefault(PREFKEY_INLAY_HINTS_RETURN_TYPES_ENABLED, true);
         STORE.setDefault(PREFKEY_INLAY_HINTS_TYPE_ARGUMENTS_ENABLED, true);
         STORE.setDefault(PREFKEY_INLAY_HINTS_VARIABLE_TYPES_ENABLED, true);
      }
   }

   static final String PREFKEY_DEFAULT_DART_SDK = "dart.default_sdk";
   static final String PREFKEY_DART_SDKS = "dart.sdks";

   static final String PREFKEY_WARNED_NO_SDK_REGISTERED = "dart.warned_no_sdk_registered";

   static final String PREFKEY_DAP_TRACE_IO = "dart.dap.trace.io";
   static final String PREFKEY_DAP_TRACE_IO_VERBOSE = "dart.dap.trace.io.verbose";

   static final String PREFKEY_LSP_TRACE_INITOPTS = "dart.lsp.trace.init_options";
   static final String PREFKEY_LSP_TRACE_IO = "dart.lsp.trace.io";
   static final String PREFKEY_LSP_TRACE_IO_VERBOSE = "dart.lsp.trace.io.verbose";

   public static final String PREFKEY_LSP_CLIENT_PREFIX = "dart.lsp.client.";
   static final String PREFKEY_FORMATTER_MAX_LINE_LENGTH = PREFKEY_LSP_CLIENT_PREFIX + "formatter.max_line_length";
   static final String PREFKEY_INLAY_HINTS_ENABLED = PREFKEY_LSP_CLIENT_PREFIX + "inlay_hints.enabled";
   static final String PREFKEY_INLAY_HINTS_DOT_SHORTHAND_TYPES_ENABLED = PREFKEY_LSP_CLIENT_PREFIX
         + "inlay_hints.dot_shorthand_types.enabled";
   static final String PREFKEY_INLAY_HINTS_PARAMETER_NAMES_MODE = PREFKEY_LSP_CLIENT_PREFIX + "inlay_hints.parameter_names.mode";
   static final String PREFKEY_INLAY_HINTS_PARAMETER_TYPES_ENABLED = PREFKEY_LSP_CLIENT_PREFIX + "inlay_hints.parameter_types.enabled";
   static final String PREFKEY_INLAY_HINTS_RETURN_TYPES_ENABLED = PREFKEY_LSP_CLIENT_PREFIX + "inlay_hints.return_types.enabled";
   static final String PREFKEY_INLAY_HINTS_TYPE_ARGUMENTS_ENABLED = PREFKEY_LSP_CLIENT_PREFIX + "inlay_hints.type_arguments.enabled";
   static final String PREFKEY_INLAY_HINTS_VARIABLE_TYPES_ENABLED = PREFKEY_LSP_CLIENT_PREFIX + "inlay_hints.variable_types.enabled";

   static final IPersistentPreferenceStore STORE = new ScopedPreferenceStore(InstanceScope.INSTANCE, Dart4EPlugin.PLUGIN_ID);

   // CHECKSTYLE:IGNORE .* FOR NEXT 3 LINES
   private static final SortedSet<DartSDK> dartSDKs = new TreeSet<>();
   private static boolean isDartSDKsInitialized = false;

   public static void addPreferenceChangeListener(final IPropertyChangeListener listener) {
      STORE.addPropertyChangeListener(listener);
   }

   private static void ensureDartSDKsInitialized() {
      synchronized (dartSDKs) {
         if (!isDartSDKsInitialized) {
            isDartSDKsInitialized = true;

            final var dartSDKsSerialized = STORE.getString(PREFKEY_DART_SDKS);
            if (Strings.isNotBlank(dartSDKsSerialized)) {
               try {
                  dartSDKs.addAll(JSON.deserializeList(dartSDKsSerialized, DartSDK.class));
               } catch (final Exception ex) {
                  Dart4EPlugin.log().error(ex);
               }
            }

            if (dartSDKs.isEmpty()) {
               final var defaultSDK = DartSDK.fromPath();
               if (defaultSDK != null) {
                  Dart4EPlugin.log().info("Registering system {0} with {1}", defaultSDK);
                  dartSDKs.add(defaultSDK);
                  setDefaultDartSDK(defaultSDK.getName());
                  save();
               }
            }
         }

         if (dartSDKs.isEmpty() && !STORE.getBoolean(PREFKEY_WARNED_NO_SDK_REGISTERED)) {
            for (final var ste : new Throwable().getStackTrace()) {
               final var prefPage = DartSDKPreferencePage.class.getName();
               // don't show warning on empty workspace if we directly go to Dart Prefs
               if (ste.getClassName().equals(prefPage))
                  return;
            }

            STORE.setValue(PREFKEY_WARNED_NO_SDK_REGISTERED, true);
            save();

            UI.runAsync(() -> {
               Dialogs.showError(Messages.Prefs_NoSDKRegistered_Title, Messages.Prefs_NoSDKRegistered_Body);
               final var dialog = PreferencesUtil.createPreferenceDialogOn( //
                  UI.getShell(), //
                  DartSDKPreferencePage.class.getName(), //
                  new String[] {DartSDKPreferencePage.class.getName()}, //
                  null //
               );
               dialog.open();
            });
         }
      }
   }

   /**
    * @return null if not found
    */
   public static @Nullable DartSDK getDartSDK(final String name) {
      if (Strings.isEmpty(name))
         return null;

      ensureDartSDKsInitialized();

      synchronized (dartSDKs) {
         for (final var sdk : dartSDKs)
            if (Objects.equals(name, sdk.getName()))
               return sdk;
      }
      return null;
   }

   public static SortedSet<DartSDK> getDartSDKs() {
      ensureDartSDKsInitialized();

      synchronized (dartSDKs) {
         return new TreeSet<>(dartSDKs);
      }
   }

   /**
    * @return null if not found
    */
   public static @Nullable DartSDK getDefaultDartSDK(final boolean verify, final boolean searchPATH) {
      final var defaultSDK = getDartSDK(STORE.getString(PREFKEY_DEFAULT_DART_SDK));

      if (defaultSDK != null) {
         if (!verify || defaultSDK.isValid())
            return defaultSDK;
      }

      ensureDartSDKsInitialized();

      synchronized (dartSDKs) {
         if (!dartSDKs.isEmpty()) {
            if (!verify)
               return dartSDKs.first();
            for (final var sdk : dartSDKs) {
               if (sdk.isValid())
                  return sdk;
            }
         }
      }

      if (!searchPATH)
         return null;

      return DartSDK.fromPath();
   }

   public static int getFormatterMaxLineLength() {
      return STORE.getInt(PREFKEY_FORMATTER_MAX_LINE_LENGTH);
   }

   public static String getInlayHintsParameterNamesMode() {
      final var mode = STORE.getString(PREFKEY_INLAY_HINTS_PARAMETER_NAMES_MODE);
      if ("none".equals(mode) || "literal".equals(mode) || "all".equals(mode))
         return mode;
      return "all";
   }

   public static boolean isDAPTraceIO() {
      if (STORE.contains(PREFKEY_DAP_TRACE_IO))
         return STORE.getBoolean(PREFKEY_DAP_TRACE_IO);
      return Platform.getDebugBoolean(Dart4EPlugin.PLUGIN_ID + "/trace/dap/io");
   }

   public static boolean isDAPTraceIOVerbose() {
      if (STORE.contains(PREFKEY_DAP_TRACE_IO_VERBOSE))
         return STORE.getBoolean(PREFKEY_DAP_TRACE_IO_VERBOSE);
      return Platform.getDebugBoolean(Dart4EPlugin.PLUGIN_ID + "/trace/dap/io/verbose");
   }

   public static boolean isInlayHintsDotShorthandTypesEnabled() {
      if (STORE.contains(PREFKEY_INLAY_HINTS_DOT_SHORTHAND_TYPES_ENABLED))
         return STORE.getBoolean(PREFKEY_INLAY_HINTS_DOT_SHORTHAND_TYPES_ENABLED);
      return true;
   }

   public static boolean isInlayHintsEnabled() {
      if (STORE.contains(PREFKEY_INLAY_HINTS_ENABLED))
         return STORE.getBoolean(PREFKEY_INLAY_HINTS_ENABLED);
      return true;
   }

   public static boolean isInlayHintsParameterTypesEnabled() {
      if (STORE.contains(PREFKEY_INLAY_HINTS_PARAMETER_TYPES_ENABLED))
         return STORE.getBoolean(PREFKEY_INLAY_HINTS_PARAMETER_TYPES_ENABLED);
      return true;
   }

   public static boolean isInlayHintsReturnTypesEnabled() {
      if (STORE.contains(PREFKEY_INLAY_HINTS_RETURN_TYPES_ENABLED))
         return STORE.getBoolean(PREFKEY_INLAY_HINTS_RETURN_TYPES_ENABLED);
      return true;
   }

   public static boolean isInlayHintsTypeArgumentsEnabled() {
      if (STORE.contains(PREFKEY_INLAY_HINTS_TYPE_ARGUMENTS_ENABLED))
         return STORE.getBoolean(PREFKEY_INLAY_HINTS_TYPE_ARGUMENTS_ENABLED);
      return true;
   }

   public static boolean isInlayHintsVariableTypesEnabled() {
      if (STORE.contains(PREFKEY_INLAY_HINTS_VARIABLE_TYPES_ENABLED))
         return STORE.getBoolean(PREFKEY_INLAY_HINTS_VARIABLE_TYPES_ENABLED);
      return true;
   }

   public static boolean isLSPTraceInitOptions() {
      if (STORE.contains(PREFKEY_LSP_TRACE_INITOPTS))
         return STORE.getBoolean(PREFKEY_LSP_TRACE_INITOPTS);
      return Platform.getDebugBoolean(Dart4EPlugin.PLUGIN_ID + "/trace/lsp/init_options");
   }

   public static boolean isLSPTraceIO() {
      if (STORE.contains(PREFKEY_LSP_TRACE_IO))
         return STORE.getBoolean(PREFKEY_LSP_TRACE_IO);
      return Platform.getDebugBoolean(Dart4EPlugin.PLUGIN_ID + "/trace/lsp/io");
   }

   public static boolean isLSPTraceIOVerbose() {
      if (STORE.contains(PREFKEY_LSP_TRACE_IO_VERBOSE))
         return STORE.getBoolean(PREFKEY_LSP_TRACE_IO_VERBOSE);
      return Platform.getDebugBoolean(Dart4EPlugin.PLUGIN_ID + "/trace/lsp/io/verbose");
   }

   public static boolean save() {
      try {
         STORE.save();
         return true;
      } catch (final IOException ex) {
         Dialogs.showStatus(Messages.Prefs_SavingPreferencesFailed, Dart4EPlugin.status().createError(ex), true);
         return false;
      }
   }

   public static void setDartSDKs(final @Nullable Set<DartSDK> newSDKs) {
      ensureDartSDKsInitialized();

      synchronized (dartSDKs) {
         dartSDKs.clear();
         if (newSDKs != null) {
            dartSDKs.addAll(newSDKs);
         }
         STORE.setValue(PREFKEY_DART_SDKS, JSON.serialize(dartSDKs));
      }
   }

   public static void setDefaultDartSDK(final String name) {
      STORE.setValue(PREFKEY_DEFAULT_DART_SDK, name);
   }

   public static void setFormatterMaxLineLength(final int maxLineLength) {
      STORE.setValue(PREFKEY_FORMATTER_MAX_LINE_LENGTH, maxLineLength);
   }

   private DartWorkspacePreference() {
   }
}
