/*
 * SPDX-FileCopyrightText: © The Dart4E authors
 * SPDX-FileContributor: Sebastian Thomschke
 * SPDX-License-Identifier: EPL-2.0
 * SPDX-ArtifactOfProjectHomePage: https://github.com/dart4e/dart4e
 */
package org.dart4e.langserver;

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

import org.dart4e.prefs.DartWorkspacePreference;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.lsp4e.LSPEclipseUtils;
import org.eclipse.lsp4e.LanguageServers;
import org.eclipse.lsp4e.LanguageServersRegistry;
import org.eclipse.lsp4e.LanguageServersRegistry.LanguageServerDefinition;
import org.eclipse.lsp4j.DidChangeConfigurationParams;
import org.eclipse.lsp4j.services.LanguageServer;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IEditorReference;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;

/**
 * Listens for workspace preference changes and notifies all running Dart
 * language servers via {@code workspace/didChangeConfiguration}.
 *
 * @author Sebastian Thomschke
 */
public final class DartLSPClientPreferenceChangeListener implements IPropertyChangeListener {

   public static final DartLSPClientPreferenceChangeListener INSTANCE = new DartLSPClientPreferenceChangeListener();

   private DartLSPClientPreferenceChangeListener() {
   }

   @Override
   public void propertyChange(final PropertyChangeEvent event) {
      final var property = event.getProperty();
      if (!property.startsWith(DartWorkspacePreference.PREFKEY_LSP_CLIENT_PREFIX))
         return;

      final var registry = LanguageServersRegistry.getInstance();
      final LanguageServerDefinition definition = registry.getDefinition(DartLangServerLauncher.DART_LANGUAGE_SERVER_ID);
      if (definition == null)
         return;

      /*
       * Dart's WorkspaceDidChangeConfigurationMessageHandler ignores params.settings and always re-requests client
       * configuration via workspace/configuration, so it is safe to send an empty map here.
       */
      final var params = new DidChangeConfigurationParams(Collections.emptyMap());
      final Set<LanguageServer> notifiedServers = ConcurrentHashMap.newKeySet();

      // 1) Workspace-wide servers (workspace-folder aware or singletons)
      LanguageServers.forProject(null) //
         .withPreferredServer(definition) //
         .excludeInactive() //
         .collectAll((wrapper, server) -> {
            if (definition.equals(wrapper.serverDefinition) && notifiedServers.add(server)) {
               server.getWorkspaceService().didChangeConfiguration(params);
            }
            return CompletableFuture.completedFuture(null);
         });

      // 2) Per-project servers
      for (final IProject project : ResourcesPlugin.getWorkspace().getRoot().getProjects()) {
         if (!project.isOpen()) {
            continue;
         }

         LanguageServers.forProject(project) //
            .withPreferredServer(definition) //
            .excludeInactive() //
            .collectAll((wrapper, server) -> {
               if (definition.equals(wrapper.serverDefinition) && notifiedServers.add(server)) {
                  server.getWorkspaceService().didChangeConfiguration(params);
               }
               return CompletableFuture.completedFuture(null);
            });
      }

      // 3) Per-document servers (for external files or unsaved documents)
      for (final IWorkbenchWindow window : PlatformUI.getWorkbench().getWorkbenchWindows()) {
         for (final IWorkbenchPage page : window.getPages()) {
            for (final IEditorReference ref : page.getEditorReferences()) {
               final IEditorPart editor = ref.getEditor(false);
               if (editor == null) {
                  continue;
               }

               final IDocument document = LSPEclipseUtils.getDocument(editor.getEditorInput());
               if (document == null) {
                  continue;
               }

               LanguageServers.forDocument(document) //
                  .withPreferredServer(definition) //
                  .collectAll((wrapper, server) -> {
                     if (definition.equals(wrapper.serverDefinition) && notifiedServers.add(server)) {
                        server.getWorkspaceService().didChangeConfiguration(params);
                     }
                     return CompletableFuture.completedFuture(null);
                  });
            }
         }
      }
   }
}
