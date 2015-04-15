package ca.nevdull.mjc.compiler;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.antlr.v4.runtime.ANTLRInputStream;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.misc.NotNull;
import org.antlr.v4.runtime.tree.ParseTreeWalker;
import org.antlr.v4.runtime.tree.TerminalNode;

import ca.nevdull.mjc.compiler.Compiler.UnderlineErrorListener;

public class DefinitionPass extends MJBaseListener {
	PassData passData;
    Scope currentScope; // define symbols in this scope
    static String preloadClasses[] = {
    			"Object",
    			"Class",
    			"String",
    			"Integer",
    			"System",
    			};
    
    DefinitionPass(PassData passData) {
    	super();
    	this.passData = passData;
        passData.globals = new GlobalScope();
        enterScope(passData.globals);
		//XXX ClassSymbol nullClass = new ClassSymbol("_NULL_", passData.globals, null);
		//XXX passData.globals.define(nullClass);
    	String[] bootclasspath = passData.options.bootClassPath;
    	List<String> nameComponents = new ArrayList<String>();
    	for (String className : preloadClasses) {
    		nameComponents.clear();
    		nameComponents.add(className);
    		importClass(nameComponents, bootclasspath);
    	}
    }
    
    private void enterScope(Scope newScope) {
    	System.out.print("enter ");
	    System.out.print(newScope);
	    System.out.print(" enclosing=");
	    System.out.print(newScope.getEnclosingScope());
	    System.out.println();
	    currentScope = newScope;
    }
    
    private void leaveScope() {
    	System.out.print("leave ");
	    System.out.println(currentScope);
	    currentScope = currentScope.getEnclosingScope();
    }

    @Override
    public void enterCompilationUnit(@NotNull MJParser.CompilationUnitContext ctx) {
    }

    @Override
    public void exitCompilationUnit(@NotNull MJParser.CompilationUnitContext ctx) {
    	assert currentScope == passData.globals;
        System.out.println(passData.globals);
    }
    
    @Override public void exitImportDeclaration(@NotNull MJParser.ImportDeclarationContext ctx) {
    	List<TerminalNode> identifiers = ctx.qualifiedName().Identifier();
    	List<String> nameComponents = new ArrayList<String>(identifiers.size());
    	for (TerminalNode ident : identifiers) {
    		nameComponents.add(ident.getText());
    	}
    	String[] classpath = passData.options.classPath;
		importClass(nameComponents, classpath);    	
    }

	private void importClass(List<String> nameComponents, String[] classPath) {
		// Read saved symbols
		StringBuilder qname = new StringBuilder();
    	for (String nameComponent : nameComponents) {
    		if (qname.length() > 0) qname.append(File.separatorChar);
    		qname.append(nameComponent);
    	}
    	String qnameString = qname.toString();
    	if (passData.globals.resolve(qnameString) != null) {
    		System.out.println("previously imported "+qnameString);
    		return;
    	}
    	boolean found = false;
        try {
        	for (String pathDir : classPath) {
            	File fname;
				if (pathDir == null || pathDir.length() == 0) {
        			if (passData.outputDir == null) fname = new File(qnameString+Compiler.IMPORT_SUFFIX);
        			else fname = new File(passData.outputDir,qnameString+Compiler.IMPORT_SUFFIX);
        		} else fname = new File(pathDir,qnameString+Compiler.IMPORT_SUFFIX);
            	if (! fname.isFile() ) continue; // try next in path
                found = true;
                System.out.println("import found "+fname.getAbsolutePath());
                ClassSymbol importClass = compileImport(fname);
                break;
        	}
			if (!found) Compiler.error("Not found "+qname+" (path "+Arrays.toString(classPath)+")");        	
		} catch (IOException excp) {
			Compiler.error("Unable to read import "+excp.toString());
			excp.printStackTrace();
		}
		if (!found) {
			//TODO install an unknown class to soak up member errors
		};        	
	}
    
	private ClassSymbol compileImport(File inFile) throws FileNotFoundException, IOException {
		ANTLRInputStream input = new ANTLRInputStream(new FileInputStream(inFile));
        MJLexer lexer = new MJLexer(input);
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        MJParser parser = new MJParser(tokens);
        parser.removeErrorListeners(); // remove ConsoleErrorListener
        parser.addErrorListener(new UnderlineErrorListener());
        parser.setBuildParseTree(true);
        MJParser.CompilationUnitContext unit = parser.compilationUnit();
        ParseTreeWalker walker = new ParseTreeWalker();
        /*
        PassData impData = new PassData();
        impData.options = this.passData.options;
        impData.parser = parser;
        impData.outputDir = null;
        DefinitionPass def = new DefinitionPass(impData);
        */
        walker.walk(this, unit);
        return unit.typeDeclaration(0).classDeclaration().defn;
	}
	
