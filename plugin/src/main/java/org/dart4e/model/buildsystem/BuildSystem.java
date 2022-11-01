/*
 * Copyright 2022 by the Dart4E authors.
 * SPDX-License-Identifier: EPL-2.0
 */
package org.dart4e.model.buildsystem;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.dart4e.Constants;
import org.dart4e.Dart4EPlugin;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.jdt.annotation.Nullable;

import de.sebthom.eclipse.commons.resources.Resources;

/**
 * @author Sebastian Thomschke
 */
public enum BuildSystem {

   DART,
   FLUTTER,
   UNKNOWN;

   private static boolean contains(final Path file, final String searchFor) throws IOException {
      if (Files.exists(file)) {
         try (var reader = Files.newBufferedReader(file)) {
            for (String line = reader.readLine(); line != null; line = reader.readLine()) {
               if (line.contains(searchFor))
                  return true;
            }
         }
      }
      return false;
   }

   public static BuildSystem guessBuildSystemOfProject(final IProject project) {
      return guessBuildSystemOfProject(Resources.toAbsolutePath(project));
   }

   public static BuildSystem guessBuildSystemOfProject(final Path projectFolder) {
      final var buildFile = projectFolder.resolve(Constants.PUBSPEC_YAML_FILENAME);
      if (!Files.exists(buildFile))
         return BuildSystem.UNKNOWN;

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
      if (this == UNKNOWN)
         return null;

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
