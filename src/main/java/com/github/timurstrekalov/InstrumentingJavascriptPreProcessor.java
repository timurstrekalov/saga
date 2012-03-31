package com.github.timurstrekalov;

import static net.sourceforge.htmlunit.corejs.javascript.Token.*;

import java.util.*;
import java.util.regex.Pattern;

import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.collect.Collections2;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.collect.Maps;
import net.sourceforge.htmlunit.corejs.javascript.Node;
import net.sourceforge.htmlunit.corejs.javascript.Parser;
import net.sourceforge.htmlunit.corejs.javascript.ast.AstNode;
import net.sourceforge.htmlunit.corejs.javascript.ast.AstRoot;
import net.sourceforge.htmlunit.corejs.javascript.ast.NodeVisitor;
import net.sourceforge.htmlunit.corejs.javascript.ast.VariableDeclaration;

import com.gargoylesoftware.htmlunit.ScriptPreProcessor;
import com.gargoylesoftware.htmlunit.html.HtmlElement;
import com.gargoylesoftware.htmlunit.html.HtmlPage;

class InstrumentingJavascriptPreProcessor implements ScriptPreProcessor {

    private static final String INITIALIZING_CODE = "_COV = {};";

    // %d denotes absolute position within the AST
    private static final String ARRAY_INITIALIZER = "_COV[%d] = 0;%n";

    private final Map<Integer, Integer> executableLines = Maps.newTreeMap();
    private final Map<String, String> sourceCodeMap = new HashMap<String, String>();

    private final List<Pattern> ignorePatterns;

    private boolean coverageObjectInitialized;

    public InstrumentingJavascriptPreProcessor(final List<String> ignorePatterns) {
        this.ignorePatterns = ImmutableList.copyOf(Collections2.transform(ignorePatterns, new Function<String, Pattern>() {
            @Override
            public Pattern apply(final String input) {
                return Pattern.compile(input);
            }
        }));
    }

    @Override
    public String preProcess(final HtmlPage htmlPage, final String sourceCode,
            final String sourceName, final int lineNumber, final HtmlElement htmlElement) {
        if (INITIALIZING_CODE.equals(sourceCode)) {
            return sourceCode;
        }

        if (!coverageObjectInitialized) {
            htmlPage.executeJavaScript(INITIALIZING_CODE);
            coverageObjectInitialized = true;
        }

        if (shouldIgnore(sourceName)) {
            return sourceCode;
        }

        sourceCodeMap.put(sourceName, sourceCode);

        final AstRoot root = new Parser().parse(sourceCode, sourceName, lineNumber);
        root.visit(new InstrumentingVisitor());

        final String tree = root.toSource();
        final StringBuilder buf = new StringBuilder(tree.length() + executableLines.size() * ARRAY_INITIALIZER.length());

        for (final Integer i : executableLines.keySet()) {
            buf.append(String.format(ARRAY_INITIALIZER, i));
        }

        buf.append(tree);

        System.out.println(buf);

        return buf.toString();
    }

    private boolean shouldIgnore(final String sourceName) {
        return Iterables.any(ignorePatterns, new Predicate<Pattern>() {
            @Override
            public boolean apply(final Pattern input) {
                return input.matcher(sourceName).matches();
            }
        });
    }

    public Map<String, String> getSourceCodeMap() {
        return sourceCodeMap;
    }

    public Map<Integer, Integer> getExecutableLines() {
        return executableLines;
    }

    private class InstrumentingVisitor implements NodeVisitor {

        @Override
        public boolean visit(final AstNode node) {
            if (isExecutableBlock(node)) {
                node.getParent().addChildBefore(newInstrumentationSnippetFor(node), node);
            }

            return true;
        }

        private boolean isExecutableBlock(final AstNode node) {
            final AstNode parent = node.getParent();
            if (parent == null) {
                return false;
            }

            final int type = node.getType();
            final int parentType = parent.getType();

            return (type == FUNCTION && (parentType == SCRIPT || parentType == BLOCK))
                    || (type == EXPR_RESULT || type == EXPR_VOID)
                    || (type == VAR && node.getClass() == VariableDeclaration.class);
        }

        private Node newInstrumentationSnippetFor(final AstNode node) {
            final int lineNr = node.getLineno();
            executableLines.put(lineNr, node.getLength());
            final String code = String.format("_COV[%d]++;%d", lineNr, node.getLength());
            return new Parser().parse(code, "injected", 0);
        }

    }

}
