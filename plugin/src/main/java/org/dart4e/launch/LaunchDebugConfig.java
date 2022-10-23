/*
 * Copyright 2022 by the Dart4E authors.
 * SPDX-License-Identifier: EPL-2.0
 */
package org.dart4e.launch;

import static net.sf.jstuff.core.validation.NullAnalysisHelper.*;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.Map;

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
import org.eclipse.lsp4e.debug.debugmodel.DSPDebugTarget;
import org.eclipse.lsp4e.debug.launcher.DSPLaunchDelegate;

/**
 * @author Sebastian Thomschke
 */
@SuppressWarnings("restriction")
public class LaunchDebugConfig extends DSPLaunchDelegate {

   private static final boolean TRACE_IO = Platform.getDebugBoolean("org.dart4e/trace/debugserv/io");

   @Override
   public void launch(final ILaunchConfiguration configuration, final String mode, final ILaunch launch,
      final @Nullable IProgressMonitor monitor) throws CoreException {
      super.launch(configuration, mode, launch, monitor);
   }

   @SuppressWarnings("resource")
   @Override
   @NonNullByDefault({})
   protected IDebugTarget createDebugTarget(final SubMonitor subMonitor, final Runnable cleanup, final InputStream inputStream,
      final OutputStream outputStream, final ILaunch launch, final Map<String, Object> dspParameters) throws CoreException {
      final var target = TRACE_IO //
         ? new DSPDebugTarget(launch, cleanup, //
            new LinePrefixingTeeInputStream(asNonNullUnsafe(inputStream), System.out, "SERVER >> "), //
            new LinePrefixingTeeOutputStream(asNonNullUnsafe(outputStream), System.out, "CLIENT >> "), //
            dspParameters) //
         : new DSPDebugTarget(launch, cleanup, inputStream, outputStream, dspParameters);
      target.initialize(subMonitor.split(80));
      return target;
   }
}
