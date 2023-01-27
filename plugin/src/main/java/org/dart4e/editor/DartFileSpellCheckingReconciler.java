/*
 * Copyright 2022 by the Dart4E authors.
 * SPDX-License-Identifier: EPL-2.0
 */
package org.dart4e.editor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import org.dart4e.Dart4EPlugin;
import org.eclipse.core.filebuffers.ITextFileBufferManager;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextInputListener;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.Position;
import org.eclipse.jface.text.Region;
import org.eclipse.jface.text.presentation.PresentationReconciler;
import org.eclipse.jface.text.source.Annotation;
import org.eclipse.jface.text.source.IAnnotationModel;
import org.eclipse.jface.text.source.IAnnotationModelExtension;
import org.eclipse.tm4e.core.model.IModelTokensChangedListener;
import org.eclipse.tm4e.core.model.ModelTokensChangedEvent;
import org.eclipse.tm4e.core.model.Range;
import org.eclipse.tm4e.ui.TMUIPlugin;
import org.eclipse.tm4e.ui.internal.model.TMDocumentModel;
import org.eclipse.tm4e.ui.text.TMPresentationReconciler;
import org.eclipse.ui.editors.text.EditorsUI;
import org.eclipse.ui.texteditor.spelling.ISpellingProblemCollector;
import org.eclipse.ui.texteditor.spelling.SpellingAnnotation;
import org.eclipse.ui.texteditor.spelling.SpellingContext;
import org.eclipse.ui.texteditor.spelling.SpellingProblem;
import org.eclipse.ui.texteditor.spelling.SpellingService;

/**
 * {@link PresentationReconciler} that performs incremental spell checking of comments
 *
 * @author Sebastian Thomschke
 */
