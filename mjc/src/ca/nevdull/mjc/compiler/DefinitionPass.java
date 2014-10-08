package ca.nevdull.mjc.compiler;

import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.misc.NotNull;
import org.antlr.v4.runtime.tree.ParseTreeProperty;

public class DefinitionPass extends MJBaseListener {
    ParseTreeProperty<Scope> scopes = new ParseTreeProperty<Scope>();
    GlobalScope globals;
    Scope currentScope; // define symbols in this scope
    ParseTreeProperty<Symbol> symbols = new ParseTreeProperty<Symbol>();

    void saveScope(ParserRuleContext ctx, Scope s) {
    	scopes.put(ctx, s);
    }
    
    void popScope() {
	    System.out.println(currentScope);
	    currentScope = currentScope.getEnclosingScope();
    }
    
    public void saveSymbol(ParserRuleContext node, Symbol sym) {
    	symbols.put(node, sym);
    }
    
    public Symbol getSymbol(ParserRuleContext node) {
    	Symbol sym = symbols.get(node);
    	assert sym != null;
    	return sym;
    }

    @Override
    public void enterCompilationUnit(@NotNull MJParser.CompilationUnitContext ctx) {
        globals = new GlobalScope();
        //TODO define globals - Object, Class, String
        currentScope = globals;
     }

    @Override
    public void exitCompilationUnit(@NotNull MJParser.CompilationUnitContext ctx) {
    	assert currentScope == globals;
        System.out.println(globals);
    }
	
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
			((ClassSymbol)scopes.get(cdecl)).setAccess(access);
		}
	}
	@Override public void exitModifier(@NotNull MJParser.ModifierContext ctx) { }
	@Override public void exitClassOrInterfaceModifier(@NotNull MJParser.ClassOrInterfaceModifierContext ctx) { }
    
	@Override 
	public void enterClassDeclaration(@NotNull MJParser.ClassDeclarationContext ctx) {
		if (ctx.type() != null) saveScope(ctx.type(), currentScope); // save for ReferencePass to look up superClass
		ClassSymbol klass = new ClassSymbol(ctx.Identifier().getSymbol(), currentScope, null);
        saveSymbol(ctx, klass);
		currentScope.define(klass);
        currentScope = klass;
        saveScope(ctx, currentScope);  //TODO is this needed/used?
	}
	
	@Override 
	public void exitClassDeclaration(@NotNull MJParser.ClassDeclarationContext ctx) {
        popScope();
	}

	@Override
	public void enterConstructorDeclaration(MJParser.ConstructorDeclarationContext ctx) {
		MethodSymbol method = new MethodSymbol(ctx.Identifier().getSymbol(), currentScope);
		currentScope.define(method);
        currentScope = method;
        saveScope(ctx, currentScope);  //TODO is this needed/used?
	}

	@Override
	public void exitConstructorDeclaration(MJParser.ConstructorDeclarationContext ctx) {
        popScope();  // formal parameters
	}

	@Override
	public void enterMethodDeclaration(@NotNull MJParser.MethodDeclarationContext ctx) {
		if (ctx.type() != null) saveScope(ctx.type(), currentScope); // save for ReferencePass to look up return type
		MethodSymbol method = new MethodSymbol(ctx.Identifier().getSymbol(), currentScope);
        saveSymbol(ctx, method);
		currentScope.define(method);
        currentScope = method;
        saveScope(ctx, currentScope);  //TODO is this needed/used?
	}

	@Override
	public void exitMethodDeclaration(@NotNull MJParser.MethodDeclarationContext ctx) {
        popScope();  // formal parameters
	}
	
	@Override
	public void enterFieldDeclaration(@NotNull MJParser.FieldDeclarationContext ctx) {
		saveScope(ctx.type(), currentScope); // save for ReferencePass to look up field type
		for (MJParser.VariableDeclaratorContext vd : ctx.variableDeclarators().variableDeclarator()) {
			defineVariable(vd.variableDeclaratorId());
		}
	}

	/**
	 * @param ctx 
	 * @param t
	 * @param vdlist
	 */
	public void defineVariable(MJParser.VariableDeclaratorIdContext vdi) {
		VarSymbol var = new VarSymbol(vdi.Identifier().getSymbol(),null);
        saveSymbol(vdi, var);
		currentScope.define(var);
		//TODO variableModifiers
	}

	@Override
	public void enterFormalParameter(@NotNull MJParser.FormalParameterContext ctx) {
		saveScope(ctx.type(), currentScope); // save for ReferencePass to look up parameter type
		defineVariable(ctx.variableDeclaratorId());
		//TODO variableModifiers
	}
	@Override public void exitVariableModifier(@NotNull MJParser.VariableModifierContext ctx) { }
	

	@Override 
	public void enterBlock(@NotNull MJParser.BlockContext ctx) {
        currentScope = new LocalScope(currentScope, ctx.getStart().getLine());
        saveScope(ctx, currentScope);
	}

	@Override
	public void exitBlock(@NotNull MJParser.BlockContext ctx) {
        popScope();		
	}
	
	@Override
	public void enterLocalVariableDeclaration(@NotNull MJParser.LocalVariableDeclarationContext ctx) {
		saveScope(ctx.type(), currentScope); // save for ReferencePass to look up field type
		for (MJParser.VariableDeclaratorContext vd : ctx.variableDeclarators().variableDeclarator()) {
			defineVariable(vd.variableDeclaratorId());
		}
		//TODO variableModifiers
		//TODO ensure no declaration of same name in enclosing blocks 
	}

	@Override
	public void enterSuperPrimary(@NotNull MJParser.SuperPrimaryContext ctx) {
		saveScope(ctx, currentScope); // save for ReferencePass
	}
	
	@Override
	public void enterIdentifierPrimary(@NotNull MJParser.IdentifierPrimaryContext ctx) {
		saveScope(ctx, currentScope); // save for ReferencePass
	}
	
	@Override
	public void enterThisPrimary(@NotNull MJParser.ThisPrimaryContext ctx) {
		saveScope(ctx, currentScope); // save for ReferencePass
	}

}
