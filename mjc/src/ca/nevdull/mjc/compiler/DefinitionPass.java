package ca.nevdull.mjc.compiler;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.ArrayList;
import java.util.List;

import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.misc.NotNull;
import org.antlr.v4.runtime.tree.TerminalNode;

public class DefinitionPass extends MJBaseListener {
	PassData passData;
    Scope currentScope; // define symbols in this scope
    static String preloadClasses[] = {
    			"Object",
    			"Class",
    			"String"
    			};
    
    DefinitionPass(PassData passData) {
    	super();
    	this.passData = passData;
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
        passData.globals = new GlobalScope();
        //TODO define globals - Object, Class, String
		ClassSymbol nullClass = new ClassSymbol("_NULL_", passData.globals, null);
		passData.globals.define(nullClass);
    	String[] bootclasspath = passData.options.bootClassPath;
    	List<String> nameComponents = new ArrayList<String>();
    	for (String className : preloadClasses) {
    		nameComponents.clear();
    		nameComponents.add(className);
    		importClass(nameComponents, bootclasspath);
    	}
        enterScope(passData.globals);
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
    	qname.append(Compiler.IMPORT_SUFFIX);
        try {
        	boolean found = false;
        	for (String pathDir : classPath) {
            	File fname;
        		if (pathDir == null || pathDir.length() == 0) {
        			if (passData.inputDir == null) fname = new File(qname.toString());
        			else fname = new File(passData.inputDir,qname.toString());
        		} else fname = new File(pathDir+File.separator+qname.toString());
            	if (! fname.isFile() ) continue; // try next in path
                found = true;
            	FileInputStream fis = new FileInputStream(fname);
                ObjectInputStream ois = new ObjectInputStream(fis);
                ClassSymbol importClass = (ClassSymbol) ois.readObject();
                System.out.println("import "+qname+" "+importClass);
                ois.close();
                passData.globals.define(importClass);
                break;
        	}
			if (!found) Compiler.error("Not found "+qname+" (path "+classPath+")");        	
		} catch (IOException | ClassNotFoundException excp) {
			Compiler.error("Unable to read symbols "+excp.getMessage());
		}
	}
    
    @Override public void exitQualifiedName(MJParser.QualifiedNameContext ctx) { }
    
    @Override
	public void exitTypeDeclaration(@NotNull MJParser.TypeDeclarationContext ctx) {
    	Access access = Access.AccessDefault;
		for (MJParser.ClassOrInterfaceModifierContext m : ctx.classOrInterfaceModifier()) {
			Access a = Access.AccessDefault;
			if (m.PUBLIC() != null) a = Access.AccessPublic;
			else if (m.PROTECTED() != null) a = Access.AccessProtected;
			else if (m.PRIVATE() != null) a = Access.AccessPrivate;
			if (a != Access.AccessDefault) {
				if (access == Access.AccessDefault) {
					access = a;
				} else {
					Compiler.error(m.getStart(),"conflicts with previous modifier "+access,"TypeDeclaration");
				}
			} else assert false : "unrecognized classOrInterfaceModifier";
		}
		MJParser.ClassDeclarationContext cdecl = ctx.classDeclaration();
		//LATER could be interfaceDeclaration
		if (cdecl != null) {
			System.out.println("exitTypeDeclaration class access "+access);
			cdecl.defn.setAccess(access);
		}
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
