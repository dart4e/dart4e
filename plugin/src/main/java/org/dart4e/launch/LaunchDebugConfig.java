/*
 * SPDX-FileCopyrightText: © The Dart4E authors
 * SPDX-FileContributor: Sebastian Thomschke
 * SPDX-License-Identifier: EPL-2.0
 * SPDX-ArtifactOfProjectHomePage: https://github.com/dart4e/dart4e
 */
package org.dart4e.launch;

import static net.sf.jstuff.core.validation.NullAnalysisHelper.asNonNullUnsafe;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;

import org.dart4e.prefs.DartWorkspacePreference;
import org.dart4e.util.io.VSCodeJsonRpcLineTracing;
import org.dart4e.util.io.VSCodeJsonRpcLineTracing.Source;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.lsp4e.debug.debugmodel.DSPDebugTarget;
import org.eclipse.lsp4e.debug.debugmodel.TransportStreams;
import org.eclipse.lsp4e.debug.debugmodel.TransportStreams.DefaultTransportStreams;
import org.eclipse.lsp4e.debug.launcher.DSPLaunchDelegate;
import org.eclipse.lsp4j.jsonrpc.Launcher;
import org.eclipse.lsp4j.jsonrpc.MessageConsumer;
import org.eclipse.lsp4j.jsonrpc.debug.DebugLauncher;

import net.sf.jstuff.core.io.stream.LineCapturingInputStream;
import net.sf.jstuff.core.io.stream.LineCapturingOutputStream;

/**
 * @author Sebastian Thomschke
 */
@SuppressWarnings("restriction")
public class LaunchDebugConfig extends DSPLaunchDelegate {

   private final class DartDebugTargetImpl extends DSPDebugTarget implements DartDebugTarget, DartDebugClient {

      private @Nullable DartDebuggerUriEvent debuggerInfo;

      protected DartDebugTargetImpl(final ILaunch launch, final Supplier<TransportStreams> streamsSupplier,
            final Map<String, Object> dspParameters) {
         super(launch, streamsSupplier, dspParameters);
      }

      @Override
      @NonNullByDefault({})
      protected Launcher<DartDebugAPI> createLauncher(final UnaryOperator<MessageConsumer> wrapper, final InputStream in,
            final OutputStream out, final ExecutorService threadPool) {
         return DebugLauncher.createLauncher(this, DartDebugAPI.class, in, out, threadPool, wrapper);
      }

      @Override
      public DartDebugAPI getDebugAPI() {
         return (DartDebugAPI) getDebugProtocolServer();
      }

      @Override
      public @Nullable String getDartDebuggerURI() {
         final var debuggerInfo = this.debuggerInfo;
         if (debuggerInfo == null)
            return null;
         return debuggerInfo.vmServiceUri;
      }

      @Override
      public boolean isHotReloadOnSave() {
         return hotReloadOnSave;
      }

      @Override
      public IProject getProject() {
         return project;
      }

      @Override
      public void onDartDebuggerUris(final Map<String, ?> args) {
         DartDebugClient.super.onDartDebuggerUris(args);
         debuggerInfo = new DartDebuggerUriEvent(args);
      }
   }

   private final IProject project;
   private boolean hotReloadOnSave;

   public LaunchDebugConfig(final IProject project, final boolean hotReloadOnSave) {
      this.project = project;
      this.hotReloadOnSave = hotReloadOnSave;
   }

   @Override
   @SuppressWarnings("resource")
   @NonNullByDefault({})
   protected DartDebugTarget createDebugTarget(final SubMonitor mon, final Supplier<TransportStreams> streamsSupplier, final ILaunch launch,
         final Map<String, Object> dspParameters) throws CoreException {
      final var isTraceIOVerbose = DartWorkspacePreference.isDAPTraceIOVerbose();
      final var effectiveStreamsSupplier = isTraceIOVerbose || DartWorkspacePreference.isDAPTraceIO() //
            ? (Supplier<TransportStreams>) () -> {
               final var streams = streamsSupplier.get();
               return new DefaultTransportStreams( //
                  new LineCapturingInputStream(asNonNullUnsafe(streams.in), line -> VSCodeJsonRpcLineTracing.traceLine(Source.SERVER_OUT,
                     line, isTraceIOVerbose)), //
                  new LineCapturingOutputStream(asNonNullUnsafe(streams.out), line -> VSCodeJsonRpcLineTracing.traceLine(Source.CLIENT_OUT,
                     line, isTraceIOVerbose)));
            }
            : streamsSupplier;

      final var target = new DartDebugTargetImpl(launch, effectiveStreamsSupplier, dspParameters);
      target.initialize(mon.split(80));

      DartDebugTarget.ACTIVE_TARGETS.removeIf(DartDebugTarget::isTerminated);
      DartDebugTarget.ACTIVE_TARGETS.add(target);

      return target;
   }
}
