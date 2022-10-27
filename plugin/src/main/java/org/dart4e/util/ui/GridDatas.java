/*
 * Copyright 2022 by the Dart4E authors.
 * SPDX-License-Identifier: EPL-2.0
 */
package org.dart4e.util.ui;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;

/**
 * @author Sebastian Thomschke
 */
public final class GridDatas {

   public static GridData alignRight() {
      return new GridData(SWT.RIGHT, SWT.CENTER, false, false, 1, 1);
   }

   public static GridData fillExcessive() {
      return fillExcessive(1, 1);
   }

   public static GridData fillExcessive(final int horizontalSpan, final int verticalSpan) {
      return new GridData(SWT.FILL, SWT.FILL, true, true, horizontalSpan, verticalSpan);
   }

   public static GridData fillHorizontal() {
      return fillHorizontal(1);
   }

   public static GridData fillHorizontal(final int horizontalSpan) {
      return new GridData(SWT.FILL, SWT.CENTER, false, false, horizontalSpan, 1);
   }

   public static GridData fillHorizontalExcessive() {
      return fillHorizontalExcessive(1);
   }

   public static GridData fillHorizontalExcessive(final int horizontalSpan) {
      return new GridData(SWT.FILL, SWT.CENTER, true, false, horizontalSpan, 1);
   }

   public static GridData fillHorizontalExcessive(final int horizontalSpan, final int heightHint) {
      final var gridData = fillHorizontalExcessive(horizontalSpan);
      gridData.heightHint = heightHint;
      return gridData;
   }

   private GridDatas() {
   }
}
