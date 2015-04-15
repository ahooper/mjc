package ca.nevdull.mjc.compiler;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.TerminalNode;
import org.antlr.v4.runtime.tree.TerminalNodeImpl;

public class CodeVisitor {
	PassData passData;
	static String indent = "    ";

	public CodeVisitor(PassData passData) {
    	super();
    	this.passData = passData;
    	traceVisit = passData.options.trace.contains("CodeVisitor");
	}
	
	////////////////////////////////////////////////////////////////////////////
	// Tracing methods

	boolean traceVisit = false;
	int nest = 0;
	
	void traceIn(String m) {
		for (int i = 0;  i < nest;  i += 1) System.out.print(indent);
		System.out.print('/');
		System.out.println(m);
		nest += 1;
	}
	void traceOut(String m) {
		nest -= 1;
		for (int i = 0;  i < nest;  i += 1) System.out.print(indent);
		System.out.print('\\');
		System.out.println(m);
	}
	void traceInOut(String m) {
		for (int i = 0;  i < nest;  i += 1) System.out.print(indent);
		System.out.print('*');
		System.out.println(m);
	}
	void traceDisc(String m) {
		for (int i = 0;  i < nest;  i += 1) System.out.print(indent);
		System.out.print('.');
		System.out.println(m);
	}
	void fail(String m) {
		Compiler.error(m);
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
	
	////////////////////////////////////////////////////////////////////////////
	//TODO this should go away after all parse tree structures are coded
	
	private void put(TerminalNode id) {
		put(id.getSymbol().getText());
		put(" ");
	}
	private void put(String text) {
		System.out.print(text);
	}
	
	////////////////////////////////////////////////////////////////////////////
	// Translation of Java types to C implementation
	
    Map<PrimitiveType,String> primitiveTypeMap = new IdentityHashMap<PrimitiveType,String>();   
    {
    	primitiveTypeMap.put(PrimitiveType.booleanType,	"jboolean");
    	primitiveTypeMap.put(PrimitiveType.byteType,	"jbyte");
    	primitiveTypeMap.put(PrimitiveType.charType,	"jchar");
    	primitiveTypeMap.put(PrimitiveType.doubleType,	"jdouble");
    	primitiveTypeMap.put(PrimitiveType.floatType,	"jfloat");
    	primitiveTypeMap.put(PrimitiveType.intType,		"jint");
    	primitiveTypeMap.put(PrimitiveType.longType,	"jlong");
    	primitiveTypeMap.put(PrimitiveType.shortType,	"jshort");
    }
	
	////////////////////////////////////////////////////////////////////////////
    // C variable names to hold intermediate values
    
    int reg = 1;
    public int nextreg() {
    	return reg++;
    }
    
    // String literals
    
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
	
	////////////////////////////////////////////////////////////////////////////
	
    /**
     * The output components of a block
     */
  
    class BlockDest {
    	private BlockDest enclosingBlock;
    	OutputList intermediateDeclarations;
    	OutputList code;
    	
    	public BlockDest(BlockDest enclosingBlock) {
    		this(enclosingBlock,enclosingBlock.code);
    	}
    	
    	public BlockDest(BlockDest enclosingBlock, OutputList destination) {
    		super();
    		this.enclosingBlock = enclosingBlock;
    		this.intermediateDeclarations = new OutputList();
    		this.code = new OutputList();
    		destination.add("{\n").add(this.intermediateDeclarations).add(this.code).add("}\n");
    	}
    	
    	public BlockDest close() {
    		return enclosingBlock;
    	}
    }
    
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
    	OutputList classInitialization;
    	OutputList instanceInitialization;
    	BlockDest block;
 		public ClassDest(ClassSymbol symbol, ClassDest enclosingClass) {
			super();
			this.symbol = symbol;
			this.enclosingClass = enclosingClass;
			this.classStructure = new OutputList();					//this.classStructure.add("/* classStructure */\n");
	    	this.instanceStructure = new OutputList();	    		//this.instanceStructure.add("/* instanceStructure */\n");
	    	this.methodBodies = new OutputList();	    			//this.methodBodies.add("/* methodBodies */\n");
	    	this.classDefinition = new OutputList();				//this.classDefinition.add("/* classDefinition */\n");
	    	this.classInitialization = new OutputList();			//this.classInitialization.add("/* classInitialization */\n");
	    	this.instanceInitialization = new OutputList();			//this.instanceInitialization.add("/* instanceInitialization */\n");
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
			return passData.outputDir == null ? new File(name)
											 : new File(passData.outputDir,name);
		}
		
		public void beginBlock(OutputList dest) {
			block = new BlockDest(block,dest);
		}

		public void beginBlock() {
			assert block != null;
			block = new BlockDest(block);
		}
		
		public void endBlock() {
			assert block != null;
			block = block.enclosingBlock;
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
		    	// classInitialization and instanceInitialization are incorporated by defineCreator
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
	OutputList imports = new OutputList();

	private void sourceComment(ParserRuleContext ctx) {
		if (currDest.block != null) {
			currDest.block.code.add("// ",Integer.toString(ctx.getStart().getLine()),": ",ctx.getText(),"\n");
		} else {
			currDest.methodBodies.add("// ",Integer.toString(ctx.getStart().getLine()),": ",ctx.getText(),"\n");			
		}
	}

	////////////////////////////////////////////////////////////////////////////

    public Void visitCompilationUnit(MJParser.CompilationUnitContext ctx) {
        if (traceVisit) traceIn("visitCompilationUnit");
        for (MJParser.ImportDeclarationContext i : ctx.importDeclaration()) visitImportDeclaration(i);
        for (MJParser.TypeDeclarationContext t : ctx.typeDeclaration()) visitTypeDeclaration(t);
        if (traceVisit) traceOut("visitCompilationUnit");
        return null;
    }
    
    public Void visitImportDeclaration(MJParser.ImportDeclarationContext ctx) {
        if (traceVisit) traceIn("visitImportDeclaration");
		sourceComment(ctx);
		// TODO Read saved symbols
    	StringBuilder qname = new StringBuilder();
    	for (TerminalNode nameComponent : ctx.qualifiedName().Identifier()) {
    		if (qname.length() > 0) qname.append(File.separatorChar);
    		qname.append(nameComponent.getText());
    	}
        if (ctx.getChildCount() > 3) {
        	//TODO .* - find name in class path, list directory
        }
    	imports.add("#include \"",qname.toString(),".h\"\n\n");
        if (traceVisit) traceOut("visitImportDeclaration");
        return null;
    }
    
    public Void visitTypeDeclaration(MJParser.TypeDeclarationContext ctx) {
        if (traceVisit) traceIn("visitTypeDeclaration");
        MJParser.ClassDeclarationContext c1 = ctx.classDeclaration();  if (c1 != null) {
        	visitClassDeclaration(c1);
        } else {
        }
        if (traceVisit) traceOut("visitTypeDeclaration");
        return null;
    }
    
    public Void visitModifier(MJParser.ModifierContext ctx) {
        if (traceVisit) traceIn("visitModifier");
        MJParser.ClassOrInterfaceModifierContext c = ctx.classOrInterfaceModifier();  if (c != null) visitClassOrInterfaceModifier(c);
        if (traceVisit) traceOut("visitModifier");
        return null;
    }
    
    public Void visitClassOrInterfaceModifier(MJParser.ClassOrInterfaceModifierContext ctx) {
        if (traceVisit) traceInOut("visitClassOrInterfaceModifier");
        return null;
    }
    
    public Void visitClassDeclaration(MJParser.ClassDeclarationContext ctx) {
        if (traceVisit) traceIn("visitClassDeclaration");
        MJParser.TypeContext t = ctx.type();  if (t != null) {
        	visitType(t);
        }
		ClassSymbol classSymbol = (ClassSymbol)ctx.defn;
		String className = classSymbol.getName();
    	String superName = null;
		ClassSymbol sup = classSymbol.getSuperClass();
		if (sup != null) {
	    	superName = sup.getName();
		}
		if (classSymbol.access == null) classSymbol.setAccess(Access.DEFAULT);
		currDest = new ClassDest(ctx.defn, currDest);
		
		currDest.classStructure.add("#ifndef ",className,"_DEFN\n")
							   .add("#define ",className,"_DEFN\n\n")
							   .add("#include \"mj.h\"\n\n");
		currDest.classStructure.add(imports);
		if (superName != null) currDest.classDefinition.add("#include \"",superName,".h\"\n\n");
		currDest.classStructure.add("typedef struct ",className,"_obj *",className,";\n")
		   					   .add("struct ",className,"_class_obj"," {\n")
							   .add(indent,"struct Class_class_obj"," *_class;\n")
							   .add(indent,"struct ",className,"_class_data"," {\n")
							   .add(indent,indent).add("struct /*Object_data*/{} _super;\n")
							   .add(indent,indent).add("Class _superclass;\n");
		currDest.instanceStructure.add("struct ",className,"_data"," {\n");
		if (superName != null) currDest.instanceStructure.add(indent,"struct ",superName,"_data"," _super;\n");
		
		currDest.methodBodies.add("#include \"",className,".h\"\n\n");
		if (superName != null) currDest.classDefinition.add("extern struct Class_class_obj ",superName,"_class",";\n");
		currDest.classDefinition.add("struct ",className,"_class_obj"," ",className,"_class"," = {\n")
							    .add(indent,"._class = NULL,/*later &Class_class,*/\n")
							    .add(indent,"._data = {\n")
							    .add(indent,indent).add("._super = {},\n");
		if (superName != null) currDest.classDefinition.add(indent,indent).add("._superclass = ","&",superName,"_class",",\n");
		else currDest.classDefinition.add(indent,indent).add("._superclass = NULL,\n");
		
		Scope encl = ((ClassSymbol)classSymbol).getEnclosingScope();
		//TODO instance member for enclosing instance

		MJParser.ClassBodyContext c = ctx.classBody();  if (c != null) visitClassBody(c);

		if (!currDest.haveConstructor) {
			defineDefaultConstructor(classSymbol);			
		}
    	defineCreator(classSymbol);
    	
		// Save symbols for import
        try {
			FileOutputStream fos = new FileOutputStream(currDest.makeFilePath(Compiler.IMPORT_SUFFIX));
	        PrintWriter pw = new PrintWriter(fos);
			for (Entry<String, Symbol> globEnt : passData.globals.symbols.entrySet()) {
				Symbol globSym = globEnt.getValue();
				if (globSym == classSymbol) continue;
				pw.append("import ").append(globSym.getName()).append(";\n");
			}
	        classSymbol.writeImport(pw);
	        pw.close();
		} catch (IOException excp) {
			Compiler.error("Unable to write symbols "+excp.getMessage());
		}

        currDest.classStructure.add(indent,"} _data;\n")
        					   .add("};\n")
        					   .add("extern struct ",className,"_class_obj"," ",className,"_class",";\n\n");
		currDest.instanceStructure.add("};\n")
								  .add("struct ",className,"_obj"," {\n")
								  .add(indent,"struct ",className,"_class_obj"," *_class;\n")
								  .add(indent,"struct ",className,"_data"," _data;\n")
								  .add("};\n")
								  .add("\n#endif /*",className,"_DEFN*/\n");
		
		currDest.classDefinition.add(indent,"}\n")
								.add("};\n");
		
		currDest = currDest.close();

		if (traceVisit) traceOut("visitClassDeclaration");
        return null;
    }

	private void defineDefaultConstructor(ClassSymbol classSymbol) {
/*
		Token token = classSymbol.getToken();
		MethodSymbol method = new MethodSymbol(token, classSymbol);
		classSymbol.define(method);
		method.setType(VoidType.getInstance());
		beginMethod(method.getName(), method.getType());
*/
		beginMethod("_init", classSymbol);
		beginFormalParameters(classSymbol.getName());
		endFormalParameters();
        currDest.beginBlock(currDest.methodBodies);
		currDest.block.code.add(indent,"/*defaultConstructor*/\n");
		currDest.block.code.add(indent,"return this;\n");
        currDest.endBlock();
		endMethod();
	}

	private void defineCreator(ClassSymbol classSymbol) {
		String className = classSymbol.getName();
		//TODO should be a static method (no "this" parameter)
		beginMethod("_create", classSymbol);
		beginFormalParameters(null/*no 'this' parameter*/);
		endFormalParameters();
		currDest.methodBodies.add("{\n")
							 .add(indent,"if (",className,"_class._class == NULL) {\n")
							 .add(indent,indent,className,"_class._class = Class_class_p;\n")
							 .add(indent,indent,"// class initialization\n")
							 .add(currDest.classInitialization)
							 .add(indent,"}\n")
							 .add(indent,className, " new;\n")
							 .add(indent,"new=calloc(1, sizeof *new);\n");
		//TODO allocation exception
		currDest.methodBodies.add(indent,"new->_class=&",className,"_class;\n")
							 .add(indent,"// instance initialization\n")
							 .add(currDest.instanceInitialization)
							 .add(indent,"return new;\n")
							 .add("}\n");
		endMethod();
	}
    
    public Void visitClassBody(MJParser.ClassBodyContext ctx) {
        if (traceVisit) traceIn("visitClassBody");
        for (MJParser.ClassBodyDeclarationContext c : ctx.classBodyDeclaration()) visitClassBodyDeclaration(c);
        if (traceVisit) traceOut("visitClassBody");
        return null;
    }
    public Void visitEmptyClassBodyDeclaration(MJParser.EmptyClassBodyDeclarationContext ctx) {
        if (traceVisit) traceInOut("visitEmptyClassBodyDeclaration");
        return null;
    }
    public Void visitBlockClassBodyDeclaration(MJParser.BlockClassBodyDeclarationContext ctx) {
        if (traceVisit) traceIn("visitBlockClassBodyDeclaration");
        TerminalNode s = ctx.STATIC();
        if (s != null) currDest.beginBlock(currDest.classInitialization);
        else currDest.beginBlock(currDest.instanceInitialization);
        //TODO if static, limit access within block
        MJParser.BlockContext b = ctx.block();  if (b != null) visitBlock(b);
        currDest.endBlock();
        if (traceVisit) traceOut("visitBlockClassBodyDeclaration");
        return null;
    }
    public Void visitMemberClassBodyDeclaration(MJParser.MemberClassBodyDeclarationContext ctx) {
        if (traceVisit) traceIn("visitMemberClassBodyDeclaration");
        for (MJParser.ModifierContext m : ctx.modifier()) visitModifier(m);
        MJParser.MemberDeclarationContext md = ctx.memberDeclaration();  if (md != null) visitMemberDeclaration(md);
        if (traceVisit) traceOut("visitMemberClassBodyDeclaration");
        return null;
    }
    public Void visitClassBodyDeclaration(MJParser.ClassBodyDeclarationContext ctx) {
        if (traceVisit) traceDisc("visitClassBodyDeclaration");
        if (ctx instanceof MJParser.EmptyClassBodyDeclarationContext) visitEmptyClassBodyDeclaration((MJParser.EmptyClassBodyDeclarationContext) ctx);
        else if (ctx instanceof MJParser.BlockClassBodyDeclarationContext) visitBlockClassBodyDeclaration((MJParser.BlockClassBodyDeclarationContext) ctx);
        else if (ctx instanceof MJParser.MemberClassBodyDeclarationContext) visitMemberClassBodyDeclaration((MJParser.MemberClassBodyDeclarationContext) ctx);
        else fail("visitClassBodyDeclaration unrecognized "+ctx.getClass().getSimpleName());
        return null;
    }
    public Void visitMemberDeclaration(MJParser.MemberDeclarationContext ctx) {
        if (traceVisit) traceIn("visitMemberDeclaration");
        MJParser.MethodDeclarationContext m = ctx.methodDeclaration();  if (m != null) visitMethodDeclaration(m);
        MJParser.FieldDeclarationContext f = ctx.fieldDeclaration();  if (f != null) visitFieldDeclaration(f);
        MJParser.ConstructorDeclarationContext co = ctx.constructorDeclaration();  if (co != null) visitConstructorDeclaration(co);
        MJParser.ClassDeclarationContext cl = ctx.classDeclaration();  if (cl != null) {
        	visitClassDeclaration(cl);
        }
        if (traceVisit) traceOut("visitMemberDeclaration");
        return null;
    }
    public Void visitMethodDeclaration(MJParser.MethodDeclarationContext ctx) {
        if (traceVisit) traceIn("visitMethodDeclaration");
		MethodSymbol method = (MethodSymbol)ctx.defn;
        MJParser.TypeContext t;
        MJParser.MethodBodyContext m = ctx.methodBody();  
        if (m != null) {
        	if (method.isAbstract) Compiler.error(ctx.start,"abstract method may not have an implementation");
        } else if (ctx.NATIVE() != null) {
        	currDest.methodBodies.add("extern ");
        } else {
			// abstract
        	if (!method.isAbstract) Compiler.error(ctx.start,"unimplemented method must have abtract modifier");
			currDest.methodBodies.add("/*"); //TODO something less kuldgey!
        }
		if ((t = ctx.type()) != null) visitType(t);
		beginMethod(method.getName(), method.getType());
        MJParser.FormalParametersContext f = ctx.formalParameters();  if (f != null) visitFormalParameters(f);
        MJParser.ArrayDimensionContext a = ctx.arrayDimension();  if (a != null) visitArrayDimension(a);
        if (m != null) {
        	visitMethodBody(m);
        } else if (ctx.NATIVE() != null) {
			currDest.methodBodies.add(";\n");
        } else {
			currDest.methodBodies.add("*/\n");
        }
		endMethod();
        if (traceVisit) traceOut("visitMethodDeclaration");
        return null;
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

	public Void visitConstructorDeclaration(MJParser.ConstructorDeclarationContext ctx) {
        if (traceVisit) traceIn("visitConstructorDeclaration");
		Symbol method = ctx.defn;
		beginMethod("_init", method.getType());
		currDest.haveConstructor = true;
        MJParser.FormalParametersContext f = ctx.formalParameters();  if (f != null) visitFormalParameters(f);
        MJParser.ConstructorBodyContext b = ctx.constructorBody();  if (b != null) visitConstructorBody(b);
		currDest.methodBodies.add(indent,"return this;\n");
		endMethod();
        if (traceVisit) traceOut("visitConstructorDeclaration");
        return null;
    }
    public Void visitFieldDeclaration(MJParser.FieldDeclarationContext ctx) {
        if (traceVisit) traceIn("visitFieldDeclaration");
		sourceComment(ctx);
		//LATER could order fields by descending alignment
        MJParser.TypeContext t = ctx.type();  if (t != null) visitType(t);
        MJParser.VariableDeclaratorsContext v = ctx.variableDeclarators();  if (v != null) visitVariableDeclarators(v,currDest.instanceStructure,currDest.instanceInitialization);  //TODO classInitialization if static
        if (traceVisit) traceOut("visitFieldDeclaration");
        return null;
    }
    public Void visitVariableDeclarators(MJParser.VariableDeclaratorsContext ctx, OutputList decl, OutputList init) {
        if (traceVisit) traceIn("visitVariableDeclarators");
        for (MJParser.VariableDeclaratorContext v : ctx.variableDeclarator()) {
        	visitVariableDeclarator(v,decl,init);
        }
        if (traceVisit) traceOut("visitVariableDeclarators");
        return null;
    }
    public Void visitVariableDeclarator(MJParser.VariableDeclaratorContext ctx, OutputList decl, OutputList init) {
        if (traceVisit) traceIn("visitVariableDeclarator");
        MJParser.VariableDeclaratorIdContext v = ctx.variableDeclaratorId();  if (v != null) visitVariableDeclaratorId(v);
		Symbol sym = ctx.defn;
		decl.add(indent).add(typeName(sym.getName(), sym.getType())).add(";\n");
        MJParser.VariableInitializerContext v1 = ctx.variableInitializer();  if (v1 != null) {
            if (sym.isStatic) currDest.beginBlock(currDest.classInitialization);
            else currDest.beginBlock(currDest.instanceInitialization);
        	visitVariableInitializer(v1,init);
			init.add(indent).add(sym.getName(),"=").add(v1.ref).add(";\n");
			endBlock();
        }
        if (traceVisit) traceOut("visitVariableDeclarator");
        return null;
    }
    
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
    
	public Void visitVariableDeclaratorId(MJParser.VariableDeclaratorIdContext ctx) {
        if (traceVisit) traceIn("visitVariableDeclaratorId");
        MJParser.ArrayDimensionContext a = ctx.arrayDimension();  if (a != null) visitArrayDimension(a);
        if (traceVisit) traceOut("visitVariableDeclaratorId");
        return null;
    }
    public Void visitArrayVariableInitializer(MJParser.ArrayVariableInitializerContext ctx, OutputList init) {
        if (traceVisit) traceIn("visitArrayVariableInitializer");
        MJParser.ArrayInitializerContext a = ctx.arrayInitializer();  if (a != null) visitArrayInitializer(a, init);
        if (traceVisit) traceOut("visitArrayVariableInitializer");
        return null;
    }
    public Void visitSimpleVariableInitializer(MJParser.SimpleVariableInitializerContext ctx, OutputList init) {
        if (traceVisit) traceIn("visitSimpleVariableInitializer");
        MJParser.ExpressionContext e = ctx.expression();  if (e != null) visitExpression(e);
        ctx.tipe = e.tipe;
        ctx.ref = e.ref;
        if (traceVisit) traceOut("visitSimpleVariableInitializer");
        return null;
    }
    public Void visitVariableInitializer(MJParser.VariableInitializerContext ctx, OutputList init) {
        if (traceVisit) traceDisc("visitVariableInitializer");
        if (ctx instanceof MJParser.ArrayVariableInitializerContext) visitArrayVariableInitializer((MJParser.ArrayVariableInitializerContext) ctx, init);
        else if (ctx instanceof MJParser.SimpleVariableInitializerContext) visitSimpleVariableInitializer((MJParser.SimpleVariableInitializerContext) ctx, init);
        else fail("visitVariableInitializer unrecognized "+ctx.getClass().getSimpleName());
        return null;
    }
    public Void visitArrayInitializer(MJParser.ArrayInitializerContext ctx, OutputList init) {
        if (traceVisit) traceIn("visitArrayInitializer");
        put("{");
        String sep = null;
        for (MJParser.VariableInitializerContext v : ctx.variableInitializer()) { 
        	if (sep != null) put(sep);
        	visitVariableInitializer(v, init);
        	sep = ",";
        }
        put("}");
        if (traceVisit) traceOut("visitArrayInitializer");
        return null;
    }
    public Void visitObjectType(MJParser.ObjectTypeContext ctx) {
        if (traceVisit) traceIn("visitObjectType");
        MJParser.ClassOrInterfaceTypeContext c = ctx.classOrInterfaceType();  if (c != null) visitClassOrInterfaceType(c);
        MJParser.ArrayDimensionContext a = ctx.arrayDimension();  if (a != null) visitArrayDimension(a);
        if (traceVisit) traceOut("visitObjectType");
        return null;
    }
    public Void visitPrimitType(MJParser.PrimitTypeContext ctx) {
        if (traceVisit) traceIn("visitPrimitType");
        MJParser.PrimitiveTypeContext p = ctx.primitiveType();  if (p != null) visitPrimitiveType(p);
        MJParser.ArrayDimensionContext a = ctx.arrayDimension();  if (a != null) visitArrayDimension(a);
        if (traceVisit) traceOut("visitPrimitType");
        return null;
    }
    public Void visitType(MJParser.TypeContext ctx) {
        if (traceVisit) traceDisc("visitType");
        if (ctx instanceof MJParser.ObjectTypeContext) visitObjectType((MJParser.ObjectTypeContext) ctx);
        else if (ctx instanceof MJParser.PrimitTypeContext) visitPrimitType((MJParser.PrimitTypeContext) ctx);
        else fail("visitType unrecognized "+ctx.getClass().getSimpleName());
        return null;
    }
    public Void visitArrayDimension(MJParser.ArrayDimensionContext ctx) {
        if (traceVisit) traceIn("visitArrayDimension");
        int dim = ctx.getChildCount() / 2;
        for (int i = 0;  i < dim;  i += 1) {
        	put("[]");
        }
        if (traceVisit) traceOut("visitArrayDimension");
        return null;
    }
    public Void visitClassOrInterfaceType(MJParser.ClassOrInterfaceTypeContext ctx) {
        if (traceVisit) traceIn("visitClassOrInterfaceType");
		for (TerminalNode i : ctx.Identifier()) {
        }
        if (traceVisit) traceOut("visitClassOrInterfaceType");
        return null;
    }
    public Void visitBooleanType(MJParser.BooleanTypeContext ctx) {
        if (traceVisit) traceInOut("visitBooleanType");
        return null;
    }
    public Void visitCharType(MJParser.CharTypeContext ctx) {
        if (traceVisit) traceInOut("visitCharType");
        return null;
    }
    public Void visitByteType(MJParser.ByteTypeContext ctx) {
        if (traceVisit) traceInOut("visitByteType");
        return null;
    }
    public Void visitShortType(MJParser.ShortTypeContext ctx) {
        if (traceVisit) traceInOut("visitShortType");
        return null;
    }
    public Void visitIntType(MJParser.IntTypeContext ctx) {
        if (traceVisit) traceInOut("visitIntType");
        return null;
    }
    public Void visitLongType(MJParser.LongTypeContext ctx) {
        if (traceVisit) traceInOut("visitLongType");
        return null;
    }
    public Void visitFloatType(MJParser.FloatTypeContext ctx) {
        if (traceVisit) traceInOut("visitFloatType");
        return null;
    }
    public Void visitDoubleType(MJParser.DoubleTypeContext ctx) {
        if (traceVisit) traceInOut("visitDoubleType");
        return null;
    }
    public Void visitPrimitiveType(MJParser.PrimitiveTypeContext ctx) {
        if (traceVisit) traceDisc("visitPrimitiveType");
        if (ctx instanceof MJParser.BooleanTypeContext) visitBooleanType((MJParser.BooleanTypeContext) ctx);
        else if (ctx instanceof MJParser.CharTypeContext) visitCharType((MJParser.CharTypeContext) ctx);
        else if (ctx instanceof MJParser.ByteTypeContext) visitByteType((MJParser.ByteTypeContext) ctx);
        else if (ctx instanceof MJParser.ShortTypeContext) visitShortType((MJParser.ShortTypeContext) ctx);
        else if (ctx instanceof MJParser.IntTypeContext) visitIntType((MJParser.IntTypeContext) ctx);
        else if (ctx instanceof MJParser.LongTypeContext) visitLongType((MJParser.LongTypeContext) ctx);
        else if (ctx instanceof MJParser.FloatTypeContext) visitFloatType((MJParser.FloatTypeContext) ctx);
        else if (ctx instanceof MJParser.DoubleTypeContext) visitDoubleType((MJParser.DoubleTypeContext) ctx);
        else fail("visitPrimitiveType unrecognized "+ctx.getClass().getSimpleName());
        return null;
    }
    public Void visitFormalParameters(MJParser.FormalParametersContext ctx) {
        if (traceVisit) traceIn("visitFormalParameters");
		String className = currDest.getSymbol().getName();
		beginFormalParameters(className);
        MJParser.FormalParameterListContext f = ctx.formalParameterList();  if (f != null) visitFormalParameterList(f);
		endFormalParameters();
        if (traceVisit) traceOut("visitFormalParameters");
        return null;
    }
    public Void visitFormalParameterList(MJParser.FormalParameterListContext ctx) {
        if (traceVisit) traceIn("visitFormalParameterList");
        for (MJParser.FormalParameterContext f : ctx.formalParameter()) {
        	visitFormalParameter(f);
        }
        if (traceVisit) traceOut("visitFormalParameterList");
        return null;
    }
    public Void visitFormalParameter(MJParser.FormalParameterContext ctx) {
        if (traceVisit) traceIn("visitFormalParameter");
		Symbol sym = ctx.defn;
		String symName = sym.getName();
		Type symType = sym.getType();
		currDest.classStructure.add(", ").add(typeName(symName, symType));			
		currDest.methodBodies.add(", ").add(typeName(symName, symType));			
        for (MJParser.VariableModifierContext v : ctx.variableModifier()) visitVariableModifier(v);
        MJParser.TypeContext t = ctx.type();  if (t != null) visitType(t);
        MJParser.VariableDeclaratorIdContext v1 = ctx.variableDeclaratorId();  if (v1 != null) visitVariableDeclaratorId(v1);
        if (traceVisit) traceOut("visitFormalParameter");
        return null;
    }

	private void beginFormalParameters(String className) {
		currDest.classStructure.add("(");
		currDest.methodBodies.add("(");
		if (className != null) {
			currDest.classStructure.add(className," ","this");
			currDest.methodBodies.add(className," ","this");
		}
	}
	private void endFormalParameters() {
		currDest.classStructure.add(")");
		currDest.methodBodies.add(") ");
	}

	public Void visitVariableModifier(MJParser.VariableModifierContext ctx) {
        if (traceVisit) traceInOut("visitVariableModifier");
        return null;
    }
    public Void visitMethodBody(MJParser.MethodBodyContext ctx) {
        if (traceVisit) traceIn("visitMethodBody");
        currDest.beginBlock(currDest.methodBodies);
        MJParser.BlockContext b = ctx.block();  if (b != null) visitBlock(b);
        currDest.endBlock();
        if (traceVisit) traceOut("visitMethodBody");
        return null;
    }
    public Void visitConstructorBody(MJParser.ConstructorBodyContext ctx) {
        if (traceVisit) traceIn("visitConstructorBody");
        currDest.beginBlock(currDest.methodBodies);
        MJParser.BlockContext b = ctx.block();  if (b != null) visitBlock(b);
        currDest.endBlock();
        if (traceVisit) traceOut("visitConstructorBody");
        return null;
    }
    public Void visitQualifiedName(MJParser.QualifiedNameContext ctx) {
        if (traceVisit) traceIn("visitQualifiedName");
        String sep = null;
        for (TerminalNode i : ctx.Identifier()) {
        	if (sep != null) put(sep);
        	put(i);
        	sep = ".";
        }
        if (traceVisit) traceOut("visitQualifiedName");
        return null;
    }
    public Void visitLiteral(MJParser.LiteralContext ctx) {
        if (traceVisit) traceIn("visitLiteral");
        TerminalNode l;
		Type t = null;
		String text = ctx.getText(); 
        if ((l = ctx.IntegerLiteral()) != null) {
			t = PrimitiveType.intType;
			if (text.endsWith("l") || text.endsWith("L")) {
				t = PrimitiveType.longType;
			}
		} else if ((l = ctx.FloatingPointLiteral()) != null) {
			t = PrimitiveType.floatType;
			if (text.endsWith("d") || text.endsWith("D")) {
				t = PrimitiveType.doubleType;
			}
		} else if ((l = ctx.CharacterLiteral()) != null) {
			t = PrimitiveType.charType;
		} else if ((l = ctx.StringLiteral()) != null) {
			//TODO t = String;
		} else if ((l = ctx.BooleanLiteral()) != null) {
			t = PrimitiveType.booleanType;
		} else {
			assert text.equals("null");
			//TODO t = Object; 
		}
		ctx.ref = new OutputAtom(text);
		ctx.tipe = t;
        if (traceVisit) traceOut("visitLiteral");
        return null;
    }
    public Void visitBlock(MJParser.BlockContext ctx) {
        if (traceVisit) traceIn("visitBlock");
		beginBlock();
        for (MJParser.BlockStatementContext b : ctx.blockStatement()) visitBlockStatement(b);
		endBlock();
        if (traceVisit) traceOut("visitBlock");
        return null;
    }

	private void beginBlock() {
		currDest.beginBlock();
	}
	private void endBlock() {
		currDest.endBlock();
	}

	public Void visitBlockStatement(MJParser.BlockStatementContext ctx) {
        if (traceVisit) traceIn("visitBlockStatement");
        MJParser.LocalVariableDeclarationStatementContext l = ctx.localVariableDeclarationStatement();  if (l != null) visitLocalVariableDeclarationStatement(l);
        MJParser.StatementContext s = ctx.statement();  if (s != null) visitStatement(s);
        if (traceVisit) traceOut("visitBlockStatement");
        return null;
    }
    public Void visitLocalVariableDeclarationStatement(MJParser.LocalVariableDeclarationStatementContext ctx) {
        if (traceVisit) traceIn("visitLocalVariableDeclarationStatement");
		sourceComment(ctx);
        MJParser.LocalVariableDeclarationContext l = ctx.localVariableDeclaration();  if (l != null) visitLocalVariableDeclaration(l);
        if (traceVisit) traceOut("visitLocalVariableDeclarationStatement");
        return null;
    }
    public Void visitLocalVariableDeclaration(MJParser.LocalVariableDeclarationContext ctx) {
        if (traceVisit) traceIn("visitLocalVariableDeclaration");
        for (MJParser.VariableModifierContext v : ctx.variableModifier()) visitVariableModifier(v);
        MJParser.TypeContext t = ctx.type();  if (t != null) visitType(t);
        MJParser.VariableDeclaratorsContext v1 = ctx.variableDeclarators();  if (v1 != null) {
        	//TODO ctx.variableModifier()
        	visitVariableDeclarators(v1,currDest.block.intermediateDeclarations,currDest.block.code);
        }
        if (traceVisit) traceOut("visitLocalVariableDeclaration");
        return null;
    }
    public Void visitBlkStatement(MJParser.BlkStatementContext ctx) {
        if (traceVisit) traceIn("visitBlkStatement");
        MJParser.BlockContext b = ctx.block();  if (b != null) visitBlock(b);
        if (traceVisit) traceOut("visitBlkStatement");
        return null;
    }
    public Void visitIfStatement(MJParser.IfStatementContext ctx) {
        if (traceVisit) traceIn("visitIfStatement");
        put(ctx.IF());
        MJParser.ParExpressionContext p = ctx.parExpression();  if (p != null) visitParExpression(p);
        List<MJParser.StatementContext> s = ctx.statement();
        visitStatement(s.get(0));
        TerminalNode e;
		if ((e = ctx.ELSE()) != null) {
        	put(e);
            visitStatement(s.get(0));        	
        }
        if (traceVisit) traceOut("visitIfStatement");
        return null;
    }
    public Void visitWhileStatement(MJParser.WhileStatementContext ctx) {
        if (traceVisit) traceIn("visitWhileStatement");
        put(ctx.WHILE());
        MJParser.ParExpressionContext p = ctx.parExpression();  if (p != null) visitParExpression(p);
        MJParser.StatementContext s = ctx.statement();  if (s != null) visitStatement(s);
        if (traceVisit) traceOut("visitWhileStatement");
        return null;
    }
    public Void visitReturnStatement(MJParser.ReturnStatementContext ctx) {
        if (traceVisit) traceIn("visitReturnStatement");
		sourceComment(ctx);
        //TODO check return type
        MJParser.ExpressionContext e = ctx.expression();  if (e != null) {
        	visitExpression(e);
        	currDest.block.code.add("return ").add(e.ref).add(";\n");
        } else {
        	currDest.block.code.add("return;\n");
        }
        if (traceVisit) traceOut("visitReturnStatement");
        return null;
    }
    public Void visitEmptyStatement(MJParser.EmptyStatementContext ctx) {
        if (traceVisit) traceIn("visitEmptyStatement");
		sourceComment(ctx);
        if (traceVisit) traceOut("visitEmptyStatement");
        return null;
    }
    public Void visitExpressionStatement(MJParser.ExpressionStatementContext ctx) {
        if (traceVisit) traceIn("visitExpressionStatement");
		sourceComment(ctx);
        MJParser.StatementExpressionContext s = ctx.statementExpression();  if (s != null) visitStatementExpression(s);
        if (traceVisit) traceOut("visitExpressionStatement");
        return null;
    }
    public Void visitLabelStatement(MJParser.LabelStatementContext ctx) {
        if (traceVisit) traceIn("visitLabelStatement");
		sourceComment(ctx);
        put(ctx.Identifier());
        put(":");
        MJParser.StatementContext s = ctx.statement();  if (s != null) visitStatement(s);
        if (traceVisit) traceOut("visitLabelStatement");
        return null;
    }
    public Void visitStatement(MJParser.StatementContext ctx) {
        if (traceVisit) traceDisc("visitStatement");
        if (ctx instanceof MJParser.BlkStatementContext) visitBlkStatement((MJParser.BlkStatementContext) ctx);
        else if (ctx instanceof MJParser.IfStatementContext) visitIfStatement((MJParser.IfStatementContext) ctx);
        else if (ctx instanceof MJParser.WhileStatementContext) visitWhileStatement((MJParser.WhileStatementContext) ctx);
        else if (ctx instanceof MJParser.ReturnStatementContext) visitReturnStatement((MJParser.ReturnStatementContext) ctx);
        else if (ctx instanceof MJParser.EmptyStatementContext) visitEmptyStatement((MJParser.EmptyStatementContext) ctx);
        else if (ctx instanceof MJParser.ExpressionStatementContext) visitExpressionStatement((MJParser.ExpressionStatementContext) ctx);
        else if (ctx instanceof MJParser.LabelStatementContext) visitLabelStatement((MJParser.LabelStatementContext) ctx);
        else fail("visitStatement unrecognized "+ctx.getClass().getSimpleName());
        return null;
    }
    public Void visitParExpression(MJParser.ParExpressionContext ctx) {
        if (traceVisit) traceIn("visitParExpression");
		sourceComment(ctx);
        MJParser.ExpressionContext e = ctx.expression();  if (e != null) visitExpression(e);
        if (traceVisit) traceOut("visitParExpression");
        return null;
    }
    public Void visitExpressionList(MJParser.ExpressionListContext ctx) {
        if (traceVisit) traceIn("visitExpressionList");
        for (MJParser.ExpressionContext e : ctx.expression()) {
        	visitExpression(e);
			currDest.block.code.add(",").add(e.ref);
        }
        if (traceVisit) traceOut("visitExpressionList");
        return null;
    }
    public Void visitStatementExpression(MJParser.StatementExpressionContext ctx) {
        if (traceVisit) traceIn("visitStatementExpression");
        MJParser.ExpressionContext e = ctx.expression();  if (e != null) visitExpression(e);
        if (traceVisit) traceOut("visitStatementExpression");
        return null;
    }
    public Void visitConstantExpression(MJParser.ConstantExpressionContext ctx) {
        if (traceVisit) traceIn("visitConstantExpression");
        MJParser.ExpressionContext e = ctx.expression();  if (e != null) visitExpression(e);
        if (traceVisit) traceOut("visitConstantExpression");
        return null;
    }
    public Void visitPrimExpression(MJParser.PrimExpressionContext ctx) {
        if (traceVisit) traceIn("visitPrimExpression");
        MJParser.PrimaryContext p = ctx.primary();  if (p != null) visitPrimary(p);
		ctx.tipe = p.tipe;
		ctx.ref = p.ref;
        if (traceVisit) traceOut("visitPrimExpression");
        return null;
    }
    public Void visitDotExpression(MJParser.DotExpressionContext ctx) {
        if (traceVisit) traceIn("visitDotExpression");
        MJParser.ExpressionContext e = ctx.expression();  if (e != null) visitExpression(e);
		Type type = e.tipe;
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
			ctx.ref = checkRef(ref, e.ref).add("->_class->_data.").add(name).add("))(").add(e.ref);			
		} else {
			ctx.tipe = type;
			ctx.ref = checkRef(ref, e.ref).add("->_data.").add(name);
		}
        if (traceVisit) traceOut("visitDotExpression");
        return null;
    }

	private OutputList checkRef(OutputList out, OutputItem ref) {
		return out.add("checkPtr(").add(ref).add(")");
	}
    public Void visitIndexExpression(MJParser.IndexExpressionContext ctx) {
        if (traceVisit) traceIn("visitIndexExpression");
        MJParser.ExpressionContext e = ctx.expression(0);  if (e != null) visitExpression(e);
        put("[");
        MJParser.ExpressionContext e1 = ctx.expression(1);  if (e1 != null) visitExpression(e1);
        put("]");
        if (traceVisit) traceOut("visitIndexExpression");
        return null;
    }
    public Void visitCallExpression(MJParser.CallExpressionContext ctx) {
        if (traceVisit) traceIn("visitCallExpression");
        MJParser.ExpressionContext e = ctx.expression();  if (e != null) visitExpression(e);
		System.out.println("visitCallExpression method");  printContextTree(e,"    ");
		if (!(e.tipe instanceof MethodSymbol)) {
			Compiler.error(e.getStop(), e.getText()+" is not a method","CallExpression");
			ctx.tipe = UnknownType.getInstance();
			return null;
		}
		OutputItem method = e.ref;
		Type type = ((MethodSymbol)e.tipe).getType();  //getType(ctx.expression());
		if (!(type instanceof VoidType)) {
			String temp = "_e"+nextreg();
			currDest.block.intermediateDeclarations.add(indent).add(typeName(temp,type)).add(";\n");
			currDest.block.code.add(indent).add(temp).add("=");
			ctx.ref = new OutputAtom(temp);
		} else {
			currDest.block.code.add(indent);
		}
		currDest.block.code.add(method); // includes the (
        MJParser.ExpressionListContext e1 = ctx.expressionList();  if (e1 != null) visitExpressionList(e1);
		ctx.tipe = type;
		currDest.block.code.add(");\n");
        if (traceVisit) traceOut("visitCallExpression");
        return null;
    }
    public Void visitNewExpression(MJParser.NewExpressionContext ctx) {
        if (traceVisit) traceIn("visitNewExpression");
        MJParser.CreatorContext c = ctx.creator();  if (c != null) visitCreator(c);
        ctx.tipe = c.tipe;
        ctx.ref = c.ref;
        if (traceVisit) traceOut("visitNewExpression");
        return null;
    }
    public Void visitCastExpression(MJParser.CastExpressionContext ctx) {
        if (traceVisit) traceIn("visitCastExpression");
        put("(");
        MJParser.TypeContext t = ctx.type();  if (t != null) visitType(t);
        put(")");
        MJParser.ExpressionContext e = ctx.expression();  if (e != null) visitExpression(e);
        if (traceVisit) traceOut("visitCastExpression");
        return null;
    }
    public Void visitPlusExpression(MJParser.PlusExpressionContext ctx) {
        if (traceVisit) traceIn("visitPlusExpression");
        put((TerminalNode) ctx.getChild(0));
        MJParser.ExpressionContext e = ctx.expression();  if (e != null) visitExpression(e);
        if (traceVisit) traceOut("visitPlusExpression");
        return null;
    }
    public Void visitNotExpression(MJParser.NotExpressionContext ctx) {
        if (traceVisit) traceIn("visitNotExpression");
        put((TerminalNode) ctx.getChild(0));
        MJParser.ExpressionContext e = ctx.expression();  if (e != null) visitExpression(e);
        if (traceVisit) traceOut("visitNotExpression");
        return null;
    }

	public void binaryOperator(MJParser.ExpressionContext ctx, MJParser.ExpressionContext left, String op, MJParser.ExpressionContext right) {
		Type leftType = left.tipe;
		Type typeRight = right.tipe;
		Type typeResult = leftType;  //TODO determine result type
		String temp = "_e"+nextreg();
		currDest.block.intermediateDeclarations.add(indent).add(typeName(temp,typeResult)).add(";\n");
		currDest.block.code.add(indent).add(temp).add(" = (").add(left.ref).add(op).add(right.ref).add(");\n");
		ctx.ref = new OutputAtom(temp);
	}

	public Void visitMultExpression(MJParser.MultExpressionContext ctx) {
        if (traceVisit) traceIn("visitMultExpression");
        MJParser.ExpressionContext e = ctx.expression(0);  if (e != null) visitExpression(e);
        MJParser.ExpressionContext e1 = ctx.expression(1);  if (e1 != null) visitExpression(e1);
		//TODO types
		String op = ctx.getChild(1).getText();
		binaryOperator(ctx, e, op, e1); 
        if (traceVisit) traceOut("visitMultExpression");
        return null;
    }
    public Void visitAddExpression(MJParser.AddExpressionContext ctx) {
        if (traceVisit) traceIn("visitAddExpression");
        MJParser.ExpressionContext e = ctx.expression(0);  if (e != null) visitExpression(e);
        MJParser.ExpressionContext e1 = ctx.expression(1);  if (e1 != null) visitExpression(e1);
		//TODO types
		String op = ctx.getChild(1).getText();
		binaryOperator(ctx, e, op, e1); 
        if (traceVisit) traceOut("visitAddExpression");
        return null;
    }
    public Void visitShiftExpression(MJParser.ShiftExpressionContext ctx) {
        if (traceVisit) traceIn("visitShiftExpression");
        MJParser.ExpressionContext e = ctx.expression(0);  if (e != null) visitExpression(e);
        MJParser.ExpressionContext e1 = ctx.expression(1);  if (e1 != null) visitExpression(e1);
		//TODO types
		String op = ctx.getChild(1).getText()+ctx.getChild(2).getText();
		if (ctx.getChildCount() == 5) op += ctx.getChild(2).getText();  // > > >
		binaryOperator(ctx, e, op, e1); 
        if (traceVisit) traceOut("visitShiftExpression");
        return null;
    }
    public Void visitCompareExpression(MJParser.CompareExpressionContext ctx) {
        if (traceVisit) traceIn("visitCompareExpression");
        MJParser.ExpressionContext e = ctx.expression(0);  if (e != null) visitExpression(e);
        MJParser.ExpressionContext e1 = ctx.expression(1);  if (e1 != null) visitExpression(e1);
		//TODO types, boolean result
		String op = ctx.getChild(1).getText();
		binaryOperator(ctx, e, op, e1);
		ctx.ref = new OutputList().add("toBoolean(").add(ctx.ref).add(")");
        if (traceVisit) traceOut("visitCompareExpression");
        return null;
    }
    public Void visitEqualExpression(MJParser.EqualExpressionContext ctx) {
        if (traceVisit) traceIn("visitEqualExpression");
        MJParser.ExpressionContext e = ctx.expression(0);  if (e != null) visitExpression(e);
        MJParser.ExpressionContext e1 = ctx.expression(1);  if (e1 != null) visitExpression(e1);
		//TODO types, boolean result
		String op = ctx.getChild(1).getText();
		binaryOperator(ctx, e, op, e1); 
		ctx.ref = new OutputList().add("toBoolean(").add(ctx.ref).add(")");
        if (traceVisit) traceOut("visitEqualExpression");
        return null;
    }
    public Void visitAndExpression(MJParser.AndExpressionContext ctx) {
        if (traceVisit) traceIn("visitAndExpression");
        MJParser.ExpressionContext e = ctx.expression(0);  if (e != null) visitExpression(e);
        MJParser.ExpressionContext e1 = ctx.expression(1);  if (e1 != null) visitExpression(e1);
		//TODO types
		binaryOperator(ctx, e, "&", e1); 
        if (traceVisit) traceOut("visitAndExpression");
        return null;
    }
    public Void visitExclExpression(MJParser.ExclExpressionContext ctx) {
        if (traceVisit) traceIn("visitExclExpression");
        MJParser.ExpressionContext e = ctx.expression(0);  if (e != null) visitExpression(e);
        MJParser.ExpressionContext e1 = ctx.expression(1);  if (e1 != null) visitExpression(e1);
		//TODO types
		binaryOperator(ctx, e, "^", e1); 
        if (traceVisit) traceOut("visitExclExpression");
        return null;
    }
    public Void visitOrExpression(MJParser.OrExpressionContext ctx) {
        if (traceVisit) traceIn("visitOrExpression");
        MJParser.ExpressionContext e = ctx.expression(0);  if (e != null) visitExpression(e);
        MJParser.ExpressionContext e1 = ctx.expression(1);  if (e1 != null) visitExpression(e1);
		//TODO types
		binaryOperator(ctx, e, "|", e1);
        if (traceVisit) traceOut("visitOrExpression");
        return null;
    }
    public Void visitCondAndExpression(MJParser.CondAndExpressionContext ctx) {
        if (traceVisit) traceIn("visitCondAndExpression");
        MJParser.ExpressionContext e = ctx.expression(0);  if (e != null) visitExpression(e);
        put("&&");
        MJParser.ExpressionContext e1 = ctx.expression(1);  if (e1 != null) visitExpression(e1);
        if (traceVisit) traceOut("visitCondAndExpression");
        return null;
    }
    public Void visitCondOrExpression(MJParser.CondOrExpressionContext ctx) {
        if (traceVisit) traceIn("visitCondOrExpression");
        MJParser.ExpressionContext e = ctx.expression(0);  if (e != null) visitExpression(e);
        put("||");
        MJParser.ExpressionContext e1 = ctx.expression(1);  if (e1 != null) visitExpression(e1);
        if (traceVisit) traceOut("visitCondOrExpression");
        return null;
    }
    public Void visitAssignExpression(MJParser.AssignExpressionContext ctx) {
        if (traceVisit) traceIn("visitAssignExpression");
        MJParser.ExpressionContext e = ctx.expression(0);  if (e != null) visitExpression(e);
        MJParser.ExpressionContext e1 = ctx.expression(1);  if (e1 != null) visitExpression(e1);
		System.out.println("visitAssignExpression e");  printContextTree(e,"    ");
        MJParser.ExpressionContext d = e;		
		while (true) {
	        if (d instanceof MJParser.PrimExpressionContext) {
	        	MJParser.PrimaryContext p = ((MJParser.PrimExpressionContext) d).primary();
	        	if (p instanceof MJParser.ParenPrimaryContext) {
	        		d = ((MJParser.ParenPrimaryContext) p).expression();
	        	} if (p instanceof MJParser.IdentifierPrimaryContext) {
	        		break;
	        	} else {
	        		Compiler.error(p.stop,"not assignable","AssignExpression");
	        		d = null;
	        		break;
	        	}
	        } else if (d instanceof MJParser.DotExpressionContext) {
	        	break;
	        } else if (d instanceof MJParser.IndexExpressionContext) {
	        	break;
	        //not valid} else if (d instanceof MJParser.CallExpressionContext){
	        	
	        //not valid} else if (d instanceof MJParser.NewExpressionContext){
	        	
	        //?can this be valid?} else if (d instanceof MJParser.CastExpressionContext){
	        	
	        //?can this be valid?} else if (d instanceof MJParser.AssignExpressionContext){
	        	
	        } else {
	        	Compiler.error(d.stop,"not assignable","AssignExpression");
	        	d = null;
	        	break;
	        }
		}
		Type type = e1.tipe;
		if (!(type instanceof PrimitiveType || type instanceof ClassSymbol)) {
			Compiler.error(e.getStop(), "not an assignable value","AssignExpression");
		}
		currDest.block.code.add(indent).add(e.ref).add("=").add(e1.ref).add(";\n");
		ctx.tipe = e.tipe;
		ctx.ref = e.ref;
        if (traceVisit) traceOut("visitAssignExpression");
        return null;
    }
    public Void visitExpression(MJParser.ExpressionContext ctx) {
        if (traceVisit) traceDisc("visitExpression");
        if (ctx instanceof MJParser.PrimExpressionContext) visitPrimExpression((MJParser.PrimExpressionContext) ctx);
        else if (ctx instanceof MJParser.DotExpressionContext) visitDotExpression((MJParser.DotExpressionContext) ctx);
        else if (ctx instanceof MJParser.IndexExpressionContext) visitIndexExpression((MJParser.IndexExpressionContext) ctx);
        else if (ctx instanceof MJParser.CallExpressionContext) visitCallExpression((MJParser.CallExpressionContext) ctx);
        else if (ctx instanceof MJParser.NewExpressionContext) visitNewExpression((MJParser.NewExpressionContext) ctx);
        else if (ctx instanceof MJParser.CastExpressionContext) visitCastExpression((MJParser.CastExpressionContext) ctx);
        else if (ctx instanceof MJParser.PlusExpressionContext) visitPlusExpression((MJParser.PlusExpressionContext) ctx);
        else if (ctx instanceof MJParser.NotExpressionContext) visitNotExpression((MJParser.NotExpressionContext) ctx);
        else if (ctx instanceof MJParser.MultExpressionContext) visitMultExpression((MJParser.MultExpressionContext) ctx);
        else if (ctx instanceof MJParser.AddExpressionContext) visitAddExpression((MJParser.AddExpressionContext) ctx);
        else if (ctx instanceof MJParser.ShiftExpressionContext) visitShiftExpression((MJParser.ShiftExpressionContext) ctx);
        else if (ctx instanceof MJParser.CompareExpressionContext) visitCompareExpression((MJParser.CompareExpressionContext) ctx);
        else if (ctx instanceof MJParser.EqualExpressionContext) visitEqualExpression((MJParser.EqualExpressionContext) ctx);
        else if (ctx instanceof MJParser.AndExpressionContext) visitAndExpression((MJParser.AndExpressionContext) ctx);
        else if (ctx instanceof MJParser.ExclExpressionContext) visitExclExpression((MJParser.ExclExpressionContext) ctx);
        else if (ctx instanceof MJParser.OrExpressionContext) visitOrExpression((MJParser.OrExpressionContext) ctx);
        else if (ctx instanceof MJParser.CondAndExpressionContext) visitCondAndExpression((MJParser.CondAndExpressionContext) ctx);
        else if (ctx instanceof MJParser.CondOrExpressionContext) visitCondOrExpression((MJParser.CondOrExpressionContext) ctx);
        else if (ctx instanceof MJParser.AssignExpressionContext) visitAssignExpression((MJParser.AssignExpressionContext) ctx);
        else fail("visitExpression unrecognized "+ctx.getClass().getSimpleName());
        return null;
    }
    public Void visitParenPrimary(MJParser.ParenPrimaryContext ctx) {
        if (traceVisit) traceIn("visitParenPrimary");
        MJParser.ExpressionContext e = ctx.expression();  if (e != null) visitExpression(e);
		ctx.tipe = ctx.expression().tipe;
		ctx.ref = ctx.expression().ref;
        if (traceVisit) traceOut("visitParenPrimary");
        return null;
    }
    public Void visitThisPrimary(MJParser.ThisPrimaryContext ctx) {
        if (traceVisit) traceIn("visitThisPrimary");
		ctx.tipe = currDest.getSymbol();
		ctx.ref = new OutputAtom("this");
        if (traceVisit) traceOut("visitThisPrimary");
        return null;
    }
    public Void visitSuperPrimary(MJParser.SuperPrimaryContext ctx) {
        if (traceVisit) traceIn("visitSuperPrimary");
        put(ctx.SUPER());
        if (traceVisit) traceOut("visitSuperPrimary");
        return null;
    }
    public Void visitLiteralPrimary(MJParser.LiteralPrimaryContext ctx) {
        if (traceVisit) traceIn("visitLiteralPrimary");
        MJParser.LiteralContext l = ctx.literal();  if (l != null) visitLiteral(l);
		ctx.tipe = ctx.literal().tipe;
		ctx.ref = ctx.literal().ref;
        if (traceVisit) traceOut("visitLiteralPrimary");
        return null;
    }

    public Void visitIdentifierPrimary(MJParser.IdentifierPrimaryContext ctx) {
        if (traceVisit) traceIn("visitIdentifierPrimary");
        Symbol sym = ctx.defn;
		Type symType = null;
        if (sym != null) {
    		String symName = sym.getName();
        	symType = sym.getType();
			System.out.println(symName+" type "+symType);
			ctx.tipe = symType;
            Scope scope = sym.getScope();
            if (scope instanceof BaseScope || scope instanceof MethodSymbol) {
            	ctx.ref = new OutputAtom(symName);
            } else if (scope instanceof ClassSymbol) {
        		if (sym != null && sym instanceof MethodSymbol) {
        			// "this" doesn't need a null pointer check
        			ctx.ref = new OutputList().add("(*(this->_class->_data.",symName).add("))(").add("this");
        			ctx.tipe = (MethodSymbol)sym;  // doesn't have result type until called
        		} else {
        			//TODO check for static field and do class access
        			ctx.ref = new OutputList().add("this->_data.",symName);
        		}
            } else {
            	System.out.println("visitIdentifierPrimary scope is "+scope.getClass().getSimpleName());
    	        ctx.ref = new OutputAtom("_unknown");
            }
        } else {
	        if (symType == null) symType = UnknownType.getInstance();
	        ctx.tipe = symType;
	        ctx.ref = new OutputAtom("_unknown");
        }
        if (traceVisit) traceOut("visitIdentifierPrimary");
        return null;
    }
    
    public Void visitPrimary(MJParser.PrimaryContext ctx) {
        if (traceVisit) traceDisc("visitPrimary");
        if (ctx instanceof MJParser.ParenPrimaryContext) visitParenPrimary((MJParser.ParenPrimaryContext) ctx);
        else if (ctx instanceof MJParser.ThisPrimaryContext) visitThisPrimary((MJParser.ThisPrimaryContext) ctx);
        else if (ctx instanceof MJParser.SuperPrimaryContext) visitSuperPrimary((MJParser.SuperPrimaryContext) ctx);
        else if (ctx instanceof MJParser.LiteralPrimaryContext) visitLiteralPrimary((MJParser.LiteralPrimaryContext) ctx);
        else if (ctx instanceof MJParser.IdentifierPrimaryContext) visitIdentifierPrimary((MJParser.IdentifierPrimaryContext) ctx);
        else fail("visitPrimary unrecognized "+ctx.getClass().getSimpleName());
        return null;
    }
    public Void visitArrayCreator(MJParser.ArrayCreatorContext ctx) {
        if (traceVisit) traceIn("visitArrayCreator");
        MJParser.CreatedNameContext c = ctx.createdName();  if (c != null) visitCreatedName(c);
        MJParser.ArrayCreatorRestContext a = ctx.arrayCreatorRest();  if (a != null) visitArrayCreatorRest(a);
        if (traceVisit) traceOut("visitArrayCreator");
        return null;
    }
    public Void visitClassCreator(MJParser.ClassCreatorContext ctx) {
        if (traceVisit) traceIn("visitClassCreator");
        MJParser.CreatedNameContext c = ctx.createdName();  if (c != null) visitCreatedName(c);
        MJParser.ClassCreatorRestContext c1 = ctx.classCreatorRest();  if (c1 != null) visitClassCreatorRest(c1);
		if (ctx.createdName().primitiveType() != null) {
			Compiler.error(ctx.getStart(), "new cannot be applied to a primitive type", "ClassCreator");
		} else {
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
			currDest.block.code.add(indent).add(typeName(rand,symType)).add("=(*(",sym.getName(),"_class._data._create))(NULL);\n");			
			currDest.block.code.add(indent).add("(*(checkPtr(",rand,")->_class->_data._init))(",rand);
			MJParser.ExpressionListContext argList = ctx.classCreatorRest().arguments().expressionList();
			if (argList != null) {
				for (MJParser.ExpressionContext arg : argList.expression()) {
					currDest.block.code.add(",").add(arg.ref);
				}
			}
			currDest.block.code.add(");\n");
			ctx.ref = new OutputAtom(rand);
			ctx.tipe = symType;
		}
        if (traceVisit) traceOut("visitClassCreator");
        return null;
    }
    public Void visitCreator(MJParser.CreatorContext ctx) {
        if (traceVisit) traceDisc("visitCreator");
        if (ctx instanceof MJParser.ArrayCreatorContext) visitArrayCreator((MJParser.ArrayCreatorContext) ctx);
        else if (ctx instanceof MJParser.ClassCreatorContext) visitClassCreator((MJParser.ClassCreatorContext) ctx);
        else fail("visitCreator unrecognized "+ctx.getClass().getSimpleName());
        return null;
    }
    public Void visitCreatedName(MJParser.CreatedNameContext ctx) {
        if (traceVisit) traceIn("visitCreatedName");
        MJParser.PrimitiveTypeContext p = ctx.primitiveType();
        if (p != null) visitPrimitiveType(p);
        else {
            String sep = null;
            for (TerminalNode i : ctx.Identifier()) {
            	if (sep != null) put(sep);
            	put(i);
            	sep = ".";
            }
        }
        if (traceVisit) traceOut("visitCreatedName");
        return null;
    }
    public Void visitArrayCreatorRest(MJParser.ArrayCreatorRestContext ctx) {
        if (traceVisit) traceIn("visitArrayCreatorRest");
        List<MJParser.ExpressionContext> el = ctx.expression();
        if (el.isEmpty()) {
            MJParser.ArrayDimensionContext ad = ctx.arrayDimension();  if (ad != null) visitArrayDimension(ad);
            MJParser.ArrayInitializerContext ai = ctx.arrayInitializer();  if (ai != null) visitArrayInitializer(ai, null/*TODO*/);
        } else {
	        for (MJParser.ExpressionContext e : el) {
	            put("[");
	        	visitExpression(e);
	            put("]");
	        }
            MJParser.ArrayDimensionContext ad = ctx.arrayDimension();  if (ad != null) visitArrayDimension(ad);
        }
        if (traceVisit) traceOut("visitArrayCreatorRest");
        return null;
    }
    public Void visitClassCreatorRest(MJParser.ClassCreatorRestContext ctx) {
        if (traceVisit) traceIn("visitClassCreatorRest");
        MJParser.ArgumentsContext a = ctx.arguments();  if (a != null) visitArguments(a);
        MJParser.ClassBodyContext c = ctx.classBody();  if (c != null) visitClassBody(c);
        if (traceVisit) traceOut("visitClassCreatorRest");
        return null;
    }
    public Void visitArguments(MJParser.ArgumentsContext ctx) {
        if (traceVisit) traceIn("visitArguments");
        put("(");
        MJParser.ExpressionListContext e = ctx.expressionList();  if (e != null) visitExpressionList(e);
        put(")");
        if (traceVisit) traceOut("visitArguments");
        return null;
    }
}
