/*
 * Copyright 2022 by the Dart4E authors.
 * SPDX-License-Identifier: EPL-2.0
 */
package org.dart4e.prefs;

import org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer;

/**
 * @author Sebastian Thomschke
 */
public final class DartWorkspacePreferenceInitializer extends AbstractPreferenceInitializer {

   @Override
   public void initializeDefaultPreferences() {
      DartWorkspacePreference.STORE.setDefault(DartWorkspacePreference.PREFKEY_WARNED_NO_SDK_REGISTERED, false);
   }
}
