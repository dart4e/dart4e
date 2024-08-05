/*
 * SPDX-FileCopyrightText: © The Dart4E authors
 * SPDX-FileContributor: Sebastian Thomschke
 * SPDX-License-Identifier: EPL-2.0
 * SPDX-ArtifactOfProjectHomePage: https://github.com/dart4e/dart4e
 */
package org.dart4e.launch;

import java.util.Collections;
import java.util.Set;

import org.eclipse.core.resources.IProject;
import org.eclipse.debug.core.model.IDebugTarget;
import org.eclipse.jdt.annotation.Nullable;

import net.sf.jstuff.core.collection.WeakHashSet;

/**
 * @author Sebastian Thomschke
 */
public interface DartDebugTarget extends IDebugTarget {

   Set<DartDebugTarget> ACTIVE_TARGETS = Collections.synchronizedSet(new WeakHashSet<>());

   DartDebugAPI getDebugAPI();

   @Nullable
   String getDartDebuggerURI();

   IProject getProject();

   boolean isHotReloadOnSave();
}
