package ca.nevdull.cob.compiler;

// Produce the target language code to the class implementation file 

import java.io.FileNotFoundException;
import java.util.ArrayDeque;

import org.antlr.v4.runtime.tree.TerminalNode;

public class CodePass extends PassCommon {
	ArrayDeque<String> klassNest = new ArrayDeque<String>();
	
	public CodePass(PassData data) {
		super(data);
	}

	@Override public Void visitFile(CobParser.FileContext ctx) {
		visitKlass(ctx.klass());
		return null;
	}

	@Override public Void visitKlass(CobParser.KlassContext ctx) {
		String name = ctx.name.getText();
		klassNest.addLast(name);
		try {
			passData.implStream = passData.openFileStream(name, ".c");
		} catch (FileNotFoundException excp) {
			Main.error("Unable to open implementations stream "+excp.getMessage());
		}
		writeImpl("// Generated at ",passData.timeStamp,"\n");
		writeImpl("// From ",passData.sourceFileName,"\n");
		writeImpl("#include \"",name,".h\"\n");
		for (CobParser.MemberContext decl : ctx.member()) {
			visit(decl);
		}
		klassNest.removeLast();
		return null;
	}
	
	@Override public Void visitMethod(CobParser.MethodContext ctx) {
		//	'static'? type ID '(' arguments? ')' '{' code '}'
		String className = klassNest.getLast();
		CobParser.TypeContext type = ctx.type();
		String typeName = type.typeName().getText();
		String array = "";
		if (type.getChildCount() > 1) array = "*";
		TerminalNode id = ctx.ID();
		String sep = "";
		if (ctx.stat == null) {
			writeImpl("static ",typeName," ",array,className,"_",id.getText(),"(",className," this");
			sep = ",";
		} else {
			writeImpl(typeName," ",array,className,"_",id.getText(),"(");
		}
		CobParser.ArgumentsContext arguments = ctx.arguments();
		if (arguments != null) {
			for (CobParser.ArgumentContext argument : arguments.argument()) {
				writeImpl(sep);  sep = ",";
				visit(argument);
			}
		}
		writeImpl(")");
		visit(ctx.compoundStatement());
		writeImpl("\n");
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
		if (type.getChildCount() > 1) array = "*";
		writeImpl(typeName," ",array,ctx.ID().getText());
		return null;
	}

    @Override public Void visitNamePrimary(CobParser.NamePrimaryContext ctx) {
    	String id = ctx.ID().getText();
    	Symbol sym = ctx.refScope.find(id);
    	if (sym == null) {
    		Main.error(ctx.ID(),id+" is not defined");
    		return null;
    	}
    	ctx.tipe = sym.type;
    	Scope scope = sym.getScope();
    	System.out.print(sym);
    	if (sym.isStatic()) System.out.print(" static");
    	System.out.print(" scope=");  System.out.print(scope);
    	System.out.println();
    	if (sym instanceof ClassSymbol) {
			writeImpl(id);    		    		
    	} else if (sym instanceof MethodSymbol) {
    		if (sym.isStatic()) {
    			assert scope instanceof ClassSymbol;
    			writeImpl(((ClassSymbol)scope).getName(),"_",id);    		
    		} else {
    			writeImpl("(*(this->class.methods.",id,"))");
    		}
    	} else if (sym instanceof VariableSymbol) {
    		if (sym.isStatic()) {
    			assert scope instanceof ClassSymbol;
    			writeImpl(((ClassSymbol)scope).getName(),"_",id);    		
    		} else if (scope instanceof ClassSymbol) {
    			writeImpl("(this->fields.",id,")");
    		} else {
    			assert scope instanceof LocalScope;
    			writeImpl(id);
    		}
    	} else {
    		Main.error(ctx.ID(),id+" is not recognized ("+sym.getClass().getSimpleName()+")");
    	}
        return null;
    }

    @Override public Void visitThisPrimary(CobParser.ThisPrimaryContext ctx) {
        writeImpl("this");
        return null;
    }

    @Override public Void visitIntegerPrimary(CobParser.IntegerPrimaryContext ctx) {
    	String t = ctx.start.getText();
        visitChildren(ctx);
        return null;
    }

    @Override public Void visitFloatingPrimary(CobParser.FloatingPrimaryContext ctx) {
    	String t = ctx.start.getText();
        visitChildren(ctx);
        return null;
    }

    @Override public Void visitStringPrimary(CobParser.StringPrimaryContext ctx) {
        visitChildren(ctx);
        return null;
    }

