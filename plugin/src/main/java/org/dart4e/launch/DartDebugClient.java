/*
 * SPDX-FileCopyrightText: © The Dart4E authors
 * SPDX-FileContributor: Sebastian Thomschke
 * SPDX-License-Identifier: EPL-2.0
 * SPDX-ArtifactOfProjectHomePage: https://github.com/dart4e/dart4e
 */
package org.dart4e.launch;

import java.util.Map;

import org.dart4e.Dart4EPlugin;
import org.dart4e.langserver.LSPEventArgs;
import org.eclipse.lsp4j.debug.services.IDebugProtocolClient;
import org.eclipse.lsp4j.jsonrpc.services.JsonNotification;

/**
 * https://github.com/dart-lang/sdk/tree/main/third_party/pkg/dap/tool#custom-events
 *
 * @author Sebastian Thomschke
 */
public interface DartDebugClient extends IDebugProtocolClient {

   /* ************************************************************************
    * dart.debuggerUris
    * ************************************************************************/

   @JsonNotification("dart.debuggerUris")
   default void onDartDebuggerUris(final Map<String, ?> args) {
      final var event = new DartDebuggerUriEvent(args);
      Dart4EPlugin.log().debug("onDartDebuggerUris: {0}", event.args);
   }

   final class DartDebuggerUriEvent extends LSPEventArgs {
      public final String vmServiceUri;

      public DartDebuggerUriEvent(final Map<String, ?> args) {
         super(args);
         vmServiceUri = getStringArg("vmServiceUri");
      }
   }

   /* ************************************************************************
    * dart.log
    * ************************************************************************/

   @JsonNotification("dart.log")
   default void onDartLog(final Map<String, ?> args) {
      final var event = new DartLogEvent(args);
      Dart4EPlugin.log().debug("onDartLog: {0}", event.args);
   }

   final class DartLogEvent extends LSPEventArgs {
      public final String message;

      public DartLogEvent(final Map<String, ?> args) {
         super(args);
         message = getStringArg("message");
      }
   }

   /* ************************************************************************
    * dart.serviceRegistered/Unregistered
    * ************************************************************************/

   @JsonNotification("dart.serviceRegistered")
   default void onDartServiceRegistered(final Map<String, ?> args) {
      final var event = new DartServiceRegisteredEvent(args);
      Dart4EPlugin.log().debug("onDartServiceRegistered: {0}", event.args);
   }

   final class DartServiceRegisteredEvent extends LSPEventArgs {
      public final String service;
      public final String method;

      public DartServiceRegisteredEvent(final Map<String, ?> args) {
         super(args);
         service = getStringArg("service");
         method = getStringArg("method");
      }
   }

   @JsonNotification("dart.serviceUnregistered")
   default void onDartServiceUnregistered(final Map<String, ?> args) {
      final var event = new DartServiceUnregisteredEvent(args);
      Dart4EPlugin.log().debug("onDartServiceUnregistered: {0}", event.args);
   }

   final class DartServiceUnregisteredEvent extends LSPEventArgs {
      public final String service;
      public final String method;

      public DartServiceUnregisteredEvent(final Map<String, ?> args) {
         super(args);
         service = getStringArg("service");
         method = getStringArg("method");
      }
   }

   /* ************************************************************************
    * dart.serviceExtensionAdded
    * ************************************************************************/

   @JsonNotification("dart.serviceExtensionAdded")
   default void onDartServiceExtensionAdded(final Map<String, ?> args) {
      final var event = new DartServiceExtensionAddedEvent(args);
      Dart4EPlugin.log().debug("onDartServiceExtensionAdded: {0}", event.args);
   }

   final class DartServiceExtensionAddedEvent extends LSPEventArgs {
      public final String extensionRPC;
      public final String isolateId;

      public DartServiceExtensionAddedEvent(final Map<String, ?> args) {
         super(args);
         extensionRPC = getStringArg("extensionRPC");
         isolateId = getStringArg("isolateId");
      }
   }
}
