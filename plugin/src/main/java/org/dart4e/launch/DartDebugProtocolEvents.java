/*
 * SPDX-FileCopyrightText: © The Dart4E authors
 * SPDX-FileContributor: Sebastian Thomschke
 * SPDX-License-Identifier: EPL-2.0
 * SPDX-ArtifactOfProjectHomePage: https://github.com/dart4e/dart4e
 */
package org.dart4e.launch;

import java.util.Map;

import org.dart4e.Dart4EPlugin;
import org.eclipse.lsp4j.debug.services.IDebugProtocolClient;
import org.eclipse.lsp4j.jsonrpc.services.JsonNotification;

/**
 * https://github.com/dart-lang/sdk/tree/main/third_party/pkg/dap/tool#custom-events
 *
 * @author Sebastian Thomschke
 */
public interface DartDebugProtocolEvents extends IDebugProtocolClient {

   class DebugProtocolEvent {
      public final Map<String, ?> args;

      protected DebugProtocolEvent(final Map<String, ?> args) {
         this.args = args;
      }

      protected boolean getBooleanArg(final String name) {
         final Object value = args.get(name);
         if (value == null)
            throw new IllegalStateException("Argument [" + name + "] not present in: " + this);

         if (value instanceof final Boolean bool)
            return bool;

         throw new IllegalStateException("Value of argument [" + name + "] not a boolean: " + value + "(" + value.getClass().getName()
               + ")");
      }

      @SuppressWarnings("unchecked")
      protected Map<String, Object> getMapArg(final String name) {
         final Object value = args.get(name);
         if (value == null)
            throw new IllegalStateException("Argument [" + name + "] not present in: " + this);

         if (value instanceof final Map map)
            return map;

         throw new IllegalStateException("Value of argument [" + name + "] not a string: " + value + "(" + value.getClass().getName()
               + ")");
      }

      protected String getStringArg(final String name) {
         final Object value = args.get(name);
         if (value == null)
            throw new IllegalStateException("Argument [" + name + "] not present in: " + this);

         if (value instanceof final String str)
            return str;

         throw new IllegalStateException("Value of argument [" + name + "] not a string: " + value + "(" + value.getClass().getName()
               + ")");
      }

      @Override
      public String toString() {
         return this.getClass().getSimpleName() + args;
      }
   }

   /* ************************************************************************
    * dart.debuggerUris
    * ************************************************************************/

   @JsonNotification("dart.debuggerUris")
   default void onDartDebuggerUris(final Map<String, ?> args) {
      final var event = new DartDebuggerUriEvent(args);
      Dart4EPlugin.log().debug("onDartDebuggerUris: {0}", event.args);
   }

   final class DartDebuggerUriEvent extends DebugProtocolEvent {
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

   final class DartLogEvent extends DebugProtocolEvent {
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

   final class DartServiceRegisteredEvent extends DebugProtocolEvent {
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

   final class DartServiceUnregisteredEvent extends DebugProtocolEvent {
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

   final class DartServiceExtensionAddedEvent extends DebugProtocolEvent {
      public final String extensionRPC;
      public final String isolateId;

      public DartServiceExtensionAddedEvent(final Map<String, ?> args) {
         super(args);
         extensionRPC = getStringArg("extensionRPC");
         isolateId = getStringArg("isolateId");
      }
   }
}
