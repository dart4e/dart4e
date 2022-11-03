/*
 * Copyright 2022 by the Dart4E authors.
 * SPDX-License-Identifier: EPL-2.0
 */
package org.dart4e.prefs;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.WeakHashMap;

import org.dart4e.Dart4EPlugin;
import org.dart4e.localization.Messages;
import org.dart4e.model.DartSDK;
import org.dart4e.navigation.DartDependenciesUpdater;
import org.ec4j.core.Resource;
import org.ec4j.core.ResourcePath.ResourcePaths;
import org.ec4j.core.ResourcePropertiesService;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ProjectScope;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.jface.preference.IPersistentPreferenceStore;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.ui.preferences.ScopedPreferenceStore;

import de.sebthom.eclipse.commons.resources.Resources;
import de.sebthom.eclipse.commons.ui.Dialogs;

/**
 * @author Sebastian Thomschke
 */
public final class DartProjectPreference {

   private static final WeakHashMap<IProject, DartProjectPreference> PREFS_BY_PROJECT = new WeakHashMap<>();

   private static final String PROPERTY_ALTERNATE_DART_SDK = "dart.project.alternate_sdk";
   private static final String PROPERTY_FORMATTER_MAX_LINE_LENGTH = "dart.formatter.max_line_length";

   public static DartProjectPreference get(final IProject project) {
      synchronized (PREFS_BY_PROJECT) {
         return PREFS_BY_PROJECT.computeIfAbsent(project, DartProjectPreference::new);
      }
   }

   private final IPersistentPreferenceStore prefs;
   private final IProject project;
   private final List<PropertyChangeEvent> changeEvents = new ArrayList<>();

   private DartProjectPreference(final IProject project) {
      this.project = project;
      prefs = new ScopedPreferenceStore(new ProjectScope(project), Dart4EPlugin.PLUGIN_ID);
      prefs.addPropertyChangeListener(changeEvents::add);
   }

   /**
    * @return null if none configured
    */
   public @Nullable DartSDK getAlternateDartSDK() {
      return DartWorkspacePreference.getDartSDK(prefs.getString(PROPERTY_ALTERNATE_DART_SDK));
   }

   /**
    * @return null if none found
    */
   public @Nullable DartSDK getEffectiveDartSDK() {
      final var sdk = getAlternateDartSDK();
      if (sdk == null)
         return DartWorkspacePreference.getDefaultDartSDK(false, true);
      return sdk;
   }

   public int getFormatterMaxLineLength() {
      // try to resolve max-line-length via .editorconfig
      try {
         final var propService = ResourcePropertiesService.builder() //
            .rootDirectory(ResourcePaths.ofPath(Resources.toAbsolutePath(project), StandardCharsets.UTF_8)) //
            .build();

         final var props = propService.queryProperties(Resource.Resources.ofPath(Resources.toAbsolutePath(project.getFile("some.dart")),
            StandardCharsets.UTF_8));
         final var maxLineLength = props.getValue("max_line_length", null, true);
         if (maxLineLength != null)
            return Integer.parseInt(maxLineLength.toString());
      } catch (final Exception ex) {
         Dart4EPlugin.log().error(ex);
      }

      final var maxLineLength = prefs.getInt(PROPERTY_FORMATTER_MAX_LINE_LENGTH);
      return maxLineLength > 0 ? maxLineLength : DartWorkspacePreference.getFormatterMaxLineLength();
   }

   public IProject getProject() {
      return project;
   }

   /**
    * Reverts the preference state to the last persistent state.
    */
   public void revert() {
      final var changedEventsCopy = new ArrayList<>(changeEvents);
      Collections.reverse(changedEventsCopy);
      for (final var event : changedEventsCopy) {
         final var oldValue = event.getOldValue();
         if (oldValue == null) {
            prefs.setToDefault(event.getProperty());
         }
         if (oldValue instanceof String) {
            prefs.setValue(event.getProperty(), (String) oldValue);
         } else if (oldValue instanceof Boolean) {
            prefs.setValue(event.getProperty(), (Boolean) oldValue);
         } else if (oldValue instanceof Integer) {
            prefs.setValue(event.getProperty(), (Integer) oldValue);
         } else if (oldValue instanceof Long) {
            prefs.setValue(event.getProperty(), (Long) oldValue);
         } else if (oldValue instanceof Float) {
            prefs.setValue(event.getProperty(), (Float) oldValue);
         } else if (oldValue instanceof Double) {
            prefs.setValue(event.getProperty(), (Double) oldValue);
         }
      }
      changeEvents.clear();
   }

   public boolean save() {
      try {
         changeEvents.clear();
         prefs.save();

         DartDependenciesUpdater.INSTANCE.onProjectConfigChanged(project);

         return true;
      } catch (final IOException ex) {
         Dialogs.showStatus(Messages.Prefs_SavingPreferencesFailed, Dart4EPlugin.status().createError(ex), true);
         return false;
      }
   }

   public void setAlternateDartSDK(final @Nullable DartSDK sdk) {
      prefs.setValue(PROPERTY_ALTERNATE_DART_SDK, sdk == null ? "" : sdk.getName());
   }

   public void setFormatterMaxLineLength(final int maxLineLength) {
      if (DartWorkspacePreference.getFormatterMaxLineLength() == maxLineLength) {
         prefs.setToDefault(PROPERTY_FORMATTER_MAX_LINE_LENGTH);
      } else {
         prefs.setValue(PROPERTY_FORMATTER_MAX_LINE_LENGTH, maxLineLength);
      }
   }
}
