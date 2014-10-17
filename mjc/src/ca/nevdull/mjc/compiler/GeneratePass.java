package ca.nevdull.mjc.compiler;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Map;

import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.ParseTreeProperty;
import org.antlr.v4.runtime.tree.TerminalNodeImpl;

import ca.nevdull.mjc.util.OutputAtom;
import ca.nevdull.mjc.util.OutputItem;
import ca.nevdull.mjc.util.OutputList;

public class GeneratePass extends MJBaseListener {
    ParseTreeProperty<Symbol> symbols;
    ParseTreeProperty<Type> types = new ParseTreeProperty<Type>();
    ParseTreeProperty<OutputItem> rands = new ParseTreeProperty<OutputItem>();
    File outputDir;
    Map<PrimitiveType,String> primitiveTypeMap = new IdentityHashMap<PrimitiveType,String>();   
    {
    	primitiveTypeMap.put(PrimitiveType.booleanType, "boolean");
    	primitiveTypeMap.put(PrimitiveType.byteType, "byte");
    	primitiveTypeMap.put(PrimitiveType.charType, "char");
    	primitiveTypeMap.put(PrimitiveType.doubleType, "double");
    	primitiveTypeMap.put(PrimitiveType.floatType, "float");
    	primitiveTypeMap.put(PrimitiveType.intType, "int");
    	primitiveTypeMap.put(PrimitiveType.longType, "long");
    	primitiveTypeMap.put(PrimitiveType.shortType, "short");
    }

	public GeneratePass(ParseTreeProperty<Symbol> symbols, ParseTreeProperty<Type> types, File outputDir) {
		super();
		this.symbols = symbols;
		this.types = types;
		this.outputDir = outputDir;
	}
	
    private void setType(ParserRuleContext node, Type type) {
    	types.put(node, type);
    }
    
    private Type getType(ParserRuleContext node) {
    	Type t = types.get(node);
    	assert t != null;
    	return t;
    }
      
    private void promoteType(ParserRuleContext node, ParserRuleContext subNode) {
    	setType(node, getType(subNode));
    }
    
    int reg = 1;
    public int nextreg() {
    	return reg++;
    }
    
    int stringnum = 1;
    Map<String, Integer> strings = new HashMap<String, Integer>();
    public int stringId(String s) {
    	Integer id = strings.get(s);
    	if (id == null) {
        	id = new Integer(stringnum++);
        	strings.put(s, id);
    	}
    	return id.intValue();
    }
    
	public void printContextTree(ParseTree t, String indent) {
		System.out.print(indent);
		System.out.print(t.getClass().getSimpleName());
		if (t instanceof ParserRuleContext) {
			System.out.print(" type="); System.out.print(types.get((ParserRuleContext)t));
			System.out.print(" symbol="); System.out.print(symbols.get((ParserRuleContext)t));
		} else if (t instanceof TerminalNodeImpl) {
			System.out.print(" ");
			System.out.print((TerminalNodeImpl)t);
		}
		System.out.println();
		if (t.getChildCount() == 0) {
		} else {
			indent = "     "+indent;
			for (int i = 0; i<t.getChildCount(); i++) {
				printContextTree(t.getChild(i), indent);
			}
		}
	}
    
    class ClassDest {
    	ClassSymbol symbol;
    	ClassDest enclosingClass;
    	OutputList vtable;
    	OutputList instance;
    	OutputList methods;
    	PrintStream outStream;
 		public ClassDest(ClassSymbol symbol, ClassDest enclosingClass) {
			super();
			this.symbol = symbol;
			this.enclosingClass = enclosingClass;
	    	this.vtable = new OutputList();
	    	this.instance = new OutputList();
	    	this.methods = new OutputList();
	    	System.out.println("ClassDest "+symbol);
	    	vtable.add("typedef struct ",symbol.getName(),"_obj *",symbol.getName(),";\n");
	    	vtable.add("struct ",symbol.getName(),"_class"," {\n");
	    	instance.add("struct ",symbol.getName(),"_obj"," {\n");
		}
		public ClassSymbol getSymbol() {
			return symbol;
		}
		private String getFileName() {
 			String thisName = symbol.getName();
			if (enclosingClass == null) return thisName;
			return enclosingClass.getFileName()+"$"+thisName;
 		}
 		