    @Override public void exitQualifiedName(MJParser.QualifiedNameContext ctx) { }
    
    @Override
	public void exitTypeDeclaration(@NotNull MJParser.TypeDeclarationContext ctx) {
    	Access access = Access.DEFAULT;
		MJParser.ClassDeclarationContext cdecl = ctx.classDeclaration();
		//LATER could be interfaceDeclaration
		if (cdecl != null) {
			for (MJParser.ClassOrInterfaceModifierContext m : ctx.classOrInterfaceModifier()) {
				access = applyAccessModifier(access, m,"TypeDeclaration");
	    		if (m.STATIC() != null) cdecl.defn.isStatic = true;
	    		if (m.ABSTRACT() != null) cdecl.defn.isAbstract = true;
	    		if (m.FINAL() != null) cdecl.defn.isFinal = true;
	    		// not fussing if static, abstract, or final modifiers are repeated
			}
			System.out.println("exitTypeDeclaration class access "+access);
			cdecl.defn.setAccess(access);
		}
	}

	private Access applyAccessModifier(Access access,
			MJParser.ClassOrInterfaceModifierContext m, String where) {
		Access a = Access.DEFAULT;
		if (m.PUBLIC() != null) a = Access.PUBLIC;
		else if (m.PROTECTED() != null) a = Access.PROTECTED;
		else if (m.PRIVATE() != null) a = Access.PRIVATE;
		if (a != Access.DEFAULT) {
			if (access == Access.DEFAULT) {
				access = a;
			} else {
				Compiler.error(m.getStart(),"conflicts with previous modifier "+access,where);
			}
		} else assert false : "unrecognized classOrInterfaceModifier";
		return access;
	}
	@Override public void exitModifier(@NotNull MJParser.ModifierContext ctx) { }
	@Override public void exitClassOrInterfaceModifier(@NotNull MJParser.ClassOrInterfaceModifierContext ctx) { }
    
	@Override 
	public void enterClassDeclaration(@NotNull MJParser.ClassDeclarationContext ctx) {
		ClassSymbol klass = new ClassSymbol(ctx.Identifier().getSymbol(), currentScope, null);
        ctx.defn = klass;
		currentScope.define(klass);
		enterScope(klass);
	}
	
	@Override 
	public void exitClassDeclaration(@NotNull MJParser.ClassDeclarationContext ctx) {
        leaveScope();
	}
	
	@Override
	public void exitMemberClassBodyDeclaration(@NotNull MJParser.MemberClassBodyDeclarationContext ctx) {
    	Access access = Access.DEFAULT;
		MJParser.MemberDeclarationContext member = ctx.memberDeclaration();
		if (member != null) {
			for (MJParser.ModifierContext m : ctx.modifier()) {
				MJParser.ClassOrInterfaceModifierContext cm = m.classOrInterfaceModifier();
				if (cm != null) {
					access = applyAccessModifier(access, cm,"MemberClassBodyDeclaration");
					if (member.methodDeclaration() != null) {
						MethodSymbol mdefn = member.methodDeclaration().defn;
			    		if (cm.STATIC() != null) mdefn.isStatic = true;
			    		if (cm.ABSTRACT() != null) mdefn.isAbstract = true;
			    		if (cm.FINAL() != null) mdefn.isFinal = true;
					} else if (member.fieldDeclaration() != null) {
						for (MJParser.VariableDeclaratorContext vd : member.fieldDeclaration().variableDeclarators().variableDeclarator()) {
							VariableSymbol vdefn = vd.defn;
				    		if (cm.STATIC() != null) vdefn.isStatic = true;
				    		if (cm.ABSTRACT() != null) Compiler.error(cm.start,"abstract cannot be applied to a fieldDeclaration","MemberClassBodyDeclaration");
				    		if (cm.FINAL() != null) vdefn.isFinal = true;
						}
					} else if (member.constructorDeclaration() != null) {
						MethodSymbol cdefn = member.constructorDeclaration().defn;
			    		if (cm.STATIC() != null) cdefn.isStatic = true;
			    		if (cm.ABSTRACT() != null) cdefn.isAbstract = true;
			    		if (cm.FINAL() != null) cdefn.isFinal = true;
					} else if (member.classDeclaration() != null) {
						ClassSymbol cdefn = member.classDeclaration().defn;
			    		if (cm.STATIC() != null) cdefn.isStatic = true;
			    		if (cm.ABSTRACT() != null) cdefn.isAbstract = true;
			    		if (cm.FINAL() != null) cdefn.isFinal = true;
					} else assert false : "unrecognized memberDeclaration";
		    		// not fussing if static, abstract, or final modifiers are repeated
				} /*else LATER - more modifiers*/
			}
			System.out.println("exitMemberClassBodyDeclaration class access "+access);
			if (member.methodDeclaration() != null) {
				member.methodDeclaration().defn.setAccess(access);
			} else if (member.fieldDeclaration() != null) {
				for (MJParser.VariableDeclaratorContext vd : member.fieldDeclaration().variableDeclarators().variableDeclarator()) {
					vd.defn.setAccess(access);
				}
			} else if (member.constructorDeclaration() != null) {
				member.constructorDeclaration().defn.setAccess(access);
			} else if (member.classDeclaration() != null) {
				member.classDeclaration().defn.setAccess(access);
			} else assert false : "unrecognized memberDeclaration";
		}
	}
	
