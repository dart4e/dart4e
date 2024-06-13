/*
 * SPDX-FileCopyrightText: © The Dart4E authors
 * SPDX-FileContributor: Sebastian Thomschke
 * SPDX-License-Identifier: EPL-2.0
 * SPDX-ArtifactOfProjectHomePage: https://github.com/dart4e/dart4e
 */
package org.dart4e.flutter.model;

import static net.sf.jstuff.core.validation.NullAnalysisHelper.lateNonNull;

import java.util.Map;
import java.util.Objects;

import org.eclipse.jdt.annotation.Nullable;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import net.sf.jstuff.core.Strings;

/**
 * Represents a Device see
 * https://github.com/flutter/flutter/blob/6a66aa282f8423de2628b7fcb30d353ce248cbe3/packages/flutter_tools/lib/src/device.dart#L418
 *
 * @author Sebastian Thomschke
 */
@JsonAutoDetect(getterVisibility = Visibility.NONE)
@JsonIgnoreProperties(ignoreUnknown = true)
public class Device {

   public enum Category {
      DESKTOP,
      MOBILE,
      OTHER,
      WEB
   }

   /** e.g. "RF8M42N6JQY", "emulator-5554", "windows", "edge" */
   public final String id;

   /** e.g. "SM G970F", "Android SDK built for x86 64", "Windows", "Edge" */
   public final String name;

   /** e.g. "Android 12 (API 31)", "Android 5.0.2 (API 21)", "Microsoft Windows [Version 10.0.19045.2130]", "Microsoft Edge 106.0.1370.52" */
   public final String sdk;

   public final @JsonAlias("emulator") boolean isEmulator;
   public final @JsonAlias("supported") boolean isSupported;

   /** e.g. "android-arm64", "android-x64", "windows-x64", "web-javascript" */
   public final String targetPlatform;

   /** e.g. "hotReload", "hotRestart", "screenshot", "fastStart", "flutterExit", "hardwareRendering", "startPaused" */
   public final Map<String, Boolean> capabilities;

   protected Device() {
      // for Jackson
      id = lateNonNull();
      name = lateNonNull();
      sdk = lateNonNull();
      isEmulator = false;
      isSupported = false;
      targetPlatform = lateNonNull();
      capabilities = lateNonNull();
   }

   @Override
   public boolean equals(@Nullable final Object obj) {
      if (this == obj)
         return true;
      if (obj == null || getClass() != obj.getClass())
         return false;
      final Device other = (Device) obj;
      return Objects.equals(id, other.id);
   }

   public Category getCategory() {
      return switch (getTargetPlatformType()) {
         case "android" -> Category.MOBILE;
         case "fuchsia" -> Category.OTHER;
         case "ios" -> Category.MOBILE;
         case "linux" -> Category.MOBILE;
         case "macos" -> Category.DESKTOP;
         case "web" -> Category.WEB;
         case "windows" -> Category.DESKTOP;
         default -> Category.OTHER;
      };
   }

   /**
    * e.g. "android", "custom", "fuchsia", "ios", "linux", "macos", "web", "windows"
    */
   public String getTargetPlatformType() {
      return Strings.substringBefore(targetPlatform, "-");
   }

   @Override
   public int hashCode() {
      return Objects.hash(id);
   }

   @Override
   public String toString() {
      return Strings.toString(this, //
         "id", id, //
         "name", name, //
         "isEmulator", isEmulator, //
         "isSupported", isSupported, //
         "sdk", sdk, //
         "targetPlatform", targetPlatform //
      );
   }
}
