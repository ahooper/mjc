package ca.nevdull.cob.compiler;

import org.antlr.v4.runtime.Parser;
import org.antlr.v4.runtime.tree.TerminalNode;

public class CodePass extends PassCommon {
	
	public CodePass(Main main, Parser parser, String outoutDir) {
		super(main, parser, outoutDir);
	}

	@Override public Void visitFile(CobParser.FileContext ctx) {
		visitKlass(ctx.klass());
		return null;
	}

	@Override public Void visitKlass(CobParser.KlassContext ctx) {
		String name = ctx.name.getText();
		out("#include \"",name,".h\"\n");
		for (CobParser.MemberContext decl : ctx.member()) {
			visit(decl);
		}
		return null;
	}
	
	@Override public Void visitMethod(CobParser.MethodContext ctx) {
		//	'static'? type ID '(' arguments? ')' '{' code '}'
		CobParser.KlassContext parent = (CobParser.KlassContext)ctx.getParent();
		String className = parent.name.getText();
		CobParser.TypeContext type = ctx.type();
		String typeName = type.typeName().getText();
		String array = "";
		if (type.getChildCount() > 1) array = "[]";
		TerminalNode id = ctx.ID();
		out("static ",typeName," ",className,"_",id.getText(),array,"(",className," this");
		CobParser.ArgumentsContext arguments = ctx.arguments();
		if (arguments != null) {
			String sep = ",";
			for (CobParser.ArgumentContext argument : arguments.argument()) {
				out(sep);  sep = ",";
				visit(argument);
			}
		}
		out(")");
		visit(ctx.compoundStatement());
		return null;
	}
	
	@Override public Void visitField(CobParser.FieldContext ctx) {
		//	'static'? type ID ( '=' code )? ( ',' ID ( '=' code )? )* ';'
		return null;
	}
	
	@Override public Void visitArgument(CobParser.ArgumentContext ctx) {
		CobParser.TypeContext type = ctx.type();
		String typeName = type.typeName().getText();
		String array = "";
		if (type.getChildCount() > 1) array = "[]";
		out(typeName," ",ctx.ID().getText(),array);
		return null;
	}

    @Override public Void visitNamePrimary(CobParser.NamePrimaryContext ctx) {
        out(" ",ctx.ID().getText());
        return null;
    }

    @Override public Void visitThisPrimary(CobParser.ThisPrimaryContext ctx) {
        out("this");
        return null;
    }

    @Override public Void visitNumberPrimary(CobParser.NumberPrimaryContext ctx) {
        visitChildren(ctx);
        return null;
    }

    @Override public Void visitStringPrimary(CobParser.StringPrimaryContext ctx) {
        visitChildren(ctx);
        return null;
    }

    @Override public Void visitParenPrimary(CobParser.ParenPrimaryContext ctx) {
        visitChildren(ctx);
        return null;
    }

    @Override public Void visitIndexPrimary(CobParser.IndexPrimaryContext ctx) {
        visitChildren(ctx);
        return null;
    }

    @Override public Void visitCallPrimary(CobParser.CallPrimaryContext ctx) {
        visitChildren(ctx);
        return null;
    }

    @Override public Void visitInvokePrimary(CobParser.InvokePrimaryContext ctx) {
    	out("(");
    	visit(ctx.primary());
    	out(")->",ctx.ID().getText(),"(<obj>");
    	CobParser.ExpressionListContext args = ctx.expressionList();
    	if (args != null) {
    		visit(args);
    	}
		out(")");
        visitChildren(ctx);
        return null;
    }

    @Override public Void visitMemberPrimary(CobParser.MemberPrimaryContext ctx) {
    	out("(");
    	visit(ctx.primary());
    	out(")->",ctx.ID().getText());
        return null;
    }

    @Override public Void visitIncrementPrimary(CobParser.IncrementPrimaryContext ctx) {
        visitChildren(ctx);
        return null;
    }

    @Override public Void visitDecrementPrimary(CobParser.DecrementPrimaryContext ctx) {
        visitChildren(ctx);
        return null;
    }

