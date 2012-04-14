package com.github.timurstrekalov;

import com.gargoylesoftware.htmlunit.*;
import com.gargoylesoftware.htmlunit.html.HtmlElement;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import com.gargoylesoftware.htmlunit.javascript.HtmlUnitContextFactory;
import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.collect.Collections2;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.sun.istack.internal.NotNull;
import net.sourceforge.htmlunit.corejs.javascript.CompilerEnvirons;
import net.sourceforge.htmlunit.corejs.javascript.Parser;
import net.sourceforge.htmlunit.corejs.javascript.Token;
import net.sourceforge.htmlunit.corejs.javascript.ast.*;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import static net.sourceforge.htmlunit.corejs.javascript.Token.*;

class ScriptInstrumenter implements ScriptPreProcessor {

    // hack, see http://sourceforge.net/tracker/?func=detail&atid=448266&aid=3106039&group_id=47038
    // still no build with that fix
    static {
        try {
            final Field field = AstNode.class.getDeclaredField("operatorNames");
            field.setAccessible(true);

            @SuppressWarnings("unchecked")
            final Map<Integer, String> operatorNames = (Map<Integer, String>) field.get(AstNode.class);
            operatorNames.put(Token.VOID, "void");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static final Map<String, ScriptData> instrumentedScriptCache = Maps.newConcurrentMap();

    private static final Logger logger = LoggerFactory.getLogger(ScriptInstrumenter.class);

    private final HtmlUnitContextFactory contextFactory;
    private final String coverageVariableName;
    private final String initializingCode;
    private final String arrayInitializer;

    private final List<ScriptData> scriptDataList = Lists.newLinkedList();

    private Collection<Pattern> ignorePatterns;
    private File outputDir;
    private boolean outputInstrumentedFiles;

    private boolean cacheInstrumentedCode;

    public ScriptInstrumenter(HtmlUnitContextFactory contextFactory, final String coverageVariableName) {
        this.contextFactory = contextFactory;
        this.coverageVariableName = coverageVariableName;

        initializingCode = String.format("%s = window.%s || {};%n", coverageVariableName, coverageVariableName);
        arrayInitializer = String.format("%s['%%s'][%%d] = 0;%n", coverageVariableName);
    }

    @Override
    public String preProcess(
            final HtmlPage htmlPage,
            final String sourceCode,
            final String sourceName,
            final int lineNumber,
            final HtmlElement htmlElement) {

        if (cacheInstrumentedCode && instrumentedScriptCache.containsKey(sourceName)) {
            final ScriptData data = instrumentedScriptCache.get(sourceName);
            scriptDataList.add(data);
            return data.getInstrumentedSourceCode();
        }

        if (shouldIgnore(sourceName)) {
            return sourceCode;
        }

        final ScriptData data = new ScriptData(sourceName, sourceCode);
        scriptDataList.add(data);

        final CompilerEnvirons environs = new CompilerEnvirons();
        environs.initFromContext(contextFactory.enterContext());

        final AstRoot root = new Parser(environs).parse(sourceCode, sourceName, lineNumber);
        root.visit(new InstrumentingVisitor(data));

        final String treeSource = root.toSource();
        final StringBuilder buf = new StringBuilder(
                initializingCode.length() +
                data.getNumberOfStatements() * arrayInitializer.length() +
                treeSource.length());

        buf.append(initializingCode);
        buf.append(String.format("%s['%s'] = {};%n", coverageVariableName, sourceName));

        for (final Integer i : data.getLineNumbersOfAllStatements()) {
            buf.append(String.format(arrayInitializer, data.getSourceName(), i));
        }

        buf.append(treeSource);

        final String instrumentedCode = buf.toString();
        data.setInstrumentedSourceCode(instrumentedCode);

        if (cacheInstrumentedCode) {
            instrumentedScriptCache.put(sourceName, data);
        }

        if (outputInstrumentedFiles) {
            try {
                final File outputFile = new File(outputDir, new File(sourceName).getName() + "-instrumented.js");
                IOUtils.write(instrumentedCode, new FileOutputStream(outputFile));
            } catch (final IOException e) {
                throw new RuntimeException(e);
            }
        }

        return instrumentedCode;
    }

    private boolean shouldIgnore(final String sourceName) {
        return ignorePatterns != null && Iterables.any(ignorePatterns, new Predicate<Pattern>() {
            @Override
            public boolean apply(final Pattern input) {
                return input.matcher(sourceName).matches();
            }
        });
    }

    public List<ScriptData> getScriptDataList() {
        return scriptDataList;
    }

    public void setIgnorePatterns(@NotNull final Collection<String> ignorePatterns) {
        this.ignorePatterns = Collections2.transform(ignorePatterns, new Function<String, Pattern>() {
            @Override
            public Pattern apply(final String input) {
                return Pattern.compile(input);
            }
        });
    }

    public void setOutputInstrumentedFiles(final boolean outputInstrumentedFiles) {
        this.outputInstrumentedFiles = outputInstrumentedFiles;
    }

    public void setOutputDir(File outputDir) {
        this.outputDir = outputDir;
    }

    public void setCacheInstrumentedCode(final boolean cacheInstrumentedCode) {
        this.cacheInstrumentedCode = cacheInstrumentedCode;
    }

    private class InstrumentingVisitor implements NodeVisitor {

        private final ScriptData data;

        public InstrumentingVisitor(final ScriptData data) {
            this.data = data;
        }

        @Override
        public boolean visit(final AstNode node) {
            handleVoidBug(node);
            handleNumberLiteralBug(node);

            if (isExecutableBlock(node)) {
                addInstrumentationSnippetFor(node);
            }

            return true;
        }

        /**
         * Even though we're hacking the AstNode class at the top, toSource() of 'void 0' nodes still returns
         * "void0" instead of "void 0". This is yet another hack to fix it (and yes, it was submitted with the
         * original patch to the issue above).
         */
        private void handleVoidBug(final AstNode node) {
            if (node.getType() == Token.VOID) {
                final AstNode operand = ((UnaryExpression) node).getOperand();
                if (operand.getType() == Token.NUMBER) {
                    final NumberLiteral numberLiteral = (NumberLiteral) operand;
                    numberLiteral.setValue(" " + Double.toString((numberLiteral.getNumber())));
                }
            }
        }

        /**
         * fix the fact that NumberLiteral outputs hexadecimal numbers without the '0x' part (e.g. 0xFF becomes FF
         * when toSource() is called), which results in invalid JS syntax. Using the actual number value instead
         * to fix this (shouldn't break anything)
         */
        private void handleNumberLiteralBug(final AstNode node) {
            if (node.getType() == Token.NUMBER) {
                final NumberLiteral numberLiteral = (NumberLiteral) node;
                numberLiteral.setValue(Double.toString(numberLiteral.getNumber()));

                handleVoidBug(node.getParent());
            }
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
                handleSwitchCase((SwitchCase) node);
            } else if (type == IF && parentType == IF) {
                flattenElseIf((IfStatement) node, (IfStatement) parent);
            } else if (parentType != CASE) {
                if (parent.hasChildren()) {
                    parent.addChildBefore(newInstrumentationNode(node.getLineno()), node);
                } else {
                    // if, else, while, do, for without {} around their respective 'blocks' for some reason
                    // don't have children. Meh. Creating blocks to ease instrumentation.
                    final Block block = newInstrumentedBlock(node);

                    if (parentType == IF) {
                        final IfStatement ifStatement = (IfStatement) parent;

                        if (ifStatement.getThenPart() == node) {
                            ifStatement.setThenPart(block);
                        } else if (ifStatement.getElsePart() == node) {
                            ifStatement.setElsePart(block);
                        }
                    } else if (parentType == WHILE || parentType == FOR || parentType == DO) {
                        ((Loop) parent).setBody(block);
                    } else {
                        logger.warn("Cannot handle node with parent that has no children, source:\n{}", parent.toSource());
                    }
                }
            }

            data.addExecutableLine(node.getLineno(), node.getLength());
        }

        private Block newInstrumentedBlock(final AstNode node) {
            final Block block = new Block();
            block.addChild(node);
            block.addChildBefore(newInstrumentationNode(node.getLineno()), node);
            return block;
        }

        /**
         * 'switch' statement cases are special in the sense that their children are not actually their children,
         * meaning the children have a reference to their parent, but the parent only has a List of all its
         * children, so we can't just addChildBefore() like we do for all other cases.
         *
         * They do, however, retain a list of all statements per each case, which we're using here
         */
        private void handleSwitchCase(final SwitchCase switchCase) {
            // empty case: statement
            if (switchCase.getStatements() == null) {
                return;
            }

            final List<AstNode> newStatements = Lists.newArrayList();

            for (final AstNode statement : switchCase.getStatements()) {
                final int lineNr = statement.getLineno();
                data.addExecutableLine(lineNr, switchCase.getLength());

                newStatements.add(newInstrumentationNode(lineNr));
                newStatements.add(statement);
            }

            switchCase.setStatements(newStatements);
        }

        /**
         * In order to make it possible to cover else-if blocks, we're flattening the shorthand else-if
         *
         * <pre>
         * {@literal
         * if (cond1) {
         *     doIf();
         * } else if (cond2) {
         *     doElseIf();
         * } else {
         *     doElse();
         * }
         * }
         * </pre>
         *
         * into
         *
         * <pre>
         * {@literal
         * if (cond1) {
         *     doIf();
         * } else {
         *     if (cond2) {
         *         doElseIf();
         *     } else {
         *         doElse();
         *     }
         * }
         * }
         * </pre>
         */
        private void flattenElseIf(final IfStatement elseIfStatement, final IfStatement ifStatement) {
            final Block block = new Block();
            block.addChild(elseIfStatement);

            ifStatement.setElsePart(block);

            final int lineNr = elseIfStatement.getLineno();

            data.addExecutableLine(lineNr, elseIfStatement.getLength());
            block.addChildBefore(newInstrumentationNode(lineNr), elseIfStatement);
        }

        private AstNode newInstrumentationNode(final int lineNr) {
            final ExpressionStatement instrumentationNode = new ExpressionStatement();
            final UnaryExpression inc = new UnaryExpression();

            inc.setIsPostfix(true);
            inc.setOperator(Token.INC);

            final ElementGet outer = new ElementGet();
            final ElementGet inner = new ElementGet();

            outer.setTarget(inner);

            final Name covDataVar = new Name();
            covDataVar.setIdentifier(coverageVariableName);

            inner.setTarget(covDataVar);

            final StringLiteral fileName = new StringLiteral();
            fileName.setValue(data.getSourceName());
            fileName.setQuoteCharacter('\'');

            inner.setElement(fileName);

            final NumberLiteral index = new NumberLiteral();
            index.setValue(Integer.toString(lineNr));

            outer.setElement(index);

            inc.setOperand(outer);

            instrumentationNode.setExpression(inc);
            instrumentationNode.setHasResult();

            return instrumentationNode;
        }

    }

}