	@Override
	public void enterConstructorDeclaration(MJParser.ConstructorDeclarationContext ctx) {
    	Token token = ctx.Identifier().getSymbol();
        String name = token.getText();
        Symbol sym = currentScope.resolve(name);
        if (sym != currentScope) {
        	Compiler.error(token, name+" is not the class name "+currentScope.getScopeName(),"ConstructorDeclaration");
        }
        assert currentScope instanceof ClassSymbol;
		MethodSymbol method = new MethodSymbol(token, currentScope);
        ctx.defn = method;
		((ClassSymbol)currentScope).setConstructor(method);
		enterScope(method);
 	}

	@Override
	public void exitConstructorDeclaration(MJParser.ConstructorDeclarationContext ctx) {
        leaveScope();  // formal parameters
	}

	@Override
	public void enterMethodDeclaration(@NotNull MJParser.MethodDeclarationContext ctx) {
		MethodSymbol method = new MethodSymbol(ctx.Identifier().getSymbol(), currentScope);
        ctx.defn = method;
		currentScope.define(method);
		enterScope(method);
	}

	@Override
	public void exitMethodDeclaration(@NotNull MJParser.MethodDeclarationContext ctx) {
        leaveScope();  // formal parameters
	}
	
	@Override
	public void enterFieldDeclaration(@NotNull MJParser.FieldDeclarationContext ctx) {
		for (MJParser.VariableDeclaratorContext vd : ctx.variableDeclarators().variableDeclarator()) {
			vd.defn = defineVariable(vd.variableDeclaratorId());
		}
	}
	
	@Override
	public void enterClassOrInterfaceType(MJParser.ClassOrInterfaceTypeContext ctx) {
		ctx.refScope = currentScope;
	}

	/**
	 * @param ctx 
	 * @param t
	 * @param vdlist
	 * @return 
	 */
	public VariableSymbol defineVariable(MJParser.VariableDeclaratorIdContext vdi) {
		VariableSymbol var = new VariableSymbol(vdi.Identifier().getSymbol(),null, currentScope);
		currentScope.define(var);
		//TODO variableModifiers
		return var;
	}

	@Override
	public void enterFormalParameter(@NotNull MJParser.FormalParameterContext ctx) {
		ctx.defn = defineVariable(ctx.variableDeclaratorId());
		//TODO variableModifiers
	}
	@Override public void exitVariableModifier(@NotNull MJParser.VariableModifierContext ctx) { }
	

	@Override 
	public void enterBlock(@NotNull MJParser.BlockContext ctx) {
		enterScope(new LocalScope(currentScope, ctx.getStart().getLine()));
	}

	@Override
	public void exitBlock(@NotNull MJParser.BlockContext ctx) {
        leaveScope();		
	}
	
	@Override
	public void enterLocalVariableDeclaration(@NotNull MJParser.LocalVariableDeclarationContext ctx) {
		for (MJParser.VariableDeclaratorContext vd : ctx.variableDeclarators().variableDeclarator()) {
			vd.defn = defineVariable(vd.variableDeclaratorId());
		}
		//TODO variableModifiers
		//TODO ensure no declaration of same name in enclosing blocks 
	}

	@Override
	public void enterSuperPrimary(@NotNull MJParser.SuperPrimaryContext ctx) {
		ctx.refScope = currentScope; // save for ReferencePass
	}
	
	@Override
	public void enterIdentifierPrimary(@NotNull MJParser.IdentifierPrimaryContext ctx) {
		ctx.refScope = currentScope; // save for ReferencePass
	}
	
	@Override
	public void enterThisPrimary(@NotNull MJParser.ThisPrimaryContext ctx) {
		ctx.refScope = currentScope; // save for ReferencePass
	}
	
	@Override
	public void exitCreatedName(MJParser.CreatedNameContext ctx) {
		ctx.refScope = currentScope; // save for ReferencePass
	}

}
