/*
 * SPDX-FileCopyrightText: © The Dart4E authors
 * SPDX-FileContributor: Sebastian Thomschke
 * SPDX-License-Identifier: EPL-2.0
 * SPDX-ArtifactOfProjectHomePage: https://github.com/dart4e/dart4e
 */
package org.dart4e.flutter.prefs;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.WeakHashMap;

import org.dart4e.Dart4EPlugin;
import org.dart4e.flutter.model.FlutterSDK;
import org.dart4e.localization.Messages;
import org.dart4e.navigation.DartDependenciesUpdater;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ProjectScope;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.jface.preference.IPersistentPreferenceStore;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.ui.preferences.ScopedPreferenceStore;

import de.sebthom.eclipse.commons.ui.Dialogs;

/**
 * @author Sebastian Thomschke
 */
public final class FlutterProjectPreference {

   private static final WeakHashMap<IProject, FlutterProjectPreference> PREFS_BY_PROJECT = new WeakHashMap<>();

   private static final String PROPERTY_ALTERNATE_FLUTTER_SDK = "flutter.project.alternate_sdk";

   public static FlutterProjectPreference get(final IProject project) {
      synchronized (PREFS_BY_PROJECT) {
         return PREFS_BY_PROJECT.computeIfAbsent(project, FlutterProjectPreference::new);
      }
   }

   private final IPersistentPreferenceStore prefs;
   private final IProject project;
   private final List<PropertyChangeEvent> changeEvents = new ArrayList<>();

   private FlutterProjectPreference(final IProject project) {
      this.project = project;
      prefs = new ScopedPreferenceStore(new ProjectScope(project), Dart4EPlugin.PLUGIN_ID);
      prefs.addPropertyChangeListener(changeEvents::add);
   }

   /**
    * @return null if none configured
    */
   public @Nullable FlutterSDK getAlternateFlutterSDK() {
      return FlutterWorkspacePreference.getFlutterSDK(prefs.getString(PROPERTY_ALTERNATE_FLUTTER_SDK));
   }

   /**
    * @return null if none found
    */
   public @Nullable FlutterSDK getEffectiveFlutterSDK() {
      final var sdk = getAlternateFlutterSDK();
      if (sdk == null)
         return FlutterWorkspacePreference.getDefaultFlutterSDK(false, true);
      return sdk;
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

   public void setAlternateFlutterSDK(final @Nullable FlutterSDK sdk) {
      prefs.setValue(PROPERTY_ALTERNATE_FLUTTER_SDK, sdk == null ? "" : sdk.getName());
   }
}
