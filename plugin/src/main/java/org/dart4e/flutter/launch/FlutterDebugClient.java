/*
 * SPDX-FileCopyrightText: © The Dart4E authors
 * SPDX-FileContributor: Sebastian Thomschke
 * SPDX-License-Identifier: EPL-2.0
 * SPDX-ArtifactOfProjectHomePage: https://github.com/dart4e/dart4e
 */
package org.dart4e.flutter.launch;

import java.util.Map;

import org.dart4e.Dart4EPlugin;
import org.dart4e.langserver.LSPEventArgs;
import org.dart4e.launch.DartDebugClient;
import org.eclipse.lsp4j.jsonrpc.services.JsonNotification;

/**
 * https://github.com/flutter/flutter/tree/master/packages/flutter_tools/lib/src/debug_adapters/README.md#custom-events
 *
 * @author Sebastian Thomschke
 */
public interface FlutterDebugClient extends DartDebugClient {

   /* ************************************************************************
    * flutter.appStart
    * ************************************************************************/

   @JsonNotification("flutter.appStart")
   default void onFlutterAppStart(final Map<String, ?> args) {
      final var event = new FlutterAppStartEvent(args);
      Dart4EPlugin.log().debug("onFlutterAppStart: {0}", event.args);
   }

   final class FlutterAppStartEvent extends LSPEventArgs {
      public final String appId;
      public final String directory;
      public final boolean supportsRestart;
      public final String launchMode; // run
      public final String mode; // debug

      public FlutterAppStartEvent(final Map<String, ?> args) {
         super(args);
         appId = getStringArg("appId");
         directory = getStringArg("directory");
         supportsRestart = getBooleanArg("supportsRestart");
         launchMode = getStringArg("launchMode");
         mode = getStringArg("mode");
      }
   }

   /* ************************************************************************
    * flutter.appStarted
    * ************************************************************************/

   @JsonNotification("flutter.appStarted")
   default void onFlutterAppStarted(final Map<String, ?> args) {
      final var event = new FlutterAppStartedEvent(args);
      Dart4EPlugin.log().debug("onFlutterAppStarted: {0}", event.args);
   }

   final class FlutterAppStartedEvent extends LSPEventArgs {
      public FlutterAppStartedEvent(final Map<String, ?> args) {
         super(args);
      }
   }

   /* ************************************************************************
    * flutter.forwardedEvent
    * ************************************************************************/

   @JsonNotification("flutter.forwardedEvent")
   default void onFlutterForwardedEvent(final Map<String, ?> args) {
      final var event = new FlutterForwardedEventEvent(args);
      Dart4EPlugin.log().debug("onFlutterForwardedEvent: {0}", event.args);
   }

   /**
    * E.g.
    * <pre>
    * {event=app.webLaunchUrl, params={url=http://localhost:60122, launched=true}}
    * </pre>
    */
   final class FlutterForwardedEventEvent extends LSPEventArgs {
      public final String event;
      public final Map<String, Object> params;

      public FlutterForwardedEventEvent(final Map<String, ?> args) {
         super(args);
         event = getStringArg("event");
         params = getMapArg("params");
      }
   }

   /* ************************************************************************
    * flutter.serviceExtensionStateChanged
    * ************************************************************************/

   @JsonNotification("flutter.serviceExtensionStateChanged")
   default void onFlutterServiceExtensionStateChanged(final Map<String, ?> args) {
      final var event = new FlutterServiceExtensionStateChangedEvent(args);
      Dart4EPlugin.log().debug("onFlutterServiceExtensionStateChanged: {0}", event.args);
   }

   /**
    * E.g.
    * <pre>
    * {extension=ext.flutter.activeDevToolsServerAddress, value=http://127.0.0.1:9102}
    * {extension=ext.flutter.connectedVmServiceUri, value=http://127.0.0.1:52894/BxITDkg2xgk=/}
    * </pre>
    */
   final class FlutterServiceExtensionStateChangedEvent extends LSPEventArgs {
      public final String extension;
      public final String value;

      public FlutterServiceExtensionStateChangedEvent(final Map<String, ?> args) {
         super(args);
         extension = getStringArg("extension");
         value = getStringArg("value");
      }
   }
}
