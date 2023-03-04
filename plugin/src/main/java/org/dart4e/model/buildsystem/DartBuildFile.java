/*
 * SPDX-FileCopyrightText: © The Dart4E authors
 * SPDX-FileContributor: Sebastian Thomschke
 * SPDX-License-Identifier: EPL-2.0
 * SPDX-ArtifactOfProjectHomePage: https://github.com/dart4e/dart4e
 */
package org.dart4e.model.buildsystem;

import static net.sf.jstuff.core.validation.NullAnalysisHelper.asNonNull;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
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
import org.eclipse.jdt.annotation.NonNull;
import org.yaml.snakeyaml.Yaml;

import de.sebthom.eclipse.commons.resources.Resources;
import de.sebthom.eclipse.commons.ui.UI;
import de.sebthom.eclipse.commons.ui.widgets.NotificationPopup;
import net.sf.jstuff.core.Strings;
import net.sf.jstuff.core.io.RuntimeIOException;
import net.sf.jstuff.core.validation.Assert;

/**
 * @author Sebastian Thomschke
 */
public class DartBuildFile extends BuildFile {

   protected Set<DartDependency> deps = Collections.emptySet();

   protected DartBuildFile(final BuildSystem bs, final IFile location) {
      super(bs, location);
   }

   public DartBuildFile(final IFile location) {
      super(BuildSystem.DART, location);
   }

   @Override
   public Set<DartDependency> getDependencies(final IProgressMonitor monitor) {
      final var project = getProject();
      final var prefs = DartProjectPreference.get(project);

      final var dartSDK = prefs.getEffectiveDartSDK();
      if (dartSDK == null)
         throw new IllegalStateException("No Dart SDK found!");

      final var pubCacheDir = dartSDK.getPubCacheDir();
      Assert.isDirectoryReadable(pubCacheDir);

      if (!isLockFileUpToDate()) {
         resolveDependencies(monitor); // update needed
      } else if (!deps.isEmpty())
         return deps;

      /*
       * parse the pubspec.lock for the resolved dependencies
       */
      final var deps = new LinkedHashSet<DartDependency>();
      try (var reader = Files.newBufferedReader(Resources.toAbsolutePath(getLockFile()))) {
         final var lockFileYaml = new Yaml().loadAs(reader, Map.class);
         @SuppressWarnings("unchecked")
         final var packages = (Map<String, Map<String, ?>>) lockFileYaml.get("packages");
         if (packages != null) {
            for (final var entry : packages.entrySet()) {
               try {
                  final var name = entry.getKey();
                  monitor.setTaskName("Locating Dart library " + name + "...");

                  final var meta = entry.getValue();
                  final var descr = asNonNull(meta.get("description")); // can be String or (Map<String, String>)
                  final var source = (String) asNonNull(meta.get("source"));
                  final var version = (String) asNonNull(meta.get("version"));

                  final java.nio.file.Path libLocation = switch (source) {
                     case "hosted" -> {
                        final var url = new URL((String) ((Map<?, ?>) descr).get("url"));
                        yield pubCacheDir.resolve(source).resolve(url.getHost()).resolve(name + "-" + version);
                     }
                     case "git" -> {
                        final var resolvedRef = ((Map<?, ?>) descr).get("resolved-ref");
                        var resolvedLocalPath = pubCacheDir.resolve(source).resolve(name + "-" + resolvedRef);
                        if (!Files.exists(resolvedLocalPath)) {
                           final var gitUrl = (String) asNonNull(((Map<?, ?>) descr).get("url"));
                           var repoName = Strings.substringAfterLast(gitUrl, "/");
                           repoName = Strings.removeEnd(repoName, ".git");
                           resolvedLocalPath = pubCacheDir.resolve(source).resolve(repoName + "-" + resolvedRef);
                        }
                        yield resolvedLocalPath;
                     }
                     case "path" -> {
                        final var path = (@NonNull String) ((Map<?, ?>) descr).get("path");
                        final var isRelative = (Boolean) ((Map<?, ?>) descr).get("relative");
                        yield isRelative ? Resources.toAbsolutePath(project).resolve(path).normalize() : Paths.get(path);
                     }
                     case "sdk" -> {
                        yield getSDKDependencyLocation((String) descr, name, version);
                     }
                     default -> throw new IllegalArgumentException("Unkown source " + source + " for package " + name);
                  };

                  final var dependencyType = (String) asNonNull(meta.get("dependency"));
                  deps.add(new DartDependency(libLocation, name, version, dependencyType.contains("dev"), dependencyType.contains(
                     "transitive")));
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

   protected java.nio.file.Path getSDKDependencyLocation(final String sdkName, final String pkgName,
      @SuppressWarnings("unused") final String pkgVersion) {
      throw new IllegalArgumentException("Unsupported SDK [" + sdkName + "] of package [" + pkgName + "]");
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
               result.add(file.getProjectRelativePath());
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

   protected IFile getLockFile() {
      return getProject().getFile(Constants.PUBSPEC_LOCK_FILENAME);
   }

   @Override
   public Set<IPath> getSourcePaths() {
      return Set.of(//
         Path.fromOSString(Constants.PROJECT_LIB_DIRNAME), //
         Path.fromOSString(Constants.PROJECT_TEST_DIRNAME) //
      );
   }

   private boolean hasMainMethod(final IFile dartFile) {
      if (!dartFile.exists())
         return false;

      try (var lines = Files.lines(Resources.toAbsolutePath(dartFile))) {
         return lines.anyMatch(line -> line //
            .replaceAll("\\s{2,}", " ") //
            .contains("void main()"));
      } catch (final IOException ex) {
         Dart4EPlugin.log().error(ex);
      }
      return false;
   }

   protected boolean isLockFileUpToDate() {
      final var lockFile = getLockFile();
      return lockFile.exists() && Resources.lastModified(lockFile) > Resources.lastModified(location);
   }

   protected void resolveDependencies(final IProgressMonitor monitor) {
      final var project = getProject();
      try {
         DartConsole.runWithConsole(monitor, "Resolving dependencies of [" + project.getName() + "]...", project, "pub", "get");
      } catch (final CoreException ex) {
         throw new RuntimeException(ex);
      }
   }
}
