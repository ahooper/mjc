package ca.nevdull.cob.compiler;

// Produce the target language code to the class implementation file 

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayDeque;
import java.util.Map.Entry;

import org.antlr.v4.runtime.tree.TerminalNode;

public class CodePass extends PassCommon {
	ArrayDeque<String> klassNest = new ArrayDeque<String>();
	private boolean trace;
	
	public CodePass(PassData data) {
		super(data);
    	trace = passData.main.trace.contains("CodePass");
	}

	@Override public Void visitFile(CobParser.FileContext ctx) {
		visitKlass(ctx.klass());
		return null;
	}

	@Override public Void visitKlass(CobParser.KlassContext ctx) {
		String className = ctx.name.getText();
		klassNest.addLast(className);
		try {
			passData.implStream = passData.openFileStream(className, Main.IMPL_SUFFIX);
		} catch (FileNotFoundException excp) {
			Main.error("Unable to open implementations stream "+excp.getMessage());
		}
		writeImpl("// Generated at ",passData.timeStamp,"\n");
		writeImpl("// From ",passData.sourceFileName,"\n");
		writeImpl("#include \"",className,Main.DEFN_SUFFIX,"\"\n");
		for (CobParser.MemberContext member : ctx.member()) {
			visit(member);
		}
		
		ClassSymbol defn = ctx.defn;
		ClassSymbol base = defn.getBase();
		String baseName = (base != null) ? base.getName() : null;
		if (defn.findMember(className) == null) {
			//TODO this should go in DefinitionsPass, and define an actual constructor node
			// define a default constructor
			writeImpl("void ",className,"_",className,"() {\n");
			if (base != null) {
				writeImpl(" ",baseName,"_",baseName,"();\n");
			}
			writeImpl("}\n");
		}
		doInitializers(ctx, className, false);
		doInitializers(ctx, className, true);
		
		// Define the Dispatch structure
		
		writeImpl("struct ",className,"_Dispatch ",className,"_Dispatch={\n");
		writeImpl("  .init={\n");
		writeImpl("    .classInitialized=0,\n");
		writeImpl("    .className=\"",className,"\",\n");
		writeImpl("  //.packageName=\n");
		writeImpl("  //.enclosingClassName=\n");
		writeImpl("  //.enclosingMethodName=\n");
		writeImpl("    .instanceSize=sizeof(struct ",className,"_Object),\n");
		writeImpl("  //.classClass=&",className,"_Class,\n");
		if (base != null) {
			writeImpl("  //.baseClass=&",baseName,"_Class,\n");
		} else {
			writeImpl("  //.baseClass=0,\n");
		}
		writeImpl("  //.arrayClass=,\n");
		writeImpl("  },\n");
		writeImpl("};\n");
		
		// Define the class initializer, that completes the Dispatch structure, and
		// then calls the class initialization
		
		writeImpl("void ",PassCommon.INIT,"_",className,"() {\n");
		writeImpl(" //LATER lock if multi-threads\n");
		writeImpl(" int initBegan = ",className,"_Dispatch.init.classInitializationBegan;\n");
		writeImpl(" // Whether or not class initialization had already began, it has begun now\n");
		writeImpl(" ",className,"_Dispatch.init.classInitializationBegan = 1;\n");
		writeImpl(" //LATER unlock if multi-threads\n");
		writeImpl(" if (initBegan) {\n");
		writeImpl("  //LATER busy wait until initialized\n");
		writeImpl(" } else {\n");
		if (base != null) {
			writeImpl("  COB_CLASS_INIT(",baseName,")\n");
			writeImpl("  memcpy(((void *)&",className,"_Dispatch)+sizeof(",className,"_Dispatch.init),",
					           "((void *)&",baseName,"_Dispatch)+sizeof(",baseName,"_Dispatch.init),",
					           "sizeof(",baseName,"_Dispatch)-sizeof(",baseName,"_Dispatch.init));\n");
		}
		for (Symbol member : defn.members.values()) {
			if (member instanceof MethodSymbol && !member.isStatic()) {
				MethodSymbol meth = (MethodSymbol)member;
				String methName = meth.getName();
				writeImpl("  ",className,"_Dispatch.",methName,"=&",className,"_",methName,";\n");
			}
		}
		writeImpl("  ",className,"__classinit_();\n");
		writeImpl("  ",className,"_Dispatch.init.classInitialized = 1;\n");
		writeImpl(" }\n");
		writeImpl("}\n");

		klassNest.removeLast();
		
		// Save symbols for import
        
		try {
			if (defn.getAutoImport()) {
				
			} else {
				PrintWriter impWriter = passData.openFileWriter(defn.getName(),Main.IMPORT_SUFFIX);
				impWriter.append("// Generated at ").append(passData.timeStamp).append("\n");
				impWriter.append("// From ").append(passData.sourceFileName).append("\n");
				for (Entry<String, Symbol> globEnt : passData.globals.getMembers().entrySet()) {
					Symbol globSym = globEnt.getValue();
					if (globSym == defn) continue;
					if (globSym instanceof ClassSymbol && ((ClassSymbol)globSym).getAutoImport()) continue;
					impWriter.append("import ").append(globSym.getName()).append(";\n");
				}
		        defn.writeImport(impWriter);
		        impWriter.close();
			}
		} catch (IOException excp) {
			Main.error("Unable to write import symbols "+excp.getMessage());
		}
		
		return null;
	}