@SuppressWarnings("restriction")
public final class DartFileSpellCheckingReconciler extends TMPresentationReconciler implements IModelTokensChangedListener,
   ITextInputListener {

   private static final boolean TRACE_SPELLCHECK_REGIONS = Platform.getDebugBoolean("org.dart4e/trace/spellcheck/regions");
   private static final boolean TRACE_SPELLCHECK_TOKENS = Platform.getDebugBoolean("org.dart4e/trace/spellcheck/tokens");

   private static final SpellingService SPELLING_SERVICE = EditorsUI.getSpellingService();

   private @Nullable Job spellcheckJob;
   private @Nullable ITextViewer viewer;

   private void addSpellcheckRegion(final List<Region> regionsToSpellcheck, final IDocument doc, final int offset, final int length)
      throws BadLocationException {
      if (TRACE_SPELLCHECK_REGIONS) {
         System.out.println("Region offset " + offset + " text: " + doc.get(offset, length));
      }
      regionsToSpellcheck.add(new Region(offset, length));
   }

   private List<Region> collectRegionsToSpellcheck(final TMDocumentModel docModel, final List<Range> changedRanges) {
      if (TRACE_SPELLCHECK_REGIONS || TRACE_SPELLCHECK_TOKENS) {
         System.out.println("----------collectRegionsToSpellcheck----------");
      }
      final var doc = docModel.getDocument();
      final var regionsToSpellcheck = new ArrayList<Region>();

      int blockCommentStartOffset = -1;
      for (final var range : changedRanges) {
         try {
            var lineIndex = -1;
            for (lineIndex = range.fromLineNumber - 1; lineIndex < range.toLineNumber; lineIndex++) {
               final var lineTokens = docModel.getLineTokens(lineIndex);
               if (lineTokens == null) {
                  continue;
               }
               for (final var token : lineTokens) {
                  if (TRACE_SPELLCHECK_TOKENS) {
                     System.out.println("Line " + lineIndex + " char " + token.startIndex + " " + token.type);
                  }

                  switch (token.type) {
                     case "comment.dart.line.double-slash": // one line of // single line comment
                        addSpellcheckRegion(regionsToSpellcheck, doc, doc.getLineOffset(lineIndex) + token.startIndex + 2, doc
                           .getLineLength(lineIndex) - (token.startIndex + 2));
                        break;

                     case "comment.block.dart", // one line of a /* block comment
                        "comment.block.documentation.dart": // one line of a /** block comment OR a /// comment
                        if (blockCommentStartOffset == -1) {
                           blockCommentStartOffset = doc.getLineOffset(lineIndex) + token.startIndex;
                        }
                        break;

                     default:
                        if (blockCommentStartOffset != -1) {
                           final var blockCommentLen = doc.getLineOffset(lineIndex) + token.startIndex - blockCommentStartOffset;
                           addSpellcheckRegion(regionsToSpellcheck, doc, blockCommentStartOffset, blockCommentLen);
                           blockCommentStartOffset = -1;
                        }
                  }
               }
            }
            if (blockCommentStartOffset != -1) {
               lineIndex--;
               final var blockCommentLen = doc.getLineOffset(lineIndex) + doc.getLineLength(lineIndex) - blockCommentStartOffset;
               addSpellcheckRegion(regionsToSpellcheck, doc, blockCommentStartOffset, blockCommentLen);
               blockCommentStartOffset = -1;
            }
         } catch (final BadLocationException ex) {
            Dart4EPlugin.log().error(ex);
         }
      }
      return regionsToSpellcheck;
   }

   @Override
   public void inputDocumentAboutToBeChanged(final @Nullable IDocument oldInput, final @Nullable IDocument newInput) {
   }

   @Override
   public void inputDocumentChanged(final @Nullable IDocument oldInput, final @Nullable IDocument newInput) {
      final var viewer = this.viewer;
      if (viewer == null)
         return;

      final var document = viewer.getDocument();
      if (document == null)
         return;

      final var model = TMUIPlugin.getTMModelManager().connect(document);
      model.addModelTokensChangedListener(this);
   }

   @Override
   public void install(@Nullable final ITextViewer viewer) {
      super.install(viewer);
      this.viewer = viewer;
      if (viewer != null) {
         viewer.addTextInputListener(this);
      }
   }

   @Override
   public void modelTokensChanged(final ModelTokensChangedEvent event) {
      if (!(event.model instanceof final TMDocumentModel docModel))
         return;

      final var doc = docModel.getDocument();

      final var textFileBuffer = ITextFileBufferManager.DEFAULT.getTextFileBuffer(doc);
      if (textFileBuffer == null)
         return;

      var spellcheckJob = this.spellcheckJob;
      if (spellcheckJob != null) {
         spellcheckJob.cancel();
      }

      final var loc = textFileBuffer.getLocation();
      spellcheckJob = this.spellcheckJob = new Job("Spellchecking" + (loc == null ? "" : " [" + loc + "]") + "...") {
         @Override
         protected IStatus run(final IProgressMonitor monitor) {
            final var annotationModel = textFileBuffer.getAnnotationModel();
            if (annotationModel != null) {
               final var regionsToSpellcheck = collectRegionsToSpellcheck(docModel, event.ranges);
               spellcheck(doc, regionsToSpellcheck, annotationModel, monitor);
            }
            return Status.OK_STATUS;
         }
      };
      spellcheckJob.setPriority(Job.DECORATE);
      spellcheckJob.schedule(2_000);
   }

   private void spellcheck(final IDocument doc, final List<Region> regionsToCheck, final IAnnotationModel annotationModel,
      final IProgressMonitor monitor) {
      SPELLING_SERVICE.check( //
         doc, //
         regionsToCheck.toArray(new Region[regionsToCheck.size()]), //
         new SpellingContext(), //
         new ISpellingProblemCollector() {
            private Map<SpellingAnnotation, Position> newSpellingErrors = new HashMap<>();

            @Override
            public void accept(final SpellingProblem problem) {
               newSpellingErrors.put(new SpellingAnnotation(problem), new Position(problem.getOffset(), problem.getLength()));
            }

            @Override
            public void beginCollecting() {
            }

            @Override
            public void endCollecting() {
               final var outdatedAnnotations = new HashSet<Annotation>();
               annotationModel.getAnnotationIterator().forEachRemaining(anno -> {
                  if (SpellingAnnotation.TYPE.equals(anno.getType())) {
                     final var pos = annotationModel.getPosition(anno);
                     final var annoStart = pos.getOffset();
                     final var annoEnd = pos.getOffset() + pos.length;
                     for (final var region : regionsToCheck) {
                        if (annoStart >= region.getOffset() && annoEnd <= region.getOffset() + region.getLength()) {
                           outdatedAnnotations.add(anno);
                           break;
                        }
                     }
                  }
               });

               if (annotationModel instanceof final IAnnotationModelExtension annotationModelExt) {
                  annotationModelExt.replaceAnnotations( //
                     outdatedAnnotations.toArray(new SpellingAnnotation[outdatedAnnotations.size()]), //
                     newSpellingErrors //
                  );
               } else {
                  outdatedAnnotations.forEach(annotationModel::removeAnnotation);
                  newSpellingErrors.forEach(annotationModel::addAnnotation);
               }
            }
         }, monitor);
   }

   @Override
   public void uninstall() {
      super.uninstall();
      if (viewer != null) {
         viewer.removeTextInputListener(this);
         viewer = null;
      }
   }
}
