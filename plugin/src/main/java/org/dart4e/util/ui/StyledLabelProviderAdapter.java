/*
 * SPDX-FileCopyrightText: © The Dart4E authors
 * SPDX-FileContributor: Sebastian Thomschke
 * SPDX-License-Identifier: EPL-2.0
 * SPDX-ArtifactOfProjectHomePage: https://github.com/dart4e/dart4e
 */
package org.dart4e.util.ui;

import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.jface.viewers.BaseLabelProvider;
import org.eclipse.jface.viewers.DelegatingStyledCellLabelProvider.IStyledLabelProvider;
import org.eclipse.jface.viewers.IColorProvider;
import org.eclipse.jface.viewers.IFontProvider;
import org.eclipse.jface.viewers.StyledString;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.Image;

/**
 * @author Sebastian Thomschke
 */
public class StyledLabelProviderAdapter extends BaseLabelProvider implements IColorProvider, IFontProvider, IStyledLabelProvider {

   @Nullable
   @Override
   public Color getBackground(@Nullable final Object element) {
      return null;
   }

   @Nullable
   @Override
   public Font getFont(@Nullable final Object element) {
      return null;
   }

   @Nullable
   @Override
   public Color getForeground(@Nullable final Object element) {
      return null;
   }

   @Nullable
   @Override
   public Image getImage(@Nullable final Object element) {
      return null;
   }

   @Nullable
   @Override
   public StyledString getStyledText(@Nullable final Object element) {
      return null;
   }
}
