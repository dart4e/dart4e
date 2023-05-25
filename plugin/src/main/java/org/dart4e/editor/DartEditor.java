/*
 * SPDX-FileCopyrightText: © The Dart4E authors
 * SPDX-FileContributor: Sebastian Thomschke
 * SPDX-License-Identifier: EPL-2.0
 * SPDX-ArtifactOfProjectHomePage: https://github.com/dart4e/dart4e
 */
package org.dart4e.editor;

import org.dart4e.Dart4EPlugin;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.model.ILineBreakpoint;
import org.eclipse.debug.ui.actions.ToggleBreakpointAction;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.contentassist.ContentAssistant;
import org.eclipse.jface.text.source.AnnotationRulerColumn;
import org.eclipse.jface.text.source.CompositeRuler;
import org.eclipse.jface.text.source.ISourceViewerExtension4;
import org.eclipse.jface.text.source.IVerticalRulerColumn;
import org.eclipse.jface.text.source.IVerticalRulerInfo;
import org.eclipse.jface.text.source.SourceViewer;
import org.eclipse.lsp4e.debug.DSPPlugin;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.tm4e.ui.TMUIPlugin;
import org.eclipse.ui.internal.genericeditor.ExtensionBasedTextEditor;

import net.sf.jstuff.core.reflection.Fields;

/**
 * @author Sebastian Thomschke
 */
@SuppressWarnings("restriction")
public final class DartEditor extends ExtensionBasedTextEditor {

   public static final String ID = DartEditor.class.getName();

   @Override
   protected IVerticalRulerColumn createAnnotationRulerColumn(final CompositeRuler ruler) {
      return new AnnotationRulerColumn(VERTICAL_RULER_WIDTH, getAnnotationAccess()) {
         @Override
         protected void mouseDoubleClicked(final int rulerLine) {
            toggleBreakpoint();
         }
      };
   }

   @Override
   public void createPartControl(final Composite parent) {
      super.createPartControl(parent);

      final var contentAssistant = getContentAssistant();
      if (contentAssistant != null) {
         contentAssistant.setAutoActivationDelay(500);
      }
   }

   private @Nullable ContentAssistant getContentAssistant() {
      final var viewer = getSourceViewer();

      if (viewer instanceof final ISourceViewerExtension4 viewer4) {
         final var contentAssistantFacade = viewer4.getContentAssistantFacade();
         if (contentAssistantFacade != null //
            && Fields.read(contentAssistantFacade, "fContentAssistant") instanceof final ContentAssistant contentAssistant)
            return contentAssistant;
      }

      if (viewer instanceof SourceViewer //
         && Fields.read(viewer, "fContentAssistant") instanceof final ContentAssistant contentAssistant)
         return contentAssistant;

      return null;
   }

   private @Nullable IDocument getDocument() {
      return getDocumentProvider().getDocument(getEditorInput());
   }

   @Override
   protected void initializeKeyBindingScopes() {
      setKeyBindingScopes(new String[] {"org.dart4e.editor.DartEditorContext"});
   }

   private void toggleBreakpoint() {
      final var doc = getDocument();
      if (doc == null)
         return;
      final var parsedDoc = TMUIPlugin.getTMModelManager().connect(doc);
      final var rulerInfo = getAdapter(IVerticalRulerInfo.class);
      if (rulerInfo == null)
         return;
      final var lineNumber = rulerInfo.getLineOfLastMouseButtonActivity();

      final var tokens = parsedDoc.getLineTokens(lineNumber);
      if (tokens == null || tokens.isEmpty())
         return;

      // check if the current line is eligible for having a breakpoint
      var lineSupportsAddingBreakpoint = false;
      for (final var token : tokens) {
         if (token.type.isBlank() || token.type.contains("comment") || token.type.contains("punctuation")) {
            continue;
         }
         lineSupportsAddingBreakpoint = true;
         break;
      }

      if (lineSupportsAddingBreakpoint) {
         final var action = new ToggleBreakpointAction(this, doc, rulerInfo);
         action.update();
         action.run();
      } else {
         // if the current line is eligible for having a breakpoint then remove any potentially existing breakpoints
         final var breakpoints = DebugPlugin.getDefault().getBreakpointManager().getBreakpoints(DSPPlugin.ID_DSP_DEBUG_MODEL);
         if (breakpoints.length == 0)
            return;

         final var resource = getEditorInput().getAdapter(IResource.class);
         if (resource == null)
            return;
         for (final var breakpoint : breakpoints) {
            try {
               if (breakpoint instanceof final ILineBreakpoint lineBreakpoint //
                  && resource.equals(breakpoint.getMarker().getResource()) //
                  && lineBreakpoint.getLineNumber() == lineNumber + 1) {
                  breakpoint.delete();
               }
            } catch (final CoreException ex) {
               Dart4EPlugin.log().error(ex);
            }
         }
      }
   }
}
