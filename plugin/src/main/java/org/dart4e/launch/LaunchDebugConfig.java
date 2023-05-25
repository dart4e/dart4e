/*
 * SPDX-FileCopyrightText: © The Dart4E authors
 * SPDX-FileContributor: Sebastian Thomschke
 * SPDX-License-Identifier: EPL-2.0
 * SPDX-ArtifactOfProjectHomePage: https://github.com/dart4e/dart4e
 */
package org.dart4e.launch;

import static net.sf.jstuff.core.validation.NullAnalysisHelper.asNonNullUnsafe;

import java.util.Map;
import java.util.function.Supplier;

import org.dart4e.util.io.LinePrefixingTeeInputStream;
import org.dart4e.util.io.LinePrefixingTeeOutputStream;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.model.IDebugTarget;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.lsp4e.debug.debugmodel.TransportStreams;
import org.eclipse.lsp4e.debug.debugmodel.TransportStreams.DefaultTransportStreams;
import org.eclipse.lsp4e.debug.launcher.DSPLaunchDelegate;

/**
 * @author Sebastian Thomschke
 */
@SuppressWarnings("restriction")
public class LaunchDebugConfig extends DSPLaunchDelegate {

   private static final boolean TRACE_IO = Platform.getDebugBoolean("org.dart4e/trace/debugserv/io");

   @Override
   @SuppressWarnings("resource")
   @NonNullByDefault({})
   protected IDebugTarget createDebugTarget(final SubMonitor mon, final Supplier<TransportStreams> streamsSupplier, final ILaunch launch,
      final Map<String, Object> dspParameters) throws CoreException {
      return TRACE_IO //
         ? super.createDebugTarget(mon, streamsSupplier, launch, dspParameters)
         : super.createDebugTarget(mon, (Supplier<TransportStreams>) () -> {
            final var streams = streamsSupplier.get();
            return new DefaultTransportStreams( //
               new LinePrefixingTeeInputStream(asNonNullUnsafe(streams.in), System.out, "SERVER >> "), //
               new LinePrefixingTeeOutputStream(asNonNullUnsafe(streams.out), System.out, "CLIENT >> "));
         }, launch, dspParameters);
   }

   @Override
   public void launch(final ILaunchConfiguration configuration, final String mode, final ILaunch launch,
      final @Nullable IProgressMonitor monitor) throws CoreException {
      super.launch(configuration, mode, launch, monitor);
   }
}
