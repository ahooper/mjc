package ca.nevdull.cob.compiler;

// Collect the class scope and symbol type structure, and attach it to the parse tree

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.antlr.v4.runtime.ANTLRInputStream;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.TerminalNode;

public class DefinitionPass extends PassCommon {
	Scope currentScope;
	private boolean trace;
/*
    static String preloadClasses[] = {
		"Object",
		"Class",
		//"String",
		//"Integer",
		//"System",
		};
*/
	private final String preloadClassPrefix = "";  //TODO later "cob.lang.";
	
	public DefinitionPass(PassData data) {
		super(data);
    	trace = passData.main.trace.contains("DefinitionPass");
		BaseScope globals = new BaseScope("globals",null);
		data.globals = globals;
		currentScope = globals;
		if (!passData.main.no_base) {
			String preloadFilePrefix = preloadClassPrefix.replace('.',File.separatorChar);
        	for (String pathDir : passData.main.bootClassPath) {
            	File fname;
				if (pathDir == null || pathDir.length() == 0) {
        			if (passData.outputDir == null) fname = new File(preloadFilePrefix);
        			else fname = new File(passData.outputDir,preloadFilePrefix);
        		} else fname = new File(pathDir,preloadFilePrefix);
            	if (! fname.isDirectory()) continue; // try next in path
            	if (trace) Main.debug("preload found "+fname.getAbsolutePath());
            	for (File f : fname.listFiles(new FilenameFilter() {
								            	    @Override public boolean accept(File dir, String name) {
								            	        return name.endsWith(Main.IMPORT_SUFFIX);
								            	    }
								            	}) ) {
                 	System.out.println("preload "+f);
/*                 	
                    try {
						//ClassSymbol klass = compileImport(f);
						//TODO mark klass as tentative, don't need import in code if still tentative (never referenced)
					} catch (IOException excp) {
						Main.error("Unable to read import "+excp.toString());
						excp.printStackTrace();
					}
*/					
            	}
                break;
        	}
/*
			List<String> nameComponents = new ArrayList<String>();
			for (String className : preloadClasses) {
				nameComponents.clear();
				nameComponents.add(className);
				importClass(nameComponents, passData.main.bootClassPath);
			}
*/
		}
	}
    
    private void beginScope(Scope newScope) {
    	if (trace) Main.debug("begin %s enclosing=%s", newScope, newScope.getEnclosingScope());
	    currentScope = newScope;
    }
    
    private void endScope() {
    	if (trace) Main.debug("end %s", currentScope);
	    currentScope = currentScope.getEnclosingScope();
    }

	private ClassSymbol importClass(List<TerminalNode> nameComponents, String[] classPath, boolean optional) {
		// Read saved symbols
		StringBuilder qname = new StringBuilder();
		TerminalNode lastName = null;
    	for (TerminalNode nameComponent : nameComponents) {
    		if (qname.length() > 0) qname.append(File.separatorChar);
    		qname.append(nameComponent.getText());
    		lastName = nameComponent;
    	}
    	String qnameString = qname.toString();
    	Symbol prev = passData.globals.find(qnameString);
		if (prev != null) {
			if (prev instanceof ClassSymbol) {
				if (trace) Main.debug("previously imported "+qnameString);
	    		return (ClassSymbol) prev;
			} else {
				Main.error(lastName,"import conflicts with "+prev);
				return null;
			}
    	}
		ClassSymbol klass;
        try {
        	for (String pathDir : classPath) {
            	File fname;
				if (pathDir == null || pathDir.length() == 0) {
        			if (passData.outputDir == null) fname = new File(qnameString+Main.IMPORT_SUFFIX);
        			else fname = new File(passData.outputDir,qnameString+Main.IMPORT_SUFFIX);
        		} else fname = new File(pathDir,qnameString+Main.IMPORT_SUFFIX);
            	if (! fname.isFile() ) continue; // try next in path
            	if (trace) Main.debug("import found "+fname.getAbsolutePath());
                klass = compileImport(fname);
                return klass;
        	}
		} catch (IOException excp) {
			Main.error("Unable to read import "+excp.toString());
			excp.printStackTrace();
		}
		if (optional) return null;
		Main.error("Class not found "+qname+" (path "+Arrays.toString(classPath)+")");        	
		klass = UnknownType.make(lastName.getSymbol());
        passData.globals.add(klass);
		return klass; 
	}
	
