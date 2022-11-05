/*
 * Copyright 2022 by the Dart4E authors.
 * SPDX-License-Identifier: EPL-2.0
 */
package org.dart4e.flutter.launch;

import org.dart4e.Dart4EPlugin;
import org.dart4e.flutter.model.Device;
import org.dart4e.flutter.model.FlutterSDK;
import org.dart4e.flutter.prefs.FlutterWorkspacePreference;
import org.dart4e.launch.LaunchConfigurations;
import org.dart4e.util.io.JSON;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.jdt.annotation.Nullable;

/**
 * @author Sebastian Thomschke
 */
public abstract class FlutterLaunchConfigurations {

   /** id of <launchGroup/> as specified in plugin.xml */
   public static final String LAUNCH_FLUTTER_GROUP = "org.dart4e.launch.flutter.group";

   private static final String LAUNCH_FLUTTER_ATTR_SDK = "launch.flutter.sdk";
   private static final String LAUNCH_FLUTTER_ATTR_DEVICE = "launch.flutter.device";

   public static @Nullable FlutterSDK getAlternativeFlutterSDK(final ILaunchConfiguration config) {
      try {
         return FlutterWorkspacePreference.getFlutterSDK(config.getAttribute(LAUNCH_FLUTTER_ATTR_SDK, ""));
      } catch (final Exception ex) {
         Dart4EPlugin.log().error(ex);
         return null;
      }
   }

   public static void setAlternativeFlutterSDK(final ILaunchConfigurationWorkingCopy config, @Nullable final FlutterSDK altSDK) {
      LaunchConfigurations.setAttribute(config, LAUNCH_FLUTTER_ATTR_SDK, altSDK == null ? null : altSDK.getName());
   }

   public static @Nullable Device getFlutterDevice(final ILaunchConfiguration config) {
      try {
         return JSON.deserializeNullable(config.getAttribute(LAUNCH_FLUTTER_ATTR_DEVICE, ""), Device.class);
      } catch (final Exception ex) {
         Dart4EPlugin.log().error(ex);
         return null;
      }
   }

   public static void setFlutterDevice(final ILaunchConfigurationWorkingCopy config, @Nullable final Device device) {
      LaunchConfigurations.setAttribute(config, LAUNCH_FLUTTER_ATTR_DEVICE, device == null ? null : JSON.serialize(device));
   }
}
