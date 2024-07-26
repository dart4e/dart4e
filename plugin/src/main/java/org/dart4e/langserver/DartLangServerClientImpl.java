/*
 * SPDX-FileCopyrightText: © The Dart4E authors
 * SPDX-FileContributor: Sebastian Thomschke
 * SPDX-License-Identifier: EPL-2.0
 * SPDX-ArtifactOfProjectHomePage: https://github.com/dart4e/dart4e
 */
package org.dart4e.langserver;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.dart4e.Constants;
import org.dart4e.prefs.DartProjectPreference;
import org.dart4e.prefs.DartWorkspacePreference;
import org.dart4e.util.TreeBuilder;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.lsp4e.LSPEclipseUtils;
import org.eclipse.lsp4e.LanguageClientImpl;
import org.eclipse.lsp4j.ConfigurationParams;
import org.eclipse.lsp4j.RegistrationParams;
import org.eclipse.lsp4j.WorkspaceFolder;

import de.sebthom.eclipse.commons.resources.Projects;

/**
 * https://github.com/dart-lang/sdk/blob/main/pkg/analysis_server/tool/lsp_spec/README.md#custom-fields-methods-and-notifications
 *
 * @author Sebastian Thomschke
 */
@SuppressWarnings("restriction") // https://bugs.eclipse.org/bugs/show_bug.cgi?id=536215
public final class DartLangServerClientImpl extends LanguageClientImpl implements DartLangServerClient {

   @Override
   @NonNullByDefault({})
   public CompletableFuture<List<Object>> configuration(final ConfigurationParams configurationParams) {
      final var configs = new ArrayList<>(configurationParams.getItems().size());
      for (final var item : configurationParams.getItems()) {
         var maxLineLength = DartWorkspacePreference.getFormatterMaxLineLength();
         final var res = LSPEclipseUtils.findResourceFor(item.getScopeUri());
         if (res != null) {
            final var project = res.getProject();
            if (project != null) {
               final var prefs = DartProjectPreference.get(project);
               maxLineLength = prefs.getFormatterMaxLineLength();
            }
         }
         // https://github.com/dart-lang/sdk/blob/main/pkg/analysis_server/tool/lsp_spec/README.md#client-workspace-configuration
         configs.add(new TreeBuilder<String>() //
            .put("enableSdkFormatter", true) //
            .put("lineLength", maxLineLength) //
            .put("completeFunctionCalls", true) //
            .put("renameFilesWithClasses", "prompt") //
            .put("enableSnippets", true) //
            .put("updateImportsOnRename", true) //
            .getMap());
      }
      return CompletableFuture.completedFuture(configs);
   }

   @Override
   public @NonNullByDefault({}) CompletableFuture<Void> registerCapability(final RegistrationParams params) {
      return super.registerCapability(params);
   }

   @Override
   @NonNullByDefault({})
   public CompletableFuture<List<WorkspaceFolder>> workspaceFolders() {
      return CompletableFuture.completedFuture(Projects //
         .getOpenProjects(p -> p.getFile(Constants.PUBSPEC_YAML_FILENAME).exists()) //
         .map(LSPEclipseUtils::toWorkspaceFolder) //
         .toList());
   }
}
