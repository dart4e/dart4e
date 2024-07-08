/*
 * SPDX-FileCopyrightText: © The Dart4E authors
 * SPDX-FileContributor: Sebastian Thomschke
 * SPDX-License-Identifier: EPL-2.0
 * SPDX-ArtifactOfProjectHomePage: https://github.com/dart4e/dart4e
 */
package org.dart4e.launch;

import java.time.Duration;
import java.util.HashSet;

import org.dart4e.Dart4EPlugin;
import org.dart4e.project.DartProjectNature;
import org.dart4e.util.AbstractResourcesChangedListener;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IResourceDeltaVisitor;
import org.eclipse.core.runtime.CoreException;

import de.sebthom.eclipse.commons.resources.Projects;
import net.sf.jstuff.core.event.AsyncEventDispatcher;
import net.sf.jstuff.core.event.ThrottlingEventDispatcher;

/**
 * @author Sebastian Thomschke
 */
public final class DartHotReloadListener extends AbstractResourcesChangedListener {

   public static final DartHotReloadListener INSTANCE = new DartHotReloadListener();

   private final ThrottlingEventDispatcher<IProject> hotReloadEventDispatcher = ThrottlingEventDispatcher //
      .builder(IProject.class, Duration.ofMillis(1_000)) //
      .delegate(new AsyncEventDispatcher<>()) //
      .build();

   private DartHotReloadListener() {
      hotReloadEventDispatcher.subscribe(this::hotReload);
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

         // ignore children

         // if the project was just opened ignore it
         if (!Projects.hasNature(project, DartProjectNature.NATURE_ID) //
               || resource == project && (delta.getFlags() & IResourceDelta.OPEN) != 0)
            return false; // no need to check children

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

         if (resource.getName().endsWith(".dart")) {
            changedProjects.add(project);
         }
         return false; // no need to check children
      };

      try {
         rootDelta.accept(visitor);
      } catch (final CoreException ex) {
         Dart4EPlugin.log().error(ex);
      }

      changedProjects.forEach(hotReloadEventDispatcher::fire);
   }

   private void hotReload(final IProject project) {
      DartDebugTarget.ACTIVE_TARGETS.stream() //
         .filter(target -> //
         !target.isDisconnected() && project.equals(target.getProject())) //
         .findFirst() //
         .ifPresent(target -> {
            try {
               Dart4EPlugin.log().debug("Hot reloading [{0}]...", target.getName());
            } catch (final Exception ex) {
               Dart4EPlugin.log().error(ex);
            }
            target.getDebugAPI().hotReload();
         });
   }
}
