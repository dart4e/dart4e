/*
 * Copyright 2022 by the Dart4E authors.
 * SPDX-License-Identifier: EPL-2.0
 */
package org.dart4e.model;

import java.nio.file.Path;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.eclipse.jdt.annotation.Nullable;

/**
 * @author Sebastian Thomschke
 */
public final class DartDependency implements Comparable<DartDependency> {

   public final boolean isTransitiveDependency;
   public final boolean isDevDependency;
   public final Path location;
   public final String name;
   public final String version;

   public DartDependency(final Path directory, final String name, final String version, final boolean isDevDependency,
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
