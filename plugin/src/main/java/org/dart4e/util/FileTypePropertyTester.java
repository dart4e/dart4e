/*
 * SPDX-FileCopyrightText: © The Dart4E authors
 * SPDX-FileContributor: Sebastian Thomschke
 * SPDX-License-Identifier: EPL-2.0
 * SPDX-ArtifactOfProjectHomePage: https://github.com/dart4e/dart4e
 */
package org.dart4e.util;

import org.eclipse.core.expressions.PropertyTester;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.Adapters;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.lsp4e.LSPEclipseUtils;
import org.eclipse.lsp4e.outline.SymbolsModel.DocumentSymbolWithURI;
import org.eclipse.lsp4j.DocumentSymbol;
import org.eclipse.lsp4j.SymbolInformation;
import org.eclipse.lsp4j.WorkspaceSymbol;

import de.sebthom.eclipse.commons.ui.Editors;

/**
 * See https://wiki.eclipse.org/Platform_Expression_Framework
 *
 * @author Sebastian Thomschke
 */
@SuppressWarnings({"deprecation", "restriction"})
public final class FileTypePropertyTester extends PropertyTester {

   private static final String PROPERTY_CONTENT_TYPE_ID = "contentTypeId";
   private static final String PROPERTY_FILE_EXTENSION = "fileExtension";

   private boolean matchesPropertyValue(final String propertyName, @Nullable final String fileName, final Object expectedPropertyValue) {
      if (fileName == null)
         return false;

      return switch (propertyName) {
         case PROPERTY_CONTENT_TYPE_ID -> {
            final var contentType = Platform.getContentTypeManager().findContentTypeFor(fileName);
            yield contentType != null && contentType.getId().equals(expectedPropertyValue);
         }
         case PROPERTY_FILE_EXTENSION -> fileName.endsWith("." + expectedPropertyValue);
         default -> false;
      };
   }

   @Override
   public boolean test(@Nullable final Object candidate, @Nullable final String property, final Object[] args,
         @Nullable final Object expectedPropertyValue) {
      if (candidate == null || property == null || expectedPropertyValue == null)
         return false;

      if (candidate instanceof final SymbolInformation symbolInfo) {
         final var location = symbolInfo.getLocation();
         if (location == null)
            return false;
         return matchesPropertyValue(property, location.getUri(), expectedPropertyValue);
      }

      if (candidate instanceof final WorkspaceSymbol wsSymbol) {
         final var location = wsSymbol.getLocation();
         final String uri = location.isLeft() ? location.getLeft().getUri() : location.getRight().getUri();
         return matchesPropertyValue(property, uri, expectedPropertyValue);
      }

      final IFile file;
      if (candidate instanceof final IFile f) {
         file = f;
      } else if (candidate instanceof final DocumentSymbolWithURI docSymbolWithURI) {
         file = LSPEclipseUtils.getFileHandle(docSymbolWithURI.uri);
      } else if (candidate instanceof DocumentSymbol) {
         file = Editors.getActiveFile();
      } else {
         file = Adapters.adapt(candidate, IFile.class);
      }

      return file != null && matchesPropertyValue(property, file.getName(), expectedPropertyValue);
   }
}
