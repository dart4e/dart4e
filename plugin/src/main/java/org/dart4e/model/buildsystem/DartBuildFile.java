/*
 * Copyright 2022 by the Dart4E authors.
 * SPDX-License-Identifier: EPL-2.0
 */
package org.dart4e.model.buildsystem;

import static net.sf.jstuff.core.validation.NullAnalysisHelper.asNonNull;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.dart4e.Constants;
import org.dart4e.Dart4EPlugin;
import org.dart4e.console.DartConsole;
import org.dart4e.model.DartDependency;
import org.dart4e.prefs.DartProjectPreference;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.yaml.snakeyaml.Yaml;

import de.sebthom.eclipse.commons.resources.Resources;
import de.sebthom.eclipse.commons.ui.UI;
import de.sebthom.eclipse.commons.ui.widgets.NotificationPopup;
import net.sf.jstuff.core.Strings;
import net.sf.jstuff.core.io.RuntimeIOException;

/**
 * @author Sebastian Thomschke
 */
public class DartBuildFile extends BuildFile {

   private Set<DartDependency> deps = Collections.emptySet();

   DartBuildFile(final BuildSystem bs, final IFile location) {
      super(bs, location);
   }

   DartBuildFile(final IFile location) {
      super(BuildSystem.DART, location);
   }

   @Override
   public Set<DartDependency> getDependencies(final IProgressMonitor monitor) {
      final var project = getProject();
      final var prefs = DartProjectPreference.get(project);
      final var dartSDK = prefs.getEffectiveDartSDK();
      if (dartSDK == null)
         throw new IllegalStateException("No Dart SDK found!");

      final var lockFile = project.getFile(Constants.PUBSPEC_LOCK_FILENAME);

      /*
       * check if pubspec.lock file exists, if not run `dart pub deps get` to create it
       */
      if (!lockFile.exists() || Resources.lastModified(lockFile) < Resources.lastModified(location)) {
         resolveDependencies(monitor); // update needed
      } else if (!deps.isEmpty())
         return deps;

      /*
       * parse the pubspec.lock for the resolved dependencies
       */
      final var deps = new LinkedHashSet<DartDependency>();
      try (var reader = Files.newBufferedReader(asNonNull(lockFile.getLocation()).toFile().toPath())) {
         final var yaml = new Yaml().loadAs(reader, Map.class);
         @SuppressWarnings("unchecked")
         final var packages = (Map<String, Map<String, Object>>) yaml.get("packages");
         if (packages != null) {
            for (final var pkgMeta : packages.entrySet()) {
               try {
                  deps.add(DartDependency.from(dartSDK, pkgMeta.getKey(), pkgMeta.getValue(), monitor));
               } catch (final Exception ex) {
                  Dart4EPlugin.log().error(ex);
                  UI.run(() -> new NotificationPopup(ex.getMessage()).open());
               }
            }
         }
      } catch (final IOException ex) {
         throw new RuntimeIOException(ex);
      }
      this.deps = deps;
      return deps;
   }

   /**
    * Returns a list of executable dart files as defined in pubspec.yaml (see https://dart.dev/tools/pub/pubspec#executables)
    * or as a fallback lib/main.dart in case it exists.
    */
   public List<IPath> getExecutables() {
      final var result = new ArrayList<IPath>();
      try (var reader = Resources.newBufferedReader(location)) {
         final var yaml = new Yaml().loadAs(reader, Map.class);
         @SuppressWarnings("unchecked")
         final var executables = (Map<String, String>) yaml.getOrDefault("executables", Collections.emptyMap());
         for (final var exe : executables.entrySet()) {
            final var filePath = "bin/" + Strings.defaultIfEmpty(exe.getValue(), exe.getKey());
            final var file = getProject().getFile(filePath);
            if (file.exists()) {
               result.add(Path.fromOSString(filePath));
            }
         }
      } catch (final CoreException ex) {
         throw new RuntimeException(ex);
      } catch (final IOException ex) {
         throw new RuntimeIOException(ex);
      }
      if (result.isEmpty()) {
         final var mainDartFile = getProject().getFile("lib/main.dart");
         if (hasMainMethod(mainDartFile)) {
            result.add(mainDartFile.getProjectRelativePath());
         }
      }
      return result;
   }

   @Override
   public Set<IPath> getSourcePaths() {
      return Set.of(//
         Path.fromOSString("lib"), //
         Path.fromOSString("test") //
      );
   }

   private boolean hasMainMethod(final IFile dartFile) {
      if (!dartFile.exists())
         return false;
      try {
         final var reader = new BufferedReader(new InputStreamReader(dartFile.getContents(true)));
         for (String line = reader.readLine(); line != null; line = reader.readLine()) {
            if (line.replaceAll("\\s{2,}", " ").contains("void main()"))
               return true;
         }
      } catch (final CoreException | IOException ex) {
         Dart4EPlugin.log().error(ex);
      }
      return false;
   }

   private void resolveDependencies(final IProgressMonitor monitor) {
      final var project = getProject();
      try {
         DartConsole.runWithConsole(monitor, "Resolving dependencies of [" + project.getName() + "]...", project, "pub", "get");
      } catch (final CoreException ex) {
         throw new RuntimeException(ex);
      }
   }
}
