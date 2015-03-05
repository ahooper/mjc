package ca.nevdull.j1.compiler;

import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.misc.NotNull;

public class ExprTypeVisitor extends J1BaseVisitor<Void> {
	CompilationUnit unit;

	public ExprTypeVisitor(CompilationUnit unit) {
		this.unit = unit;
	}
	/*
	expression											locals [ Type tipe ]
		 memberExpression
		 indexExpression
		 callExpression
		 newExpression
		 castExpression
		 plusExpression
		 notExpression
		 mulitplyExpression
		 addExpression
		 shiftExpression
		 compareExpression
		 equalsExpression
		 binAndExpression
		 binExclExpression
		 binOrExpression
		 conAndExpression
		 conOrExpression
		 assignExpression
 	primary												locals [ Scope refScope, Type tipe ]
		 thisPrimary
		 superPrimary
	createdName											locals [ Scope refScope, Type tipe ]
	*/	
	@Override public Void visitPrimaryExpression(@NotNull J1Parser.PrimaryExpressionContext ctx) {
		visitChildren(ctx);
		ctx.tipe = ctx.expression().tipe;
		return null;
	}
	@Override public Void visitMemberExpression(@NotNull J1Parser.MemberExpressionContext ctx) { return visitChildren(ctx); }
	@Override public Void visitIndexExpression(@NotNull J1Parser.IndexExpressionContext ctx) { return visitChildren(ctx); }
	@Override public Void visitCallExpression(@NotNull J1Parser.CallExpressionContext ctx) { return visitChildren(ctx); }
	@Override public Void visitNewExpression(@NotNull J1Parser.NewExpressionContext ctx) { return visitChildren(ctx); }
	@Override public Void visitCastExpression(@NotNull J1Parser.CastExpressionContext ctx) { return visitChildren(ctx); }
	@Override public Void visitPlusExpression(@NotNull J1Parser.PlusExpressionContext ctx) { return visitChildren(ctx); }
	@Override public Void visitNotExpression(@NotNull J1Parser.NotExpressionContext ctx) { return visitChildren(ctx); }
	@Override public Void visitMulitplyExpression(@NotNull J1Parser.MulitplyExpressionContext ctx) { return visitChildren(ctx); }
	@Override public Void visitAddExpression(@NotNull J1Parser.AddExpressionContext ctx) { return visitChildren(ctx); }
	@Override public Void visitShiftExpression(@NotNull J1Parser.ShiftExpressionContext ctx) { return visitChildren(ctx); }
	@Override public Void visitCompareExpression(@NotNull J1Parser.CompareExpressionContext ctx) {
		visitChildren(ctx);
		//TODO
		ctx.tipe = PrimitiveType.booleanType;
		return null;
	}

	@Override public Void visitInstanceExpression(@NotNull J1Parser.InstanceExpressionContext ctx) {
		visitChildren(ctx);
		ctx.tipe = PrimitiveType.booleanType;
		return null;
	}
	@Override public Void visitEqualsExpression(@NotNull J1Parser.EqualsExpressionContext ctx) {
		visitChildren(ctx);
		//TODO
		ctx.tipe = PrimitiveType.booleanType;
		return null;
	}
	@Override public Void visitBinAndExpression(@NotNull J1Parser.BinAndExpressionContext ctx) { return visitChildren(ctx); }
	@Override public Void visitBinExclExpression(@NotNull J1Parser.BinExclExpressionContext ctx) { return visitChildren(ctx); }
	@Override public Void visitBinOrExpression(@NotNull J1Parser.BinOrExpressionContext ctx) { return visitChildren(ctx); }
	@Override public Void visitConAndExpression(@NotNull J1Parser.ConAndExpressionContext ctx) { return visitChildren(ctx); }
	@Override public Void visitConOrExpression(@NotNull J1Parser.ConOrExpressionContext ctx) { return visitChildren(ctx); }
	@Override public Void visitAssignExpression(@NotNull J1Parser.AssignExpressionContext ctx) { return visitChildren(ctx); }
	
	@Override public Void visitParenPrimary(@NotNull J1Parser.ParenPrimaryContext ctx) {
		visitChildren(ctx);
		ctx.tipe = ctx.expression().tipe;
		return null;
	}
	@Override public Void visitThisPrimary(@NotNull J1Parser.ThisPrimaryContext ctx) { return visitChildren(ctx); }
	@Override public Void visitSuperPrimary(@NotNull J1Parser.SuperPrimaryContext ctx) { return visitChildren(ctx); }
	
	@Override public Void visitLiteralPrimary(@NotNull J1Parser.LiteralPrimaryContext ctx) {
		visitChildren(ctx);
		ctx.tipe = ctx.literal().tipe;
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
	
	@Override public Void visitCreatedName(@NotNull J1Parser.CreatedNameContext ctx) { return visitChildren(ctx); }

	@Override public Void visitIntLiteral(@NotNull J1Parser.IntLiteralContext ctx) {
		ctx.tipe = PrimitiveType.intType;
		return null;
	}
	
	@Override public Void visitFloatLiteral(@NotNull J1Parser.FloatLiteralContext ctx)  {
		ctx.tipe = PrimitiveType.floatType;
		return null;
	}

	@Override public Void visitCharLiteral(@NotNull J1Parser.CharLiteralContext ctx)  {
		ctx.tipe = PrimitiveType.charType;
		return null;
	}

	@Override public Void visitStrLiteral(@NotNull J1Parser.StrLiteralContext ctx)  {
		ctx.tipe = unit.stringClass;
		return null;
	}

	@Override public Void visitBoolLiteral(@NotNull J1Parser.BoolLiteralContext ctx)  {
		ctx.tipe = PrimitiveType.booleanType;
		return null;
	}

	@Override public Void visitNullLiteral(@NotNull J1Parser.NullLiteralContext ctx)  {
		ctx.tipe = PrimitiveType.nullType;
		return null;
	}

}