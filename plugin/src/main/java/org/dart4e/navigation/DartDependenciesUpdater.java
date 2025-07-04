/*
 * SPDX-FileCopyrightText: © The Dart4E authors
 * SPDX-FileContributor: Sebastian Thomschke
 * SPDX-License-Identifier: EPL-2.0
 * SPDX-ArtifactOfProjectHomePage: https://github.com/dart4e/dart4e
 */
package org.dart4e.navigation;

import static net.sf.jstuff.core.validation.NullAnalysisHelper.*;

import java.util.HashSet;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.dart4e.Constants;
import org.dart4e.Dart4EPlugin;
import org.dart4e.model.DartDependency;
import org.dart4e.model.buildsystem.BuildFile;
import org.dart4e.prefs.DartProjectPreference;
import org.dart4e.project.DartProjectNature;
import org.dart4e.util.AbstractResourcesChangedListener;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IResourceDeltaVisitor;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jdt.annotation.Nullable;

import de.sebthom.eclipse.commons.resources.Projects;

/**
 * @author Sebastian Thomschke
 */
public final class DartDependenciesUpdater extends AbstractResourcesChangedListener {

   public static final String STDLIB_MAGIC_FOLDER_NAME = "!!!dartstdlib";
   public static final String DEPS_MAGIC_FOLDER_NAME = "!!dartdeps";

   public static final DartDependenciesUpdater INSTANCE = new DartDependenciesUpdater();

   private DartDependenciesUpdater() {
   }

   public void onProjectConfigChanged(final IProject project) {
      if (!DartProjectNature.hasNature(project))
         return; // ignore

      final var job = new Job("Updating 'Dart Dependencies' list of project '" + project.getName() + "'...") {
         @Override
         protected IStatus run(final IProgressMonitor monitor) {
            return updateProjectDependencies(project, monitor);
         }
      };
      job.setRule(project); // synchronize job execution on project
      job.setPriority(Job.BUILD);
      job.schedule();
   }

   public void onProjectsConfigChanged(final List<IProject> projects) {
      for (final var project : projects) {
         onProjectConfigChanged(project);
      }
   }

   public void removeDependenciesFolder(final IProject project, final @Nullable IProgressMonitor monitor) throws CoreException {
      for (final var folderName : new String[] {STDLIB_MAGIC_FOLDER_NAME, DEPS_MAGIC_FOLDER_NAME}) {
         final var folder = project.getFolder(folderName);
         if (folder.exists() && (folder.isVirtual() || folder.isLinked())) {
            folder.delete(false, monitor);
         }
      }
   }

   @Override
   public void resourceChanged(final IResourceChangeEvent event) {
      if (event.getType() != IResourceChangeEvent.POST_CHANGE)
         return;

      final var rootDelta = event.getDelta();
      final var changedProjects = new HashSet<IProject>();
      final IResourceDeltaVisitor visitor = delta -> {
         final var resource = delta.getResource();
         final var project = resource.getProject();
         if (project == null) // e.g. Workspace Root
            return true; // check children

         if (!Projects.hasNature(project, DartProjectNature.NATURE_ID))
            return false; // ignore children

         // if the project was just opened update dependencies
         if (resource == project && (delta.getFlags() & IResourceDelta.OPEN) != 0) {
            changedProjects.add(project);
            return false; // no need to check children
         }

         if ((delta.getFlags() & IResourceDelta.CONTENT) == 0)
            return true; // check children

         switch (delta.getKind()) {
            case IResourceDelta.ADDED, IResourceDelta.CHANGED, IResourceDelta.REMOVED:
               break;
            default:
               return true; // check children
         }

         if (resource.getType() != IResource.FILE)
            return true; // check children

         switch (resource.getName()) {
            case Constants.PUBSPEC_YAML_FILENAME, Constants.PUBSPEC_LOCK_FILENAME -> changedProjects.add(project);
         }
         return false; // no need to check children
      };

      try {
         rootDelta.accept(visitor);
      } catch (final CoreException ex) {
         Dart4EPlugin.log().error(ex);
      }

      for (final IProject p : changedProjects) {
         onProjectConfigChanged(p);
      }
   }

