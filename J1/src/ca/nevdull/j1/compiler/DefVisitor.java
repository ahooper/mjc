package ca.nevdull.j1.compiler;

import org.antlr.v4.runtime.misc.NotNull;

import ca.nevdull.j1.compiler.J1Parser.VariableDeclaratorIdContext;

public class DefVisitor extends J1BaseVisitor<Void> {
	Scope currentScope;
	CompilationUnit unit;

	public DefVisitor(CompilationUnit unit) {
		this.unit = unit;
	}

	@Override public Void visitCompilationUnit(@NotNull J1Parser.CompilationUnitContext ctx) {
		currentScope = unit;
		visitChildren(ctx);
		System.out.println(currentScope);
		currentScope = currentScope.getEnclosingScope();
		return null;
	}

	@Override public Void visitMethodDeclaration(@NotNull J1Parser.MethodDeclarationContext ctx) {
	    J1Parser.TypeContext type = ctx.type();
		if (type != null) visit(type);
		visit(ctx.dimensions());
		if (ctx.dimensions().dim > 0) ; //TODO
		MethodSymbol meth = new MethodSymbol(currentScope, ctx.Identifier().getSymbol());
		currentScope.define(meth);
		currentScope = meth;
		ctx.defn = meth;
		J1Parser.FormalParametersContext fp = ctx.formalParameters();
		if (fp != null) visit(fp);
		J1Parser.MethodBodyContext body = ctx.methodBody();
		if (body != null) visit(body);
		System.out.println(currentScope);
		currentScope = meth.getEnclosingScope();
		return null;
	}
	
	@Override public Void visitDimensions(@NotNull J1Parser.DimensionsContext ctx) {
		ctx.dim = ctx.getChildCount() / 2;
		return null;
	}
	
	@Override public Void visitFormalParameter(@NotNull J1Parser.FormalParameterContext ctx) {
	    J1Parser.TypeContext type = ctx.type();
		visit(type);
		VariableDeclaratorIdContext vdId = ctx.variableDeclaratorId();
		visit(vdId);
		if (vdId.dimensions().dim > 0) ; //TODO
		VariableSymbol var = new VariableSymbol(vdId.Identifier().getSymbol());
		currentScope.define(var);
		ctx.defn = var;
		return null;
	}

	@Override public Void visitVariableDeclarator(@NotNull J1Parser.VariableDeclaratorContext ctx) {
		VariableDeclaratorIdContext vdId = ctx.variableDeclaratorId();
		visit(vdId);
		if (vdId.dimensions().dim > 0) ; //TODO
		VariableSymbol var = new VariableSymbol(vdId.Identifier().getSymbol());
		currentScope.define(var);
		ctx.defn = var;
		J1Parser.VariableInitializerContext init = ctx.variableInitializer();
		if (init != null) visit(init);
		return null;
	}
		
	@Override public Void visitClassOrInterfaceType(@NotNull J1Parser.ClassOrInterfaceTypeContext ctx) {
		ctx.refScope = currentScope;
		visitChildren(ctx);
		return null;
	}

	@Override public Void visitBlock(@NotNull J1Parser.BlockContext ctx) {
		currentScope = new LocalScope(currentScope, ctx.start);
		visitChildren(ctx);
		System.out.println(currentScope);
		currentScope = currentScope.getEnclosingScope();
		return null;
	}

	@Override public Void visitIdentPrimary(@NotNull J1Parser.IdentPrimaryContext ctx) {
		Compiler.debug(ctx.Identifier().getText()+" refScope="+currentScope);
		ctx.refScope = currentScope;
		return null;
	}

}
