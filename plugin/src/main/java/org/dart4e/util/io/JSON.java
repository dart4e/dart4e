/*
 * Copyright 2022 by the Dart4E authors.
 * SPDX-License-Identifier: EPL-2.0
 */
package org.dart4e.util.io;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;

import org.dart4e.Dart4EPlugin;
import org.eclipse.jdt.annotation.Nullable;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.deser.std.FromStringDeserializer;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;

import net.sf.jstuff.core.io.RuntimeIOException;

/**
 * @author Sebastian Thomschke
 */
public abstract class JSON {

   private static final JsonMapper JSON_MAPPER = JsonMapper.builder() //
      // .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES) //
      .build();

   static {
      // this disables usage of com.fasterxml.jackson.databind.ext.NioPathDeserializer
      // which results in freezes because on first usage all drive letters are iterated
      // which will hang for mapped but currently not reachable network drives
      final var m = new SimpleModule("CustomNioPathSerialization");
      @SuppressWarnings("null")
      final JsonSerializer<@Nullable Object> serializer = new ToStringSerializer();
      m.addSerializer(Path.class, serializer);
      m.addDeserializer(Path.class, new FromStringDeserializer<Path>(Path.class) {
         private static final long serialVersionUID = 1L;

         @Override
         protected Path _deserialize(final String value, final DeserializationContext ctxt) {
            return Paths.get(value);
         }
      });
      JSON_MAPPER.registerModule(m);
   }

   public static <T> T deserialize(final InputStream json, final TypeReference<T> type) throws RuntimeIOException {
      try {
         return JSON_MAPPER.readerFor(type).readValue(json);
      } catch (final IOException ex) {
         throw new RuntimeIOException(ex);
      }
   }

   public static <T> T deserialize(final String json, final Class<T> type) throws RuntimeIOException {
      try {
         return JSON_MAPPER.readerFor(type).readValue(json);
      } catch (final IOException ex) {
         throw new RuntimeIOException(ex);
      }
   }

   public static <T> @Nullable T deserializeNullable(final @Nullable String json, final Class<T> type) {
      if (json == null)
         return null;
      try {
         return JSON_MAPPER.readerFor(type).readValue(json);
      } catch (final IOException ex) {
         Dart4EPlugin.log().error(ex);
         return null;
      }
   }

   public static <T> T deserialize(final String json, final TypeReference<T> type) throws RuntimeIOException {
      try {
         return JSON_MAPPER.readerFor(type).readValue(json);
      } catch (final IOException ex) {
         throw new RuntimeIOException(ex);
      }
   }

   public static <T> T deserialize(final URL json, final TypeReference<T> type) throws RuntimeIOException {
      try {
         return JSON_MAPPER.readerFor(type).readValue(json);
      } catch (final IOException ex) {
         throw new RuntimeIOException(ex);
      }
   }

   public static <T> List<T> deserializeList(final InputStream json, final Class<T> type) throws RuntimeIOException {
      try {
         final List<T> list = JSON_MAPPER.readerForListOf(type).readValue(json);
         if (list == null)
            return Collections.emptyList();
         return list;
      } catch (final IOException ex) {
         throw new RuntimeIOException(ex);
      }
   }

   public static <T> List<T> deserializeList(final String json, final Class<T> type) throws RuntimeIOException {
      if (json.isEmpty())
         return Collections.emptyList();
      try {
         final List<T> list = JSON_MAPPER.readerForListOf(type).readValue(json);
         if (list == null)
            return Collections.emptyList();
         return list;
      } catch (final IOException ex) {
         throw new RuntimeIOException(ex);
      }
   }

   public static <T> List<T> deserializeList(final URL json, final Class<T> type) throws RuntimeIOException {
      try {
         final List<T> list = JSON_MAPPER.readerForListOf(type).readValue(json);
         if (list == null)
            return Collections.emptyList();
         return list;
      } catch (final IOException ex) {
         throw new RuntimeIOException(ex);
      }
   }

   public static String serialize(final @Nullable Object obj) throws RuntimeIOException {
      try {
         return JSON_MAPPER.writeValueAsString(obj);
      } catch (final IOException ex) {
         throw new RuntimeIOException(ex);
      }
   }
}
