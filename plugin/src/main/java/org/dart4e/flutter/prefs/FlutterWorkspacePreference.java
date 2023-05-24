/*
 * SPDX-FileCopyrightText: © The Dart4E authors
 * SPDX-FileContributor: Sebastian Thomschke
 * SPDX-License-Identifier: EPL-2.0
 * SPDX-ArtifactOfProjectHomePage: https://github.com/dart4e/dart4e
 */
package org.dart4e.flutter.prefs;

import java.io.IOException;
import java.util.Objects;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import org.dart4e.Dart4EPlugin;
import org.dart4e.flutter.model.FlutterSDK;
import org.dart4e.localization.Messages;
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
public final class FlutterWorkspacePreference {

   private static final String PROPERTY_DEFAULT_FLUTTER_SDK = "flutter.default_sdk";
   private static final String PROPERTY_FLUTTER_SDKS = "flutter.sdks";
   private static final String PROPERTY_WARNED_NO_SDK_REGISTERED = "flutter.warned_no_sdk_registered";

   static final IPersistentPreferenceStore PREFS = new ScopedPreferenceStore(InstanceScope.INSTANCE, Dart4EPlugin.PLUGIN_ID);

   // CHECKSTYLE:IGNORE .* FOR NEXT 3 LINES
   private static final SortedSet<FlutterSDK> flutterSDKs = new TreeSet<>();
   private static boolean isFlutterSDKsInitialized = false;

   private static void ensureFlutterSDKsInitialized() {
      synchronized (flutterSDKs) {
         if (!isFlutterSDKsInitialized) {
            isFlutterSDKsInitialized = true;

            final var flutterSDKsSerialized = PREFS.getString(PROPERTY_FLUTTER_SDKS);
            if (Strings.isNotBlank(flutterSDKsSerialized)) {
               try {
                  flutterSDKs.addAll(JSON.deserializeList(flutterSDKsSerialized, FlutterSDK.class));
               } catch (final Exception ex) {
                  Dart4EPlugin.log().error(ex);
               }
            }

            if (flutterSDKs.isEmpty()) {
               final var defaultSDK = FlutterSDK.fromPath();
               if (defaultSDK != null) {
                  Dart4EPlugin.log().info("Registering system {0} with {1}", defaultSDK);
                  flutterSDKs.add(defaultSDK);
                  setDefaultFlutterSDK(defaultSDK.getName());
                  save();
               }
            }
         }

         if (flutterSDKs.isEmpty() && !PREFS.getBoolean(PROPERTY_WARNED_NO_SDK_REGISTERED)) {
            for (final var ste : new Throwable().getStackTrace()) {
               final var prefPage = FlutterSDKPreferencePage.class.getName();
               // don't show warning on empty workspace if we directly go to Flutter Prefs
               if (ste.getClassName().equals(prefPage))
                  return;
            }

            PREFS.setValue(PROPERTY_WARNED_NO_SDK_REGISTERED, true);
            save();

            UI.runAsync(() -> {
               Dialogs.showError(Messages.Flutter_Prefs_NoSDKRegistered_Title, Messages.Flutter_Prefs_NoSDKRegistered_Body);
               final var dialog = PreferencesUtil.createPreferenceDialogOn( //
                  UI.getShell(), //
                  FlutterSDKPreferencePage.class.getName(), //
                  new String[] {FlutterSDKPreferencePage.class.getName()}, //
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
   public static @Nullable FlutterSDK getFlutterSDK(final String name) {
      if (Strings.isEmpty(name))
         return null;

      ensureFlutterSDKsInitialized();

      synchronized (flutterSDKs) {
         for (final var sdk : flutterSDKs)
            if (Objects.equals(name, sdk.getName()))
               return sdk;
      }
      return null;
   }

   public static SortedSet<FlutterSDK> getFlutterSDKs() {
      ensureFlutterSDKsInitialized();

      synchronized (flutterSDKs) {
         return new TreeSet<>(flutterSDKs);
      }
   }

   /**
    * @return null if not found
    */
   public static @Nullable FlutterSDK getDefaultFlutterSDK(final boolean verify, final boolean searchPATH) {
      final var defaultSDK = getFlutterSDK(PREFS.getString(PROPERTY_DEFAULT_FLUTTER_SDK));

      if (defaultSDK != null) {
         if (!verify || defaultSDK.isValid())
            return defaultSDK;
      }

      ensureFlutterSDKsInitialized();

      synchronized (flutterSDKs) {
         if (!flutterSDKs.isEmpty()) {
            if (!verify)
               return flutterSDKs.first();
            for (final var sdk : flutterSDKs) {
               if (sdk.isValid())
                  return sdk;
            }
         }
      }

      if (!searchPATH)
         return null;

      return FlutterSDK.fromPath();
   }

   public static boolean save() {
      try {
         PREFS.save();
         return true;
      } catch (final IOException ex) {
         Dialogs.showStatus(Messages.Prefs_SavingPreferencesFailed, Dart4EPlugin.status().createError(ex), true);
         return false;
      }
   }

   public static void setFlutterSDKs(final @Nullable Set<FlutterSDK> newSDKs) {
      ensureFlutterSDKsInitialized();

      synchronized (flutterSDKs) {
         flutterSDKs.clear();
         if (newSDKs != null) {
            flutterSDKs.addAll(newSDKs);
         }
         PREFS.setValue(PROPERTY_FLUTTER_SDKS, JSON.serialize(flutterSDKs));
      }
   }

   public static void setDefaultFlutterSDK(final String name) {
      PREFS.setValue(PROPERTY_DEFAULT_FLUTTER_SDK, name);
   }

   private FlutterWorkspacePreference() {
   }
}
