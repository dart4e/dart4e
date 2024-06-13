/*
 * SPDX-FileCopyrightText: © The Dart4E authors
 * SPDX-FileContributor: Sebastian Thomschke
 * SPDX-License-Identifier: EPL-2.0
 * SPDX-ArtifactOfProjectHomePage: https://github.com/dart4e/dart4e
 */
package org.dart4e.project;

import static net.sf.jstuff.core.validation.NullAnalysisHelper.lateNonNull;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;

import org.dart4e.Dart4EPlugin;
import org.dart4e.localization.Messages;
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.osgi.util.NLS;
import org.eclipse.ui.INewWizard;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.ide.IDE;

import de.sebthom.eclipse.commons.ui.DialogPages;
import de.sebthom.eclipse.commons.ui.UI;
import net.sf.jstuff.core.concurrent.Threads;

/**
 * @author Sebastian Thomschke
 */
public final class NewDartFileWizard extends Wizard implements INewWizard {

   private NewDartFilePage dartFilePage = lateNonNull();
   private IStructuredSelection selection = lateNonNull();

   @Override
   public void addPages() {
      dartFilePage = new NewDartFilePage(NewDartFilePage.class.getSimpleName(), selection);
      dartFilePage.setTitle(Messages.Label_Dart_File);
      dartFilePage.setDescription(Messages.NewDartFile_Descr);
      dartFilePage.setFileExtension("dart");
      addPage(dartFilePage);
   }

   @Override
   public void init(final IWorkbench workbench, final IStructuredSelection selection) {
      this.selection = selection;
      setWindowTitle(Messages.NewDartFile);
      setNeedsProgressMonitor(true);
   }

   @Override
   public boolean performFinish() {
      final var folderName = dartFilePage.getContainerFullPath().toOSString();
      final var fileName = dartFilePage.getFileName();

      try {
         getContainer().run(true, false, progress -> {
            try {
               progress.beginTask(NLS.bind(Messages.NewDartFile_Creating, fileName), 2);

               final var folder = ResourcesPlugin.getWorkspace().getRoot().findMember(new Path(folderName));
               if (!folder.exists() || !(folder instanceof IContainer))
                  throw new CoreException(Dart4EPlugin.status().createError(Messages.NewDartFile_DirectoryDoesNotExist, folderName));

               final var newDartFile = ((IContainer) folder).getFile(new Path(fileName));
               final var newDartFileContent = "";
               try (InputStream stream = new ByteArrayInputStream(newDartFileContent.getBytes())) {
                  if (newDartFile.exists()) {
                     newDartFile.setContents(stream, true, true, progress);
                  } else {
                     newDartFile.create(stream, true, progress);
                  }
               } catch (final IOException ex) {
                  Dart4EPlugin.log().error(ex, ex.getMessage());
               }
               progress.worked(1);
               progress.setTaskName(Messages.NewDartFile_OpeningInEditor);
               UI.run(() -> {
                  final var page = UI.getActiveWorkbenchPage();
                  try {
                     IDE.openEditor(page, newDartFile, true);
                  } catch (final PartInitException ex) {
                     Dart4EPlugin.log().error(ex, ex.getMessage());
                  }
               });
               progress.worked(1);
            } catch (final CoreException ex) {
               throw new InvocationTargetException(ex);
            } finally {
               progress.done();
            }
         });
      } catch (final InterruptedException ex) {
         Threads.handleInterruptedException(ex);
         return false;
      } catch (final InvocationTargetException ex) {
         Dart4EPlugin.log().error(ex.getTargetException(), ex.getMessage());
         DialogPages.setMessage(dartFilePage, Dart4EPlugin.status().createError(ex.getTargetException()));
         return false;
      }
      return true;
   }
}
