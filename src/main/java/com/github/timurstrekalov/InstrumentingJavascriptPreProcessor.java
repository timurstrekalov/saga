package com.github.timurstrekalov;

import com.gargoylesoftware.htmlunit.ScriptPreProcessor;
import com.gargoylesoftware.htmlunit.html.HtmlElement;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.collect.*;
import net.sourceforge.htmlunit.corejs.javascript.Parser;
import net.sourceforge.htmlunit.corejs.javascript.ast.*;
import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import static net.sourceforge.htmlunit.corejs.javascript.Token.*;

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

        try {
            IOUtils.write(buf, new FileOutputStream(new File(sourceName).getName() + "-instrumented.js"));
        } catch (final IOException e) {
            throw new RuntimeException(e);
        }

        return buf.toString();
    }

    private static AstNode newInstrumentationNode(final int lineNr) {
        return (AstNode) (new Parser().parse(String.format("_COV[%d]++;", lineNr), "injected", 0).getFirstChild());
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
                addInstrumentationSnippetFor(node);
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

            return type == SWITCH
                    || type == FOR
                    || type == DO
                    || type == WHILE
                    || type == CONTINUE
                    || type == BREAK
                    || type == TRY
                    || type == THROW
                    || type == CASE
                    || type == IF
                    || type == EXPR_RESULT
                    || type == EXPR_VOID
                    || type == RETURN
                    || (type == FUNCTION && (parentType == SCRIPT || parentType == BLOCK))
                    || (type == VAR && node.getClass() == VariableDeclaration.class && parentType != FOR);
        }

        private void addInstrumentationSnippetFor(final AstNode node) {
            final AstNode parent = node.getParent();

            final int type = node.getType();
            final int parentType = parent.getType();

            if (type == CASE) {
                // 'switch' statement cases are special in the sense that their children are not actually their children,
                // meaning the children have a reference to their parent, but the parent only has a List of all its
                // children, so we can't just addChildBefore() like we do for all other cases.
                //
                // They do, however, retain a list of all statements per each case.
                final SwitchCase switchCase = (SwitchCase) node;
                final List<AstNode> newStatements = Lists.newArrayList();

                for (final AstNode statement : switchCase.getStatements()) {
                    final int lineNr = statement.getLineno();
                    executableLines.put(lineNr, node.getLength());

                    newStatements.add(newInstrumentationNode(lineNr));
                    newStatements.add(statement);
                }

                switchCase.setStatements(newStatements);
            } else if (type == IF && parentType == IF) {
                flattenElseIf((IfStatement) node, (IfStatement) parent);
            } else {
                if (parentType != CASE) {
                    final int lineNr = node.getLineno();

                    executableLines.put(lineNr, node.getLength());
                    parent.addChildBefore(newInstrumentationNode(lineNr), node);
                }
            }
        }

        private void flattenElseIf(final IfStatement elseIfStatement, final IfStatement ifStatement) {
            final Scope scope = new Scope();
            scope.addChild(elseIfStatement);

            ifStatement.setElsePart(scope);

            final int lineNr = elseIfStatement.getLineno();

            executableLines.put(lineNr, elseIfStatement.getLength());
            scope.addChildBefore(newInstrumentationNode(lineNr), elseIfStatement);
        }

    }

}