	private void doInitializers(CobParser.KlassContext ctx, String name, boolean statics) {
		String method = statics ? PassCommon.CLASSINIT : PassCommon.INSTANCEINIT;
		writeImpl("void ",name,"_",method,"(");		
		if (!statics) writeImpl(name," this");
		writeImpl(") {\n");
		writeImpl(" COB_ENTER_METHOD(",name,",\"",method,"\")\n");
		writeImpl(" COB_SOURCE_FILE(\"",passData.sourceFileName,"\")\n");
		if (!statics) writeImpl(" COB_CLASS_INIT(",klassNest.getLast(),")\n");
		for (CobParser.MemberContext member : ctx.member()) {
			if (member instanceof CobParser.InitializerContext) {
				CobParser.InitializerContext init = (CobParser.InitializerContext)member;
				if ((init.stat != null) ^ statics) {
				} else {
					visitCompoundStatement(init.compoundStatement());
				}
			} else if (member instanceof CobParser.FieldListContext) {
				CobParser.FieldListContext list = (CobParser.FieldListContext)member;
				if ((list.stat == null) ^ statics) {
					for (CobParser.FieldContext field : list.field()) {
						CobParser.ExpressionContext expr = field.expression();
						if (expr != null) {
							writeImpl(" ",statics?(name+"_"):"this->fields.",field.ID().getText(),"=");
							visit(expr);
							writeImpl(";\n");
						}
					}
				}
			}
		}
		writeImpl("}\n");
	}
	
	@Override public Void visitMethod(CobParser.MethodContext ctx) {
		//	'static'? type ID '(' arguments? ')' compoundStatement
		String className = klassNest.getLast();
		Type type = ctx.type().tipe;
		TerminalNode id = ctx.ID();
		String sep = "";
		if (ctx.stat == null) {
			writeImpl("static ",type.getNameString()," ",type.getArrayString(),className,"_",id.getText(),"(",className," this");
			sep = ",";
		} else {
			writeImpl(type.getNameString()," ",type.getArrayString(),className,"_",id.getText(),"(");
		}
		CobParser.ArgumentsContext arguments = ctx.arguments();
		if (arguments != null) {
			for (CobParser.ArgumentContext argument : arguments.argument()) {
				writeImpl(sep);  sep = ",";
				visit(argument);
			}
		}
		writeImpl(")");
		visitBody(ctx.body());
		writeImpl("\n");
		return null;
	}
	
	@Override public Void visitConstructor(CobParser.ConstructorContext ctx) {
		//	ID '(' arguments? ')' compoundStatement
		String className = klassNest.getLast();
		TerminalNode id = ctx.ID();
		String sep = "";
		writeImpl("void ",className,"_",id.getText(),"(",className," this");
		CobParser.ArgumentsContext arguments = ctx.arguments();
		if (arguments != null) {
			for (CobParser.ArgumentContext argument : arguments.argument()) {
				writeImpl(sep);  sep = ",";
				visit(argument);
			}
		}
		writeImpl(")");
		visitBody(ctx.body());
		writeImpl("\n");
		sep = "";
		writeImpl(className," ",className,"_NEW(");
		if (arguments != null) {
			for (CobParser.ArgumentContext argument : arguments.argument()) {
				writeImpl(sep);  sep = ",";
				visit(argument);
			}
		}
		writeImpl(") {\n");
		writeImpl(" ",className," new=malloc(sizeof(struct ",className,"_Object));\n");
		writeImpl(" //LATER check allocation failure\n");
		writeImpl(" ",className,"_",id.getText(),"(new");
		sep = ",";
		if (arguments != null) {
			for (CobParser.ArgumentContext argument : arguments.argument()) {
				writeImpl(sep);  sep = ",";
				visit(argument);
			}
		}
		writeImpl(");\n");
		writeImpl("}\n");
		return null;
	}
	
