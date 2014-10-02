package ca.nevdull.mjc.compiler;

import org.antlr.v4.runtime.misc.NotNull;
import org.antlr.v4.runtime.tree.ParseTreeProperty;

public class ReferencePass extends MJBaseListener {
    ParseTreeProperty<Scope> scopes;
    GlobalScope globals;
    Scope currentScope; // resolve symbols starting in this scope
	
    /**
	 * @param scopes
	 * @param globals
	 */
	public ReferencePass(ParseTreeProperty<Scope> scopes, GlobalScope globals) {
		super();
		this.scopes = scopes;
		this.globals = globals;
	}

    @Override
    public void enterCompilationUnit(@NotNull MJParser.CompilationUnitContext ctx) {
        currentScope = globals;
    }

    @Override
    public void exitCompilationUnit(@NotNull MJParser.CompilationUnitContext ctx) {
    	assert currentScope == globals;
        //TODO: report undefined names
    }
    
	@Override 
	public void enterClassDeclaration(@NotNull MJParser.ClassDeclarationContext ctx) {
        currentScope = scopes.get(ctx);
	}
	
	@Override 
	public void exitClassDeclaration(@NotNull MJParser.ClassDeclarationContext ctx) {
		currentScope = currentScope.getEnclosingScope();	
	}

	@Override
	public void enterMethodDeclaration(@NotNull MJParser.MethodDeclarationContext ctx) {
        currentScope = scopes.get(ctx);
	}

	@Override
	public void exitMethodDeclaration(@NotNull MJParser.MethodDeclarationContext ctx) {
		currentScope = currentScope.getEnclosingScope();	
	}

	@Override 
	public void enterBlock(@NotNull MJParser.BlockContext ctx) {
        currentScope = scopes.get(ctx);
	}

	@Override public void exitBlock(@NotNull MJParser.BlockContext ctx) {
		currentScope = currentScope.getEnclosingScope();	
	}
}
