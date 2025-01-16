/*
 * SPDX-FileCopyrightText: © The Dart4E authors
 * SPDX-FileContributor: Sebastian Thomschke
 * SPDX-License-Identifier: EPL-2.0
 * SPDX-ArtifactOfProjectHomePage: https://github.com/dart4e/dart4e
 */
package org.dart4e.navigation;

import org.dart4e.Constants;
import org.dart4e.Dart4EPlugin;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.lsp4e.outline.SymbolsLabelProvider;
import org.eclipse.lsp4e.outline.SymbolsModel.DocumentSymbolWithURI;
import org.eclipse.lsp4j.DocumentSymbol;
import org.eclipse.lsp4j.SymbolKind;
import org.eclipse.lsp4j.WorkspaceSymbol;
import org.eclipse.swt.graphics.Image;

/**
 * LSP4E's SymbolsLabelProvider does not provide images for all
 * SymbolKinds, thus we extend it.
 *
 * @author Sebastian Thomschke
 */
@SuppressWarnings({"restriction"})
public final class DartOutlineSymbolsProvider extends SymbolsLabelProvider {

   @Override
   public @Nullable Image getImage(final @NonNullByDefault({}) Object item) {
      SymbolKind kind = null;
      if (item instanceof final DocumentSymbol docSymbol) {
         kind = docSymbol.getKind();
      } else if (item instanceof final DocumentSymbolWithURI docSymbol) {
         kind = docSymbol.symbol.getKind();
      } else if (item instanceof final WorkspaceSymbol symbol) {
         kind = symbol.getKind();
      }

      if (kind != null) {
         switch (kind) {
            case EnumMember:
               // TODO until https://github.com/eclipse/lsp4e/pull/257
               return Dart4EPlugin.get().getSharedImage(Constants.IMAGE_OUTLINE_SYMBOL_ENUM_MEMBER);
            case Struct:
               return Dart4EPlugin.get().getSharedImage(Constants.IMAGE_OUTLINE_SYMBOL_TYPEDEF);
            default:
         }
      }

      return super.getImage(item);
   }
}
