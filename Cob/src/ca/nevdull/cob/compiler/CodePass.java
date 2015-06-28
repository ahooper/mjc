package ca.nevdull.cob.compiler;

// Produce the target language code to the class implementation file 

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayDeque;
import java.util.Map.Entry;

import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.TerminalNode;

import ca.nevdull.cob.compiler.CobParser.ExpressionContext;

public class CodePass extends PassCommon {
	ArrayDeque<String> klassNest = new ArrayDeque<String>();
	private boolean trace;
	private ClassSymbol stringType;
	private ClassSymbol objectType;
	
	public CodePass(PassData data) {
		super(data);
    	trace = passData.main.trace.contains("CodePass");
    	objectType = findClass("Object");
    	stringType = findClass("String");
	}

	private ClassSymbol findClass(String name) {
		Symbol sym = passData.globals.find(name);
		assert sym != null && sym instanceof ClassSymbol;
		return (ClassSymbol)sym;
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
			writeImpl("void ",className,"_",className,"(",className," this) {\n");
			writeImpl(" // default constructor\n");
			writeImpl(" COB_ENTER_METHOD(",className,",\"",className,"\")\n");
			if (base != null) {
				writeImpl(" ",baseName,"_",baseName,"((",baseName,")this);\n");
			}
			writeImpl("}\n");
			writeNew(className, className, null);
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
			// copy superclass dispatch prefix
			writeImpl("  memcpy(((void *)&",className,"_Dispatch)+sizeof(",className,"_Dispatch.init),\n",
					  "         ((void *)&",baseName,"_Dispatch)+sizeof(",baseName,"_Dispatch.init),\n",
					  "         sizeof(",baseName,"_Dispatch)-sizeof(",baseName,"_Dispatch.init));\n");
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
		writeImpl("void ",className,"_",id.getText(),"(",className," this");
		String sep = ",";
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
		writeNew(className, id.getText(), arguments);
		return null;
	}

