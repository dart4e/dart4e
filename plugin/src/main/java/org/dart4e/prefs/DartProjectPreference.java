/*
 * Copyright 2022 by the Dart4E authors.
 * SPDX-License-Identifier: EPL-2.0
 */
package org.dart4e.prefs;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.WeakHashMap;

import org.dart4e.Dart4EPlugin;
import org.dart4e.localization.Messages;
import org.dart4e.model.DartSDK;
import org.dart4e.model.buildsystem.BuildSystem;
import org.dart4e.navigation.DartDependenciesUpdater;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ProjectScope;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.jface.preference.IPersistentPreferenceStore;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.ui.preferences.ScopedPreferenceStore;

import de.sebthom.eclipse.commons.ui.Dialogs;
import net.sf.jstuff.core.Strings;

/**
 * @author Sebastian Thomschke
 */
public final class DartProjectPreference {

   private static final WeakHashMap<IProject, DartProjectPreference> PREFS_BY_PROJECT = new WeakHashMap<>();

   private static final String PROPERTY_ALTERNATE_AUTO_BUILD = "dart.project.auto_build";
   private static final String PROPERTY_ALTERNATE_DART_SDK = "dart.project.alternate_sdk";
   private static final String PROPERTY_BUILD_SYSTEM = "dart.project.build_system";

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
      prefs.setDefault(PROPERTY_ALTERNATE_AUTO_BUILD, true);
      prefs.addPropertyChangeListener(changeEvents::add);
   }

   /**
    * @return null if none configured
    */
   public @Nullable DartSDK getAlternateDartSDK() {
      return DartWorkspacePreference.getDartSDK(prefs.getString(PROPERTY_ALTERNATE_DART_SDK));
   }

   public BuildSystem getBuildSystem() {
      final var bs = prefs.getString(PROPERTY_BUILD_SYSTEM);
      if (Strings.isNotBlank(bs)) {
         try {
            return BuildSystem.valueOf(bs);
         } catch (final IllegalArgumentException ex) {
            Dart4EPlugin.log().error(ex);
         }
      }

      return BuildSystem.guessBuildSystemOfProject(project);
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

   public IProject getProject() {
      return project;
   }

   public boolean isAutoBuild() {
      return prefs.getBoolean(PROPERTY_ALTERNATE_AUTO_BUILD);
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

   public void setAutoBuild(final boolean value) {
      prefs.setValue(PROPERTY_ALTERNATE_AUTO_BUILD, value);
   }

   public void setBuildSystem(final @Nullable BuildSystem buildSystem) {
      prefs.setValue(PROPERTY_BUILD_SYSTEM, buildSystem == null ? "" : buildSystem.name());
   }
}
