/*
 * Copyright 2022 by the Dart4E authors.
 * SPDX-License-Identifier: EPL-2.0
 */
package org.dart4e.navigation;

import static net.sf.jstuff.core.validation.NullAnalysisHelper.asNonNullUnsafe;

import org.dart4e.Constants;
import org.dart4e.Dart4EPlugin;
import org.dart4e.model.buildsystem.BuildFile;
import org.dart4e.prefs.DartProjectPreference;
import org.dart4e.project.DartProjectNature;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.jface.viewers.BaseLabelProvider;
import org.eclipse.jface.viewers.ILabelDecorator;
import org.eclipse.jface.viewers.LabelProviderChangedEvent;
import org.eclipse.swt.graphics.Image;

import de.sebthom.eclipse.commons.ui.UI;

/**
 * @author Sebastian Thomschke
 */
public class DartResourcesDecorator extends BaseLabelProvider implements ILabelDecorator {

   public static DartResourcesDecorator getInstance() {
      return (DartResourcesDecorator) asNonNullUnsafe(UI.getWorkbench().getDecoratorManager() //
         .getLabelDecorator(DartResourcesDecorator.class.getName()));
   }

   @Override
   public @Nullable Image decorateImage(final @Nullable Image image, final @Nullable Object element) {
      if (element == null)
         return null;
      final var res = (IResource) element;
      var project = res.getProject();

      if (!DartProjectNature.hasNature(project))
         return image;

      project = asNonNullUnsafe(project);

      if (res.isVirtual()) {
         if (res instanceof IFolder && res.getName().equals(DartDependenciesUpdater.DEPS_MAGIC_FOLDER_NAME))
            return Dart4EPlugin.get().getSharedImage(Constants.IMAGE_DART_DEPENDENCIES);
         return image;
      }

      if (res.isLinked()) {
         if (res instanceof IFolder && res.getName().equals(DartDependenciesUpdater.STDLIB_MAGIC_FOLDER_NAME))
            return Dart4EPlugin.get().getSharedImage(Constants.IMAGE_DART_DEPENDENCIES);
         return image;
      }

      if (res instanceof final IFolder folder) {
         final var buildFile = BuildFile.of(project);
         if (buildFile == null)
            return image;

         final var folderPath = asNonNullUnsafe(folder.getLocation());
         final var projectPath = asNonNullUnsafe(project.getLocation());
         try {
            for (final var sourcePath : buildFile.getSourcePaths()) {
               final var sourceFolder = projectPath.append(sourcePath);
               if (folderPath.equals(sourceFolder))
                  return Dart4EPlugin.get().getSharedImage(Constants.IMAGE_DART_SOURCE_FOLDER);
               if (sourceFolder.isPrefixOf(folderPath))
                  return Dart4EPlugin.get().getSharedImage(Constants.IMAGE_DART_SOURCE_PACKAGE);
            }
         } catch (final Exception ex) {
            Dart4EPlugin.log().error(ex);
         }
      }

      return image;
   }

   @Override
   public @Nullable String decorateText(final @Nullable String text, final @Nullable Object element) {
      if (element instanceof final IFolder folder) {
         final var project = folder.getProject();

         if (DartProjectNature.hasNature(project)) {
            if (folder.isLinked() //
               && folder.getName().equals(DartDependenciesUpdater.STDLIB_MAGIC_FOLDER_NAME) //
            ) {
               final var prefs = DartProjectPreference.get(asNonNullUnsafe(project));
               final var dartSDK = prefs.getEffectiveDartSDK();
               return "Dart Standard Library" + (dartSDK == null ? "" : " [" + dartSDK.getVersion() + "]");
            }

            if (folder.isVirtual() //
               && folder.getName().equals(DartDependenciesUpdater.DEPS_MAGIC_FOLDER_NAME) //
            ) {
               int depCount = 0;
               try {
                  depCount = folder.members().length;
               } catch (final CoreException ex) {
                  Dart4EPlugin.log().error(ex);
               }
               return "Dart Dependencies" + (depCount == 0 ? "" : " (" + depCount + ")");
            }
         }
      }
      return text;
   }

   public void refreshElements(final IResource... res) {
      if (res.length == 0)
         return;

      UI.run(() -> fireLabelProviderChanged(new LabelProviderChangedEvent(this, res)));
   }
}
