/*
 * Copyright 2022 by the Dart4E authors.
 * SPDX-License-Identifier: EPL-2.0
 */
package org.dart4e.util;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Consumer;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;

/**
 * Builder to create Map with maps.
 *
 * @author Sebastian Thomschke
 */
public final class TreeBuilder<K> {

   private final Map<K, Object> map;

   public TreeBuilder() {
      this.map = new TreeMap<>();
   }

   public TreeBuilder(final Map<K, Object> map) {
      this.map = map;
   }

   public TreeBuilder<K> compute(final Consumer<TreeBuilder<K>> action) {
      action.accept(this);
      return this;
   }

   @SuppressWarnings("unchecked")
   public TreeBuilder<K> compute(final K k, final Consumer<TreeBuilder<K>> action) {
      final TreeBuilder<K> leafBuilder;
      final var leaf = map.get(k);
      if (leaf instanceof Map) {
         leafBuilder = new TreeBuilder<>((Map<K, Object>) leaf);
      } else {
         leafBuilder = new TreeBuilder<>();
      }
      action.accept(leafBuilder);
      map.put(k, leafBuilder.getMap());

      return this;
   }

   public Map<K, Object> getMap() {
      return map;
   }

   public TreeBuilder<K> put(final K k, @Nullable final Boolean v) {
      if (v == null)
         return this;

      map.put(k, v);
      return this;
   }

   public TreeBuilder<K> put(final K k, final K k2, @Nullable final Boolean v) {
      put(k, k2, (Object) v);
      return this;
   }

   public TreeBuilder<K> put(final K k, final K k2, @Nullable final List<String> v) {
      put(k, k2, (Object) v);
      return this;
   }

   public TreeBuilder<K> put(final K k, final K k2, @Nullable final Number v) {
      put(k, k2, (Object) v);
      return this;
   }

   @SuppressWarnings("unchecked")
   private void put(final K k, final K k2, @Nullable final Object v) {
      if (v == null)
         return;
      final var leaf = map.get(k);
      if (leaf instanceof Map) {
         ((Map<K, Object>) leaf).put(k2, v);
      } else {
         final var newLeaf = new TreeMap<K, Object>();
         newLeaf.put(k2, v);
         map.put(k, newLeaf);
      }
   }

   public TreeBuilder<K> put(final K k, final K k2, @Nullable final String v) {
      put(k, k2, (Object) v);
      return this;
   }

   public TreeBuilder<K> put(final K k, @Nullable final List<String> v) {
      if (v == null)
         return this;

      map.put(k, v);
      return this;
   }

   public TreeBuilder<K> put(final K k, @Nullable final Map<K, ?> v) {
      if (v == null)
         return this;

      if (map == v)
         throw new IllegalArgumentException("[v] Illegal self-reference");

      map.put(k, v);
      return this;
   }

   public TreeBuilder<K> put(final K k, @Nullable final Number v) {
      if (v == null)
         return this;

      map.put(k, v);
      return this;
   }

   public TreeBuilder<K> put(final K k, @Nullable final String v) {
      if (v == null)
         return this;

      map.put(k, v);
      return this;
   }

   public TreeBuilder<K> putAll(final Map<K, @NonNull ?> map) {
      this.map.putAll(map);
      return this;
   }

   public TreeBuilder<K> put(final K k, @Nullable final TreeBuilder<K> v) {
      if (v == null)
         return this;

      if (this == v)
         throw new IllegalArgumentException("[v] Illegal self-reference");

      map.put(k, v.getMap());
      return this;
   }
}
