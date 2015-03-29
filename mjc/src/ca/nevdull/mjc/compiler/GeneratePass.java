package ca.nevdull.mjc.compiler;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.PrintStream;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Map;

import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.misc.NotNull;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.ParseTreeProperty;
import org.antlr.v4.runtime.tree.TerminalNode;
import org.antlr.v4.runtime.tree.TerminalNodeImpl;

import ca.nevdull.mjc.compiler.util.OutputAtom;
import ca.nevdull.mjc.compiler.util.OutputItem;
import ca.nevdull.mjc.compiler.util.OutputList;

public class GeneratePass extends MJBaseListener {
    PassData passData;
    ParseTreeProperty<OutputItem> rands = new ParseTreeProperty<OutputItem>();
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

	public GeneratePass(PassData passData) {
		super();
		this.passData = passData;
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
		/* TODO
		if (t instanceof ParserRuleContext) {
			System.out.print(" type="); System.out.print(passData.types.get((ParserRuleContext)t));
			System.out.print(" symbol="); System.out.print(passData.symbols.get((ParserRuleContext)t));
		} else*/ if (t instanceof TerminalNodeImpl) {
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
    	OutputList instanceStructure;
    	OutputList methodBodies;
    	OutputList classDefinition;
    	OutputList staticInitialization;
 		public ClassDest(ClassSymbol symbol, ClassDest enclosingClass) {
			super();
			this.symbol = symbol;
			this.enclosingClass = enclosingClass;
			this.classStructure = new OutputList();					//this.classStructure.add("/* classStructure */\n");
	    	this.instanceStructure = new OutputList();	    		//this.instanceStructure.add("/* instanceStructure */\n");
	    	this.methodBodies = new OutputList();	    			//this.methodBodies.add("/* methodBodies */\n");
	    	this.classDefinition = new OutputList();				//this.classDefinition.add("/* classDefinition */\n");
	    	this.staticInitialization = new OutputList();			//this.staticInitialization.add("/* staticInitialization */\n");
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

		private File makeFilePath(String suffix) {
			String name = getFileName()+suffix;
			return passData.inputDir == null ? new File(name)
									 : new File(passData.inputDir,name);
		}
 		
		public ClassDest close() {
			PrintStream defnStream = null;
			PrintStream implStream = null;
			try {
		    	defnStream = new PrintStream(makeFilePath(".h"));
		    	implStream = new PrintStream(makeFilePath(".c"));
		    	classStructure.print(defnStream);
		    	instanceStructure.print(defnStream);
		    	methodBodies.print(implStream);
		    	classDefinition.print(implStream);
		    	//staticInitialization is incorporated by defineCreator
			} catch (FileNotFoundException excp) {
				Compiler.error("Unable to open "+excp.getMessage());
			} finally {
				if (defnStream != null) defnStream.close();
				if (implStream != null) implStream.close();
			}
			return enclosingClass;
		}
    }
    ClassDest currDest = null;
    static String indent = "    ";
	OutputList imports = new OutputList();

	@Override public void exitCompilationUnit(MJParser.CompilationUnitContext ctx) { }
    @Override public void exitImportDeclaration(@NotNull MJParser.ImportDeclarationContext ctx) {
		// TODO Read saved symbols
    	StringBuilder qname = new StringBuilder();
    	for (TerminalNode nameComponent : ctx.qualifiedName().Identifier()) {
    		if (qname.length() > 0) qname.append(File.separatorChar);
    		qname.append(nameComponent.getText());
    	}
    	imports.add("#include \"",qname.toString(),".h\"\n\n");
    }
	@Override public void exitTypeDeclaration(MJParser.TypeDeclarationContext ctx) { }
	@Override public void exitModifier(MJParser.ModifierContext ctx) { }
	@Override public void exitClassOrInterfaceModifier(MJParser.ClassOrInterfaceModifierContext ctx) { }
	@Override public void enterClassDeclaration(MJParser.ClassDeclarationContext ctx) {
		Symbol classSymbol = ctx.defn;
		String className = classSymbol.getName();
    	String superName = null;
		ClassSymbol sup = ((ClassSymbol)classSymbol).getSuperClass();
		if (sup != null) {
	    	superName = sup.getName();
		}
		currDest = new ClassDest(ctx.defn, currDest);
		
		currDest.classStructure.add("#ifndef ",className,"_DEFN\n");
		currDest.classStructure.add("#define ",className,"_DEFN\n\n");
		currDest.classStructure.add("#include \"mj.h\"\n\n");
		currDest.classStructure.add(imports);
		if (superName != null) currDest.classDefinition.add("#include \"",superName,".h\"\n\n");
		currDest.classStructure.add("typedef struct ",className,"_obj *",className,";\n");
		currDest.classStructure.add("struct ",className,"_class_obj"," {\n");
		currDest.classStructure.add(indent).add("struct Class_class_obj"," *_class;\n");
		currDest.classStructure.add(indent).add("struct ",className,"_class_data"," {\n");
		currDest.classStructure.add(indent,indent).add("struct /*Object_data*/{} _super;\n");
		currDest.classStructure.add(indent,indent).add("Class _superclass;\n");
		currDest.instanceStructure.add("struct ",className,"_data"," {\n");
		if (superName != null) currDest.instanceStructure.add(indent).add("struct ",superName,"_data"," _super;\n");
		
		currDest.methodBodies.add("#include \"",className,".h\"\n\n");
		if (superName != null) currDest.classDefinition.add("extern struct Class_class_obj ",superName,"_class",";\n");
		currDest.classDefinition.add("struct ",className,"_class_obj"," ",className,"_class"," = {\n");
		currDest.classDefinition.add(indent).add("._class = NULL,/*later &Class_class,*/\n");
		currDest.classDefinition.add(indent).add("._data = {\n");
		currDest.classDefinition.add(indent,indent).add("._super = {},\n");
		if (superName != null) currDest.classDefinition.add(indent,indent).add("._superclass = ","&",superName,"_class",",\n");
		else currDest.classDefinition.add(indent,indent).add("._superclass = NULL,\n");
		
		Scope encl = ((ClassSymbol)classSymbol).getEnclosingScope();
	}
	@Override public void exitClassDeclaration(MJParser.ClassDeclarationContext ctx) {
		ClassSymbol classSymbol = ctx.defn;
		String className = classSymbol.getName();
		if (!currDest.haveConstructor) {
			defineDefaultConstructor(classSymbol);			
		}
    	defineCreator(classSymbol);
    	
		// Save symbols for import
        try {
			FileOutputStream fos = new FileOutputStream(currDest.makeFilePath(Compiler.IMPORT_SUFFIX));
	        ObjectOutputStream oos = new ObjectOutputStream(fos);
			oos.writeObject(classSymbol);
	        oos.close();
		} catch (IOException excp) {
			Compiler.error("Unable to write symbols "+excp.getMessage());
		}

        currDest.classStructure.add(indent).add("} _data;\n");
		currDest.classStructure.add("};\n");
		currDest.classStructure.add("extern struct ",className,"_class_obj"," ",className,"_class",";\n\n");
		currDest.instanceStructure.add("};\n");
		currDest.instanceStructure.add("struct ",className,"_obj"," {\n");
		currDest.instanceStructure.add(indent).add("struct ",className,"_class_obj"," *_class;\n");
		currDest.instanceStructure.add(indent).add("struct ",className,"_data"," _data;\n");
		currDest.instanceStructure.add("};\n");
		currDest.instanceStructure.add("\n#endif /*",className,"_DEFN*/\n");
		
		currDest.classDefinition.add(indent).add("}\n");
		currDest.classDefinition.add("};\n");
		
		currDest = currDest.close();
	}

	private void defineDefaultConstructor(ClassSymbol classSymbol) {
		Token token = classSymbol.getToken();
		MethodSymbol method = new MethodSymbol(token, classSymbol);
		classSymbol.define(method);
		method.setType(VoidType.getInstance());
		beginMethod(method.getName(), method.getType());
		beginFormalParameters(classSymbol.getName());
		endFormalParameters();
		beginBlock();
		endBlock();
		endMethod();
	}

	private void defineCreator(ClassSymbol classSymbol) {
		String className = classSymbol.getName();
		//currDest.methodBodies.add("void *calloc(size_t nmemb, size_t size);\n");
		//TODO should be a static method (no "this" parameter)
		beginMethod("_create", classSymbol);
		beginFormalParameters(className); //TODO eliminate formal parameter
		endFormalParameters();
		beginBlock();
		currDest.methodBodies.add(indent).add("/* 'this' will always be NULL */\n");
		currDest.methodBodies.add(indent).add("if (",className,"_class._class == NULL) ",className,"_class._class = Class_class_p;\n");
		currDest.methodBodies.add(indent).add(className, " new;\n");
		currDest.methodBodies.add(indent).add("new=calloc(1, sizeof *new);\n");
		//TODO allocation exception
		currDest.methodBodies.add(indent).add("new->_class=&",className,"_class;\n");
		currDest.methodBodies.add(currDest.staticInitialization);
		currDest.methodBodies.add(indent).add("return new;\n");
		endBlock();
		endMethod();
	}
	@Override public void exitClassBody(MJParser.ClassBodyContext ctx) { }
	@Override public void exitMemberClassBodyDeclaration(MJParser.MemberClassBodyDeclarationContext ctx) { }
	@Override public void exitBlockClassBodyDeclaration(MJParser.BlockClassBodyDeclarationContext ctx) { }
	@Override public void exitEmptyClassBodyDeclaration(MJParser.EmptyClassBodyDeclarationContext ctx) { }
	@Override public void exitMemberDeclaration(MJParser.MemberDeclarationContext ctx) { }
	@Override public void enterMethodDeclaration(MJParser.MethodDeclarationContext ctx) {
		Symbol method = ctx.defn;
		if (ctx.methodBody() != null) {
		} else if (ctx.NATIVE() != null) {
			currDest.methodBodies.add("extern ");
		} else {
			// abstract
			currDest.methodBodies.add("/*"); //TODO something less kuldgey!
		}
		beginMethod(method.getName(), method.getType());
	}
	@Override public void exitMethodDeclaration(MJParser.MethodDeclarationContext ctx) {
		if (ctx.methodBody() != null) {
		} else if (ctx.NATIVE() != null) {
			currDest.methodBodies.add(";");
		} else {
			currDest.methodBodies.add("*/");
		}
		endMethod();
	}

	private void beginMethod(String methodName, Type returnType) {
		currDest.classStructure.add(indent,indent).add(typeName("(*"+methodName+")", returnType));
		String qualName = currDest.getSymbol().getName()+"_"+methodName;
		currDest.classDefinition.add(indent,indent).add(".",methodName," = &",qualName).add(",\n");
		currDest.methodBodies.add(typeName(qualName, returnType));
	}
	private void endMethod() {
		currDest.classStructure.add(";\n");
		//nothing for currDest.classInitialization
		currDest.methodBodies.add("\n");
	}

	@Override public void enterConstructorDeclaration(MJParser.ConstructorDeclarationContext ctx) {
		Symbol method = ctx.defn;
		beginMethod(method.getName(), method.getType());
		currDest.haveConstructor = true;
	}
	@Override public void exitConstructorDeclaration(MJParser.ConstructorDeclarationContext ctx) {
		endMethod();
	}
	@Override public void enterFieldDeclaration(MJParser.FieldDeclarationContext ctx) {
		//LATER could order fields by descending alignment
		for (MJParser.VariableDeclaratorContext vd : ctx.variableDeclarators().variableDeclarator()) {
			Symbol sym = vd.defn;
			currDest.instanceStructure.add(indent).add(typeName(sym.getName(), sym.getType())).add(";\n");			
		}
	}
	@Override public void exitFieldDeclaration(MJParser.FieldDeclarationContext ctx) {
		for (MJParser.VariableDeclaratorContext vd : ctx.variableDeclarators().variableDeclarator()) {
			Symbol sym = vd.defn;
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
		passUpType(ctx, ctx.expression());
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
		currDest.classStructure.add("(",className," ","this");
		currDest.methodBodies.add("(",className," ","this");
	}
	private void endFormalParameters() {
		currDest.classStructure.add(")");
		currDest.methodBodies.add(") ");
	}
	
	@Override public void exitFormalParameterList(MJParser.FormalParameterListContext ctx) { }
	@Override public void enterFormalParameter(MJParser.FormalParameterContext ctx) {
		Symbol sym = ctx.defn;
		String symName = sym.getName();
		Type symType = sym.getType();
		currDest.classStructure.add(", ").add(typeName(symName, symType));			
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
		ctx.tipe = t;
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
			Symbol sym = vd.defn;
			currDest.methodBodies.add(indent).add(typeName(sym.getName(), sym.getType())).add(";\n");
		}
	}
	@Override public void exitLocalVariableDeclaration(MJParser.LocalVariableDeclarationContext ctx) {
		for (MJParser.VariableDeclaratorContext vd : ctx.variableDeclarators().variableDeclarator()) {
			Symbol sym = vd.defn;
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
		Type leftType = left.tipe;
		Type typeRight = right.tipe;
		Type typeResult = leftType;  //TODO determine result type
		String rand = "_e"+nextreg();
		currDest.methodBodies.add(indent).add(typeName(rand,typeResult)).add("=").add(rands.get(left)).add(op).add(rands.get(right)).add(";\n");
		rands.put(ctx, new OutputAtom(rand));
	}

	@Override public void exitAssignExpression(MJParser.AssignExpressionContext ctx) {
		MJParser.ExpressionContext dest = ctx.expression(0);
		//System.out.println("exitAssignExpression dest");  printContextTree(dest,"    ");
		Symbol sym = passData.symbols.get(dest);
		if (sym == null) {
			Compiler.error(dest.getStop(), dest.getText()+" is not a variable or field","AssignExpression");
			return;
		} else if (!(sym instanceof VariableSymbol)) {
			Compiler.error(dest.getStop(), dest.getText()+" is not a variable or field","AssignExpression");
			return;
		}
		MJParser.ExpressionContext val = ctx.expression(1);
		Type type = val.tipe;
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
		Symbol sym = passData.symbols.get(exp);
		if (sym == null) {
			Compiler.error(exp.getStop(), exp.getText()+" is not a method","CallExpression");
			return;
		} else if (!(sym instanceof MethodSymbol)) {
			Compiler.error(exp.getStop(), exp.getText()+" is not a method","CallExpression");
			ctx.tipe = UnknownType.getInstance();
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
		ctx.tipe = type;
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
		ctx.tipe = ctx.primary().tipe;
		rands.put(ctx, rands.get(ctx.primary()));
		passData.symbols.put(ctx, passData.symbols.get(ctx.primary()));
		//System.out.println("exitPrimExpression "+ rands.get(ctx));
	}
	@Override public void exitCondOrExpression(MJParser.CondOrExpressionContext ctx) { }
	@Override public void exitCastExpression(MJParser.CastExpressionContext ctx) { }
	@Override public void exitDotExpression(MJParser.DotExpressionContext ctx) {
 		MJParser.ExpressionContext exp = ctx.expression();
		Type type = exp.tipe;
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
        	Compiler.error(token, "not a reference: "+type,"DotExpression");
        }
		OutputList ref = new OutputList();
		if (sym != null && sym instanceof MethodSymbol) {
			ctx.tipe = (MethodSymbol)sym;  // let method be a pseudo-type until called
			//TODO handle static method
			ref.add("(*(");
			checkRef(ref, rands.get(exp)).add("->_class->_data.").add(name).add("))(").add(rands.get(exp));			
		} else {
			ctx.tipe = type;
			checkRef(ref, rands.get(exp)).add("->_data.").add(name);
		}
		passData.symbols.put(ctx, sym);
		rands.put(ctx, ref);
	}

	private OutputList checkRef(OutputList out, OutputItem ref) {
		return out.add("((").add(ref).add(")?(").add(ref).add("):throwNPE())");
	}
	@Override public void exitShiftExpression(MJParser.ShiftExpressionContext ctx) { }
	@Override public void exitPlusExpression(MJParser.PlusExpressionContext ctx) { }
	@Override public void exitNewExpression(MJParser.NewExpressionContext ctx) {
		ctx.tipe = ctx.creator().tipe;
		rands.put(ctx, rands.get(ctx.creator()));
	}
	@Override public void exitLiteralPrimary(MJParser.LiteralPrimaryContext ctx) {
		ctx.tipe = ctx.literal().tipe;
		rands.put(ctx, rands.get(ctx.literal()));
	}
	@Override public void exitSuperPrimary(MJParser.SuperPrimaryContext ctx) { }
	@Override public void exitIdentifierPrimary(MJParser.IdentifierPrimaryContext ctx) {
        Symbol sym = ctx.defn;
		Type symType = null;
        if (sym != null) {
    		String symName = sym.getName();
        	symType = sym.getType();
			System.out.println(symName+" type "+symType);
			ctx.tipe = symType;
            Scope scope = sym.getScope();
            OutputItem access = null;
            if (scope instanceof BaseScope || scope instanceof MethodSymbol) {
            	access = new OutputAtom(symName);
            } else if (scope instanceof ClassSymbol) {
            	access = new OutputList();
        		if (sym != null && sym instanceof MethodSymbol) {
        			// "this" doesn't need a null pointer check
        			((OutputList)access).add("(*(this->_class->_data.",symName).add("))(").add("this");
        			ctx.tipe = (MethodSymbol)sym;  // doesn't have result type until called
        		} else {
        			((OutputList)access).add("this->_data.",symName);
        		}
            } else {
            	System.out.println("exitIdentifierPrimary scope is "+scope.getClass().getSimpleName());
            }
            rands.put(ctx, access);
        } else {
	        if (symType == null) symType = UnknownType.getInstance();
	        ctx.tipe = symType;
			rands.put(ctx, new OutputAtom("_unknown"));
        }
	}
	@Override public void exitThisPrimary(MJParser.ThisPrimaryContext ctx) {
		ctx.tipe = currDest.getSymbol();
		rands.put(ctx, new OutputAtom("this"));
	}
	@Override public void exitParenPrimary(MJParser.ParenPrimaryContext ctx) {
		ctx.tipe = ctx.expression().tipe;
		rands.put(ctx, rands.get(ctx.expression()));
		passData.symbols.put(ctx, passData.symbols.get(ctx.expression()));
	}
	@Override public void exitArrayCreator(MJParser.ArrayCreatorContext ctx) { }
	@Override public void enterClassCreator(MJParser.ClassCreatorContext ctx) {
		if (ctx.createdName().primitiveType() == null) {
			// must be class type
			Symbol sym = ctx.createdName().defn;
			Type symType = null;
			if (sym != null && sym instanceof ClassSymbol) {
				symType = (ClassSymbol)sym;
			} else {
				symType = UnknownType.getInstance();
			}
			System.out.println("ClassCreator "+sym+" type "+symType);
			String rand = "_n"+nextreg();
			System.out.println("ClassCreator"+sym);
			currDest.methodBodies.add(indent).add(typeName(rand,symType)).add("=(*(",sym.getName(),"_class._data._create))(NULL);\n");			
			currDest.methodBodies.add(indent).add("(*(",rand,"->_class->_data.",sym.getName(),"))(",rand);
			MJParser.ExpressionListContext argList = ctx.classCreatorRest().arguments().expressionList();
			if (argList != null) {
				for (MJParser.ExpressionContext arg : argList.expression()) {
					currDest.methodBodies.add(",").add(rands.get(arg));
				}
			}
			currDest.methodBodies.add(");\n");
			rands.put(ctx, new OutputAtom(rand));
			ctx.tipe = symType;
		} else {
			Compiler.error(ctx.getStart(), "new cannot be applied to a primitive type", "ClassCreator");
		}
	}
	@Override public void exitClassCreator(MJParser.ClassCreatorContext ctx) { }
	@Override public void exitCreatedName(MJParser.CreatedNameContext ctx) { }
	@Override public void exitArrayCreatorRest(MJParser.ArrayCreatorRestContext ctx) { }
	@Override public void exitClassCreatorRest(MJParser.ClassCreatorRestContext ctx) { }
	@Override public void enterArguments(MJParser.ArgumentsContext ctx) { }
	@Override public void exitArguments(MJParser.ArgumentsContext ctx) { }

}