	private ClassSymbol autoImport(TerminalNode ident) {
    	List<TerminalNode> nameComponents = new ArrayList<TerminalNode>();
    	nameComponents.add(ident);
    	ClassSymbol klass = importClass(nameComponents, passData.main.classPath, true/*optional*/);
    	if (klass == null) klass = importClass(nameComponents, passData.main.bootClassPath, false/*optional*/);
    	klass.setAutoImport(true);
		return klass;
	}
    
	private ClassSymbol compileImport(File inFile) throws FileNotFoundException, IOException {
		ANTLRInputStream input = new ANTLRInputStream(new InputStreamReader(new FileInputStream(inFile),"UTF-8"));
		input.name = inFile.getName();
        CobLexer lexer = new CobLexer(input);
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        CobParser parser = new CobParser(tokens);
        parser.removeErrorListeners(); // remove ConsoleErrorListener
        parser.addErrorListener(new Main.VerboseListener()); // add ours
        parser.setBuildParseTree(true);
        ParseTree tree = parser.file();
        Scope saveScope = currentScope;
        currentScope = passData.globals;
        this.visit(tree);
        currentScope = saveScope;
        return ((CobParser.FileContext)tree).klass().defn;
	}

	@Override public Void visitFile(CobParser.FileContext ctx) {
		visitChildren(ctx);
    	if (trace) Main.debug("globals %s", passData.globals);		
		return null;
	}

	@Override public Void visitImpourt(CobParser.ImpourtContext ctx) {
    	TerminalNode ident = ctx.ID();
    	List<TerminalNode> nameComponents = new ArrayList<TerminalNode>();
    	nameComponents.add(ident);
		importClass(nameComponents, passData.main.classPath, false/*optional*/);    	
		return null;
	}

	@Override public Void visitKlass(CobParser.KlassContext ctx) {
		Token nameToken = ctx.name;
		Token baseToken = ctx.base;
		ClassSymbol baseClass = null;
		ClassSymbol thisClass = new ClassSymbol(nameToken, currentScope, baseClass);
		thisClass.setType(thisClass);
		ctx.defn = thisClass;
		currentScope.add(thisClass);
		if (baseToken != null) {
			// Base is imported only after the subject class is defined, so the subject's
			// name is known during import of the base
			String baseName = baseToken.getText();
			Symbol baseSymbol = currentScope.find(baseName);
			if (baseSymbol == null) {
				ClassSymbol imp = autoImport(ctx.ID(1));
				if (imp != null) {
					baseClass = imp;
				} else {
					Main.error(baseToken,"Base "+baseName+" is not defined");
					baseClass = UnknownType.make(baseToken);
				}
			} else if (baseSymbol instanceof ClassSymbol) {
				baseClass = (ClassSymbol)baseSymbol;
			} else {
				Main.error(baseToken,"Base "+baseName+" is not a class");
			}
		}
		thisClass.setBase(baseClass);
		beginScope(thisClass);
		for (CobParser.MemberContext member : ctx.member()) {
			visit(member);
		}
		endScope();
		return null;
	}
	
