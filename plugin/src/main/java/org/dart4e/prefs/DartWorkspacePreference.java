/*
 * Copyright 2022 by the Dart4E authors.
 * SPDX-License-Identifier: EPL-2.0
 */
package org.dart4e.prefs;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import org.dart4e.Dart4EPlugin;
import org.dart4e.localization.Messages;
import org.dart4e.model.DartSDK;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.jface.preference.IPersistentPreferenceStore;
import org.eclipse.ui.dialogs.PreferencesUtil;
import org.eclipse.ui.preferences.ScopedPreferenceStore;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.deser.std.FromStringDeserializer;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;

import de.sebthom.eclipse.commons.ui.Dialogs;
import de.sebthom.eclipse.commons.ui.UI;
import net.sf.jstuff.core.Strings;
import net.sf.jstuff.core.io.RuntimeIOException;

/**
 * @author Sebastian Thomschke
 */
public final class DartWorkspacePreference {

   private static final ObjectMapper JSON = new ObjectMapper();

   private static final String PROPERTY_DEFAULT_DART_SDK = "dart.default_sdk";
   private static final String PROPERTY_DART_SDKS = "dart.sdks";
   private static final String PROPERTY_WARNED_NO_SDK_REGISTERED = "dart.warned_no_sdk_registered";
   private static final String PROPERTY_FORMATTER_MAX_LINE_LENGTH = "dart.formatter.max_line_length";

   static {
      // this disables usage of com.fasterxml.jackson.databind.ext.NioPathDeserializer
      // which results in freezes because on first usage all drive letters are iterated
      // which will hang for mapped but currently not reachable network drives
      final var m = new SimpleModule("CustomNioPathSerialization");
      @SuppressWarnings("null")
      final JsonSerializer<@Nullable Object> serializer = new ToStringSerializer();
      m.addSerializer(Path.class, serializer);
      m.addDeserializer(Path.class, new FromStringDeserializer<Path>(Path.class) {
         private static final long serialVersionUID = 1L;

         @Override
         protected Path _deserialize(final String value, final DeserializationContext ctxt) throws IOException {
            return Paths.get(value);
         }
      });
      JSON.registerModule(m);
   }

   static final IPersistentPreferenceStore PREFS = new ScopedPreferenceStore(InstanceScope.INSTANCE, Dart4EPlugin.PLUGIN_ID);

   // CHECKSTYLE:IGNORE .* FOR NEXT 3 LINES
   private static final SortedSet<DartSDK> dartSDKs = new TreeSet<>();
   private static boolean isDartSDKsInitialized = false;

   private static void ensureDartSDKsInitialized() {
      synchronized (dartSDKs) {
         if (!isDartSDKsInitialized) {
            isDartSDKsInitialized = true;

            final var dartSDKsSerialized = PREFS.getString(PROPERTY_DART_SDKS);
            if (Strings.isNotBlank(dartSDKsSerialized)) {
               try {
                  dartSDKs.addAll(JSON.readValue(dartSDKsSerialized, new TypeReference<List<DartSDK>>() {}));
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

         if (dartSDKs.isEmpty() && !PREFS.getBoolean(PROPERTY_WARNED_NO_SDK_REGISTERED)) {
            for (final var ste : new Throwable().getStackTrace()) {
               final var prefPage = DartSDKPreferencePage.class.getName();
               // don't show warning on empty workspace if we directly go to Dart Prefs
               if (ste.getClassName().equals(prefPage))
                  return;
            }

            PREFS.setValue(PROPERTY_WARNED_NO_SDK_REGISTERED, true);
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
      final var defaultSDK = getDartSDK(PREFS.getString(PROPERTY_DEFAULT_DART_SDK));

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
      final var maxLineLength = PREFS.getInt(PROPERTY_FORMATTER_MAX_LINE_LENGTH);
      return maxLineLength > 0 ? maxLineLength : 80;
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

   public static void setDartSDKs(final @Nullable Set<DartSDK> newSDKs) {
      ensureDartSDKsInitialized();

      synchronized (dartSDKs) {
         dartSDKs.clear();
         if (newSDKs != null) {
            dartSDKs.addAll(newSDKs);
         }
         try {
            PREFS.setValue(PROPERTY_DART_SDKS, JSON.writeValueAsString(dartSDKs));
         } catch (final JsonProcessingException ex) {
            throw new RuntimeIOException(ex);
         }
      }
   }

   public static void setDefaultDartSDK(final String name) {
      PREFS.setValue(PROPERTY_DEFAULT_DART_SDK, name);
   }

   public static void setFormatterMaxLineLength(final int maxLineLength) {
      PREFS.setValue(PROPERTY_FORMATTER_MAX_LINE_LENGTH, maxLineLength);
   }

   private DartWorkspacePreference() {
   }
}
