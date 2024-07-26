/*
 * SPDX-FileCopyrightText: © The Dart4E authors
 * SPDX-FileContributor: Sebastian Thomschke
 * SPDX-License-Identifier: EPL-2.0
 * SPDX-ArtifactOfProjectHomePage: https://github.com/dart4e/dart4e
 */
package org.dart4e.langserver;

import java.util.Map;

/**
 * @author Sebastian Thomschke
 */
public abstract class LSPEventArgs {
   public final Map<String, ?> args;

   protected LSPEventArgs(final Map<String, ?> args) {
      this.args = args;
   }

   protected boolean getBooleanArg(final String name) {
      final Object value = args.get(name);
      if (value == null)
         throw new IllegalStateException("Argument [" + name + "] not present in: " + this);

      if (value instanceof final Boolean bool)
         return bool;

      throw new IllegalStateException("Value of argument [" + name + "] is not a boolean: " + value + "(" + value.getClass().getName()
            + ")");
   }

   @SuppressWarnings("unchecked")
   protected Map<String, Object> getMapArg(final String name) {
      final Object value = args.get(name);
      if (value == null)
         throw new IllegalStateException("Argument [" + name + "] not present in: " + this);

      if (value instanceof final Map map)
         return map;

      throw new IllegalStateException("Value of argument [" + name + "] is not a map: " + value + "(" + value.getClass().getName() + ")");
   }

   protected String getStringArg(final String name) {
      final Object value = args.get(name);
      if (value == null)
         throw new IllegalStateException("Argument [" + name + "] not present in: " + this);

      if (value instanceof final String str)
         return str;

      throw new IllegalStateException("Value of argument [" + name + "] is not a string: " + value + "(" + value.getClass().getName()
            + ")");
   }

   @Override
   public String toString() {
      return this.getClass().getSimpleName() + args;
   }
}
