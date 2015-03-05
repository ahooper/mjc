package ca.nevdull.j1.compiler;

import org.antlr.v4.runtime.misc.NotNull;

public class ModelVisitor extends J1BaseVisitor<Void> {

	public ModelVisitor() {
		// TODO Auto-generated constructor stub
	}

	@Override public Void visitCompilationUnit(@NotNull J1Parser.CompilationUnitContext ctx) {
		return visitChildren(ctx);
	}

	@Override public Void visitMethodDeclaration(@NotNull J1Parser.MethodDeclarationContext ctx) {
		return visitChildren(ctx);
	}

	@Override public Void visitFormalParameters(@NotNull J1Parser.FormalParametersContext ctx) {
		return visitChildren(ctx);
	}

	@Override public Void visitFormalParameterList(@NotNull J1Parser.FormalParameterListContext ctx) {
		return visitChildren(ctx);
	}

	@Override public Void visitFormalParameter(@NotNull J1Parser.FormalParameterContext ctx) {
		return visitChildren(ctx);
	}

	@Override public Void visitMethodBody(@NotNull J1Parser.MethodBodyContext ctx) {
		return visitChildren(ctx);
	}
	@Override public Void visitVariableDeclarators(@NotNull J1Parser.VariableDeclaratorsContext ctx) {
		return visitChildren(ctx);
	}

	@Override public Void visitVariableDeclarator(@NotNull J1Parser.VariableDeclaratorContext ctx) {
		return visitChildren(ctx);
	}

	@Override public Void visitVariableDeclaratorId(@NotNull J1Parser.VariableDeclaratorIdContext ctx) {
		return visitChildren(ctx);
	}

	@Override public Void visitVariableInitializer(@NotNull J1Parser.VariableInitializerContext ctx) {
		return visitChildren(ctx);
	}

	@Override public Void visitObjectType(@NotNull J1Parser.ObjectTypeContext ctx) {
		return visitChildren(ctx);
	}

	@Override public Void visitPrimType(@NotNull J1Parser.PrimTypeContext ctx) {
		return visitChildren(ctx);
	}

	@Override public Void visitBlock(@NotNull J1Parser.BlockContext ctx) {
		return visitChildren(ctx);
	}

	@Override public Void visitBlockStatement(@NotNull J1Parser.BlockStatementContext ctx) {
		return visitChildren(ctx);
	}

	@Override public Void visitLocalVariableDeclarationStatement(@NotNull J1Parser.LocalVariableDeclarationStatementContext ctx) {
		return visitChildren(ctx);
	}

	@Override public Void visitLocalVariableDeclaration(@NotNull J1Parser.LocalVariableDeclarationContext ctx) {
		return visitChildren(ctx);
	}

	@Override public Void visitBlkStatement(@NotNull J1Parser.BlkStatementContext ctx) {
		return visitChildren(ctx);
	}

	@Override public Void visitIfStatement(@NotNull J1Parser.IfStatementContext ctx) {
		return visitChildren(ctx);
	}

	@Override public Void visitWhileStatement(@NotNull J1Parser.WhileStatementContext ctx) {
		return visitChildren(ctx);
	}

	@Override public Void visitReturnStatement(@NotNull J1Parser.ReturnStatementContext ctx) {
		return visitChildren(ctx);
	}

	@Override public Void visitBreakStatement(@NotNull J1Parser.BreakStatementContext ctx) {
		return visitChildren(ctx);
	}

	@Override public Void visitContinueStatement(@NotNull J1Parser.ContinueStatementContext ctx) {
		return visitChildren(ctx);
	}

	@Override public Void visitEmptyStatement(@NotNull J1Parser.EmptyStatementContext ctx) {
		return visitChildren(ctx);
	}

	@Override public Void visitExprStatement(@NotNull J1Parser.ExprStatementContext ctx) {
		return visitChildren(ctx);
	}

	@Override public Void visitParExpression(@NotNull J1Parser.ParExpressionContext ctx) {
		return visitChildren(ctx);
	}

	@Override public Void visitExpressionList(@NotNull J1Parser.ExpressionListContext ctx) {
		return visitChildren(ctx);
	}

	@Override public Void visitPrimaryExpression(@NotNull J1Parser.PrimaryExpressionContext ctx) {
		return visitChildren(ctx);
	}

	@Override public Void visitMemberExpression(@NotNull J1Parser.MemberExpressionContext ctx) {
		return visitChildren(ctx);
	}

	@Override public Void visitIndexExpression(@NotNull J1Parser.IndexExpressionContext ctx) {
		return visitChildren(ctx);
	}

	@Override public Void visitCallExpression(@NotNull J1Parser.CallExpressionContext ctx) {
		return visitChildren(ctx);
	}

