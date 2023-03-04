/*
 * Copyright 2022 by the Dart4E authors.
 * SPDX-License-Identifier: EPL-2.0
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
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.jface.preference.IPersistentPreferenceStore;
import org.eclipse.ui.dialogs.PreferencesUtil;
import org.eclipse.ui.preferences.ScopedPreferenceStore;

import de.sebthom.eclipse.commons.ui.Dialogs;
import de.sebthom.eclipse.commons.ui.UI;
import net.sf.jstuff.core.Strings;

/**
 * @author Sebastian Thomschke
 */
public final class DartWorkspacePreference {

   static final String PREFKEY_DEFAULT_DART_SDK = "dart.default_sdk";
   static final String PREFKEY_DART_SDKS = "dart.sdks";
   static final String PREFKEY_FORMATTER_MAX_LINE_LENGTH = "dart.formatter.max_line_length";
   static final String PREFKEY_WARNED_NO_SDK_REGISTERED = "dart.warned_no_sdk_registered";

   static final IPersistentPreferenceStore STORE = new ScopedPreferenceStore(InstanceScope.INSTANCE, Dart4EPlugin.PLUGIN_ID);

   // CHECKSTYLE:IGNORE .* FOR NEXT 3 LINES
   private static final SortedSet<DartSDK> dartSDKs = new TreeSet<>();
   private static boolean isDartSDKsInitialized = false;

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

            UI.run(() -> {
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
      final var maxLineLength = STORE.getInt(PREFKEY_FORMATTER_MAX_LINE_LENGTH);
      return maxLineLength > 0 ? maxLineLength : 80;
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