	private void writeNew(String className, String constructorName,
			CobParser.ArgumentsContext arguments) {
		String sep = "";
		writeImpl(className," ",className,"_",PassCommon.NEW,"(");
		if (arguments != null) {
			for (CobParser.ArgumentContext argument : arguments.argument()) {
				writeImpl(sep);  sep = ",";
				visit(argument);
			}
		}
		writeImpl(") {\n");
		writeImpl(" ",className," new=malloc(sizeof(struct ",className,"_Object));\n");
		writeImpl(" //LATER check allocation failure\n");
		writeImpl(" ",className,"_",constructorName,"(new");
		sep = ",";
		if (arguments != null) {
			for (CobParser.ArgumentContext argument : arguments.argument()) {
				writeImpl(sep);  sep = ",";
				visit(argument);
			}
		}
		writeImpl(");\n");
		writeImpl("}\n");
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
    		ctx.tipe = UnknownType.make(ctx.ID().getSymbol());
    		ctx.exp = new Exp(id);
    		return null;
    	}
    	ctx.tipe = sym.type;
    	Scope scope = sym.getScope();
    	if (trace) Main.debug("%s%s scope=%s", sym, (sym.isStatic())?" static":"", scope);
    	if (sym instanceof ClassSymbol) {
    		ctx.exp = new Exp(id);    		    		
    	} else if (sym instanceof MethodSymbol) {
    		if (sym.isStatic()) {
    			assert scope instanceof ClassSymbol;
    			ctx.exp = new Exp(((ClassSymbol)scope).getName(),"_",id);    		
    		} else {
    			ctx.exp = new Exp("(*(this->dispatch->",id,"))");
    		}
    	} else if (sym instanceof VariableSymbol) {
    		if (sym.isStatic()) {
    			assert scope instanceof ClassSymbol;
    			ctx.exp = new Exp(((ClassSymbol)scope).getName(),"_",id);    		
    		} else if (scope instanceof ClassSymbol) {
    			ctx.exp = new Exp("(this->fields.",id,")");
    		} else {
    			assert scope instanceof LocalScope;
    			ctx.exp = new Exp(id);
    		}
    	} else {
    		Main.error(ctx.ID(),id+" is not recognized ("+sym.getClass().getSimpleName()+")");
    	}
        return null;
    }

    @Override public Void visitThisPrimary(CobParser.ThisPrimaryContext ctx) {
    	Symbol sym = ctx.refScope.find("this");
    	if (sym == null) {
    		Main.error(ctx.start,"'this' is not defined");
    		ctx.tipe = UnknownType.make(ctx.start);
    		ctx.exp = new Exp("this");
    		return null;
    	}
    	ctx.tipe = sym.type;
    	Scope scope = sym.getScope();
    	if (trace) Main.debug("%s%s scope=%s", sym, (sym.isStatic())?" static":"", scope);
    	assert sym instanceof VariableSymbol && !sym.isStatic();
    	ctx.exp = new Exp("this");
        return null;
    }

    @Override public Void visitIntegerPrimary(CobParser.IntegerPrimaryContext ctx) {
    	ctx.tipe = PrimitiveType.intType;
    	String t = ctx.start.getText();
    	if (t.endsWith("l") || t.endsWith("L")) ctx.tipe = PrimitiveType.longType;
    	ctx.exp = new Exp(t);
        return null;
    }

    @Override public Void visitFloatingPrimary(CobParser.FloatingPrimaryContext ctx) {
    	ctx.tipe = PrimitiveType.floatType;
    	String t = ctx.start.getText();
    	if (t.endsWith("l") || t.endsWith("L")) ctx.tipe = PrimitiveType.doubleType;
    	ctx.exp = new Exp(t);
        return null;
    }

    @Override public Void visitStringPrimary(CobParser.StringPrimaryContext ctx) {
    	ctx.tipe = stringType;
    	ctx.exp = new Exp(ctx.getText());
        return null;
    }

    @Override public Void visitNullPrimary(CobParser.NullPrimaryContext ctx) {
        ctx.tipe = objectType;
        ctx.exp = new Exp("cob_null");
        return null;
    }

    @Override public Void visitTruePrimary(CobParser.TruePrimaryContext ctx) {
    	ctx.tipe = PrimitiveType.booleanType;
    	ctx.exp = new Exp("cob_true");
        return null;
    }

    @Override public Void visitFalsePrimary(CobParser.FalsePrimaryContext ctx) {
    	ctx.tipe = PrimitiveType.booleanType;
    	ctx.exp = new Exp("cob_false");
        return null;
    }

    @Override public Void visitParenPrimary(CobParser.ParenPrimaryContext ctx) {
        CobParser.SequenceContext seq = ctx.sequence();
        visitSequence(seq);
    	ctx.tipe = seq.tipe;
    	ctx.exp = new Exp("(").add(seq.exp).add(")");
        return null;
    }

    @Override public Void visitNewPrimary(CobParser.NewPrimaryContext ctx) {
    	String id = ctx.ID().getText();
    	Symbol sym = ctx.refScope.find(id);
    	if (sym == null) {
    		Main.error(ctx.ID(),id+" is not defined");
    		ctx.tipe = UnknownType.make(ctx.ID().getSymbol());
    		ctx.exp = new Exp(id);
    		return null;
    	}
    	if (!(sym instanceof ClassSymbol)) {
    		Main.error(ctx.ID(),id+" is not a type");
    		ctx.tipe = UnknownType.make(ctx.ID().getSymbol());
    		ctx.exp = new Exp(id);
    		return null;
    	}
    	ctx.tipe = sym.type;
    	ctx.exp = new Exp(ctx.ID().getText(),"_",PassCommon.NEW,"(");
    	CobParser.ExpressionListContext args = ctx.expressionList();
    	if (args != null) {
    		visit(args);
    		ctx.exp.add(args.exp);
    	}
		ctx.exp.add(")");
        return null;
    }

    @Override public Void visitIndexPrimary(CobParser.IndexPrimaryContext ctx) {
    	CobParser.PrimaryContext primary = ctx.primary();
    	visit(primary);
    	Type type = primary.tipe;
    	Exp exp = primary.exp;
    	if (type instanceof ArrayType) {
	    	CobParser.SequenceContext seq = ctx.sequence();
	    	visit(seq);
	    	exp.add("[").add(seq.exp).add("]");
	    	ctx.tipe = ((ArrayType)type).getElementType();    		
    	} else {
    		Main.error(primary.stop,"array type expected");
    		ctx.tipe = UnknownType.make(primary.start);
    	}
    	ctx.exp = exp;
        return null;
    }

    @Override public Void visitCallPrimary(CobParser.CallPrimaryContext ctx) {
		CobParser.PrimaryContext primary = ctx.primary();
		if (   primary instanceof CobParser.NamePrimaryContext
			|| primary instanceof CobParser.ThisPrimaryContext) {
	        visit(primary);
	    	Exp exp = primary.exp;
        	exp.add("(");
        	CobParser.ExpressionListContext arguments = ctx.expressionList();
        	if (arguments != null) {
        		String sep = "";
        		//TODO compare types
        		visit(arguments);
    			for (CobParser.AssignmentContext argument : arguments.assignment()) {
    				visit(argument);
        	    	exp.add(sep).add(argument.exp);
        	    	sep = ",";
    			}
        	}
        	exp.add(")");
    		if (primary instanceof CobParser.NamePrimaryContext) {
    			ctx.tipe = ((CobParser.NamePrimaryContext)primary).tipe;
    		} else if (primary instanceof CobParser.ThisPrimaryContext) {
    			ctx.tipe = ((CobParser.ThisPrimaryContext)primary).tipe;
    		} else assert false;
			ctx.exp = exp;
		} else {
			Main.error(primary.stop,"Expecting name for call");
    		ctx.tipe = UnknownType.make(ctx.stop);
    		ctx.exp = primary.exp;
		}
        return null;
    }

    @Override public Void visitInvokePrimary(CobParser.InvokePrimaryContext ctx) {
		CobParser.PrimaryContext primary = ctx.primary();
        Token ID = ctx.ID().getSymbol();
        visit(primary);
    	Type type = primary.tipe;
    	Exp exp = null;  String close = null;
    	if (type instanceof ClassSymbol) {
    		Symbol sym = ((ClassSymbol)type).findMember(ID.getText());
    		if (sym != null) {
    			//TODO must be field
    			if (sym.isStatic()) {
    		    	exp = primary.exp;
    		    	exp.add("_",ID.getText(),"(");
    			} else {
    		    	exp = new Exp("(",type.getNameString()," ",type.getArrayString(),"_t=");
    		        exp.add(primary.exp);
    		    	exp.add(",_t->dispatch->").add(ctx.ID().getText()).add("(t");
    		    	close = "))";
    			}
		    	ctx.tipe = sym.getType();
    		} else {
    			Main.error(ID,ID.getText()+" is not a member of "+type);
        		exp = primary.exp.add(".").add(ID.getText());
    			ctx.tipe = UnknownType.make(ID);
    		}
        	CobParser.ExpressionListContext args = ctx.expressionList();
        	if (args != null) {
        		//TODO compare types
        		visit(args);
        	}
    		if (close != null) exp.add(close);
    	} else if (type instanceof UnknownType) {
    		Main.debug("selection from unknown\n");
    		exp = primary.exp.add(".").add(ID.getText());
			ctx.tipe = UnknownType.make(ID);
    	} else {
    		Main.error(ID,"selection subject is not a class");
printContextTree(primary, "    "); 
    		Main.debug("Symbol is %s\n", type.getClass().getSimpleName());
    		exp = primary.exp.add(".").add(ID.getText());
			ctx.tipe = UnknownType.make(ID);
    	}
    	ctx.exp = exp;
        return null;
    }

    @Override public Void visitMemberPrimary(CobParser.MemberPrimaryContext ctx) {
        CobParser.PrimaryContext primary = ctx.primary();
        Token ID = ctx.ID().getSymbol();
        visit(primary);
    	Type type = primary.tipe;
    	Exp exp = null;
    	if (type instanceof ClassSymbol) {
    		Symbol sym = ((ClassSymbol)type).findMember(ID.getText());
    		if (sym != null) {
    			//TODO must be field
    			if (sym.isStatic()) {
    		    	exp = primary.exp;
    		    	exp.add("_",ID.getText());
    			} else {
    		    	exp = new Exp("(");
    		        exp.add(primary.exp);
    		    	exp.add(")->fields.").add(ID.getText());
    			}
		    	ctx.tipe = sym.getType();
    		} else {
    			Main.error(ID,ID.getText()+" is not a member of "+type);
        		exp = primary.exp.add(".").add(ID.getText());
    			ctx.tipe = UnknownType.make(ID);
    		}
    	} else {
    		Main.error(ID,"selection subject is not a class");
printContextTree(primary, "    "); 
    		Main.debug("Symbol is %s\n", type.getClass().getSimpleName());
    		exp = primary.exp.add(".").add(ID.getText());
			ctx.tipe = UnknownType.make(ID);
    	}
    	ctx.exp = exp;
        return null;
    }

    @Override public Void visitIncrementPrimary(CobParser.IncrementPrimaryContext ctx) {
        CobParser.PrimaryContext primary = ctx.primary();
        visit(primary);
        Exp exp = primary.exp;
        exp.add(ctx.op.getText());
        ctx.exp = exp;
        ctx.tipe = primary.tipe;
        return null;
    }

    @Override public Void visitExpressionList(CobParser.ExpressionListContext ctx) {
        Exp exp = null;
    	for (CobParser.AssignmentContext e : ctx.assignment()) {
    		visit(e);
    		if (exp == null) exp = e.exp; else exp.add(",").add(e.exp);
    	}
    	ctx.exp = exp;
        return null;
    }

    @Override public Void visitPrimaryUnary(CobParser.PrimaryUnaryContext ctx) {
        CobParser.PrimaryContext primary = ctx.primary();
        visit(primary);
        ctx.exp = primary.exp;
        ctx.tipe = primary.tipe;
        return null;
    }

    @Override public Void visitIncrementUnary(CobParser.IncrementUnaryContext ctx) {
        Exp exp = new Exp(ctx.op.getText());
        CobParser.UnaryContext unary = ctx.unary();
        visit(unary);
        exp.add(unary.exp);
        ctx.exp = exp;
        ctx.tipe = unary.tipe;
        return null;
    }

    @Override public Void visitOperatorUnary(CobParser.OperatorUnaryContext ctx) {
        Exp exp = new Exp(ctx.op.getText());
        CobParser.CastContext cast = ctx.cast();
        visit(cast);
        exp.add(cast.exp);
        ctx.exp = exp;
        ctx.tipe = cast.tipe;
        return null;
    }

    @Override public Void visitUnaryCast(CobParser.UnaryCastContext ctx) {
        CobParser.UnaryContext unary = ctx.unary();
        visit(unary);
        ctx.exp = unary.exp;
        ctx.tipe = unary.tipe;
        return null;
    }

    @Override public Void visitTypeCast(CobParser.TypeCastContext ctx) {
        CobParser.TypeNameContext type = ctx.typeName();
        visitTypeName(type);
        Exp exp = new Exp("(",type.tipe.toString(),")");
        CobParser.CastContext cast = ctx.cast();
        visit(cast);
        exp.add(cast.exp);
        ctx.tipe = type.tipe;
        return null;
    }

    @Override public Void visitCastExpression(CobParser.CastExpressionContext ctx) {
        CobParser.CastContext cast = ctx.cast();
        visit(cast);
        ctx.exp = cast.exp;
        ctx.tipe = cast.tipe;
        return null;
    }

    @Override public Void visitMultiplyExpression(CobParser.MultiplyExpressionContext ctx) {
        ctx.exp = arithmenticBinary(ctx.l,ctx.op,ctx.r);
    	ctx.tipe = ctx.l.tipe;
        return null;
    }

    @Override public Void visitAddExpression(CobParser.AddExpressionContext ctx) {
        ctx.exp = arithmenticBinary(ctx.l,ctx.op,ctx.r);
    	ctx.tipe = ctx.l.tipe;
        return null;
    }

	private Exp arithmenticBinary(CobParser.ExpressionContext l, Token op,
			CobParser.ExpressionContext r) {
		visit(l);
        visit(r);
        if (l.tipe == r.tipe) {
        	//LATER allow conversions
        } else {
        	Main.error(op,"operand types must be the same "+l.tipe+":"+r.tipe);
        }
        Exp exp = l.exp;
        exp.add(op.getText());
        exp.add(r.exp);
        return exp;
	}

    @Override public Void visitShiftExpression(CobParser.ShiftExpressionContext ctx) {
		visit(ctx.l);
        visit(ctx.r);
        if (ctx.r.tipe == PrimitiveType.intType) {
        } else if (ctx.r.tipe instanceof UnknownType) {
        } else {
        	Main.error(ctx.op,"shift amount must be int not "+ctx.r.tipe);
        }
        Exp exp = ctx.l.exp;
        exp.add(ctx.op.getText());
        exp.add(ctx.r.exp);
        ctx.exp = exp;
        ctx.tipe = ctx.l.tipe;
        return null;
    }

    @Override public Void visitCompareExpression(CobParser.CompareExpressionContext ctx) {
		visit(ctx.l);
        visit(ctx.r);
        Exp exp = ctx.l.exp;
        exp.add(ctx.op.getText());
        exp.add(ctx.r.exp);
        ctx.exp = exp;
        if (ctx.l.tipe == ctx.r.tipe) {
        	//LATER allow conversions (requires to defer writing operands so conversions can be inserted)
        } else {
        	Main.error(ctx.op,"operand types must be the same");
        }
    	ctx.tipe = PrimitiveType.booleanType;
        return null;
    }

    @Override public Void visitEqualExpression(CobParser.EqualExpressionContext ctx) {
		visit(ctx.l);
        visit(ctx.r);
        Exp exp = ctx.l.exp;
        exp.add(ctx.op.getText());
        exp.add(ctx.r.exp);
        ctx.exp = exp;
        if (ctx.l.tipe == ctx.r.tipe) {
        	//LATER allow conversions (requires to defer writing operands so conversions can be inserted)
        } else {
        	Main.error(ctx.op,"operand types must be the same");
        }
        ctx.tipe = PrimitiveType.booleanType;
        return null;
    }

    @Override public Void visitAndExpression(CobParser.AndExpressionContext ctx) {
        ctx.exp = integerBinary(ctx.l,ctx.op,ctx.r);
//TODO ctx.tipe =
        return null;
    }

    @Override public Void visitExclusiveExpression(CobParser.ExclusiveExpressionContext ctx) {
        ctx.exp = integerBinary(ctx.l,ctx.op,ctx.r);
//TODO ctx.tipe =
        return null;
    }

	private Exp integerBinary(CobParser.ExpressionContext l, Token op,
			CobParser.ExpressionContext r) {
		visit(l);
        visit(r);
        if (l.tipe == r.tipe) {
        	//LATER allow conversions
        } else {
        	Main.error(op,"operand types must be the same");
        }
        Exp exp = l.exp;
        exp.add(op.getText());
        exp.add(r.exp);
        return exp;
	}

    @Override public Void visitOrExpression(CobParser.OrExpressionContext ctx) {
        ctx.exp = integerBinary(ctx.l,ctx.op,ctx.r);
//TODO ctx.tipe =
        return null;
    }

    @Override public Void visitAndThenExpression(CobParser.AndThenExpressionContext ctx) {
		visit(ctx.l);
        visit(ctx.r);
        Exp exp = ctx.l.exp;
        exp.add(ctx.op.getText());
        exp.add(ctx.r.exp);
        ctx.exp = exp;
//TODO ctx.tipe =
        return null;
    }

    @Override public Void visitOrElseExpression(CobParser.OrElseExpressionContext ctx) {
		visit(ctx.l);
        visit(ctx.r);
        Exp exp = ctx.l.exp;
        exp.add(ctx.op.getText());
        exp.add(ctx.r.exp);
        ctx.exp = exp;
//TODO ctx.tipe =
        return null;
    }

    @Override public Void visitConditionalExpression(CobParser.ConditionalExpressionContext ctx) {
		visit(ctx.c);
        visit(ctx.t);
        visit(ctx.f);
        Exp exp = ctx.c.exp;
        exp.add("?").add(ctx.t.exp);
        exp.add(":").add(ctx.f.exp);
        ctx.exp = exp;
//TODO ctx.tipe =
        return null;
    }

    @Override public Void visitAssignment(CobParser.AssignmentContext ctx) {
    	if (ctx.op == null) {
        	CobParser.ExpressionContext expr = ctx.expression();
        	visit(expr);
        	ctx.exp = expr.exp;
        	ctx.tipe = expr.tipe;
    	} else {
			visit(ctx.l);
	        visit(ctx.r);
	        Exp exp = ctx.l.exp;
	        exp.add(ctx.op.getText());
	        exp.add(ctx.r.exp);
	        ctx.exp = exp;
    	}
//TODO ctx.tipe =
        return null;
    }

    @Override public Void visitSequence(CobParser.SequenceContext ctx) {
    	Exp exp = null;
    	String sep = "";
 		for (CobParser.AssignmentContext assign : ctx.assignment()) {
        	visitAssignment(assign);
 			if (exp == null) exp = assign.exp;
 			else exp.add(sep).add(assign.exp);
        	ctx.tipe = assign.tipe;  // last one stays
        	sep = ",";
        }
 		ctx.exp = exp;
        return null;
    }

    @Override public Void visitConstantExpression(CobParser.ConstantExpressionContext ctx) {
    	CobParser.ExpressionContext expr = ctx.expression();
        visit(expr);
        //TODO ensure constant computable
        ctx.tipe = expr.tipe;
        return null;
    }

    @Override public Void visitCompoundStatement(CobParser.CompoundStatementContext ctx) {
    	writeImpl("{\n");
//Main.debug("visitCompoundStatement parent class %s", ctx.getParent().getClass().getSimpleName());
		ParserRuleContext parent = ctx.getParent();
		// Kludgy: should go in visitBody, but has to follow the '{' which is written here
		if (parent instanceof CobParser.BodyContext) {
	    	if (parent.getParent() instanceof CobParser.MethodContext) {
	    		MethodSymbol methodDefn = (MethodSymbol) ((CobParser.MethodContext)parent.getParent()).defn;
	    		writeImpl(" COB_ENTER_METHOD(",klassNest.getLast(),",\"",methodDefn.getName(),"\")\n");
	    		if (methodDefn.isStatic()) writeImpl(" COB_CLASS_INIT(",klassNest.getLast(),")\n");
	    		writeImpl(" COB_SOURCE_FILE(\"",passData.sourceFileName,"\")\n");
	    	} else if (parent.getParent() instanceof CobParser.ConstructorContext) {
	    		MethodSymbol methodDefn = (MethodSymbol) ((CobParser.ConstructorContext)parent.getParent()).defn;
	    		assert methodDefn.isStatic();
	    		writeImpl(" COB_ENTER_METHOD(",klassNest.getLast(),",\"",methodDefn.getName(),"\")\n");
	    		writeImpl(" COB_CLASS_INIT(",klassNest.getLast(),")\n");
	    		writeImpl(" COB_SOURCE_FILE(\"",passData.sourceFileName,"\")\n");
	    	}
		}
    	for (CobParser.BlockItemContext item : ctx.blockItem()) {
    		visit(item);
    	}
		writeImpl(" COB_SOURCE_LINE(",Integer.toString(ctx.stop.getLine()),")\n");
        writeImpl("}");
        return null;
    }

    @Override public Void visitBlockItem(CobParser.BlockItemContext ctx) {
    	CobParser.DeclarationContext decl = ctx.declaration();
    	if (decl != null) visitDeclaration(decl);
    	CobParser.StatementContext stmt = ctx.statement();
    	if (stmt != null) visit(stmt);
        writeImpl("\n");
        return null;
    }

    @Override public Void visitDeclaration(CobParser.DeclarationContext ctx) {
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
		CobParser.DeclarationContext decl = (CobParser.DeclarationContext)ctx.getParent();
		Type type = decl.type().tipe;
		writeImpl(" ",type.getNameString()," ",type.getArrayString(),ctx.ID().getText());
		CobParser.ExpressionContext expr = ctx.expression();
		if (expr != null) {
			writeImpl("=");
			visit(expr);
			writeImpl(expr.exp);
		}
        return null;
    }

	private void writeSourceLine(CobParser.StatementContext ctx) {
		if (ctx.getParent() instanceof CobParser.BlockItemContext) {
    		writeImpl(" COB_SOURCE_LINE(",Integer.toString(ctx.start.getLine()),")\n");
    	}
	}

    @Override public Void visitLabelStatement(CobParser.LabelStatementContext ctx) {
    	writeImpl(ctx.ID().getText(),":");
    	visit(ctx.statement());
        return null;
    }

    @Override public Void visitCmpdStatement(CobParser.CmpdStatementContext ctx) {
        visitCompoundStatement(ctx.compoundStatement());
        return null;
    }

    @Override public Void visitExpressionStatement(CobParser.ExpressionStatementContext ctx) {
    	writeSourceLine(ctx);
    	writeImpl(" ");
    	CobParser.SequenceContext seq = ctx.sequence();
    	if (seq != null) {
    		visitSequence(seq);
    		writeImpl(seq.exp);
    	}
    	writeImpl(";");
        return null;
    }

    @Override public Void visitIfStatement(CobParser.IfStatementContext ctx) {
    	writeSourceLine(ctx);
    	writeImpl(" if(");
    	CobParser.SequenceContext seq = ctx.sequence();
		visitSequence(seq);
		writeImpl(seq.exp);
    	writeImpl(")");
    	visit(ctx.t);
    	if (ctx.f != null) {
        	writeImpl(" else ");
        	visit(ctx.f);
    	}
        return null;
    }

    @Override public Void visitSwitchStatement(CobParser.SwitchStatementContext ctx) {
    	writeSourceLine(ctx);
    	writeImpl(" switch(");
    	CobParser.SequenceContext seq = ctx.sequence();
		visitSequence(seq);
		writeImpl(seq.exp);
    	writeImpl(")");
    	for (CobParser.SwitchItemContext item : ctx.switchItem()) {
    		visitSwitchItem(item);
    	}
        return null;
    }

    @Override public Void visitWhileStatement(CobParser.WhileStatementContext ctx) {
    	writeSourceLine(ctx);
    	writeImpl(" while(");
    	CobParser.SequenceContext seq = ctx.sequence();
		visitSequence(seq);
		writeImpl(seq.exp);
    	writeImpl(")");
    	visit(ctx.statement());
        return null;
    }

    @Override public Void visitDoStatement(CobParser.DoStatementContext ctx) {
    	writeSourceLine(ctx);
    	writeImpl(" do ");
    	visit(ctx.statement());
    	writeImpl("while(");
    	CobParser.SequenceContext seq = ctx.sequence();
		visitSequence(seq);
		writeImpl(seq.exp);
    	writeImpl(")");
        return null;
    }

    @Override public Void visitForStatement(CobParser.ForStatementContext ctx) {
    	writeSourceLine(ctx);
    	writeImpl(" for(");
    	if (ctx.b != null) {
			visitSequence(ctx.b);
			writeImpl(ctx.b.exp);
    	}
    	writeImpl(";");
    	if (ctx.w != null) {
			visitSequence(ctx.w);
			writeImpl(ctx.w.exp);
    	}
    	writeImpl(";");
    	if (ctx.a != null) {
			visitSequence(ctx.a);
			writeImpl(ctx.a.exp);
    	}
    	writeImpl(")");
    	visit(ctx.statement());
        return null;
    }

    @Override public Void visitForDeclStatement(CobParser.ForDeclStatementContext ctx) {
    	writeSourceLine(ctx);
    	writeImpl(" for(");
		visitDeclaration(ctx.declaration());
    	if (ctx.w != null) {
			visitSequence(ctx.w);
			writeImpl(ctx.w.exp);
    	}
    	writeImpl(";");
    	if (ctx.a != null) {
			visitSequence(ctx.a);
			writeImpl(ctx.a.exp);
    	}
    	writeImpl(")");
    	visit(ctx.statement());
        return null;
    }

    @Override public Void visitContinueStatement(CobParser.ContinueStatementContext ctx) {
    	writeSourceLine(ctx);
        writeImpl(" continue;");
        return null;
    }

    @Override public Void visitBreakStatement(CobParser.BreakStatementContext ctx) {
    	writeSourceLine(ctx);
        writeImpl(" break;");
        return null;
    }

    @Override public Void visitReturnStatement(CobParser.ReturnStatementContext ctx) {
    	writeSourceLine(ctx);
    	writeImpl(" return ");
    	CobParser.SequenceContext seq = ctx.sequence();
    	if (seq != null) {
    		//TODO check return type
    		visitSequence(seq);
//printContextTree(seq,"    ");
    		writeImpl(seq.exp);
    	} else {
    		//TODO check return type is void
    	}
    	writeImpl(";");
        return null;
    }

    @Override public Void visitSwitchItem(CobParser.SwitchItemContext ctx) {
    	CobParser.ConstantExpressionContext cexp = ctx.constantExpression();
    	if (cexp != null) {
    		writeImpl(" case ");
    		visitConstantExpression(cexp);
    		writeImpl(":");
    	} else {
    		writeImpl(" default:");
    	}
    	visit(ctx.statement());
        return null;
    }	
	
	@Override public Void visitTerminal(TerminalNode t) {
		writeImpl(" ",t.getSymbol().getText());
		return null;
	}
	
}
