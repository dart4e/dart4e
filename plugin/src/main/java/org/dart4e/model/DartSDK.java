/*
 * SPDX-FileCopyrightText: © The Dart4E authors
 * SPDX-FileContributor: Sebastian Thomschke
 * SPDX-License-Identifier: EPL-2.0
 * SPDX-ArtifactOfProjectHomePage: https://github.com/dart4e/dart4e
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
import java.util.regex.Pattern;

import org.dart4e.Dart4EPlugin;
import org.dart4e.console.DartConsole;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.annotation.Nullable;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import de.sebthom.eclipse.commons.ui.Consoles;
import net.sf.jstuff.core.Strings;
import net.sf.jstuff.core.SystemUtils;
import net.sf.jstuff.core.concurrent.Threads;
import net.sf.jstuff.core.functional.Suppliers;
import net.sf.jstuff.core.io.Processes;
import net.sf.jstuff.core.ref.MutableRef;
import net.sf.jstuff.core.validation.Args;

/**
 * @author Sebastian Thomschke
 */
@JsonAutoDetect( //
   fieldVisibility = JsonAutoDetect.Visibility.NONE, //
   setterVisibility = JsonAutoDetect.Visibility.NONE, //
   getterVisibility = JsonAutoDetect.Visibility.NONE, //
   isGetterVisibility = JsonAutoDetect.Visibility.NONE //
)
@JsonIgnoreProperties(ignoreUnknown = true)
public class DartSDK implements Comparable<DartSDK> {

   /**
    * Points to a folder containing the Dart SDK
    */
   public static final String ENV_DART_HOME = "DART_HOME";

   /**
    * Points to a folder containing downloaded dartlibs
    */
   public static final String ENV_PUB_CACHE = "PUB_CACHE";

   private static final Supplier<@Nullable DartSDK> SDK_FROM_PATH = Suppliers.memoize(() -> {
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
   }, (sdk, ageMS) -> ageMS > (sdk == null ? 10_000 : 60_000));

   /**
    * Tries to locate the Dart SDK via DART_HOME and PATH environment variables
    *
    * @return null if not found
    */
   public static @Nullable DartSDK fromPath() {
      return SDK_FROM_PATH.get();
   }

   private @JsonProperty String name;
   private @JsonProperty Path installRoot;

   private final Supplier<@Nullable String> getVersionCached = Suppliers.memoize(() -> {
      final var dartExe = getDartExecutable();
      if (!Files.isExecutable(dartExe))
         return null;

      final var processBuilder = Processes.builder(dartExe).withArg("--version");
      final var versionPattern = Pattern.compile("Dart SDK version:\\s*(\\S+)");
      try (var reader = new BufferedReader(new InputStreamReader(processBuilder.start().getStdOut()))) {
         String line;
         while ((line = reader.readLine()) != null) {
            // Example line: "Dart SDK version: 3.7.2 (stable) (Tue Mar 11 04:27:50 2025 -0700) on "windows_x64"
            final var matcher = versionPattern.matcher(line);
            if (matcher.find())
               return matcher.group(1); // Captured version (e.g., "3.7.2")
         }
      } catch (final IOException ex) {
         Dart4EPlugin.log().error(ex);
      }
      return null;
   }, (version, ageMS) -> ageMS > (version == null ? 10_000 : 60_000));

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

      if (Consoles.isAnsiColorsSupported()) {
         env.put("ANSICON", "1");
         env.put("TERM", "screen-256color");
      }
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

   public Path getDartExecutable() {
      return installRoot.resolve(SystemUtils.IS_OS_WINDOWS ? "bin\\dart.exe" : "bin/dart");
   }

   public Processes.Builder getDartProcessBuilder(final boolean cleanEnv) {
      return Processes.builder(getDartExecutable()) //
         .withArg("--disable-analytics") //
         .withEnvironment(env -> {
            if (cleanEnv) {
               env.clear();
            }
            configureEnvVars(env);
         });
   }

   public Path getPubCacheDir() {
      final var pathFromEnv = System.getenv(ENV_PUB_CACHE);
      if (pathFromEnv != null)
         return Paths.get(pathFromEnv).normalize().toAbsolutePath();

      if (SystemUtils.IS_OS_WINDOWS) {
         // https://dart.dev/resources/dart-3-migration#other-tools-changes
         final var version = getVersion();
         return (version != null && version.startsWith("2.") //
               ? Paths.get(SystemUtils.getEnvironmentVariable("APPDATA", ""), "Pub", "Cache") // Dart < 3
               : Paths.get(SystemUtils.getEnvironmentVariable("LOCALAPPDATA", ""), "Pub", "Cache") // Dart >= 3
         ).normalize().toAbsolutePath();
      }
      return Paths.get(SystemUtils.getEnvironmentVariable("HOME", ""), ".pub-cache").normalize().toAbsolutePath();
   }

   public Path getInstallRoot() {
      return installRoot;
   }

   public String getName() {
      return name;
   }

   public Path getStandardLibDir() {
      return installRoot.resolve("lib");
   }

   public @Nullable String getVersion() {
      return getVersionCached.get();
   }

   public void installInteractiveShell(final IProgressMonitor monitor) throws CoreException {
      final var interactivePackageIsInstalled = MutableRef.of(false);
      try {
         getDartProcessBuilder(false) //
            .withArgs("pub", "global", "list") //
            .withRedirectOutput(line -> {
               if (line.startsWith("interactive")) {
                  interactivePackageIsInstalled.set(true);
               }
            }).start().waitForExit();
      } catch (final InterruptedException ex) {
         Threads.handleInterruptedException(ex);
         Dart4EPlugin.log().error(ex);
      } catch (final Exception ex) {
         Dart4EPlugin.log().error(ex);
      }

      if (!interactivePackageIsInstalled.get()) {
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
   public boolean isValid() {
      return getVersion() != null;
   }

   public String toShortString() {
      return name + " (" + installRoot + ")";
   }

   @Override
   public String toString() {
      return "DartSDK [name=" + name + ", installRoot=" + installRoot + "]";
   }
}
