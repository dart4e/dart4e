/*
 * SPDX-FileCopyrightText: © The Dart4E authors
 * SPDX-FileContributor: Sebastian Thomschke
 * SPDX-License-Identifier: EPL-2.0
 * SPDX-ArtifactOfProjectHomePage: https://github.com/dart4e/dart4e
 */
package org.dart4e.flutter.launch;

import java.util.Map;

import org.dart4e.Dart4EPlugin;
import org.dart4e.launch.DartDebugProtocolEvents;
import org.eclipse.lsp4j.jsonrpc.services.JsonNotification;

/**
 * https://github.com/flutter/flutter/tree/master/packages/flutter_tools/lib/src/debug_adapters#custom-events
 *
 * @author Sebastian Thomschke
 */
public interface FlutterDebugProtocolEvents extends DartDebugProtocolEvents {

   /* ************************************************************************
    * flutter.appStart
    * ************************************************************************/

   @JsonNotification("flutter.appStart")
   default void onFlutterAppStart(final Map<String, ?> args) {
      final var event = new FlutterAppStartEvent(args);
      Dart4EPlugin.log().debug("onFlutterAppStart: {0}", event.args);
   }

   final class FlutterAppStartEvent extends DebugProtocolEvent {
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

   final class FlutterAppStartedEvent extends DebugProtocolEvent {
      public FlutterAppStartedEvent(final Map<String, ?> args) {
         super(args);
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
    * {extension=ext.flutter.activeDevToolsServerAddress, value=http://127.0.0.1:9102}
    * {extension=ext.flutter.connectedVmServiceUri, value=http://127.0.0.1:52894/BxITDkg2xgk=/}
    */
   final class FlutterServiceExtensionStateChangedEvent extends DebugProtocolEvent {
      public final String extension;
      public final String value;

      public FlutterServiceExtensionStateChangedEvent(final Map<String, ?> args) {
         super(args);
         extension = getStringArg("extension");
         value = getStringArg("value");
      }
   }
}
