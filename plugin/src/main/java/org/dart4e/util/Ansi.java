/*
 * SPDX-FileCopyrightText: © The Dart4E authors
 * SPDX-FileContributor: Sebastian Thomschke
 * SPDX-License-Identifier: EPL-2.0
 * SPDX-ArtifactOfProjectHomePage: https://github.com/dart4e/dart4e
 */
package org.dart4e.util;

/**
 * @author Sebastian Thomschke
 */
public interface Ansi {
   String RESET = "\033[0m";
   String BOLD = "\033[1m";
   String RED = "\033[31m";
   String GREEN = "\033[32m";
   String YELLOW = "\033[33m";
   String BLUE = "\033[34m";
   String MAGENTA = "\033[35m";
   String CYAN = "\033[36m";
   String WHITE = "\033[37m";
   String GRAY = "\033[90m";
}