    @Override public Void visitNullPrimary(CobParser.NullPrimaryContext ctx) {
        writeImpl("cob_null");
        return null;
    }

    @Override public Void visitTruePrimary(CobParser.TruePrimaryContext ctx) {
        writeImpl("cob_true");
        return null;
    }

    @Override public Void visitFalsePrimary(CobParser.FalsePrimaryContext ctx) {
        writeImpl("cob_false");
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
    	writeImpl("(");
    	visit(ctx.primary());
    	writeImpl(")->methods.",ctx.ID().getText(),"(<obj>");  //TODO
    	CobParser.ExpressionListContext args = ctx.expressionList();
    	if (args != null) {
    		visit(args);
    	}
		writeImpl(")");
        visitChildren(ctx);
        return null;
    }

    @Override public Void visitMemberPrimary(CobParser.MemberPrimaryContext ctx) {
    	writeImpl("(");
    	visit(ctx.primary());
    	writeImpl(")->fields.",ctx.ID().getText());
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
    	writeImpl("{\n");
    	if (ctx.getParent() instanceof CobParser.MethodContext) {
    		writeImpl(" COB_SOURCE_FILE(\"",passData.sourceFileName,"\")\n");
    	}
    	for (CobParser.BlockItemContext item : ctx.blockItem()) {
    		visit(item);
    	}
		writeImpl(" COB_SOURCE_LINE(",Integer.toString(ctx.stop.getLine()),")\n");
        writeImpl("}");
        return null;
    }

    @Override public Void visitBlockItem(CobParser.BlockItemContext ctx) {
        visitChildren(ctx);
        writeImpl("\n");
        return null;
    }

    @Override public Void visitDeclaration(CobParser.DeclarationContext ctx) {
        visitChildren(ctx);
        return null;
    }

	private void writeSourceLine(CobParser.StatementContext ctx) {
		if (ctx.getParent() instanceof CobParser.BlockItemContext) {
    		writeImpl(" COB_SOURCE_LINE(",Integer.toString(ctx.start.getLine()),")\n");
    	}
	}

    @Override public Void visitLabelStatement(CobParser.LabelStatementContext ctx) {
        visitChildren(ctx);
        return null;
    }

    @Override public Void visitCmpdStatement(CobParser.CmpdStatementContext ctx) {
        visitChildren(ctx);
        return null;
    }

    @Override public Void visitExpressionStatement(CobParser.ExpressionStatementContext ctx) {
    	writeSourceLine(ctx);
        visitChildren(ctx);
        return null;
    }

    @Override public Void visitIfStatement(CobParser.IfStatementContext ctx) {
    	writeSourceLine(ctx);
        visitChildren(ctx);
        return null;
    }

    @Override public Void visitSwitchStatement(CobParser.SwitchStatementContext ctx) {
    	writeSourceLine(ctx);
        visitChildren(ctx);
        return null;
    }

    @Override public Void visitWhileStatement(CobParser.WhileStatementContext ctx) {
    	writeSourceLine(ctx);
        visitChildren(ctx);
        return null;
    }

    @Override public Void visitDoStatement(CobParser.DoStatementContext ctx) {
    	writeSourceLine(ctx);
        visitChildren(ctx);
        return null;
    }

    @Override public Void visitForStatement(CobParser.ForStatementContext ctx) {
    	writeSourceLine(ctx);
        visitChildren(ctx);
        return null;
    }

    @Override public Void visitForDeclStatement(CobParser.ForDeclStatementContext ctx) {
    	writeSourceLine(ctx);
        visitChildren(ctx);
        return null;
    }

    @Override public Void visitContinueStatement(CobParser.ContinueStatementContext ctx) {
    	writeSourceLine(ctx);
        visitChildren(ctx);
        return null;
    }

    @Override public Void visitBreakStatement(CobParser.BreakStatementContext ctx) {
    	writeSourceLine(ctx);
        visitChildren(ctx);
        return null;
    }

    @Override public Void visitReturnStatement(CobParser.ReturnStatementContext ctx) {
    	writeSourceLine(ctx);
    	writeImpl(" return ");
    	CobParser.SequenceContext seq = ctx.sequence();
    	if (seq != null) {
    		visitSequence(seq);
    	}
        return null;
    }

    @Override public Void visitSwitchItem(CobParser.SwitchItemContext ctx) {
        visitChildren(ctx);
        return null;
    }	
	
	@Override public Void visitTerminal(TerminalNode t) {
		writeImpl(" ",t.getSymbol().getText());
		return null;
	}
	
}
