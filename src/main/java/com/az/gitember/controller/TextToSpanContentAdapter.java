package com.az.gitember.controller;

import com.az.gitember.controller.lang.BaseTokenTypeAdapter;
import com.az.gitember.controller.lang.LangResolver;
import com.az.gitember.data.Pair;
import com.az.gitember.service.GitemberUtil;
import javafx.scene.Node;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.Token;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.jgit.diff.Edit;
import org.eclipse.jgit.diff.EditList;
import org.fxmisc.richtext.model.StyleSpans;
import org.fxmisc.richtext.model.StyleSpansBuilder;

import java.io.BufferedReader;
import java.io.StringReader;
import java.util.*;
import java.util.stream.Collectors;


/**
 * Adapt to Text for TextFlow.
 */
public class TextToSpanContentAdapter {

    public static final double FONT_SIZE = 20;
    public static final double FONT_SYMBOL_WIDTH = 11.99;
    public static final double ROW_HEIGHT = FONT_SIZE + 4;

    private boolean rawDiff = false;
    private EditList patch = null;
    private boolean leftSide;
    private final ArrayList<String> lines;
    private final LangResolver langResolver;
    private final List<Token> parsedCode;
    private final String content;

    /**
     * @param extension The file extension
     * @param patch     patch
     * @param leftSide  what side ro read left - old, right - new
     */
    public TextToSpanContentAdapter(final String content, final String extension, final EditList patch, boolean leftSide) {
        this(content, extension, false);
        this.patch = patch;
        this.leftSide = leftSide;
    }

    TextToSpanContentAdapter(final String contentRaw, final String extension, final boolean rawDiff) {
        content = contentRaw;
        this.langResolver = new LangResolver(extension, content);
        this.rawDiff = rawDiff;
        this.lines = getLines(content);

        final CommonTokenStream commonTokenStream = new CommonTokenStream(langResolver.getLexer());
        commonTokenStream.fill();
        parsedCode = commonTokenStream.getTokens();

    }

    public StyleSpans<Collection<String>> computeHighlighting() {

        final StyleSpansBuilder<Collection<String>> spansBuilder
                = new StyleSpansBuilder<>();
        int lastKwEnd = 0;

        final BaseTokenTypeAdapter adapter = langResolver.getAdapter();


        final Iterator<Token> tokenIterator = parsedCode.iterator();
        Token token = tokenIterator.next();
        for (int i = 0; i < content.length(); i++) {
            int startIdx = token.getStartIndex();
            int stopIdx = token.getStopIndex() + 1;

            if (i == token.getStartIndex()) {
                int len = startIdx - lastKwEnd;
                spansBuilder.add(Collections.emptyList(), len);

                final String style = adapter.adaptToStyleClass(token.getType());
                spansBuilder.add(Collections.singletonList(style), stopIdx - startIdx);
                lastKwEnd = stopIdx;
                token = tokenIterator.next();

                //System.out.println(">>> " + startIdx + " " + stopIdx + "   " + (stopIdx - startIdx) + "   [" + token.getText() + "] real [" + content.substring(startIdx, stopIdx)  + "] " + style);
            }
        }

        StyleSpans<Collection<String>> rez = spansBuilder.create();
        return rez;

    }


    private Map<Integer, List<String>> diffDecoration = null;
    private Map<Integer, List<String>> pathcDecoration = null;

    public Map<Integer, List<String>> getDiffDecoration() {
        if (diffDecoration == null) {
            if (rawDiff) {
                diffDecoration = new HashMap<>();
                for (int lineIdx = 0; lineIdx < lines.size(); lineIdx++) {
                    final String line = lines.get(lineIdx);
                    final String style;
                    if (line.startsWith("+")) {
                        style = "diff-new";
                    } else if (line.startsWith("-")) {
                        style = "diff-deleted";
                    } else if (line.startsWith("@@")) {
                        style = "diff-modified";
                    } else {
                        style = null;
                    }
                    if (style != null) {
                        diffDecoration.put(lineIdx, Collections.singletonList(style));
                    }
                }
            } else {
                diffDecoration = Collections.EMPTY_MAP;
            }
        }
        return  diffDecoration;
    }

    /**
     * Highlight new , deleted and changed lines by patch.
     */
    private Map<Integer, List<String>> getDecorateByPatch() {
        if (pathcDecoration == null) {

            if (patch == null) {
                pathcDecoration = Collections.EMPTY_MAP;
            } else {
                pathcDecoration = new HashMap<>();
                for (Edit delta : patch) {
                    int origPos = delta.getBeginB();
                    int origLines = delta.getLengthB();
                    String styleClass = GitemberUtil.getDiffSyleClass(delta, "diff-line");
                    if (leftSide) {
                        origPos = delta.getBeginA();
                        origLines = delta.getLengthA();
                    }

                    for (int line = origPos; line < (origLines + origPos); line++) {
                        pathcDecoration.put(line, Collections.singletonList(styleClass));
                    }
                }

            }

        }

        return pathcDecoration;

    }


    private ArrayList<String> getLines(final String content) {
        return (ArrayList<String>) new BufferedReader(new StringReader(content))
                .lines()
                .collect(Collectors.toList());
    }


}
