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

import ca.nevdull.mjc.compiler.util.OutputAtom;
import ca.nevdull.mjc.compiler.util.OutputItem;
import ca.nevdull.mjc.compiler.util.OutputList;

public class GeneratePass extends MJBaseListener {
    ParseTreeProperty<Symbol> symbols;
    ParseTreeProperty<Type> types = new ParseTreeProperty<Type>();
    ParseTreeProperty<OutputItem> rands = new ParseTreeProperty<OutputItem>();
    File outputDir;
    Map<PrimitiveType,String> primitiveTypeMap = new IdentityHashMap<PrimitiveType,String>();   
    {
    	primitiveTypeMap.put(PrimitiveType.booleanType, "jboolean");
    	primitiveTypeMap.put(PrimitiveType.byteType, "jbyte");
    	primitiveTypeMap.put(PrimitiveType.charType, "jchar");
    	primitiveTypeMap.put(PrimitiveType.doubleType, "jdouble");
    	primitiveTypeMap.put(PrimitiveType.floatType, "jfloat");
    	primitiveTypeMap.put(PrimitiveType.intType, "jint");
    	primitiveTypeMap.put(PrimitiveType.longType, "jlong");
    	primitiveTypeMap.put(PrimitiveType.shortType, "jshort");
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
	
	/*
	 * class structure
	 * instance structure
	 * method table structure
	 * method prototypes
	 * class structure initialization
	 * method table initialization
	 * method bodies
	 */
    
	/**
	 * The output components of a class implementation
	 */
    class ClassDest {
    	ClassSymbol symbol;
    	ClassDest enclosingClass;
    	boolean haveConstructor = false;
    	OutputList classStructure;
    	OutputList methodTableStructure;
    	OutputList instanceStructure;
    	OutputList classStructureInitialization;
    	OutputList methodPrototypes;
    	OutputList methodTableInitialization;
    	OutputList methodBodies;
    	PrintStream outStream;
 		public ClassDest(ClassSymbol symbol, ClassDest enclosingClass) {
			super();
			this.symbol = symbol;
			this.enclosingClass = enclosingClass;
			this.classStructure = new OutputList();					this.classStructure.add("/* classStructure */\n");
	    	this.methodTableStructure = new OutputList();	    	this.methodTableStructure.add("/* methodTableStructure */\n");
	    	this.instanceStructure = new OutputList();	    		this.instanceStructure.add("/* instanceStructure */\n");
	    	this.classStructureInitialization = new OutputList();	this.classStructureInitialization.add("/* classStructureInitialization */\n");
	    	this.methodPrototypes = new OutputList();	    		this.methodPrototypes.add("/* methodPrototypes */\n");
	    	this.methodTableInitialization = new OutputList();	    this.methodTableInitialization.add("/* methodTableInitialization */\n");
	    	this.methodBodies = new OutputList();	    			this.methodBodies.add("/* methodBodies */\n");
	    	this.classStructure = new OutputList();	    			this.classStructure.add("/* classStructure */\n");
	    	this.instanceStructure = new OutputList();	    		this.instanceStructure.add("/* instanceStructure */\n");
	    	this.methodBodies = new OutputList();	    			this.methodBodies.add("/* methodBodies */\n");
	    	System.out.println("ClassDest "+symbol);
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
			String fileName = getFileName()+".c";
			try {
				outStream = new PrintStream(outputDir == null ? new File(fileName) : new File(outputDir,fileName));
		    	classStructure.print(outStream);
		    	methodTableStructure.print(outStream);
		    	instanceStructure.print(outStream);
		    	classStructureInitialization.print(outStream);
		    	methodPrototypes.print(outStream);
		    	methodTableInitialization.print(outStream);
		    	methodBodies.print(outStream);
				outStream.close();
			} catch (FileNotFoundException excp) {
				Compiler.error(null,"Unable to open "+fileName);
			}
			return enclosingClass;
		}
    }
    ClassDest currDest = null;
    static String indent = "    ";

	@Override public void exitCompilationUnit(MJParser.CompilationUnitContext ctx) { }
	@Override public void exitTypeDeclaration(MJParser.TypeDeclarationContext ctx) { }
	@Override public void exitModifier(MJParser.ModifierContext ctx) { }
	@Override public void exitClassOrInterfaceModifier(MJParser.ClassOrInterfaceModifierContext ctx) { }
	@Override public void enterClassDeclaration(MJParser.ClassDeclarationContext ctx) {
		Symbol classSymbol = symbols.get(ctx);
		String className = classSymbol.getName();
    	String superName = null;
		MJParser.TypeContext sup = ctx.type();
		if (sup != null) {
			Type t = getType(sup);
		    if (t instanceof ClassSymbol) {
		    	superName = t.getName();
		    } else {
		    	superName = "Unknown";
		    }
		} else {
			superName = "Object";
		}
		currDest = new ClassDest((ClassSymbol)symbols.get(ctx), currDest);
		currDest.classStructure.add("#ifndef ",className,"_DEFN\n");
		currDest.classStructure.add("#define ",className,"_DEFN\n");
		currDest.classStructure.add("typedef struct ",className,"_obj *",className,";\n");
		currDest.classStructure.add("struct ",className,"_class_s"," {\n");
		currDest.methodTableStructure.add("struct ",className,"_methods_s"," {\n");
		currDest.methodTableStructure.add(indent).add("size_t _objSize");
		currDest.instanceStructure.add("struct ",className,"_obj"," {\n");
		currDest.classStructureInitialization.add("#ifndef ",className,"_IMPL\n");
		currDest.classStructureInitialization.add("extern struct ",className,"_class_s"," ",className,"_class",";\n");
		currDest.classStructureInitialization.add("extern struct ",className,"_methods_s"," ",className,"_methods",";\n");
		currDest.classStructureInitialization.add("#else\n");
		currDest.classStructureInitialization.add("struct ",className,"_class_s"," ",className,"_class"," = {\n");
		currDest.methodTableInitialization.add("struct ",className,"_methods_s"," ",className,"_methods"," = {\n");
		currDest.methodTableInitialization.add(indent).add("sizeof(struct ",className,"_obj)");
		currDest.classStructure.add(indent).add("struct ",superName,"_class_s"," _super;\n");
    	currDest.instanceStructure.add(indent).add("struct ",className,"_methods_s"," *_methods;\n");
    	currDest.instanceStructure.add(indent).add("struct ",superName,"_obj"," _super;\n");
		Scope encl = ((ClassSymbol)classSymbol).getEnclosingScope();
	}
	@Override public void exitClassDeclaration(MJParser.ClassDeclarationContext ctx) {
		ClassSymbol classSymbol = (ClassSymbol)symbols.get(ctx);
		String className = classSymbol.getName();
		if (!currDest.haveConstructor) {
			// Define a default constructor
	    	Token token = classSymbol.getToken();
			MethodSymbol method = new MethodSymbol(token, classSymbol);
			classSymbol.define(method);
			method.setType(VoidType.getInstance());
			beginMethod(method);
			beginFormalParameters(className);
			endFormalParameters();
			beginBlock();
			endBlock();
			endMethod();			
		}
		currDest.classStructure.add("};\n");
		currDest.instanceStructure.add("};\n");
		currDest.methodTableStructure.add("\n};\n");
		currDest.classStructureInitialization.add("\n};\n");
		currDest.methodTableInitialization.add("\n};\n");
		currDest.methodBodies.add("#endif /*",className,"_IMPL*/\n");
		currDest.methodBodies.add("#endif /*",className,"_DEFN*/\n");
		currDest = currDest.close();
	}
	@Override public void exitClassBody(MJParser.ClassBodyContext ctx) { }
	@Override public void exitMemberClassBodyDeclaration(MJParser.MemberClassBodyDeclarationContext ctx) { }
	@Override public void exitBlockClassBodyDeclaration(MJParser.BlockClassBodyDeclarationContext ctx) { }
	@Override public void exitEmptyClassBodyDeclaration(MJParser.EmptyClassBodyDeclarationContext ctx) { }
	@Override public void exitMemberDeclaration(MJParser.MemberDeclarationContext ctx) { }
	@Override public void enterMethodDeclaration(MJParser.MethodDeclarationContext ctx) {
		beginMethod(symbols.get(ctx));
	}
	@Override public void exitMethodDeclaration(MJParser.MethodDeclarationContext ctx) {
		endMethod();
	}

	private void beginMethod(Symbol sym) {
		String methodName = sym.getName();
		Type returnType = sym.getType();
		currDest.methodTableStructure.add(",\n").add(indent).add(typeName("(*"+methodName+")", returnType));
		String qualName = currDest.getSymbol().getName()+"_"+methodName;
		currDest.methodPrototypes.add(typeName(qualName, returnType));
		currDest.methodTableInitialization.add(",\n").add(indent).add("&",qualName);
		currDest.methodBodies.add(typeName(qualName, returnType));
	}
	private void endMethod() {
		//nothing for currDest.methodTableStructure
		currDest.methodPrototypes.add(";\n");
		//nothing for currDest.methodTableInitialization
		currDest.methodBodies.add("\n");
	}

	@Override public void enterConstructorDeclaration(MJParser.ConstructorDeclarationContext ctx) {
		beginMethod(symbols.get(ctx));
		currDest.haveConstructor = true;
	}
	@Override public void exitConstructorDeclaration(MJParser.ConstructorDeclarationContext ctx) {
		endMethod();
	}
	@Override public void enterFieldDeclaration(MJParser.FieldDeclarationContext ctx) {
		//LATER could order fields by descending alignment
		for (MJParser.VariableDeclaratorContext vd : ctx.variableDeclarators().variableDeclarator()) {
			Symbol sym = symbols.get(vd.variableDeclaratorId());
			currDest.instanceStructure.add(indent).add(typeName(sym.getName(), sym.getType())).add(";\n");			
		}
	}
	@Override public void exitFieldDeclaration(MJParser.FieldDeclarationContext ctx) {
		for (MJParser.VariableDeclaratorContext vd : ctx.variableDeclarators().variableDeclarator()) {
			Symbol sym = symbols.get(vd.variableDeclaratorId());
			if (vd.variableInitializer() != null) {
				currDest.methodBodies.add(indent).add(sym.getName(),"=").add(rands.get(vd.variableInitializer())).add(";\n");
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
		String className = currDest.getSymbol().getName();
		beginFormalParameters(className);
	}
	@Override public void exitFormalParameters(MJParser.FormalParametersContext ctx) {
		endFormalParameters();
	}

	private void beginFormalParameters(String className) {
		currDest.methodTableStructure.add("(",className," ","this");
		currDest.methodPrototypes.add("(",className," ","this");
		currDest.methodBodies.add("(",className," ","this");
	}
	private void endFormalParameters() {
		currDest.methodTableStructure.add(")");
		currDest.methodPrototypes.add(")");
		currDest.methodBodies.add(") ");
	}
	
	@Override public void exitFormalParameterList(MJParser.FormalParameterListContext ctx) { }
	@Override public void enterFormalParameter(MJParser.FormalParameterContext ctx) {
		Symbol sym = symbols.get(ctx.variableDeclaratorId());
		String symName = sym.getName();
		Type symType = sym.getType();
		currDest.methodTableStructure.add(", ").add(typeName(symName, symType));			
		currDest.methodPrototypes.add(", ").add(typeName(symName, symType));			
		currDest.methodBodies.add(", ").add(typeName(symName, symType));			
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
		beginBlock();
	}
	@Override public void exitBlock(MJParser.BlockContext ctx) {
		endBlock();
	}

	private void beginBlock() {
		currDest.methodBodies.add("{\n");
	}
	private void endBlock() {
		currDest.methodBodies.add("}\n");
	}

	@Override public void exitBlockStatement(MJParser.BlockStatementContext ctx) { }
	@Override public void exitLocalVariableDeclarationStatement(MJParser.LocalVariableDeclarationStatementContext ctx) { }
	@Override public void enterLocalVariableDeclaration(MJParser.LocalVariableDeclarationContext ctx) {
		for (MJParser.VariableDeclaratorContext vd : ctx.variableDeclarators().variableDeclarator()) {
			Symbol sym = symbols.get(vd.variableDeclaratorId());
			currDest.methodBodies.add(indent).add(typeName(sym.getName(), sym.getType())).add(";\n");
		}
	}
	@Override public void exitLocalVariableDeclaration(MJParser.LocalVariableDeclarationContext ctx) {
		for (MJParser.VariableDeclaratorContext vd : ctx.variableDeclarators().variableDeclarator()) {
			Symbol sym = symbols.get(vd.variableDeclaratorId());
			if (vd.variableInitializer() != null) {
				currDest.methodBodies.add(indent).add(sym.getName(),"=").add(rands.get(vd.variableInitializer())).add(";\n");				
			}
		}
	}
	@Override public void exitWhileStatement(MJParser.WhileStatementContext ctx) { }
	@Override public void enterExpressionStatement(MJParser.ExpressionStatementContext ctx) {
		currDest.methodBodies.add("// ",Integer.toString(ctx.getStart().getLine()),": ",ctx.getText(),"\n");
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
		currDest.methodBodies.add(indent).add(typeName(rand,typeResult)).add("=").add(rands.get(left)).add(op).add(rands.get(right)).add(";\n");
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
		currDest.methodBodies.add(indent).add(rands.get(dest)).add("=").add(rands.get(val)).add(";\n");
		rands.put(ctx, rands.get(dest));
	}
	@Override public void exitNotExpression(MJParser.NotExpressionContext ctx) { }
	@Override public void enterCallExpression(MJParser.CallExpressionContext ctx) {	}
	@Override public void exitCallExpression(MJParser.CallExpressionContext ctx) {
		MJParser.ExpressionContext exp = ctx.expression();
		//System.out.println("exitCallExpression method");  printContextTree(exp,"    ");
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
			currDest.methodBodies.add(indent).add(typeName(rand,type)).add("=");
			rands.put(ctx, new OutputAtom(rand));
		} else {
			currDest.methodBodies.add(indent);
		}
		currDest.methodBodies.add(method); // includes the (
		MJParser.ExpressionListContext argList = ctx.expressionList();
		if (argList != null) {
			for (MJParser.ExpressionContext arg : argList.expression()) {
				currDest.methodBodies.add(",").add(rands.get(arg));
			}
		}
		setType(ctx, type);
		currDest.methodBodies.add(");\n");
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
			checkRef(ref, rands.get(exp)).add("->_methods->").add(name).add("(").add(rands.get(exp));			
		} else {
			setType(ctx, type);
			checkRef(ref, rands.get(exp)).add("->").add(name);
		}
		symbols.put(ctx, sym);
		rands.put(ctx, ref);
	}

	private OutputList checkRef(OutputList out, OutputItem ref) {
		return out.add("((").add(ref).add(")?(").add(ref).add("):throwNPE())");
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
		Type symType = null;
        if (sym != null) {
    		String symName = sym.getName();
        	symType = sym.getType();
			System.out.println(symName+" type "+symType);
            setType(ctx, symType);
            Scope scope = sym.getScope();
            OutputItem access = null;
            if (scope instanceof BaseScope || scope instanceof MethodSymbol) {
            	access = new OutputAtom(symName);
            } else if (scope instanceof ClassSymbol) {
            	access = new OutputList();
        		if (sym != null && sym instanceof MethodSymbol) {
        			// "this" doesn't need a null pointer check
        			((OutputList)access).add("this->_methods->",symName).add("(").add("this");
        			setType(ctx, (MethodSymbol)sym);  // doesn't have result type until called
        		} else {
        			((OutputList)access).add("this->",symName);
        		}
            } else {
            	System.out.println("exitIdentifierPrimary scope is "+scope.getClass().getSimpleName());
            }
            rands.put(ctx, access);
        } else {
	        if (symType == null) symType = UnknownType.getInstance();
	        setType(ctx, symType);
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