	@Override public Void visitMethod(CobParser.MethodContext ctx) {
		//	'static'? type ID '(' arguments? ')' compoundStatement
		CobParser.TypeContext typeCtx = ctx.type();
		visitType(typeCtx);
		TerminalNode id = ctx.ID();
		Token symbol = id.getSymbol();
		MethodSymbol methSym = new MethodSymbol(symbol,currentScope,typeCtx.tipe);
		methSym.setStatic(ctx.stat != null);
		ctx.defn = methSym;
		assert currentScope instanceof ClassSymbol;
		ClassSymbol klass = (ClassSymbol)currentScope;
		Symbol prev = klass.findMember(symbol.getText());
		if (prev != null) {
			if (prev.getScope() == klass) {
				// overloading or duplicate
				Main.error(symbol, "overloading is not yet implemented");
			} else if (prev instanceof MethodSymbol) {
				methSym.setOverrides((MethodSymbol)prev);
			} else {
				Main.error(symbol, "override kind mismatch");
			}
		}
		currentScope.add(methSym);
		beginScope(methSym);
		if (ctx.stat == null) {
			VariableSymbol varSym = new VariableSymbol("this",klass);
			currentScope.add(varSym);
		}
		CobParser.ArgumentsContext arguments = ctx.arguments();
		if (arguments != null) {
			for (CobParser.ArgumentContext argument : arguments.argument()) {
				visit(argument);
			}
		}
		visitBody(ctx.body());
		endScope();
		return null;
	}
	
	@Override public Void visitConstructor(CobParser.ConstructorContext ctx) {
		//	ID '(' arguments? ')' compoundStatement
		TerminalNode id = ctx.ID();
		String name = id.getText();
		if (currentScope instanceof ClassSymbol
				&& name.equals(((ClassSymbol)currentScope).getName()) ) {
		} else {
			Main.error(id,"Constructor name must match class name");
		}
		MethodSymbol methSym = new MethodSymbol(id.getSymbol(),currentScope,PrimitiveType.voidType);
		methSym.setStatic(true);
		ctx.defn = methSym;
		currentScope.add(methSym);
		beginScope(methSym);
		CobParser.ArgumentsContext arguments = ctx.arguments();
		if (arguments != null) {
			for (CobParser.ArgumentContext argument : arguments.argument()) {
				visit(argument);
			}
		}
		visitBody(ctx.body());
		endScope();
		return null;
	}
	
	@Override public Void visitNativeMethod(CobParser.NativeMethodContext ctx) {
		//	'native' type ID '(' arguments? ')' ';'
		CobParser.TypeContext typeCtx = ctx.type();
		visitType(typeCtx);
		TerminalNode id = ctx.ID();
		MethodSymbol methSym = new MethodSymbol(id.getSymbol(),currentScope,typeCtx.tipe);
		methSym.setNative(true);
		ctx.defn = methSym;
		currentScope.add(methSym);
		beginScope(methSym);
		CobParser.ArgumentsContext arguments = ctx.arguments();
		if (arguments != null) {
			for (CobParser.ArgumentContext argument : arguments.argument()) {
				visit(argument);
			}
		}
		endScope();
		return null;
	}
	
	@Override public Void visitInitializer(CobParser.InitializerContext ctx) {
		//	'static'? compoundStatement
		visitCompoundStatement(ctx.compoundStatement());
		return null;
	}
	
	@Override public Void visitFieldList(CobParser.FieldListContext ctx) {
		//	'static'? type ID ( '=' expression )? ( ',' ID ( '=' expression )? )* ';'
		CobParser.TypeContext typeCtx = ctx.type();
		visitType(typeCtx);
		for (CobParser.FieldContext field : ctx.field()) {
			visitField(field);
		}
		return null;
	}
	
	@Override public Void visitField(CobParser.FieldContext ctx) {
		//	ID ( '=' expression )?
		CobParser.FieldListContext list = (CobParser.FieldListContext)ctx.getParent();
		VariableSymbol varSym = new VariableSymbol(ctx.ID().getSymbol(),list.type().tipe);
		varSym.setStatic(list.stat != null);
		currentScope.add(varSym);
		CobParser.ExpressionContext e = ctx.expression();
		if (e != null) {
			visit(e);  // get reference scopes for initializations
		}
		return null;
	}
	
