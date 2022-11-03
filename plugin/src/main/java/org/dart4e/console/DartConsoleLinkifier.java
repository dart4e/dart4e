/*
 * Copyright 2022 by the Dart4E authors.
 * SPDX-License-Identifier: EPL-2.0
 */
package org.dart4e.console;

import java.net.URI;
import java.util.regex.Pattern;

import org.dart4e.Dart4EPlugin;
import org.dart4e.editor.DartEditor;
import org.dart4e.launch.program.ProgramLaunchConfigLauncher;
import org.dart4e.navigation.DartDependenciesUpdater;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.debug.ui.console.FileLink;
import org.eclipse.debug.ui.console.IConsole;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.ui.console.IPatternMatchListenerDelegate;
import org.eclipse.ui.console.PatternMatchEvent;
import org.eclipse.ui.console.TextConsole;

import de.sebthom.eclipse.commons.resources.Projects;
import net.sf.jstuff.core.Strings;

/**
 * @author Sebastian Thomschke
 */
public final class DartConsoleLinkifier implements IPatternMatchListenerDelegate {

   private @Nullable TextConsole console;
   private static final Pattern SOURCE_LOCATION_PATTERN = Pattern.compile(
      "(package:|dart:|file:\\/\\/\\/)((\\w|[-:\\/])+\\.dart)\\:(\\d+)\\:(\\d+)");

   @Override
   public void connect(final TextConsole console) {
      this.console = console;
   }

   @Override
   public void disconnect() {
      console = null;
   }

   private @Nullable IProject getProject() {
      if (console instanceof final DartConsole dartConsole)
         return dartConsole.project;
      if (console instanceof final IConsole debugConsole) {
         final var projectName = debugConsole.getProcess().getAttribute(ProgramLaunchConfigLauncher.PROCESS_ATTRIBUTE_PROJECT_NAME);
         return Projects.getProject(projectName);
      }
      return null;
   }

   @Override
   public void matchFound(final PatternMatchEvent event) {
      final var console = this.console;
      if (console == null)
         return;

      final var project = getProject();
      if (project == null)
         return;

      final var offset = event.getOffset();
      final var length = event.getLength();
      final var doc = console.getDocument();
      try {
         // (dart:io/file_impl.dart:356:9)
         // (package:hotreloader/main.dart:5:14)
         // (file:///D:/workspaces/projects/dart-hotreloader/bin/main.dart:5:14)
         final var sourceLoc = doc.get(offset, length); // e.g. (dart:io/file_impl.dart:356:9) or (package:hotreloader/main.dart:5:14)

         final var m = SOURCE_LOCATION_PATTERN.matcher(sourceLoc);
         m.find();
         final var sourceFilePath = m.group(2);
         IFile sourceFile = null;
         switch (m.group(1)) {
            case "dart:":
               sourceFile = project.getFolder(DartDependenciesUpdater.STDLIB_MAGIC_FOLDER_NAME).getFile(sourceFilePath);
               break;
            case "package:":
               sourceFile = project.getFolder(DartDependenciesUpdater.DEPS_MAGIC_FOLDER_NAME).getFile(sourceFilePath);
               if (!sourceFile.exists()) {
                  sourceFile = project.getFile("lib/" + Strings.substringAfter(sourceFilePath, "/"));
               }
               break;
            case "file:///":
               final var files = ResourcesPlugin.getWorkspace().getRoot().findFilesForLocationURI(new URI("file:///" + sourceFilePath));
               if (files.length > 0) {
                  for (final var file : files) {
                     if (project.equals(file.getProject())) {
                        sourceFile = file;
                        break;
                     }
                  }
                  if (sourceFile == null) {
                     sourceFile = files[0];
                  }
               }
               break;
         }
         if (sourceFile != null && sourceFile.exists()) {
            final int lineNumber = Integer.parseInt(m.group(4));
            final var link = new FileLink(sourceFile, DartEditor.ID, -1, -1, lineNumber);
            console.addHyperlink(link, offset + 1, length - 2);
         }
      } catch (final Exception ex) {
         Dart4EPlugin.log().debug(ex);
      }
   }
}