	@Override public Void visitNewExpression(@NotNull J1Parser.NewExpressionContext ctx) {
		return visitChildren(ctx);
	}

	@Override public Void visitCastExpression(@NotNull J1Parser.CastExpressionContext ctx) {
		return visitChildren(ctx);
	}

	@Override public Void visitPlusExpression(@NotNull J1Parser.PlusExpressionContext ctx) {
		return visitChildren(ctx);
	}

	@Override public Void visitNotExpression(@NotNull J1Parser.NotExpressionContext ctx) {
		return visitChildren(ctx);
	}

	@Override public Void visitMulitplyExpression(@NotNull J1Parser.MulitplyExpressionContext ctx) {
		return visitChildren(ctx);
	}

	@Override public Void visitAddExpression(@NotNull J1Parser.AddExpressionContext ctx) {
		return visitChildren(ctx);
	}

	@Override public Void visitShiftExpression(@NotNull J1Parser.ShiftExpressionContext ctx) {
		return visitChildren(ctx);
	}

	@Override public Void visitCompareExpression(@NotNull J1Parser.CompareExpressionContext ctx) {
		return visitChildren(ctx);
	}

	@Override public Void visitInstanceExpression(@NotNull J1Parser.InstanceExpressionContext ctx) {
		return visitChildren(ctx);
	}

	@Override public Void visitEqualsExpression(@NotNull J1Parser.EqualsExpressionContext ctx) {
		return visitChildren(ctx);
	}

	@Override public Void visitBinAndExpression(@NotNull J1Parser.BinAndExpressionContext ctx) {
		return visitChildren(ctx);
	}

	@Override public Void visitBinExclExpression(@NotNull J1Parser.BinExclExpressionContext ctx) {
		return visitChildren(ctx);
	}

	@Override public Void visitBinOrExpression(@NotNull J1Parser.BinOrExpressionContext ctx) {
		return visitChildren(ctx);
	}

	@Override public Void visitConAndExpression(@NotNull J1Parser.ConAndExpressionContext ctx) {
		return visitChildren(ctx);
	}

	@Override public Void visitConOrExpression(@NotNull J1Parser.ConOrExpressionContext ctx) {
		return visitChildren(ctx);
	}

	@Override public Void visitAssignExpression(@NotNull J1Parser.AssignExpressionContext ctx) {
		return visitChildren(ctx);
	}

	@Override public Void visitParenPrimary(@NotNull J1Parser.ParenPrimaryContext ctx) {
		return visitChildren(ctx);
	}

	@Override public Void visitThisPrimary(@NotNull J1Parser.ThisPrimaryContext ctx) {
		return visitChildren(ctx);
	}

	@Override public Void visitSuperPrimary(@NotNull J1Parser.SuperPrimaryContext ctx) {
		return visitChildren(ctx);
	}

	@Override public Void visitLiteralPrimary(@NotNull J1Parser.LiteralPrimaryContext ctx) {
		return visitChildren(ctx);
	}

	@Override public Void visitIdentPrimary(@NotNull J1Parser.IdentPrimaryContext ctx) {
		return visitChildren(ctx);
	}

	@Override public Void visitCreator(@NotNull J1Parser.CreatorContext ctx) {
		return visitChildren(ctx);
	}

	@Override public Void visitCreatedName(@NotNull J1Parser.CreatedNameContext ctx) {
		return visitChildren(ctx);
	}

	@Override public Void visitArrayCreatorRest(@NotNull J1Parser.ArrayCreatorRestContext ctx) {
		return visitChildren(ctx);
	}

	@Override public Void visitClassCreatorRest(@NotNull J1Parser.ClassCreatorRestContext ctx) {
		return visitChildren(ctx);
	}

	@Override public Void visitArguments(@NotNull J1Parser.ArgumentsContext ctx) {
		return visitChildren(ctx);
	}

	@Override public Void visitIntLiteral(@NotNull J1Parser.IntLiteralContext ctx) {
		return visitChildren(ctx);
	}

	@Override public Void visitFloatLiteral(@NotNull J1Parser.FloatLiteralContext ctx) {
		return visitChildren(ctx);
	}

	@Override public Void visitCharLiteral(@NotNull J1Parser.CharLiteralContext ctx) {
		return visitChildren(ctx);
	}

	@Override public Void visitStrLiteral(@NotNull J1Parser.StrLiteralContext ctx) {
		return visitChildren(ctx);
	}

	@Override public Void visitBoolLiteral(@NotNull J1Parser.BoolLiteralContext ctx) {
		return visitChildren(ctx);
	}

	@Override public Void visitNullLiteral(@NotNull J1Parser.NullLiteralContext ctx) {
		return visitChildren(ctx);
	}
}