	@Override public Void visitArgument(CobParser.ArgumentContext ctx) {
		CobParser.TypeContext typeCtx = ctx.type();
		visitType(typeCtx);
		TerminalNode id = ctx.ID();
		VariableSymbol varSym = new VariableSymbol(id.getSymbol(),typeCtx.tipe);
		currentScope.add(varSym);
		ctx.defn = varSym;
		return null;
	}
	
	@Override public Void visitType(CobParser.TypeContext ctx) {
		visitTypeName(ctx.typeName());
		if (ctx.getChildCount() > 1) {
			ctx.tipe = new ArrayType(ctx.typeName().tipe);
		} else {
			ctx.tipe = ctx.typeName().tipe;
		}
		return null;
	}
	
	@Override public Void visitTypeName(CobParser.TypeNameContext ctx) {
		ctx.refScope = currentScope;
		TerminalNode id = ctx.ID();
		if (id == null) {
			ctx.tipe = PrimitiveType.getByName(ctx.start.getText());
			assert ctx.tipe != null;
		} else {
			String name = id.getText();
			Symbol defn = currentScope.find(name);
			if (defn == null) {
				ClassSymbol imp = autoImport(id);
				if (imp != null) {
					ctx.tipe = imp;
				} else {
					Main.error(id,"Class "+name+" is not defined");
					ctx.tipe = UnknownType.make(id.getSymbol());
				}
			} else if (defn instanceof ClassSymbol){
				ctx.tipe = (ClassSymbol)defn;
			} else {
				Main.error(id,name+" is not a type");
			}
		}
		return null;
	}
	
	@Override public Void visitBody(CobParser.BodyContext ctx) {
		CobParser.CompoundStatementContext cs = ctx.compoundStatement();
		if (cs != null) visitCompoundStatement(cs);
		return null;
	}

	@Override public Void visitNamePrimary(CobParser.NamePrimaryContext ctx) {
		ctx.refScope = currentScope;
		return null;
	}

    @Override public Void visitThisPrimary(CobParser.ThisPrimaryContext ctx) {
		ctx.refScope = currentScope;
        return null;
    }

    @Override public Void visitNewPrimary(CobParser.NewPrimaryContext ctx) {
		ctx.refScope = currentScope;
		visitChildren(ctx);
        return null;
    }
	
	@Override public Void visitCompoundStatement(CobParser.CompoundStatementContext ctx) {
		beginScope(new LocalScope(ctx.start.getLine(),currentScope));
		for (CobParser.BlockItemContext item : ctx.blockItem()) {
			visitBlockItem(item);
		}
		endScope();
		return null;
	}
	
	@Override public Void visitDeclaration(CobParser.DeclarationContext ctx) {
		//	type ID ( '=' expression )? ( ',' ID ( '=' expression )? )* ';'	
		CobParser.TypeContext typeCtx = ctx.type();
		visitType(typeCtx);
		for (CobParser.VariableContext var : ctx.variable()) {
			visitVariable(var);
		}
		return null;
	}
	
	@Override public Void visitVariable(CobParser.VariableContext ctx) {
		//	ID ( '=' expression )?
		CobParser.DeclarationContext decl = (CobParser.DeclarationContext)ctx.getParent();
		VariableSymbol varSym = new VariableSymbol(ctx.ID().getSymbol(),decl.type().tipe);
		varSym.setStatic(false);
		ctx.defn = varSym;
		currentScope.add(varSym);
		CobParser.ExpressionContext e = ctx.expression();
		if (e != null) {
			visit(e);  // get reference scopes for initializations
		}
		return null;
	}
	
	@Override public Void visitForDeclStatement(CobParser.ForDeclStatementContext ctx) {
		beginScope(new LocalScope(ctx.start.getLine(),currentScope));
        visitChildren(ctx);
		endScope();
		return null;
	}
	
    @Override public Void visitStringPrimary(CobParser.StringPrimaryContext ctx) {
    	//TODO assign a static struct to hold unique strings
        visitChildren(ctx);
        return null;
    }
	
}
