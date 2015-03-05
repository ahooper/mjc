package ca.nevdull.j1.compiler;

import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.misc.NotNull;

public class DeclRefVisitor extends J1BaseVisitor<Void> {
	CompilationUnit unit;

	public DeclRefVisitor(CompilationUnit unit) {
		this.unit = unit;
	}
	
	@Override public Void visitMethodDeclaration(@NotNull J1Parser.MethodDeclarationContext ctx) {
		visitChildren(ctx);
		J1Parser.TypeContext type = ctx.type();
		ctx.tipe = (type != null) ? type.tipe : PrimitiveType.voidType;
		return null;
	}

	@Override public Void visitFormalParameter(@NotNull J1Parser.FormalParameterContext ctx) {
		J1Parser.TypeContext type = ctx.type();
		visit(type);
		J1Parser.VariableDeclaratorIdContext vdId = ctx.variableDeclaratorId();
		visit(vdId);
		ctx.tipe = ArrayType.create(type.tipe, vdId.dimensions().dim);
		ctx.defn.setType(ctx.tipe);
		return null;
	}

	public Void visitVariableDeclarators(@NotNull J1Parser.VariableDeclaratorsContext ctx, Type type) {
		for (J1Parser.VariableDeclaratorContext vd : ctx.variableDeclarator()) visitVariableDeclarator(vd,type);
		return null;
	}

	public Void visitVariableDeclarator(@NotNull J1Parser.VariableDeclaratorContext ctx, Type type) {
		J1Parser.VariableDeclaratorIdContext vdId = ctx.variableDeclaratorId();
		visit(vdId);
		ctx.tipe = ArrayType.create(type, vdId.dimensions().dim);
		ctx.defn.setType(ctx.tipe);
		return null;
	}

	@Override public Void visitObjectType(@NotNull J1Parser.ObjectTypeContext ctx) {
		visitChildren(ctx);
		ctx.tipe = ArrayType.create(ctx.classOrInterfaceType().tipe, ctx.dimensions().dim);
		return null;
	}
	
	@Override public Void visitPrimType(@NotNull J1Parser.PrimTypeContext ctx) {
		visitChildren(ctx);
		ctx.tipe = ArrayType.create(ctx.primitiveType().tipe, ctx.dimensions().dim);
		return null;
	}

	@Override public Void visitClassOrInterfaceType(@NotNull J1Parser.ClassOrInterfaceTypeContext ctx) {
		String name = ctx.Identifier().getText();
		Symbol sym = ctx.refScope.resolve(name);
		if (sym instanceof ClassSymbol) {
			Compiler.debug(name+" resolved in "+sym.scope.getName());
			ctx.tipe = (ClassSymbol)sym;
		} else {
			Compiler.error(ctx.Identifier(), name+" is not a class", "ClassOrInterfaceType");
			ctx.tipe = ErrorType.singleton;
		}
		return null;
	}
	
	@Override public Void visitPrimitiveType(@NotNull J1Parser.PrimitiveTypeContext ctx) {
		ctx.tipe = PrimitiveType.resolve(ctx.start.getText());
		assert ctx.tipe != null;
		return null;
	}

	@Override public Void visitLocalVariableDeclaration(@NotNull J1Parser.LocalVariableDeclarationContext ctx) {
		J1Parser.TypeContext type = ctx.type();
		visit(type);
		visitVariableDeclarators(ctx.variableDeclarators(), type.tipe);
		return null;
	}

	@Override public Void visitIdentPrimary(@NotNull J1Parser.IdentPrimaryContext ctx) {
		Token token = ctx.Identifier().getSymbol();
		String name = token.getText();
		Symbol sym = ctx.refScope.resolve(name);
		if (sym instanceof MethodSymbol) {
			Compiler.debug(name+" resolved in "+sym.scope.getName());
			ctx.tipe = (MethodSymbol)sym;
		} else if (sym != null) {
			Compiler.debug(name+" resolved in "+sym.scope.getName());
			ctx.tipe = sym.getType();
        	// check for forward local reference forbidden
        	int refLocation = token.getTokenIndex();
        	Scope defScope = sym.getScope();
        	int defLocation = sym.getToken().getTokenIndex();
        	System.out.println(name+" ref@"+refLocation+" def@"+defLocation);
        	if (   ctx.refScope instanceof BaseScope
        		&& defScope instanceof BaseScope
        		&& refLocation < defLocation ) {
        		Compiler.error(token, name+" is forward local variable reference","IdentifierPrimary");
        		sym = null;
        	}
		} else {
			Compiler.error(token, name+" is not defined", "visitIdentPrimary");
			ctx.tipe = ErrorType.singleton;
		}
		return null;
	}

}