		public ClassDest close() {
	    	vtable.add("};\n");
	    	instance.add("};\n");
			String fileName = getFileName()+".c";
			try {
				outStream = new PrintStream(outputDir == null ? new File(fileName) : new File(outputDir,fileName));
				vtable.print(outStream);
				instance.print(outStream);
				methods.print(outStream);
				outStream.close();
			} catch (FileNotFoundException excp) {
				Compiler.error(null,"Unable to open "+fileName);
			}
			return enclosingClass;
		}
    }
    ClassDest currDest = null;

	@Override public void exitCompilationUnit(MJParser.CompilationUnitContext ctx) { }
	@Override public void exitTypeDeclaration(MJParser.TypeDeclarationContext ctx) { }
	@Override public void exitModifier(MJParser.ModifierContext ctx) { }
	@Override public void exitClassOrInterfaceModifier(MJParser.ClassOrInterfaceModifierContext ctx) { }
	@Override public void enterClassDeclaration(MJParser.ClassDeclarationContext ctx) {
		Symbol sym = symbols.get(ctx);
		currDest = new ClassDest((ClassSymbol)symbols.get(ctx), currDest);
		MJParser.TypeContext sup = ctx.type();
		if (sup != null) {
			Type t = getType(sup);
		    if (t instanceof ClassSymbol) {
		    	currDest.vtable.add("struct ",t.getName(),"_class"," super;\n");
		    	currDest.instance.add("struct ",t.getName(),"_class"," *_class;\n");
		    	currDest.instance.add("struct ",t.getName(),"_obj"," super;\n");
		    }
		} else {
	    	currDest.vtable.add("struct Object_class super;\n");
	    	currDest.instance.add("struct Object_class"," *_class;\n");
	    	currDest.instance.add("struct Object_obj super;\n");
		}
	}
	@Override public void exitClassDeclaration(MJParser.ClassDeclarationContext ctx) {
		currDest = currDest.close();
	}
	@Override public void exitClassBody(MJParser.ClassBodyContext ctx) { }
	@Override public void exitMemberClassBodyDeclaration(MJParser.MemberClassBodyDeclarationContext ctx) { }
	@Override public void exitBlockClassBodyDeclaration(MJParser.BlockClassBodyDeclarationContext ctx) { }
	@Override public void exitEmptyClassBodyDeclaration(MJParser.EmptyClassBodyDeclarationContext ctx) { }
	@Override public void exitMemberDeclaration(MJParser.MemberDeclarationContext ctx) { }
	@Override public void enterMethodDeclaration(MJParser.MethodDeclarationContext ctx) {
		Symbol sym = symbols.get(ctx);
		currDest.vtable.add(typeName("(*"+sym.getName()+")", sym.getType()));
		currDest.methods.add(typeName(sym.getName()+"_"+currDest.getSymbol().getName(), sym.getType()));
	}
	@Override public void exitMethodDeclaration(MJParser.MethodDeclarationContext ctx) {
		currDest.vtable.add(";\n");
		currDest.methods.add("\n");
	}
	@Override public void exitConstructorDeclaration(MJParser.ConstructorDeclarationContext ctx) { }
	@Override public void enterFieldDeclaration(MJParser.FieldDeclarationContext ctx) {
		for (MJParser.VariableDeclaratorContext vd : ctx.variableDeclarators().variableDeclarator()) {
			Symbol sym = symbols.get(vd.variableDeclaratorId());
			currDest.instance.add(typeName(sym.getName(), sym.getType())).add(";\n");			
		}
	}
	@Override public void exitFieldDeclaration(MJParser.FieldDeclarationContext ctx) {
		for (MJParser.VariableDeclaratorContext vd : ctx.variableDeclarators().variableDeclarator()) {
			Symbol sym = symbols.get(vd.variableDeclaratorId());
			if (vd.variableInitializer() != null) {
				currDest.methods.add(sym.getName(),"=").add(rands.get(vd.variableInitializer())).add(";\n");
				//TODO should be in constructor/initializer
			}
		}
	}
	@Override public void exitVariableDeclarators(MJParser.VariableDeclaratorsContext ctx) { }
	@Override public void exitVariableDeclarator(MJParser.VariableDeclaratorContext ctx) { }
	@Override public void exitVariableDeclaratorId(MJParser.VariableDeclaratorIdContext ctx) { }
	private OutputList typeName(String name, Type t) {
		OutputList ret = new OutputList();
		if (t instanceof ArrayType) {
			ret.add(typeName(name, ((ArrayType)t).getElementType())).add("[]");
		} else if (t instanceof ClassSymbol) {
			ret.add(t.getName()," ",name);
		} else if (t instanceof PrimitiveType) {
			ret.add(primitiveTypeMap.get(t)," ",name);
		} else if (t instanceof VoidType) {
			ret.add("void ",name);
		} else {
			ret.add("errorType ",name);
		}
		return ret;
	}
	@Override public void exitSimpleVariableInitializer(MJParser.SimpleVariableInitializerContext ctx) {
		promoteType(ctx, ctx.expression());
		rands.put(ctx, rands.get(ctx.expression()));
	}
	@Override public void exitArrayVariableInitializer(MJParser.ArrayVariableInitializerContext ctx) {
		/*TODO*/
	}
	@Override public void exitArrayInitializer(MJParser.ArrayInitializerContext ctx) { 
		/*TODO*/
	}
	@Override public void exitPrimitType(MJParser.PrimitTypeContext ctx) { }
	@Override public void exitObjectType(MJParser.ObjectTypeContext ctx) { }
	@Override public void exitArrayDimension(MJParser.ArrayDimensionContext ctx) { }
	@Override public void exitClassOrInterfaceType(MJParser.ClassOrInterfaceTypeContext ctx) { }
	@Override public void exitBooleanType(MJParser.BooleanTypeContext ctx) { }
	@Override public void exitDoubleType(MJParser.DoubleTypeContext ctx) { }
	@Override public void exitCharType(MJParser.CharTypeContext ctx) { }
	@Override public void exitFloatType(MJParser.FloatTypeContext ctx) { }
	@Override public void exitIntType(MJParser.IntTypeContext ctx) { }
	@Override public void exitShortType(MJParser.ShortTypeContext ctx) { }
	@Override public void exitByteType(MJParser.ByteTypeContext ctx) { }
	@Override public void exitLongType(MJParser.LongTypeContext ctx) { }
	@Override public void enterFormalParameters(MJParser.FormalParametersContext ctx) {
		currDest.vtable.add("(",currDest.getSymbol().getName()," ","this");
		currDest.methods.add("(",currDest.getSymbol().getName()," ","this");
	}
	@Override public void exitFormalParameters(MJParser.FormalParametersContext ctx) {
		currDest.vtable.add(")");
		currDest.methods.add(") ");
	}
	@Override public void exitFormalParameterList(MJParser.FormalParameterListContext ctx) { }
	@Override public void enterFormalParameter(MJParser.FormalParameterContext ctx) {
		Symbol sym = symbols.get(ctx.variableDeclaratorId());
		currDest.vtable.add(", ").add(typeName(sym.getName(), sym.getType()));			
		currDest.methods.add(", ").add(typeName(sym.getName(), sym.getType()));			
	}
	@Override public void exitVariableModifier(MJParser.VariableModifierContext ctx) { }
	@Override public void exitMethodBody(MJParser.MethodBodyContext ctx) { }
	@Override public void exitConstructorBody(MJParser.ConstructorBodyContext ctx) { }
	@Override public void exitQualifiedName(MJParser.QualifiedNameContext ctx) { }
	@Override public void exitLiteral(MJParser.LiteralContext ctx) {
		String text = ctx.getText(); 
		Type t = null;
		if (ctx.IntegerLiteral() != null) {
			t = PrimitiveType.intType;
			if (text.endsWith("l") || text.endsWith("L")) {
				t = PrimitiveType.longType;
			}
		} else if (ctx.FloatingPointLiteral() != null) {
			t = PrimitiveType.floatType;
			if (text.endsWith("d") || text.endsWith("D")) {
				t = PrimitiveType.doubleType;
			}
		} else if (ctx.CharacterLiteral() != null) {
			t = PrimitiveType.charType;
		} else if (ctx.BooleanLiteral() != null) {
			t = PrimitiveType.booleanType;
		} else if (ctx.StringLiteral() != null) {
			//TODO t = String;
		} else {
			assert text.equals("null");
			//TODO t = Object; 
		}
		rands.put(ctx, new OutputAtom(text));
		setType(ctx, t);
	}
	@Override public void enterBlock(MJParser.BlockContext ctx) {
		currDest.methods.add("{\n");
	}
	@Override public void exitBlock(MJParser.BlockContext ctx) {
		currDest.methods.add("}\n");
	}
	@Override public void exitBlockStatement(MJParser.BlockStatementContext ctx) { }
	@Override public void exitLocalVariableDeclarationStatement(MJParser.LocalVariableDeclarationStatementContext ctx) { }
	@Override public void enterLocalVariableDeclaration(MJParser.LocalVariableDeclarationContext ctx) {
		for (MJParser.VariableDeclaratorContext vd : ctx.variableDeclarators().variableDeclarator()) {
			Symbol sym = symbols.get(vd.variableDeclaratorId());
			currDest.methods.add(typeName(sym.getName(), sym.getType())).add(";\n");
		}
	}
	@Override public void exitLocalVariableDeclaration(MJParser.LocalVariableDeclarationContext ctx) {
		for (MJParser.VariableDeclaratorContext vd : ctx.variableDeclarators().variableDeclarator()) {
			Symbol sym = symbols.get(vd.variableDeclaratorId());
			if (vd.variableInitializer() != null) {
				currDest.methods.add(sym.getName(),"=").add(rands.get(vd.variableInitializer())).add(";\n");				
			}
		}
	}
	@Override public void exitWhileStatement(MJParser.WhileStatementContext ctx) { }
	@Override public void enterExpressionStatement(MJParser.ExpressionStatementContext ctx) {
		currDest.methods.add("// ",Integer.toString(ctx.getStart().getLine()),": ",ctx.getText(),"\n");
	}
	@Override public void exitExpressionStatement(MJParser.ExpressionStatementContext ctx) { }
	@Override public void exitEmnptyStatement(MJParser.EmnptyStatementContext ctx) { }
	@Override public void exitReturnStatement(MJParser.ReturnStatementContext ctx) { }
	@Override public void exitLabelStatement(MJParser.LabelStatementContext ctx) { }
	@Override public void exitBlkStatement(MJParser.BlkStatementContext ctx) { }
	@Override public void exitIfStatement(MJParser.IfStatementContext ctx) { }
	@Override public void exitParExpression(MJParser.ParExpressionContext ctx) { }
	@Override public void exitExpressionList(MJParser.ExpressionListContext ctx) {	}
	@Override public void exitStatementExpression(MJParser.StatementExpressionContext ctx) { }
	@Override public void exitConstantExpression(MJParser.ConstantExpressionContext ctx) { }
	@Override public void exitCompareExpression(MJParser.CompareExpressionContext ctx) { }
	@Override public void exitExclExpression(MJParser.ExclExpressionContext ctx) {
		String op = ctx.getChild(1).getText();
		binaryOperator(ctx, ctx.expression(0), op, ctx.expression(1)); 
	}
	@Override public void exitAddExpression(MJParser.AddExpressionContext ctx) {
		String op = ctx.getChild(1).getText();
		binaryOperator(ctx, ctx.expression(0), op, ctx.expression(1)); 
	}

