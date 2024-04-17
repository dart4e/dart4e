/*
 * SPDX-FileCopyrightText: © The Dart4E authors
 * SPDX-FileContributor: Sebastian Thomschke
 * SPDX-License-Identifier: EPL-2.0
 * SPDX-ArtifactOfProjectHomePage: https://github.com/dart4e/dart4e
 */
package org.dart4e.flutter.launch;

import java.util.concurrent.CompletableFuture;

import org.eclipse.lsp4j.debug.services.IDebugProtocolServer;
import org.eclipse.lsp4j.jsonrpc.services.JsonRequest;

/**
 * https://github.com/flutter/flutter/tree/master/packages/flutter_tools/lib/src/debug_adapters#custom-requests
 *
 * @author Sebastian Thomschke
 */
public interface FlutterDebugAPI extends IDebugProtocolServer {

   /**
    * @param reason manual|save
    */
   record HotReloadArgs(String reason) {
   }

   @JsonRequest("hotReload")
   default CompletableFuture<Void> hotReload(@SuppressWarnings("unused") final HotReloadArgs args) {
      throw new UnsupportedOperationException();
   }

   @JsonRequest("hotRestart")
   default CompletableFuture<Void> hotRestart(@SuppressWarnings("unused") final HotReloadArgs args) {
      throw new UnsupportedOperationException();
   }
}