	@Override public Void visitNativeMethod(CobParser.NativeMethodContext ctx) {
		//	'native' type ID '(' arguments? ')' ';'
		// produces nothing
		return null;
	}
	
	@Override public Void visitInitializer(CobParser.InitializerContext ctx) {
		assert false;  // visited in doInitializers
		return null;
	}
	
	@Override public Void visitFieldList(CobParser.FieldListContext ctx) {
		//	'static'? type ID ( '=' expression )? ( ',' ID ( '=' expression )? )* ';'
		return null;
	}
	
	@Override public Void visitField(CobParser.FieldContext ctx) {
		//	ID ( '=' expression )?
		return null;
	}
	
	@Override public Void visitArgument(CobParser.ArgumentContext ctx) {
		Type type = ctx.type().tipe;
		writeImpl(type.getNameString()," ",type.getArrayString(),ctx.ID().getText());
		return null;
	}
	
	@Override public Void visitBody(CobParser.BodyContext ctx) {
		CobParser.CompoundStatementContext cs = ctx.compoundStatement();
		if (cs != null) visitCompoundStatement(cs);
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
    	if (trace) Main.debug("%s%s scope=%s", sym, (sym.isStatic())?" static":"", scope);
    	if (sym instanceof ClassSymbol) {
			writeImpl(id);    		    		
    	} else if (sym instanceof MethodSymbol) {
    		if (sym.isStatic()) {
    			assert scope instanceof ClassSymbol;
    			writeImpl(((ClassSymbol)scope).getName(),"_",id);    		
    		} else {
    			writeImpl("(*(this->dispatch->",id,"))");
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

    @Override public Void visitNewPrimary(CobParser.NewPrimaryContext ctx) {
    	writeImpl(ctx.ID().getText(),"_NEW","(");
    	CobParser.ExpressionListContext args = ctx.expressionList();
    	if (args != null) {
    		visit(args);
    	}
		writeImpl(")");
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
    	writeImpl(")->dispatch->",ctx.ID().getText(),"($obj$");  //TODO
    	CobParser.ExpressionListContext args = ctx.expressionList();
    	if (args != null) {
    		visit(args);
    	}
		writeImpl(")");
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
    		MethodSymbol methodDefn = (MethodSymbol) ((CobParser.MethodContext)ctx.getParent()).defn;
    		writeImpl(" COB_ENTER_METHOD(",klassNest.getLast(),",\"",methodDefn.getName(),"\")\n");
    		if (methodDefn.isStatic()) writeImpl(" COB_CLASS_INIT(",klassNest.getLast(),")\n");
    		writeImpl(" COB_SOURCE_FILE(\"",passData.sourceFileName,"\")\n");
    	} else if (ctx.getParent() instanceof CobParser.ConstructorContext) {
    		MethodSymbol methodDefn = (MethodSymbol) ((CobParser.ConstructorContext)ctx.getParent()).defn;
    		assert methodDefn.isStatic();
    		writeImpl(" COB_ENTER_METHOD(",klassNest.getLast(),",\"",methodDefn.getName(),"\")\n");
    		writeImpl(" COB_CLASS_INIT(",klassNest.getLast(),")\n");
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
		CobParser.TypeContext typeCtx = ctx.type();
		String sep = "";
		for (CobParser.VariableContext var : ctx.variable()) {
			writeImpl(sep);
			visitVariable(var);
			sep = ",";
		}
        writeImpl(";");
        return null;
    }

    @Override public Void visitVariable(CobParser.VariableContext ctx) {
		CobParser.DeclarationContext list = (CobParser.DeclarationContext)ctx.getParent();
		Type type = list.type().tipe;
		writeImpl(" ",type.getNameString()," ",type.getArrayString(),ctx.ID().getText());
		CobParser.ExpressionContext expr = ctx.expression();
		if (expr != null) {
			writeImpl("=");
			visit(expr);
		}
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
    	writeImpl(";");
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
