/*
 * SPDX-FileCopyrightText: © The Dart4E authors
 * SPDX-FileContributor: Sebastian Thomschke
 * SPDX-License-Identifier: EPL-2.0
 * SPDX-ArtifactOfProjectHomePage: https://github.com/dart4e/dart4e
 */
package org.dart4e.flutter.model;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import org.apache.commons.io.input.CharSequenceInputStream;
import org.dart4e.Dart4EPlugin;
import org.dart4e.model.DartSDK;
import org.dart4e.util.io.JSON;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jdt.annotation.Nullable;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import net.sf.jstuff.core.Strings;
import net.sf.jstuff.core.SystemUtils;
import net.sf.jstuff.core.concurrent.Threads;
import net.sf.jstuff.core.functional.Suppliers;
import net.sf.jstuff.core.io.Processes;
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
public final class FlutterSDK implements Comparable<FlutterSDK> {

   /**
    * Points to a folder containing the Flutter SDK
    */
   public static final String ENV_FLUTTER_ROOT = "FLUTTER_ROOT";

   private static final Supplier<@Nullable FlutterSDK> SDK_FROM_PATH = Suppliers.memoize(() -> {
      final var flutterRoot = SystemUtils.getEnvironmentVariable(ENV_FLUTTER_ROOT, "");
      if (Strings.isBlank(flutterRoot))
         return null;

      var sdk = new FlutterSDK(Paths.get(flutterRoot));
      if (sdk.isValid())
         return sdk;

      final var flutterExe = SystemUtils.findExecutable("flutter", true);
      if (flutterExe == null)
         return null;

      sdk = new FlutterSDK(flutterExe.getParent().getParent());
      return sdk.isValid() ? sdk : null;
   }, (sdk, ageMS) -> ageMS > (sdk == null ? 15_000 : 60_000));

   /**
    * Tries to locate the Flutter SDK via FLUTTER_ROOT and PATH environment variables
    *
    * @return null if not found
    */
   @Nullable
   public static FlutterSDK fromPath() {
      return SDK_FROM_PATH.get();
   }

   private @JsonProperty String name;
   private @JsonProperty Path installRoot;

   private @Nullable DartSDK dartSDK;

   private final Supplier<Boolean> isValidCached = Suppliers.memoize(() -> {
      final var flutterExe = getFlutterExecutable();
      if (!Files.isExecutable(flutterExe))
         return false;

      final var processBuilder = Processes.builder(flutterExe).withArg("--version");
      /* outputs something like:
       *   Flutter 3.16.5 • channel stable • https://github.com/flutter/flutter.git
       *   Framework • revision 78666c8dc5 (5 weeks ago) • 2023-12-19 16:14:14 -0800
       *   Engine • revision 3f3e560236
       *   Tools • Dart 3.2.3 • DevTools 2.28.4
       */
      try (var reader = new BufferedReader(new InputStreamReader(processBuilder.start().getStdOut()))) {
         final String line = reader.readLine();
         if (line != null && line.contains("Flutter"))
            return true;
      } catch (final IOException ex) {
         // ignore
      }
      return false;
   }, (exeIsValid, ageMS) -> ageMS > (exeIsValid ? 60_000 : 15_000));

   @SuppressWarnings("unused")
   private FlutterSDK() {
      // for Jackson

      // only to satisfy annotation-based null-safety analysis:
      name = "";
      installRoot = Path.of("");
   }

   public FlutterSDK(final Path installRoot) {
      this.installRoot = installRoot.normalize().toAbsolutePath();
      name = "flutter-" + getVersion();
   }

   public FlutterSDK(final String name, final Path installRoot) {
      Args.notBlank("name", name);

      this.name = name;
      this.installRoot = installRoot.normalize().toAbsolutePath();
   }

   @Override
   public int compareTo(final FlutterSDK o) {
      return Strings.compare(name, o.name);
   }

   public void configureEnvVars(final Map<String, Object> env) {
      configureEnvVars(env, getDartSDK());
   }

   public void configureEnvVars(final Map<String, Object> env, final DartSDK alternativeDartSDK) {
      env.merge("PATH", installRoot, //
         (oldValue, dartPath) -> dartPath + File.pathSeparator + oldValue //
      );
      alternativeDartSDK.configureEnvVars(env);
      env.put(ENV_FLUTTER_ROOT, installRoot);
      if (System.getenv(DartSDK.ENV_PUB_CACHE) == null) {
         env.put(DartSDK.ENV_PUB_CACHE, installRoot.resolve(".pub-cache"));
      }
   }

   @Override
   public boolean equals(@Nullable final Object obj) {
      if (this == obj)
         return true;
      if (obj == null || getClass() != obj.getClass())
         return false;
      final var other = (FlutterSDK) obj;
      return Objects.equals(name, other.name) //
         && Objects.equals(installRoot, other.installRoot);
   }

   public DartSDK getDartSDK() {
      var dartSDK = this.dartSDK;
      if (dartSDK == null) {
         dartSDK = this.dartSDK = new DartSDK(installRoot.resolve("bin/cache/dart-sdk"));
      }
      return dartSDK;
   }

   public CompletionStage<List<Device>> getSupportedDevices() {
      final var future = new CompletableFuture<List<Device>>();

      final var job = new Job("Determining Flutter devices...") {
         @Override
         protected IStatus run(final IProgressMonitor monitor) {
            final var sb = new StringBuilder();
            try {
               getFlutterProcessBuilder(false).withArgs("devices", "--machine") //
                  .withRedirectOutput(sb) //
                  .start() //
                  .waitForExit(10, TimeUnit.SECONDS);

               try (var in = CharSequenceInputStream.builder().setCharSequence(sb).setCharset(StandardCharsets.UTF_8).get()) {
                  future.complete( //
                     JSON.deserializeList(in, Device.class) //
                        .stream() //
                        .filter(device -> device.isSupported) //
                        .toList() //
                  );
               }
               return Status.OK_STATUS;
            } catch (final IOException ex) {
               Dart4EPlugin.log().error(ex);
               future.completeExceptionally(ex);
            } catch (final InterruptedException ex) {
               Threads.handleInterruptedException(ex);
               future.completeExceptionally(ex);
            }
            return Status.CANCEL_STATUS;
         }
      };
      job.setPriority(Job.DECORATE);
      job.schedule();
      return future;
   }

   public Path getFlutterExecutable() {
      return installRoot.resolve(SystemUtils.IS_OS_WINDOWS ? "bin\\flutter.bat" : "bin/flutter");
   }

   public Processes.Builder getFlutterProcessBuilder(final boolean cleanEnv) {
      return Processes.builder(getFlutterExecutable()) //
         .withArg("--suppress-analytics") //
         .withEnvironment(env -> {
            if (cleanEnv) {
               env.clear();
            }
            configureEnvVars(env);
         });
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

   @Nullable
   public String getVersion() {
      try (var lines = Files.lines(installRoot.resolve("version"))) {
         final var version = lines.findFirst().orElse("");
         return Strings.isBlank(version) ? null : version;
      } catch (final IOException ex) {
         Dart4EPlugin.log().error(ex);
         return null;
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
