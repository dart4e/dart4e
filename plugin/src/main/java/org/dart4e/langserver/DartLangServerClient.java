/*
 * SPDX-FileCopyrightText: © The Dart4E authors
 * SPDX-FileContributor: Sebastian Thomschke
 * SPDX-License-Identifier: EPL-2.0
 * SPDX-ArtifactOfProjectHomePage: https://github.com/dart4e/dart4e
 */
package org.dart4e.langserver;

import java.util.Map;

import org.dart4e.Dart4EPlugin;
import org.eclipse.lsp4j.jsonrpc.services.JsonNotification;
import org.eclipse.lsp4j.services.LanguageClient;

/**
 * https://github.com/dart-lang/sdk/blob/main/pkg/analysis_server/tool/lsp_spec/README.md#custom-fields-methods-and-notifications
 *
 * @author Sebastian Thomschke
 */
public interface DartLangServerClient extends LanguageClient {

   @JsonNotification("dart/textDocument/publishOutline")
   default void onPublishOutline(final Map<String, ?> args) {
      final var event = new PublishOutlineEvent(args);
      Dart4EPlugin.log().debug("onPublishOutline: {0}", event.args);
   }

   @JsonNotification("dart/textDocument/publishFlutterOutline")
   default void onPublishFlutterOutline(final Map<String, ?> args) {
      final var event = new PublishOutlineEvent(args);
      Dart4EPlugin.log().debug("onPublishFlutterOutline: {0}", event.args);
   }

   final class PublishOutlineEvent extends LSPEventArgs {
      public final String uri;
      public final Map<String, ?> outline;

      public PublishOutlineEvent(final Map<String, ?> args) {
         super(args);
         uri = getStringArg("uri");
         outline = getMapArg("outline");
      }
   }
}
