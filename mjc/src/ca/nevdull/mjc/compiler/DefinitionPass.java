package ca.nevdull.mjc.compiler;

import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.misc.NotNull;
import org.antlr.v4.runtime.tree.ParseTreeProperty;

public class DefinitionPass extends MJBaseListener {
    ParseTreeProperty<Scope> scopes = new ParseTreeProperty<Scope>();
    GlobalScope globals;
    Scope currentScope; // define symbols in this scope

    void saveScope(ParserRuleContext ctx, Scope s) {
    	scopes.put(ctx, s);
    }
    
    void popScope() {
	    System.out.println(currentScope);
	    currentScope = currentScope.getEnclosingScope();
    }

    @Override
    public void enterCompilationUnit(@NotNull MJParser.CompilationUnitContext ctx) {
        globals = new GlobalScope();
        currentScope = globals;
        //TODO: define built-in types
    }

    @Override
    public void exitCompilationUnit(@NotNull MJParser.CompilationUnitContext ctx) {
    	assert currentScope == globals;
        System.out.println(globals);
        //TODO: report undefined names
    }
    
	@Override 
	public void enterClassDeclaration(@NotNull MJParser.ClassDeclarationContext ctx) {
		ClassSymbol klass = new ClassSymbol(ctx.Identifier().getSymbol(), currentScope, null/*TODO:pick up extends*/);
		currentScope.define(klass);
        currentScope = klass;
        saveScope(ctx, currentScope);			
	}
	
	@Override 
	public void exitClassDeclaration(@NotNull MJParser.ClassDeclarationContext ctx) {
        popScope();
	}

	@Override
	public void enterMethodDeclaration(@NotNull MJParser.MethodDeclarationContext ctx) {
		MethodSymbol method = new MethodSymbol(ctx.Identifier().getSymbol(), currentScope);
		currentScope.define(method);
        currentScope = method;
        saveScope(ctx, currentScope);					
	}

	@Override
	public void exitMethodDeclaration(@NotNull MJParser.MethodDeclarationContext ctx) {
        popScope();  // formal parameters
	}

	@Override
	public void exitVariableDeclaratorId(@NotNull MJParser.VariableDeclaratorIdContext ctx) {
		VarSymbol var = new VarSymbol(ctx.Identifier().getSymbol());
		currentScope.define(var);
	}

	@Override 
	public void enterFormalParameters(@NotNull MJParser.FormalParametersContext ctx) {
	}

	@Override 
	public void exitFormalParameters(@NotNull MJParser.FormalParametersContext ctx) {
	}

	@Override 
	public void enterBlock(@NotNull MJParser.BlockContext ctx) {
        // push new local scope
        currentScope = new LocalScope(currentScope, ctx.getStart().getLine());
        saveScope(ctx, currentScope);
	}

	@Override public void exitBlock(@NotNull MJParser.BlockContext ctx) {
        popScope();		
	}

}
