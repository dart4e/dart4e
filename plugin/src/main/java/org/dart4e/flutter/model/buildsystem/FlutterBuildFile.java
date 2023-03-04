/*
 * SPDX-FileCopyrightText: © The Dart4E authors
 * SPDX-FileContributor: Sebastian Thomschke
 * SPDX-License-Identifier: EPL-2.0
 * SPDX-ArtifactOfProjectHomePage: https://github.com/dart4e/dart4e
 */
package org.dart4e.flutter.model.buildsystem;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.dart4e.flutter.console.FlutterConsole;
import org.dart4e.flutter.prefs.FlutterProjectPreference;
import org.dart4e.model.buildsystem.BuildSystem;
import org.dart4e.model.buildsystem.DartBuildFile;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;

/**
 * @author Sebastian Thomschke
 */
public class FlutterBuildFile extends DartBuildFile {

   protected FlutterBuildFile(final BuildSystem bs, final IFile location) {
      super(bs, location);
   }

   public FlutterBuildFile(final IFile location) {
      super(BuildSystem.FLUTTER, location);
   }

   @Override
   protected Path getSDKDependencyLocation(final String sdkName, final String pkgName, final String pkgVersion) {
      if (!"flutter".equals(sdkName))
         throw new IllegalArgumentException("Unsupported SDK [" + sdkName + "] of package [" + pkgName + "]");

      final var prefs = FlutterProjectPreference.get(getProject());
      final var flutterSDK = prefs.getEffectiveFlutterSDK();
      if (flutterSDK == null)
         throw new IllegalStateException("No Flutter SDK found!");

      var path = flutterSDK.getInstallRoot().resolve(Paths.get("packages", pkgName));
      if (Files.isDirectory(path))
         return path;

      path = flutterSDK.getInstallRoot().resolve(Paths.get("bin/cache/pkg", pkgName));
      if (Files.isDirectory(path))
         return path;

      throw new IllegalArgumentException("Package [" + pkgName + "] of SDK [" + sdkName + "] not found.");
   }

   @Override
   protected void resolveDependencies(final IProgressMonitor monitor) {
      final var project = getProject();
      try {
         FlutterConsole.runWithConsole(monitor, "Resolving dependencies of [" + project.getName() + "]...", project, "pub", "get");
      } catch (final CoreException ex) {
         throw new RuntimeException(ex);
      }
   }
}
