/*
 * Copyright 2022 by the Dart4E authors.
 * SPDX-License-Identifier: EPL-2.0
 */
package org.dart4e.model.buildsystem;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import org.dart4e.Constants;
import org.dart4e.Dart4EPlugin;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.annotation.Nullable;

/**
 * @author Sebastian Thomschke
 */
public enum BuildSystem {

   DART,
   FLUTTER;

   private static boolean contains(final IFile file, final String searchFor) throws CoreException, IOException {
      if (file.exists()) {
         try (var reader = new BufferedReader(new InputStreamReader(file.getContents(true)))) {
            for (String line = reader.readLine(); line != null; line = reader.readLine()) {
               if (line.contains(searchFor))
                  return true;
            }
         }
      }
      return false;
   }

   public static BuildSystem guessBuildSystemOfProject(final IProject project) {
      final var buildFile = project.getFile(Constants.PUBSPEC_YAML_FILENAME);
      try {
         if (contains(buildFile, "flutter:"))
            return FLUTTER;
      } catch (final Exception ex) {
         Dart4EPlugin.log().error(ex);
      }
      return DART;
   }

   @Nullable
   public BuildFile findBuildFile(final IProject project) {
      final var buildFile = project.getFile(Constants.PUBSPEC_YAML_FILENAME);
      if (buildFile.exists())
         return toBuildFile(buildFile);
      return null;
   }

   public BuildFile toBuildFile(final IFile buildFilePath) {
      return switch (this) {
         case DART, FLUTTER -> new DartBuildFile(buildFilePath);
         default -> throw new UnsupportedOperationException("Unsupported build-system: " + this);
      };
   }
}