	public void binaryOperator(MJParser.ExpressionContext ctx, MJParser.ExpressionContext left, String op, MJParser.ExpressionContext right) {
		Type leftType = getType(left);
		Type typeRight = getType(right);
		Type typeResult = leftType;  //TODO determine result type
		String rand = "_e"+nextreg();
		currDest.methods.add(typeName(rand,typeResult)).add("=").add(rands.get(left)).add(op).add(rands.get(right)).add(";\n");
		rands.put(ctx, new OutputAtom(rand));
	}

	@Override public void exitAssignExpression(MJParser.AssignExpressionContext ctx) {
		MJParser.ExpressionContext dest = ctx.expression(0);
		//System.out.println("exitAssignExpression dest");  printContextTree(dest,"    ");
		Symbol sym = symbols.get(dest);
		if (sym == null) {
			Compiler.error(dest.getStop(), dest.getText()+" is not a variable or field","AssignExpression");
			return;
		} else if (!(sym instanceof VarSymbol)) {
			Compiler.error(dest.getStop(), dest.getText()+" is not a variable or field","AssignExpression");
			return;
		}
		MJParser.ExpressionContext val = ctx.expression(1);
		Type type = getType(val);
		if (!(type instanceof PrimitiveType || type instanceof ClassSymbol)) {
			Compiler.error(dest.getStop(), "not an assignable value","AssignExpression");
		}
		currDest.methods.add(rands.get(dest)).add("=").add(rands.get(val)).add(";\n");
		rands.put(ctx, rands.get(dest));
	}
	@Override public void exitNotExpression(MJParser.NotExpressionContext ctx) { }
	@Override public void enterCallExpression(MJParser.CallExpressionContext ctx) {	}
	@Override public void exitCallExpression(MJParser.CallExpressionContext ctx) {
		MJParser.ExpressionContext exp = ctx.expression();
		System.out.println("exitCallExpression method");  printContextTree(exp,"    ");
		Symbol sym = symbols.get(exp);
		if (sym == null) {
			Compiler.error(exp.getStop(), exp.getText()+" is not a method","CallExpression");
			return;
		} else if (!(sym instanceof MethodSymbol)) {
			Compiler.error(exp.getStop(), exp.getText()+" is not a method","CallExpression");
			setType(ctx, UnknownType.getInstance());
			return;
		}
		OutputItem method = rands.get(exp);
		Type type = sym.getType();  //getType(ctx.expression());
		if (!(type instanceof VoidType)) {
			String rand = "_e"+nextreg();
			currDest.methods.add(typeName(rand,type)).add("=");
			rands.put(ctx, new OutputAtom(rand));
		}
		currDest.methods.add(method); // includes the (
		MJParser.ExpressionListContext argList = ctx.expressionList();
		if (argList != null) {
			for (MJParser.ExpressionContext arg : argList.expression()) {
				currDest.methods.add(",").add(rands.get(arg));
			}
		}
		setType(ctx, type);
		currDest.methods.add(");\n");
	}
	@Override public void exitOrExpression(MJParser.OrExpressionContext ctx) {
		//TODO types
		binaryOperator(ctx, ctx.expression(0), "|", ctx.expression(1));
	}
	@Override public void exitIndexExpression(MJParser.IndexExpressionContext ctx) { }
	@Override public void exitEqualExpression(MJParser.EqualExpressionContext ctx) { }
	@Override public void exitMultExpression(MJParser.MultExpressionContext ctx) {
		//TODO types
		String op = ctx.getChild(1).getText();
		binaryOperator(ctx, ctx.expression(0), op, ctx.expression(1)); 
	}
	@Override public void exitCondAndExpression(MJParser.CondAndExpressionContext ctx) { }
	@Override public void exitAndExpression(MJParser.AndExpressionContext ctx) {
		//TODO types
		binaryOperator(ctx, ctx.expression(0), "&", ctx.expression(1)); 
	}
	@Override public void exitPrimExpression(MJParser.PrimExpressionContext ctx) {
		promoteType(ctx, ctx.primary());
		rands.put(ctx, rands.get(ctx.primary()));
		symbols.put(ctx, symbols.get(ctx.primary()));
		//System.out.println("exitPrimExpression "+ rands.get(ctx));
	}
	@Override public void exitCondOrExpression(MJParser.CondOrExpressionContext ctx) { }
	@Override public void exitCastExpression(MJParser.CastExpressionContext ctx) { }
	@Override public void exitDotExpression(MJParser.DotExpressionContext ctx) {
 		MJParser.ExpressionContext exp = ctx.expression();
		Type type = getType(exp);
    	Token token = ctx.Identifier().getSymbol();
        String name = token.getText();
        Symbol sym = null;
        if (type instanceof ScopingSymbol) {
        	sym = ((ScopingSymbol) type).resolveMember(name);
        	//TODO check access
        	if (sym == null) {
        		Compiler.error(token, name+" is not defined in "+type.getName(),"DotExpression");
        		type = UnknownType.getInstance();
        	} else {
        		type = sym.getType();
        	}
        } else if (!(type instanceof UnknownType)) {
        	Compiler.error(token, "not a reference: "+type.toString(),"DotExpression");
        }
		OutputList ref = new OutputList();
		if (sym != null && sym instanceof MethodSymbol) {
			setType(ctx, (MethodSymbol)sym);  // let method be a pseudo-type until called
			//TODO handle static method
			ref.add(rands.get(exp)).add("->_class->").add(name).add("(").add(rands.get(exp));			
		} else {
			setType(ctx, type);
			ref.add(rands.get(exp)).add("->").add(name);
		}
		symbols.put(ctx, sym);
		rands.put(ctx, ref);
	}
	@Override public void exitShiftExpression(MJParser.ShiftExpressionContext ctx) { }
	@Override public void exitPlusExpression(MJParser.PlusExpressionContext ctx) { }
	@Override public void exitNewExpression(MJParser.NewExpressionContext ctx) { }
	@Override public void exitLiteralPrimary(MJParser.LiteralPrimaryContext ctx) {
		promoteType(ctx, ctx.literal());
		rands.put(ctx, rands.get(ctx.literal()));
	}
	@Override public void exitSuperPrimary(MJParser.SuperPrimaryContext ctx) { }
	@Override public void exitIdentifierPrimary(MJParser.IdentifierPrimaryContext ctx) {
        Symbol sym = symbols.get(ctx);
		Type t = null;
        if (sym != null) {
        	t = sym.getType();
    		System.out.println(sym.getName()+" type "+t);
            setType(ctx, t);
            Scope scope = sym.getScope();
            OutputItem access = null;
            if (scope instanceof BaseScope || scope instanceof MethodSymbol) {
            	access = new OutputAtom(sym.getName());
            } else if (scope instanceof ClassSymbol) {
            	access = new OutputList();
        		if (sym != null && sym instanceof MethodSymbol) {
        			((OutputList)access).add("this->_class->",sym.getName()).add("(").add("this");
        			setType(ctx, (MethodSymbol)sym);  // doesn't have result type until called
        		} else {
        			((OutputList)access).add("this->",sym.getName());
        		}
            } else {
            	System.out.println("exitIdentifierPrimary scope is "+scope.getClass().getSimpleName());
            }
            rands.put(ctx, access);
        } else {
	        if (t == null) t = UnknownType.getInstance();
	        setType(ctx, t);
			rands.put(ctx, new OutputAtom("_unknown"));
        }
	}
	@Override public void exitThisPrimary(MJParser.ThisPrimaryContext ctx) {
		setType(ctx, currDest.getSymbol());
		rands.put(ctx, new OutputAtom("this"));
	}
	@Override public void exitParenPrimary(MJParser.ParenPrimaryContext ctx) {
		promoteType(ctx, ctx.expression());
		rands.put(ctx, rands.get(ctx.expression()));
		symbols.put(ctx, symbols.get(ctx.expression()));
	}
	@Override public void exitCreator(MJParser.CreatorContext ctx) { }
	@Override public void exitCreatedName(MJParser.CreatedNameContext ctx) { }
	@Override public void exitArrayCreatorRest(MJParser.ArrayCreatorRestContext ctx) { }
	@Override public void exitClassCreatorRest(MJParser.ClassCreatorRestContext ctx) { }
	@Override public void exitArguments(MJParser.ArgumentsContext ctx) { }


}
