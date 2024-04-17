/*
 * SPDX-FileCopyrightText: © The Dart4E authors
 * SPDX-FileContributor: Sebastian Thomschke
 * SPDX-License-Identifier: EPL-2.0
 * SPDX-ArtifactOfProjectHomePage: https://github.com/dart4e/dart4e
 */
package org.dart4e.launch;

import java.util.concurrent.CompletableFuture;

import org.eclipse.lsp4j.debug.services.IDebugProtocolServer;
import org.eclipse.lsp4j.jsonrpc.services.JsonRequest;

/**
 * https://github.com/dart-lang/sdk/tree/main/third_party/pkg/dap/tool#custom-requests
 *
 * @author Sebastian Thomschke
 */
public interface DartDebugAPI extends IDebugProtocolServer {

   @JsonRequest("hotReload")
   default CompletableFuture<Void> hotReload() {
      throw new UnsupportedOperationException();
   }
}