    @Override public Void visitExpressionList(CobParser.ExpressionListContext ctx) {
        visitChildren(ctx);
        return null;
    }

    @Override public Void visitPrimaryUnary(CobParser.PrimaryUnaryContext ctx) {
        visitChildren(ctx);
        return null;
    }

    @Override public Void visitIncrementUnary(CobParser.IncrementUnaryContext ctx) {
        visitChildren(ctx);
        return null;
    }

    @Override public Void visitDecrementUnary(CobParser.DecrementUnaryContext ctx) {
        visitChildren(ctx);
        return null;
    }

    @Override public Void visitOperatorUnary(CobParser.OperatorUnaryContext ctx) {
        visitChildren(ctx);
        return null;
    }

    @Override public Void visitUnaryCast(CobParser.UnaryCastContext ctx) {
        visitChildren(ctx);
        return null;
    }

    @Override public Void visitTypeCast(CobParser.TypeCastContext ctx) {
        visitChildren(ctx);
        return null;
    }

    @Override public Void visitCastExpression(CobParser.CastExpressionContext ctx) {
        visitChildren(ctx);
        return null;
    }

    @Override public Void visitMultiplyExpression(CobParser.MultiplyExpressionContext ctx) {
        visitChildren(ctx);
        return null;
    }

    @Override public Void visitAddExpression(CobParser.AddExpressionContext ctx) {
        visitChildren(ctx);
        return null;
    }

    @Override public Void visitShiftExpression(CobParser.ShiftExpressionContext ctx) {
        visitChildren(ctx);
        return null;
    }

    @Override public Void visitCompareExpression(CobParser.CompareExpressionContext ctx) {
        visitChildren(ctx);
        return null;
    }

    @Override public Void visitEqualExpression(CobParser.EqualExpressionContext ctx) {
        visitChildren(ctx);
        return null;
    }

    @Override public Void visitAndExpression(CobParser.AndExpressionContext ctx) {
        visitChildren(ctx);
        return null;
    }

    @Override public Void visitExclusiveExpression(CobParser.ExclusiveExpressionContext ctx) {
        visitChildren(ctx);
        return null;
    }

    @Override public Void visitOrExpression(CobParser.OrExpressionContext ctx) {
        visitChildren(ctx);
        return null;
    }

    @Override public Void visitAndThenExpression(CobParser.AndThenExpressionContext ctx) {
        visitChildren(ctx);
        return null;
    }

    @Override public Void visitOrElseExpression(CobParser.OrElseExpressionContext ctx) {
        visitChildren(ctx);
        return null;
    }

    @Override public Void visitConditionalExpression(CobParser.ConditionalExpressionContext ctx) {
        visitChildren(ctx);
        return null;
    }

    @Override public Void visitAssignment(CobParser.AssignmentContext ctx) {
        visitChildren(ctx);
        return null;
    }

    @Override public Void visitSequence(CobParser.SequenceContext ctx) {
        visitChildren(ctx);
        return null;
    }

    @Override public Void visitConstantExpression(CobParser.ConstantExpressionContext ctx) {
        visitChildren(ctx);
        return null;
    }

    @Override public Void visitCompoundStatement(CobParser.CompoundStatementContext ctx) {
    	out("{\n");
    	for (CobParser.BlockItemContext item : ctx.blockItem()) {
    		visit(item);
    	}
        out("}\n");
        return null;
    }

    @Override public Void visitBlockItem(CobParser.BlockItemContext ctx) {
        visitChildren(ctx);
        out("\n");
        return null;
    }

    @Override public Void visitDeclaration(CobParser.DeclarationContext ctx) {
        visitChildren(ctx);
        return null;
    }

    @Override public Void visitStatement(CobParser.StatementContext ctx) {
        visitChildren(ctx);
        return null;
    }

    @Override public Void visitSwitchItem(CobParser.SwitchItemContext ctx) {
        visitChildren(ctx);
        return null;
    }	
	
	@Override public Void visitTerminal(TerminalNode t) {
		out(" ",t.getSymbol().getText());
		return null;
	}
	
}
