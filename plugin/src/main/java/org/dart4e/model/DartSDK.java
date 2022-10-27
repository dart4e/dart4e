/*
 * Copyright 2022 by the Dart4E authors.
 * SPDX-License-Identifier: EPL-2.0
 */
package org.dart4e.model;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;

import org.dart4e.Dart4EPlugin;
import org.dart4e.console.DartConsole;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.annotation.Nullable;

import com.fasterxml.jackson.annotation.JsonIgnore;

import net.sf.jstuff.core.Strings;
import net.sf.jstuff.core.SystemUtils;
import net.sf.jstuff.core.functional.Suppliers;
import net.sf.jstuff.core.io.Processes;
import net.sf.jstuff.core.ref.MutableRef;
import net.sf.jstuff.core.validation.Args;

/**
 * @author Sebastian Thomschke
 */
public final class DartSDK implements Comparable<DartSDK> {

   /**
    * Points to a folder containing the Dart SDK
    */
   public static final String ENV_DART_HOME = "DART_HOME";

   /**
    * Points to a folder containing downloaded dartlibs
    */
   public static final String ENV_PUB_CACHE = "PUB_CACHE";

   @JsonIgnore
   private static final Supplier<@Nullable DartSDK> DARTSDK_FROM_PATH = Suppliers.memoize(() -> {
      final var dartHome = SystemUtils.getEnvironmentVariable(ENV_DART_HOME, "");
      if (Strings.isBlank(dartHome))
         return null;

      var sdk = new DartSDK(Paths.get(dartHome));
      if (sdk.isValid())
         return sdk;

      final var dartExe = SystemUtils.findExecutable("dart", true);
      if (dartExe == null)
         return null;

      sdk = new DartSDK(dartExe.getParent().getParent());
      return sdk.isValid() ? sdk : null;
   }, dartSDK -> dartSDK == null ? 15_000 : 60_000);

   /**
    * Tries to locate the Dart SDK via DART_HOME and PATH environment variables
    *
    * @return null if not found
    */
   @Nullable
   public static DartSDK fromPath() {
      return DARTSDK_FROM_PATH.get();
   }

   private String name;
   private Path installRoot;

   @JsonIgnore
   private final Supplier<Boolean> isValidCached = Suppliers.memoize(() -> {
      final var dartExe = getDartExecutable();
      if (!Files.isExecutable(dartExe))
         return false;

      final var processBuilder = Processes.builder(dartExe);
      try (var reader = new BufferedReader(new InputStreamReader(processBuilder.start().getStdOut()))) {
         if (reader.readLine().contains("Dart"))
            return true;
      } catch (final IOException ex) {
         // ignore
      }
      return false;
   }, valid -> valid ? 60_000 : 15_000);

   @SuppressWarnings("unused")
   private DartSDK() {
      // for Jackson

      // only to satisfy annotation-based null-safety analysis:
      name = "";
      installRoot = Path.of("");
   }

   public DartSDK(final Path installRoot) {
      this.installRoot = installRoot.normalize().toAbsolutePath();
      name = "dart-" + getVersion();
   }

   public DartSDK(final String name, final Path installRoot) {
      Args.notBlank("name", name);

      this.name = name;
      this.installRoot = installRoot.normalize().toAbsolutePath();
   }

   @Override
   public int compareTo(final DartSDK o) {
      return Strings.compare(name, o.name);
   }

   public void configureEnvVars(final Map<String, Object> env) {
      env.merge("PATH", installRoot, //
         (oldValue, dartPath) -> dartPath + File.pathSeparator + oldValue //
      );
      env.put(ENV_DART_HOME, installRoot);
      env.put(ENV_PUB_CACHE, getPubCacheDir());
   }

   @Override
   public boolean equals(@Nullable final Object obj) {
      if (this == obj)
         return true;
      if (obj == null || getClass() != obj.getClass())
         return false;
      final var other = (DartSDK) obj;
      return Objects.equals(name, other.name) //
         && Objects.equals(installRoot, other.installRoot);
   }

   @JsonIgnore
   public Path getDartExecutable() {
      return installRoot.resolve(SystemUtils.IS_OS_WINDOWS ? "bin\\dart.exe" : "bin/dart");
   }

   @JsonIgnore
   public Processes.Builder getDartProcessBuilder(final boolean cleanEnv) {
      return Processes.builder(getDartExecutable()) //
         .withEnvironment(env -> {
            if (cleanEnv) {
               env.clear();
            }
            configureEnvVars(env);
         });
   }

   @JsonIgnore
   public Path getPubCacheDir() {
      final var pathFromEnv = System.getenv(ENV_PUB_CACHE);
      if (pathFromEnv != null) {
         final var p = Paths.get(pathFromEnv);
         if (Files.exists(p))
            return p.normalize().toAbsolutePath();
      }

      return (SystemUtils.IS_OS_WINDOWS //
         ? Paths.get(SystemUtils.getEnvironmentVariable("APPDATA", ""), "Pub", "Cache")
         : Paths.get(SystemUtils.getEnvironmentVariable("HOME", ""), ".pub-cache") //
      ).normalize().toAbsolutePath();
   }

   public Path getInstallRoot() {
      return installRoot;
   }

   public String getName() {
      return name;
   }

   @JsonIgnore
   public Path getStandardLibDir() {
      return installRoot.resolve("lib");
   }

   @Nullable
   @JsonIgnore
   public String getVersion() {
      try {
         final var version = Files.lines(installRoot.resolve("version")).findFirst().orElse("");
         return Strings.isBlank(version) ? null : version;
      } catch (final IOException ex) {
         Dart4EPlugin.log().error(ex);
         return null;
      }
   }

   public void installInteractiveShell(final IProgressMonitor monitor) throws CoreException {
      final var interativePackageIsInstalled = MutableRef.of(false);
      try {
         getDartProcessBuilder(false) //
            .withArgs("pub", "global", "list") //
            .withRedirectOutput(line -> {
               if (line.startsWith("interactive")) {
                  interativePackageIsInstalled.set(true);
               }
            }).start().waitForExit();
      } catch (final Exception ex) {
         Dart4EPlugin.log().error(ex);
      }

      if (!interativePackageIsInstalled.get()) {
         DartConsole.runWithConsole(monitor, "Installing interactive Dart Shell...", this, null, "pub", "global", "activate",
            "interactive");
      }
   }

   @Override
   public int hashCode() {
      return Objects.hash(name, installRoot);
   }

   /**
    * If <code>installRoot</code> actually points to a valid location containing the Dart compiler
    */
   @JsonIgnore
   public boolean isValid() {
      return isValidCached.get();
   }

   public String toShortString() {
      return name + " (" + installRoot + ")";
   }

   @Override
   public String toString() {
      return "DartSDK [name=" + name + ", installRoot=" + installRoot + "]";
   }
}
