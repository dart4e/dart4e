/*
 * Copyright 2022 by the Dart4E authors.
 * SPDX-License-Identifier: EPL-2.0
 */
package org.dart4e.model;

import static net.sf.jstuff.core.validation.NullAnalysisHelper.*;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Path;
import java.util.Map;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.annotation.Nullable;

import net.sf.jstuff.core.validation.Args;

/**
 * @author Sebastian Thomschke
 */
public final class DartDependency implements Comparable<DartDependency> {

   public static DartDependency from(final DartSDK sdk, final String name, final Map<String, Object> pkgMeta,
      final IProgressMonitor monitor) throws IOException {
      Args.isDirectoryReadable("sdk.getPubCacheDir()", sdk.getPubCacheDir());

      /*
       * determine expected location on file system
       */
      monitor.setTaskName("Locating Dart library " + name + "...");

      final var source = (String) asNonNull(pkgMeta.get("source"));
      final var version = (String) asNonNull(pkgMeta.get("version"));
      @SuppressWarnings("unchecked")
      final var descr = (Map<String, String>) asNonNull(pkgMeta.get("description"));
      final var pubCacheDir = sdk.getPubCacheDir().resolve(source);
      final Path libLocation = switch (source) {
         case "hosted" -> {
            final var url = new URL(descr.get("url"));
            yield pubCacheDir.resolve(url.getHost()).resolve(name + "-" + version);
         }
         case "git" -> {
            final var resolvedRef = descr.get("resolved-ref");
            yield pubCacheDir.resolve(name + "-" + resolvedRef);
         }
         default -> throw new IllegalArgumentException("Unkown source " + source + " for package " + name);
      };

      final var dependencyType = (String) asNonNull(pkgMeta.get("dependency"));
      return new DartDependency(libLocation, name, version, dependencyType.contains("dev"), dependencyType.contains("transitive"));
   }

   public final boolean isTransitiveDependency;
   public final boolean isDevDependency;
   public final Path location;
   public final String name;
   public final String version;

   private DartDependency(final Path directory, final String name, final String version, final boolean isDevDependency,
      final boolean isTransitiveDependency) {
      location = directory;
      this.name = name;
      this.version = version;
      this.isDevDependency = isDevDependency;
      this.isTransitiveDependency = isTransitiveDependency;
   }

   @Override
   public int compareTo(final DartDependency obj) {
      return name.compareTo(obj.name);
   }

   @Override
   public boolean equals(@Nullable final Object obj) {
      if (this == obj)
         return true;
      if (obj == null || getClass() != obj.getClass())
         return false;
      final var other = (DartDependency) obj;
      return location.equals(other.location);
   }

   @Override
   public int hashCode() {
      return location.hashCode();
   }

   @Override
   public String toString() {
      return ToStringBuilder.reflectionToString(this, ToStringStyle.MULTI_LINE_STYLE);
   }
}