   /**
    * Updates the dependency tree according `pubspec.lock`
    */
   private IStatus updateProjectDependencies(final IProject project, final IProgressMonitor monitor) {
      try {
         final var prefs = DartProjectPreference.get(project);

         final var sdk = prefs.getEffectiveDartSDK();
         if (sdk == null)
            return Dart4EPlugin.status().createError("Cannot update 'Dart Dependencies' list. Dart SDK cannot be found!");

         /*
          * create/update dart stdlib top-level virtual folder
          */
         final var stdLibFolder = project.getFolder(STDLIB_MAGIC_FOLDER_NAME);
         if (stdLibFolder.exists()) {
            if (!stdLibFolder.isLinked())
               return Dart4EPlugin.status().createError("Cannot update Dart standard library folder. Physical folder with name '"
                     + STDLIB_MAGIC_FOLDER_NAME + "' exists!");
            if (!asNonNull(stdLibFolder.getLocation()).toFile().toPath().equals(sdk.getStandardLibDir())) {
               stdLibFolder.createLink(sdk.getStandardLibDir().toUri(), IResource.REPLACE, monitor);
            }
         } else {
            stdLibFolder.createLink(sdk.getStandardLibDir().toUri(), IResource.REPLACE, monitor);
         }
         if (!stdLibFolder.isDerived()) {
            stdLibFolder.setDerived(true, monitor);
         }

         /*
          * create/update "Dart Dependencies" top-level virtual folder
          */
         final var depsFolder = project.getFolder(DEPS_MAGIC_FOLDER_NAME);
         final var buildFile = BuildFile.of(project);

         // if no build file exists remove the dependencies folder
         if (buildFile == null) {
            if (depsFolder.exists() && depsFolder.isVirtual()) {
               depsFolder.delete(true, monitor);
            }
            return Status.OK_STATUS;
         }

         if (depsFolder.exists()) {
            if (!depsFolder.isVirtual())
               return Dart4EPlugin.status().createError("Cannot update 'Dart Dependencies' list. Physical folder with name '"
                     + DEPS_MAGIC_FOLDER_NAME + "' exists!");
         } else {
            depsFolder.create(IResource.VIRTUAL, true, monitor);
         }
         if (!depsFolder.isDerived()) {
            depsFolder.setDerived(true, monitor);
         }

         final var depsToCheck = buildFile //
            .getDependencies(monitor).stream() //
            .collect(Collectors.toMap(d -> d.name //
                  + ("0.0.0".equals(d.version) ? "" : " [" + d.version + "]") //
                  + (d.isDevDependency ? " (dev)" : ""), //
               Function.identity()));

         for (final IResource folder : depsFolder.members()) {
            if (depsToCheck.containsKey(folder.getName())) {
               final DartDependency dep = asNonNullUnsafe(depsToCheck.get(folder.getName()));
               final IPath rawLoc = folder.getRawLocation();
               if (rawLoc != null && dep.location.equals(rawLoc.toFile().toPath())) {
                  depsToCheck.remove(folder.getName());
               } else {
                  folder.delete(true, monitor); // delete broken folder link
               }
            } else {
               folder.delete(true, monitor); // delete folder link to (now) unused dependency
            }
         }

         for (final var dep : depsToCheck.entrySet()) {
            final var folder = depsFolder.getFolder(dep.getKey());
            folder.createLink(dep.getValue().location.toUri(), IResource.BACKGROUND_REFRESH, monitor);
         }
         return Status.OK_STATUS;
      } catch (final Exception ex) {
         return Dart4EPlugin.status().createError(ex, "Failed to update 'Dart Dependencies' list.");
      }
   }
}
