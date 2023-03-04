/*
 * SPDX-FileCopyrightText: © The Dart4E authors
 * SPDX-FileContributor: Sebastian Thomschke
 * SPDX-License-Identifier: EPL-2.0
 * SPDX-ArtifactOfProjectHomePage: https://github.com/dart4e/dart4e
 */
package org.dart4e.model.buildsystem;

import static net.sf.jstuff.core.validation.NullAnalysisHelper.asNonNullUnsafe;

import java.util.Set;

import org.dart4e.model.DartDependency;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.annotation.Nullable;

import net.sf.jstuff.core.Strings;

/**
 * @author Sebastian Thomschke
 */
public abstract class BuildFile {

   public static @Nullable BuildFile of(final IProject project) {
      return BuildSystem.guessBuildSystemOfProject(project).findBuildFile(project);
   }

   public final BuildSystem buildSystem;
   public final IFile location;

   protected BuildFile(final BuildSystem buildSystem, final IFile location) {
      this.buildSystem = buildSystem;
      this.location = location;
   }

   public boolean exists() {
      return location.exists();
   }

   public abstract Set<DartDependency> getDependencies(IProgressMonitor monitor);

   public IProject getProject() {
      return asNonNullUnsafe(location.getProject());
   }

   public String getProjectRelativePath() {
      return location.getProjectRelativePath().toString();
   }

   public abstract Set<IPath> getSourcePaths();

   @Override
   public String toString() {
      return Strings.toString(this, //
         "project", getProject().getName(), //
         "path", getProjectRelativePath() //
      );
   }
}
