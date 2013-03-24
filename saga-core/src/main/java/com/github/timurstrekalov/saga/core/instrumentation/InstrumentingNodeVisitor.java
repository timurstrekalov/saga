package com.github.timurstrekalov.saga.core.instrumentation;

import java.util.List;

import com.github.timurstrekalov.saga.core.cfg.Config;
import com.github.timurstrekalov.saga.core.model.ScriptData;
import com.google.common.collect.Lists;
import net.sourceforge.htmlunit.corejs.javascript.Token;
import net.sourceforge.htmlunit.corejs.javascript.ast.AstNode;
import net.sourceforge.htmlunit.corejs.javascript.ast.Block;
import net.sourceforge.htmlunit.corejs.javascript.ast.ElementGet;
import net.sourceforge.htmlunit.corejs.javascript.ast.ExpressionStatement;
import net.sourceforge.htmlunit.corejs.javascript.ast.IfStatement;
import net.sourceforge.htmlunit.corejs.javascript.ast.LabeledStatement;
import net.sourceforge.htmlunit.corejs.javascript.ast.Loop;
import net.sourceforge.htmlunit.corejs.javascript.ast.Name;
import net.sourceforge.htmlunit.corejs.javascript.ast.NodeVisitor;
import net.sourceforge.htmlunit.corejs.javascript.ast.NumberLiteral;
import net.sourceforge.htmlunit.corejs.javascript.ast.StringLiteral;
import net.sourceforge.htmlunit.corejs.javascript.ast.SwitchCase;
import net.sourceforge.htmlunit.corejs.javascript.ast.UnaryExpression;
import net.sourceforge.htmlunit.corejs.javascript.ast.VariableDeclaration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static net.sourceforge.htmlunit.corejs.javascript.Token.BLOCK;
import static net.sourceforge.htmlunit.corejs.javascript.Token.BREAK;
import static net.sourceforge.htmlunit.corejs.javascript.Token.CASE;
import static net.sourceforge.htmlunit.corejs.javascript.Token.CONTINUE;
import static net.sourceforge.htmlunit.corejs.javascript.Token.DO;
import static net.sourceforge.htmlunit.corejs.javascript.Token.EMPTY;
import static net.sourceforge.htmlunit.corejs.javascript.Token.EXPR_RESULT;
import static net.sourceforge.htmlunit.corejs.javascript.Token.EXPR_VOID;
import static net.sourceforge.htmlunit.corejs.javascript.Token.FOR;
import static net.sourceforge.htmlunit.corejs.javascript.Token.FUNCTION;
import static net.sourceforge.htmlunit.corejs.javascript.Token.IF;
import static net.sourceforge.htmlunit.corejs.javascript.Token.RETURN;
import static net.sourceforge.htmlunit.corejs.javascript.Token.SCRIPT;
import static net.sourceforge.htmlunit.corejs.javascript.Token.SWITCH;
import static net.sourceforge.htmlunit.corejs.javascript.Token.THROW;
import static net.sourceforge.htmlunit.corejs.javascript.Token.TRY;
import static net.sourceforge.htmlunit.corejs.javascript.Token.VAR;
import static net.sourceforge.htmlunit.corejs.javascript.Token.WHILE;

class InstrumentingNodeVisitor implements NodeVisitor {

    private static final Logger logger = LoggerFactory.getLogger(InstrumentingNodeVisitor.class);

    private final ScriptData data;
    private final int lineNumberOffset;

    public InstrumentingNodeVisitor(final ScriptData data, final int lineNumberOffset) {
        this.data = data;
        this.lineNumberOffset = lineNumberOffset;
    }

    @Override
    public boolean visit(final AstNode node) {
        handleNumberLiteralBug(node);

        if (isExecutableBlock(node)) {
            addInstrumentationSnippetFor(node);
        }

        return true;
    }

    /**
     * fix the fact that NumberLiteral outputs hexadecimal numbers without the '0x' part (e.g. 0xFF becomes FF
     * when toSource() is called), which results in invalid JS syntax. Using the actual number value instead
     * to fix this (shouldn't break anything)
     */
    private void handleNumberLiteralBug(final AstNode node) {
        if (node.getType() == Token.NUMBER) {
            final NumberLiteral numberLiteral = (NumberLiteral) node;
            numberLiteral.setValue(getValue(numberLiteral));
        }
    }

    private String getValue(final NumberLiteral literal) {
        if (Math.floor(literal.getNumber()) == literal.getNumber()) {
            return Long.toString((long) literal.getNumber());
        }

        return Double.toString(literal.getNumber());
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

        if (type == WHILE || type == FOR || type == DO) {
            fixLoops((Loop) node);
        }

        if (type == IF) {
            fixIf((IfStatement) node);
        }

        if (type == CASE) {
            handleSwitchCase((SwitchCase) node);
        } else if (type == IF && parentType == IF) {
            final IfStatement elseIfStatement = (IfStatement) node;
            final IfStatement ifStatement = (IfStatement) parent;

            if (ifStatement.getElsePart() == elseIfStatement) {
                flattenElseIf(elseIfStatement, ifStatement);
                data.addExecutableLine(getActualLineNumber(node));
            }
        } else if (parentType != CASE) {
            // issue #54
            if (parent.getClass() == LabeledStatement.class) {
                return;
            }

            if (parent.hasChildren()) {
                parent.addChildBefore(newInstrumentationNode(getActualLineNumber(node)), node);
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
                    logger.warn("Cannot handle node with parent that has no children, parent class: {}, parent source:\n{}",
                            parent.getClass(), parent.toSource());
                }
            }

            data.addExecutableLine(getActualLineNumber(node));
        }
    }

    /**
     * when loops contain only ';' as body or nothing at all (happens when they are minified),
     * certain things might go horribly wrong (like the jquery 1.4.2 case)
     */
    private void fixLoops(final Loop loop) {
        if (loop.getBody().getType() == EMPTY) {
            loop.setBody(new Block());
        }
    }

    /**
     * The same as loops (the if (true); case)
     * @see #fixLoops(net.sourceforge.htmlunit.corejs.javascript.ast.Loop)
     */
    private void fixIf(final IfStatement ifStatement) {
        if (ifStatement.getThenPart().getType() == EMPTY) {
            ifStatement.setThenPart(new Block());
        }
    }

    private int getActualLineNumber(final AstNode node) {
        return node.getLineno() - lineNumberOffset;
    }

    private Block newInstrumentedBlock(final AstNode node) {
        final Block block = new Block();
        block.addChild(node);
        block.addChildBefore(newInstrumentationNode(getActualLineNumber(node)), node);
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
            final int lineNr = getActualLineNumber(statement);
            data.addExecutableLine(lineNr);

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

        final int lineNr = getActualLineNumber(elseIfStatement);

        data.addExecutableLine(lineNr);
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
        covDataVar.setIdentifier(Config.COVERAGE_VARIABLE_NAME);

        inner.setTarget(covDataVar);

        final StringLiteral fileName = new StringLiteral();
        fileName.setValue(data.getSourceUriAsString());
        fileName.setQuoteCharacter('\'');

        inner.setElement(fileName);

        final NumberLiteral index = new NumberLiteral();
        index.setNumber(lineNr);
        index.setValue(Integer.toString(lineNr));

        outer.setElement(index);

        inc.setOperand(outer);

        instrumentationNode.setExpression(inc);
        instrumentationNode.setHasResult();

        return instrumentationNode;
    }

}
