package ca.nevdull.mjc.compiler;

import java.util.List;

import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.TerminalNode;

public class ModelVisitor {
	PassData passData;
	boolean traceVisit = false;
	int nest = 0;
	String indent = "    ";
	
	public ModelVisitor(PassData passData) {
    	super();
    	this.passData = passData;
    	traceVisit = passData.options.trace.contains("ModelVisitor");
	}
	void traceIn(String m) {
		for (int i = 0;  i < nest;  i += 1) System.out.print(indent);
		System.out.print('/');
		System.out.println(m);
		nest += 1;
	}
	void traceOut(String m) {
		nest -= 1;
		for (int i = 0;  i < nest;  i += 1) System.out.print(indent);
		System.out.print('\\');
		System.out.println(m);
	}
	void traceDisc(String m) {
		for (int i = 0;  i < nest;  i += 1) System.out.print(indent);
		System.out.println(m);
	}
	void fail(String m) {
		Compiler.error(m);
	}
	private void put(TerminalNode id) {
		put(id.getSymbol().getText());
		put(" ");
	}
	private void put(String text) {
		System.out.print(text);
	}
    public Void visitCompilationUnit(MJParser.CompilationUnitContext ctx) {
        if (traceVisit) traceIn("visitCompilationUnit");
        for (MJParser.ImportDeclarationContext i : ctx.importDeclaration()) visitImportDeclaration(i);
        for (MJParser.TypeDeclarationContext t : ctx.typeDeclaration()) visitTypeDeclaration(t);
        if (traceVisit) traceOut("visitCompilationUnit");
        return null;
    }
    public Void visitImportDeclaration(MJParser.ImportDeclarationContext ctx) {
        if (traceVisit) traceIn("visitImportDeclaration");
        put(ctx.IMPORT());
        MJParser.QualifiedNameContext q = ctx.qualifiedName();  if (q != null) visitQualifiedName(q);
        if (ctx.getChildCount() > 3) {
        	put(".*");
        }
        put(";\n");
        if (traceVisit) traceOut("visitImportDeclaration");
        return null;
    }
    public Void visitTypeDeclaration(MJParser.TypeDeclarationContext ctx) {
        if (traceVisit) traceIn("visitTypeDeclaration");
        MJParser.ClassDeclarationContext c1 = ctx.classDeclaration();  if (c1 != null) {
            for (MJParser.ClassOrInterfaceModifierContext c : ctx.classOrInterfaceModifier()) visitClassOrInterfaceModifier(c);
        	visitClassDeclaration(c1);
        } else {
        	put(";\n");
        }
        if (traceVisit) traceOut("visitTypeDeclaration");
        return null;
    }
    public Void visitModifier(MJParser.ModifierContext ctx) {
        if (traceVisit) traceIn("visitModifier");
        MJParser.ClassOrInterfaceModifierContext c = ctx.classOrInterfaceModifier();  if (c != null) visitClassOrInterfaceModifier(c);
        if (traceVisit) traceOut("visitModifier");
        return null;
    }
    public Void visitClassOrInterfaceModifier(MJParser.ClassOrInterfaceModifierContext ctx) {
        if (traceVisit) traceIn("visitClassOrInterfaceModifier");
        TerminalNode m;
		if ((m = ctx.PUBLIC()) != null) put(m);
		else if ((m = ctx.PROTECTED()) != null) put(m);
		else if ((m = ctx.PRIVATE()) != null) put(m);
		else if ((m = ctx.STATIC()) != null) put(m);
		else if ((m = ctx.ABSTRACT()) != null) put(m);
		else if ((m = ctx.FINAL()) != null) put(m);
        if (traceVisit) traceOut("visitClassOrInterfaceModifier");
        return null;
    }
    public Void visitClassDeclaration(MJParser.ClassDeclarationContext ctx) {
        if (traceVisit) traceIn("visitClassDeclaration");
        put(ctx.CLASS());
        put(ctx.Identifier());
        MJParser.TypeContext t = ctx.type();  if (t != null) {
        	put(ctx.EXTENDS());
        	visitType(t);
        }
        MJParser.ClassBodyContext c = ctx.classBody();  if (c != null) visitClassBody(c);
        if (traceVisit) traceOut("visitClassDeclaration");
        return null;
    }
    public Void visitClassBody(MJParser.ClassBodyContext ctx) {
        if (traceVisit) traceIn("visitClassBody");
        put("{\n");
        for (MJParser.ClassBodyDeclarationContext c : ctx.classBodyDeclaration()) visitClassBodyDeclaration(c);
        put("\n}\n");
        if (traceVisit) traceOut("visitClassBody");
        return null;
    }
    public Void visitEmptyClassBodyDeclaration(MJParser.EmptyClassBodyDeclarationContext ctx) {
        if (traceVisit) traceIn("visitEmptyClassBodyDeclaration");
        put(";\n");
        if (traceVisit) traceOut("visitEmptyClassBodyDeclaration");
        return null;
    }
    public Void visitBlockClassBodyDeclaration(MJParser.BlockClassBodyDeclarationContext ctx) {
        if (traceVisit) traceIn("visitBlockClassBodyDeclaration");
        TerminalNode s;  if ((s = ctx.STATIC()) != null) put(s);
        MJParser.BlockContext b = ctx.block();  if (b != null) visitBlock(b);
        if (traceVisit) traceOut("visitBlockClassBodyDeclaration");
        return null;
    }
    public Void visitMemberClassBodyDeclaration(MJParser.MemberClassBodyDeclarationContext ctx) {
        if (traceVisit) traceIn("visitMemberClassBodyDeclaration");
        for (MJParser.ModifierContext m : ctx.modifier()) visitModifier(m);
        MJParser.MemberDeclarationContext md = ctx.memberDeclaration();  if (md != null) visitMemberDeclaration(md);
        if (traceVisit) traceOut("visitMemberClassBodyDeclaration");
        return null;
    }
    public Void visitClassBodyDeclaration(MJParser.ClassBodyDeclarationContext ctx) {
        if (traceVisit) traceDisc("visitClassBodyDeclaration");
        if (ctx instanceof MJParser.EmptyClassBodyDeclarationContext) visitEmptyClassBodyDeclaration((MJParser.EmptyClassBodyDeclarationContext) ctx);
        else if (ctx instanceof MJParser.BlockClassBodyDeclarationContext) visitBlockClassBodyDeclaration((MJParser.BlockClassBodyDeclarationContext) ctx);
        else if (ctx instanceof MJParser.MemberClassBodyDeclarationContext) visitMemberClassBodyDeclaration((MJParser.MemberClassBodyDeclarationContext) ctx);
        else fail("visitClassBodyDeclaration unrecognized "+ctx.getClass().getSimpleName());
        return null;
    }
    public Void visitMemberDeclaration(MJParser.MemberDeclarationContext ctx) {
        if (traceVisit) traceIn("visitMemberDeclaration");
        MJParser.MethodDeclarationContext m = ctx.methodDeclaration();  if (m != null) visitMethodDeclaration(m);
        MJParser.FieldDeclarationContext f = ctx.fieldDeclaration();  if (f != null) visitFieldDeclaration(f);
        MJParser.ConstructorDeclarationContext co = ctx.constructorDeclaration();  if (co != null) visitConstructorDeclaration(co);
        MJParser.ClassDeclarationContext cl = ctx.classDeclaration();  if (cl != null) visitClassDeclaration(cl);
        if (traceVisit) traceOut("visitMemberDeclaration");
        return null;
    }
    public Void visitMethodDeclaration(MJParser.MethodDeclarationContext ctx) {
        if (traceVisit) traceIn("visitMethodDeclaration");
        MJParser.TypeContext t; 
		if ((t = ctx.type()) != null) visitType(t);
		else put(ctx.VOID());
        put(ctx.Identifier());
        MJParser.FormalParametersContext f = ctx.formalParameters();  if (f != null) visitFormalParameters(f);
        MJParser.ArrayDimensionContext a = ctx.arrayDimension();  if (a != null) visitArrayDimension(a);
        MJParser.MethodBodyContext m = ctx.methodBody();  
        if (m != null) visitMethodBody(m);
        else if (ctx.NATIVE() != null) {
            put(ctx.NATIVE());
            put(";\n");
        } else put(";\n");
        if (traceVisit) traceOut("visitMethodDeclaration");
        return null;
    }
    public Void visitConstructorDeclaration(MJParser.ConstructorDeclarationContext ctx) {
        if (traceVisit) traceIn("visitConstructorDeclaration");
        put(ctx.Identifier());
        MJParser.FormalParametersContext f = ctx.formalParameters();  if (f != null) visitFormalParameters(f);
        MJParser.ConstructorBodyContext b = ctx.constructorBody();  if (b != null) visitConstructorBody(b);
        if (traceVisit) traceOut("visitConstructorDeclaration");
        return null;
    }
    public Void visitFieldDeclaration(MJParser.FieldDeclarationContext ctx) {
        if (traceVisit) traceIn("visitFieldDeclaration");
        MJParser.TypeContext t = ctx.type();  if (t != null) visitType(t);
        MJParser.VariableDeclaratorsContext v = ctx.variableDeclarators();  if (v != null) visitVariableDeclarators(v);
        put(";\n");
        if (traceVisit) traceOut("visitFieldDeclaration");
        return null;
    }
    public Void visitVariableDeclarators(MJParser.VariableDeclaratorsContext ctx) {
        if (traceVisit) traceIn("visitVariableDeclarators");
        String sep = null;
        for (MJParser.VariableDeclaratorContext v : ctx.variableDeclarator()) {
        	if (sep != null) put(sep);
        	visitVariableDeclarator(v);
        	sep = ",";
        }
        if (traceVisit) traceOut("visitVariableDeclarators");
        return null;
    }
    public Void visitVariableDeclarator(MJParser.VariableDeclaratorContext ctx) {
        if (traceVisit) traceIn("visitVariableDeclarator");
        MJParser.VariableDeclaratorIdContext v = ctx.variableDeclaratorId();  if (v != null) visitVariableDeclaratorId(v);
        MJParser.VariableInitializerContext v1 = ctx.variableInitializer();  if (v1 != null) {
        	put("=");
        	visitVariableInitializer(v1);
        }
        if (traceVisit) traceOut("visitVariableDeclarator");
        return null;
    }
    public Void visitVariableDeclaratorId(MJParser.VariableDeclaratorIdContext ctx) {
        if (traceVisit) traceIn("visitVariableDeclaratorId");
        put(ctx.Identifier());
        MJParser.ArrayDimensionContext a = ctx.arrayDimension();  if (a != null) visitArrayDimension(a);
        if (traceVisit) traceOut("visitVariableDeclaratorId");
        return null;
    }
    public Void visitArrayVariableInitializer(MJParser.ArrayVariableInitializerContext ctx) {
        if (traceVisit) traceIn("visitArrayVariableInitializer");
        MJParser.ArrayInitializerContext a = ctx.arrayInitializer();  if (a != null) visitArrayInitializer(a);
        if (traceVisit) traceOut("visitArrayVariableInitializer");
        return null;
    }
    public Void visitSimpleVariableInitializer(MJParser.SimpleVariableInitializerContext ctx) {
        if (traceVisit) traceIn("visitSimpleVariableInitializer");
        MJParser.ExpressionContext e = ctx.expression();  if (e != null) visitExpression(e);
        if (traceVisit) traceOut("visitSimpleVariableInitializer");
        return null;
    }
    public Void visitVariableInitializer(MJParser.VariableInitializerContext ctx) {
        if (traceVisit) traceDisc("visitVariableInitializer");
        if (ctx instanceof MJParser.ArrayVariableInitializerContext) visitArrayVariableInitializer((MJParser.ArrayVariableInitializerContext) ctx);
        else if (ctx instanceof MJParser.SimpleVariableInitializerContext) visitSimpleVariableInitializer((MJParser.SimpleVariableInitializerContext) ctx);
        else fail("visitVariableInitializer unrecognized "+ctx.getClass().getSimpleName());
        return null;
    }
    public Void visitArrayInitializer(MJParser.ArrayInitializerContext ctx) {
        if (traceVisit) traceIn("visitArrayInitializer");
        put("{");
        String sep = null;
        for (MJParser.VariableInitializerContext v : ctx.variableInitializer()) { 
        	if (sep != null) put(sep);
        	visitVariableInitializer(v);
        	sep = ",";
        }
        put("}");
        if (traceVisit) traceOut("visitArrayInitializer");
        return null;
    }
    public Void visitObjectType(MJParser.ObjectTypeContext ctx) {
        if (traceVisit) traceIn("visitObjectType");
        MJParser.ClassOrInterfaceTypeContext c = ctx.classOrInterfaceType();  if (c != null) visitClassOrInterfaceType(c);
        MJParser.ArrayDimensionContext a = ctx.arrayDimension();  if (a != null) visitArrayDimension(a);
        if (traceVisit) traceOut("visitObjectType");
        return null;
    }
    public Void visitPrimitType(MJParser.PrimitTypeContext ctx) {
        if (traceVisit) traceIn("visitPrimitType");
        MJParser.PrimitiveTypeContext p = ctx.primitiveType();  if (p != null) visitPrimitiveType(p);
        MJParser.ArrayDimensionContext a = ctx.arrayDimension();  if (a != null) visitArrayDimension(a);
        if (traceVisit) traceOut("visitPrimitType");
        return null;
    }
    public Void visitType(MJParser.TypeContext ctx) {
        if (traceVisit) traceDisc("visitType");
        if (ctx instanceof MJParser.ObjectTypeContext) visitObjectType((MJParser.ObjectTypeContext) ctx);
        else if (ctx instanceof MJParser.PrimitTypeContext) visitPrimitType((MJParser.PrimitTypeContext) ctx);
        else fail("visitType unrecognized "+ctx.getClass().getSimpleName());
        return null;
    }
    public Void visitArrayDimension(MJParser.ArrayDimensionContext ctx) {
        if (traceVisit) traceIn("visitArrayDimension");
        int dim = ctx.getChildCount() / 2;
        for (int i = 0;  i < dim;  i += 1) {
        	put("[]");
        }
        if (traceVisit) traceOut("visitArrayDimension");
        return null;
    }
    public Void visitClassOrInterfaceType(MJParser.ClassOrInterfaceTypeContext ctx) {
        if (traceVisit) traceIn("visitClassOrInterfaceType");
        String sep = null;
		for (TerminalNode i : ctx.Identifier()) {
        	if (sep != null) put(sep);
        	put(i);
        	sep = ".";
        }
        if (traceVisit) traceOut("visitClassOrInterfaceType");
        return null;
    }
    public Void visitBooleanType(MJParser.BooleanTypeContext ctx) {
        if (traceVisit) traceIn("visitBooleanType");
        put(ctx.BOOLEAN());
        if (traceVisit) traceOut("visitBooleanType");
        return null;
    }
    public Void visitCharType(MJParser.CharTypeContext ctx) {
        if (traceVisit) traceIn("visitCharType");
        put(ctx.CHAR());
        if (traceVisit) traceOut("visitCharType");
        return null;
    }
    public Void visitByteType(MJParser.ByteTypeContext ctx) {
        if (traceVisit) traceIn("visitByteType");
        put(ctx.BYTE());
        if (traceVisit) traceOut("visitByteType");
        return null;
    }
    public Void visitShortType(MJParser.ShortTypeContext ctx) {
        if (traceVisit) traceIn("visitShortType");
        put(ctx.SHORT());
        if (traceVisit) traceOut("visitShortType");
        return null;
    }
    public Void visitIntType(MJParser.IntTypeContext ctx) {
        if (traceVisit) traceIn("visitIntType");
        put(ctx.INT());
        if (traceVisit) traceOut("visitIntType");
        return null;
    }
    public Void visitLongType(MJParser.LongTypeContext ctx) {
        if (traceVisit) traceIn("visitLongType");
        put(ctx.LONG());
        if (traceVisit) traceOut("visitLongType");
        return null;
    }
    public Void visitFloatType(MJParser.FloatTypeContext ctx) {
        if (traceVisit) traceIn("visitFloatType");
        put(ctx.FLOAT());
        if (traceVisit) traceOut("visitFloatType");
        return null;
    }
    public Void visitDoubleType(MJParser.DoubleTypeContext ctx) {
        if (traceVisit) traceIn("visitDoubleType");
        put(ctx.DOUBLE());
        if (traceVisit) traceOut("visitDoubleType");
        return null;
    }
    public Void visitPrimitiveType(MJParser.PrimitiveTypeContext ctx) {
        if (traceVisit) traceDisc("visitPrimitiveType");
        if (ctx instanceof MJParser.BooleanTypeContext) visitBooleanType((MJParser.BooleanTypeContext) ctx);
        else if (ctx instanceof MJParser.CharTypeContext) visitCharType((MJParser.CharTypeContext) ctx);
        else if (ctx instanceof MJParser.ByteTypeContext) visitByteType((MJParser.ByteTypeContext) ctx);
        else if (ctx instanceof MJParser.ShortTypeContext) visitShortType((MJParser.ShortTypeContext) ctx);
        else if (ctx instanceof MJParser.IntTypeContext) visitIntType((MJParser.IntTypeContext) ctx);
        else if (ctx instanceof MJParser.LongTypeContext) visitLongType((MJParser.LongTypeContext) ctx);
        else if (ctx instanceof MJParser.FloatTypeContext) visitFloatType((MJParser.FloatTypeContext) ctx);
        else if (ctx instanceof MJParser.DoubleTypeContext) visitDoubleType((MJParser.DoubleTypeContext) ctx);
        else fail("visitPrimitiveType unrecognized "+ctx.getClass().getSimpleName());
        return null;
    }
    public Void visitFormalParameters(MJParser.FormalParametersContext ctx) {
        if (traceVisit) traceIn("visitFormalParameters");
        put("(");
        MJParser.FormalParameterListContext f = ctx.formalParameterList();  if (f != null) visitFormalParameterList(f);
        put(")");
        if (traceVisit) traceOut("visitFormalParameters");
        return null;
    }
    public Void visitFormalParameterList(MJParser.FormalParameterListContext ctx) {
        if (traceVisit) traceIn("visitFormalParameterList");
        String sep = null;
        for (MJParser.FormalParameterContext f : ctx.formalParameter()) {
        	if (sep != null) put(sep);
        	visitFormalParameter(f);
        	sep = ",";
        }
        if (traceVisit) traceOut("visitFormalParameterList");
        return null;
    }
    public Void visitFormalParameter(MJParser.FormalParameterContext ctx) {
        if (traceVisit) traceIn("visitFormalParameter");
        for (MJParser.VariableModifierContext v : ctx.variableModifier()) visitVariableModifier(v);
        MJParser.TypeContext t = ctx.type();  if (t != null) visitType(t);
        MJParser.VariableDeclaratorIdContext v1 = ctx.variableDeclaratorId();  if (v1 != null) visitVariableDeclaratorId(v1);
        if (traceVisit) traceOut("visitFormalParameter");
        return null;
    }
    public Void visitVariableModifier(MJParser.VariableModifierContext ctx) {
        if (traceVisit) traceIn("visitVariableModifier");
        put(ctx.FINAL());
        if (traceVisit) traceOut("visitVariableModifier");
        return null;
    }
    public Void visitMethodBody(MJParser.MethodBodyContext ctx) {
        if (traceVisit) traceIn("visitMethodBody");
        MJParser.BlockContext b = ctx.block();  if (b != null) visitBlock(b);
        if (traceVisit) traceOut("visitMethodBody");
        return null;
    }
    public Void visitConstructorBody(MJParser.ConstructorBodyContext ctx) {
        if (traceVisit) traceIn("visitConstructorBody");
        MJParser.BlockContext b = ctx.block();  if (b != null) visitBlock(b);
        if (traceVisit) traceOut("visitConstructorBody");
        return null;
    }
    public Void visitQualifiedName(MJParser.QualifiedNameContext ctx) {
        if (traceVisit) traceIn("visitQualifiedName");
        String sep = null;
        for (TerminalNode i : ctx.Identifier()) {
        	if (sep != null) put(sep);
        	put(i);
        	sep = ".";
        }
        if (traceVisit) traceOut("visitQualifiedName");
        return null;
    }
    public Void visitLiteral(MJParser.LiteralContext ctx) {
        if (traceVisit) traceIn("visitLiteral");
        TerminalNode l;
        if ((l = ctx.IntegerLiteral()) != null) put(l);
        else if ((l = ctx.FloatingPointLiteral()) != null) put(l);
        else if ((l = ctx.CharacterLiteral()) != null) put(l);
        else if ((l = ctx.StringLiteral()) != null) put(l);
        else if ((l = ctx.BooleanLiteral()) != null) put(l);
        else put("null");
        if (traceVisit) traceOut("visitLiteral");
        return null;
    }
    public Void visitBlock(MJParser.BlockContext ctx) {
        if (traceVisit) traceIn("visitBlock");
        put("{\n");
        for (MJParser.BlockStatementContext b : ctx.blockStatement()) visitBlockStatement(b);
        put("}\n");
        if (traceVisit) traceOut("visitBlock");
        return null;
    }
    public Void visitBlockStatement(MJParser.BlockStatementContext ctx) {
        if (traceVisit) traceIn("visitBlockStatement");
        MJParser.LocalVariableDeclarationStatementContext l = ctx.localVariableDeclarationStatement();  if (l != null) visitLocalVariableDeclarationStatement(l);
        MJParser.StatementContext s = ctx.statement();  if (s != null) visitStatement(s);
        if (traceVisit) traceOut("visitBlockStatement");
        return null;
    }
    public Void visitLocalVariableDeclarationStatement(MJParser.LocalVariableDeclarationStatementContext ctx) {
        if (traceVisit) traceIn("visitLocalVariableDeclarationStatement");
        MJParser.LocalVariableDeclarationContext l = ctx.localVariableDeclaration();  if (l != null) visitLocalVariableDeclaration(l);
        put(";\n");
        if (traceVisit) traceOut("visitLocalVariableDeclarationStatement");
        return null;
    }
    public Void visitLocalVariableDeclaration(MJParser.LocalVariableDeclarationContext ctx) {
        if (traceVisit) traceIn("visitLocalVariableDeclaration");
        for (MJParser.VariableModifierContext v : ctx.variableModifier()) visitVariableModifier(v);
        MJParser.TypeContext t = ctx.type();  if (t != null) visitType(t);
        MJParser.VariableDeclaratorsContext v1 = ctx.variableDeclarators();  if (v1 != null) visitVariableDeclarators(v1);
        if (traceVisit) traceOut("visitLocalVariableDeclaration");
        return null;
    }
    public Void visitBlkStatement(MJParser.BlkStatementContext ctx) {
        if (traceVisit) traceIn("visitBlkStatement");
        MJParser.BlockContext b = ctx.block();  if (b != null) visitBlock(b);
        if (traceVisit) traceOut("visitBlkStatement");
        return null;
    }
    public Void visitIfStatement(MJParser.IfStatementContext ctx) {
        if (traceVisit) traceIn("visitIfStatement");
        put(ctx.IF());
        MJParser.ParExpressionContext p = ctx.parExpression();  if (p != null) visitParExpression(p);
        List<MJParser.StatementContext> s = ctx.statement();
        visitStatement(s.get(0));
        TerminalNode e;
		if ((e = ctx.ELSE()) != null) {
        	put(e);
            visitStatement(s.get(0));        	
        }
        if (traceVisit) traceOut("visitIfStatement");
        return null;
    }
    public Void visitWhileStatement(MJParser.WhileStatementContext ctx) {
        if (traceVisit) traceIn("visitWhileStatement");
        put(ctx.WHILE());
        MJParser.ParExpressionContext p = ctx.parExpression();  if (p != null) visitParExpression(p);
        MJParser.StatementContext s = ctx.statement();  if (s != null) visitStatement(s);
        if (traceVisit) traceOut("visitWhileStatement");
        return null;
    }
    public Void visitReturnStatement(MJParser.ReturnStatementContext ctx) {
        if (traceVisit) traceIn("visitReturnStatement");
        put(ctx.RETURN());
        MJParser.ExpressionContext e = ctx.expression();  if (e != null) visitExpression(e);
        put(";\n");
        if (traceVisit) traceOut("visitReturnStatement");
        return null;
    }
    public Void visitEmnptyStatement(MJParser.EmnptyStatementContext ctx) {
        if (traceVisit) traceIn("visitEmnptyStatement");
        put(";\n");
        if (traceVisit) traceOut("visitEmnptyStatement");
        return null;
    }
    public Void visitExpressionStatement(MJParser.ExpressionStatementContext ctx) {
        if (traceVisit) traceIn("visitExpressionStatement");
        MJParser.StatementExpressionContext s = ctx.statementExpression();  if (s != null) visitStatementExpression(s);
        put(";\n");
        if (traceVisit) traceOut("visitExpressionStatement");
        return null;
    }
    public Void visitLabelStatement(MJParser.LabelStatementContext ctx) {
        if (traceVisit) traceIn("visitLabelStatement");
        put(ctx.Identifier());
        put(":");
        MJParser.StatementContext s = ctx.statement();  if (s != null) visitStatement(s);
        if (traceVisit) traceOut("visitLabelStatement");
        return null;
    }
    public Void visitStatement(MJParser.StatementContext ctx) {
        if (traceVisit) traceDisc("visitStatement");
        if (ctx instanceof MJParser.BlkStatementContext) visitBlkStatement((MJParser.BlkStatementContext) ctx);
        else if (ctx instanceof MJParser.IfStatementContext) visitIfStatement((MJParser.IfStatementContext) ctx);
        else if (ctx instanceof MJParser.WhileStatementContext) visitWhileStatement((MJParser.WhileStatementContext) ctx);
        else if (ctx instanceof MJParser.ReturnStatementContext) visitReturnStatement((MJParser.ReturnStatementContext) ctx);
        else if (ctx instanceof MJParser.EmnptyStatementContext) visitEmnptyStatement((MJParser.EmnptyStatementContext) ctx);
        else if (ctx instanceof MJParser.ExpressionStatementContext) visitExpressionStatement((MJParser.ExpressionStatementContext) ctx);
        else if (ctx instanceof MJParser.LabelStatementContext) visitLabelStatement((MJParser.LabelStatementContext) ctx);
        else fail("visitStatement unrecognized "+ctx.getClass().getSimpleName());
        return null;
    }
    public Void visitParExpression(MJParser.ParExpressionContext ctx) {
        if (traceVisit) traceIn("visitParExpression");
        put("(");
        MJParser.ExpressionContext e = ctx.expression();  if (e != null) visitExpression(e);
        put(")");
        if (traceVisit) traceOut("visitParExpression");
        return null;
    }
    public Void visitExpressionList(MJParser.ExpressionListContext ctx) {
        if (traceVisit) traceIn("visitExpressionList");
        String sep = null;
        for (MJParser.ExpressionContext e : ctx.expression()) {
        	if (sep != null) put(sep);
        	visitExpression(e);
        	sep = ",";
        }
        if (traceVisit) traceOut("visitExpressionList");
        return null;
    }
    public Void visitStatementExpression(MJParser.StatementExpressionContext ctx) {
        if (traceVisit) traceIn("visitStatementExpression");
        MJParser.ExpressionContext e = ctx.expression();  if (e != null) visitExpression(e);
        if (traceVisit) traceOut("visitStatementExpression");
        return null;
    }
    public Void visitConstantExpression(MJParser.ConstantExpressionContext ctx) {
        if (traceVisit) traceIn("visitConstantExpression");
        MJParser.ExpressionContext e = ctx.expression();  if (e != null) visitExpression(e);
        if (traceVisit) traceOut("visitConstantExpression");
        return null;
    }
    public Void visitPrimExpression(MJParser.PrimExpressionContext ctx) {
        if (traceVisit) traceIn("visitPrimExpression");
        MJParser.PrimaryContext p = ctx.primary();  if (p != null) visitPrimary(p);
        if (traceVisit) traceOut("visitPrimExpression");
        return null;
    }
    public Void visitDotExpression(MJParser.DotExpressionContext ctx) {
        if (traceVisit) traceIn("visitDotExpression");
        MJParser.ExpressionContext e = ctx.expression();  if (e != null) visitExpression(e);
        put(".");
        put(ctx.Identifier());
        if (traceVisit) traceOut("visitDotExpression");
        return null;
    }
    public Void visitIndexExpression(MJParser.IndexExpressionContext ctx) {
        if (traceVisit) traceIn("visitIndexExpression");
        MJParser.ExpressionContext e = ctx.expression(0);  if (e != null) visitExpression(e);
        put("[");
        MJParser.ExpressionContext e1 = ctx.expression(1);  if (e1 != null) visitExpression(e1);
        put("]");
        if (traceVisit) traceOut("visitIndexExpression");
        return null;
    }
    public Void visitCallExpression(MJParser.CallExpressionContext ctx) {
        if (traceVisit) traceIn("visitCallExpression");
        MJParser.ExpressionContext e = ctx.expression();  if (e != null) visitExpression(e);
        put("(");
        MJParser.ExpressionListContext e1 = ctx.expressionList();  if (e1 != null) visitExpressionList(e1);
        put(")");
        if (traceVisit) traceOut("visitCallExpression");
        return null;
    }
    public Void visitNewExpression(MJParser.NewExpressionContext ctx) {
        if (traceVisit) traceIn("visitNewExpression");
        put(ctx.NEW());
        MJParser.CreatorContext c = ctx.creator();  if (c != null) visitCreator(c);
        if (traceVisit) traceOut("visitNewExpression");
        return null;
    }
    public Void visitCastExpression(MJParser.CastExpressionContext ctx) {
        if (traceVisit) traceIn("visitCastExpression");
        put("(");
        MJParser.TypeContext t = ctx.type();  if (t != null) visitType(t);
        put(")");
        MJParser.ExpressionContext e = ctx.expression();  if (e != null) visitExpression(e);
        if (traceVisit) traceOut("visitCastExpression");
        return null;
    }
    public Void visitPlusExpression(MJParser.PlusExpressionContext ctx) {
        if (traceVisit) traceIn("visitPlusExpression");
        put((TerminalNode) ctx.getChild(0));
        MJParser.ExpressionContext e = ctx.expression();  if (e != null) visitExpression(e);
        if (traceVisit) traceOut("visitPlusExpression");
        return null;
    }
    public Void visitNotExpression(MJParser.NotExpressionContext ctx) {
        if (traceVisit) traceIn("visitNotExpression");
        put((TerminalNode) ctx.getChild(0));
        MJParser.ExpressionContext e = ctx.expression();  if (e != null) visitExpression(e);
        if (traceVisit) traceOut("visitNotExpression");
        return null;
    }
    public Void visitMultExpression(MJParser.MultExpressionContext ctx) {
        if (traceVisit) traceIn("visitMultExpression");
        MJParser.ExpressionContext e = ctx.expression(0);  if (e != null) visitExpression(e);
        put((TerminalNode) ctx.getChild(1));
        MJParser.ExpressionContext e1 = ctx.expression(1);  if (e1 != null) visitExpression(e1);
        if (traceVisit) traceOut("visitMultExpression");
        return null;
    }
    public Void visitAddExpression(MJParser.AddExpressionContext ctx) {
        if (traceVisit) traceIn("visitAddExpression");
        MJParser.ExpressionContext e = ctx.expression(0);  if (e != null) visitExpression(e);
        put((TerminalNode) ctx.getChild(1));
        MJParser.ExpressionContext e1 = ctx.expression(1);  if (e1 != null) visitExpression(e1);
        if (traceVisit) traceOut("visitAddExpression");
        return null;
    }
    public Void visitShiftExpression(MJParser.ShiftExpressionContext ctx) {
        if (traceVisit) traceIn("visitShiftExpression");
        MJParser.ExpressionContext e = ctx.expression(0);  if (e != null) visitExpression(e);
        putOperator(ctx);
        MJParser.ExpressionContext e1 = ctx.expression(1);  if (e1 != null) visitExpression(e1);
        if (traceVisit) traceOut("visitShiftExpression");
        return null;
    }
	private void putOperator(ParserRuleContext ctx) {
		List<ParseTree> op = ctx.children;
		for (ParseTree o : op.subList(1, op.size()-1)) put(((TerminalNode)o).getText());
        //for (ParseTree c : ctx.children) if (c instanceof TerminalNode) put(((TerminalNode)c).getText());  //TODO
	}
    public Void visitCompareExpression(MJParser.CompareExpressionContext ctx) {
        if (traceVisit) traceIn("visitCompareExpression");
        MJParser.ExpressionContext e = ctx.expression(0);  if (e != null) visitExpression(e);
        putOperator(ctx);
        MJParser.ExpressionContext e1 = ctx.expression(1);  if (e1 != null) visitExpression(e1);
        if (traceVisit) traceOut("visitCompareExpression");
        return null;
    }
    public Void visitEqualExpression(MJParser.EqualExpressionContext ctx) {
        if (traceVisit) traceIn("visitEqualExpression");
        MJParser.ExpressionContext e = ctx.expression(0);  if (e != null) visitExpression(e);
        put((TerminalNode) ctx.getChild(1));
        MJParser.ExpressionContext e1 = ctx.expression(1);  if (e1 != null) visitExpression(e1);
        if (traceVisit) traceOut("visitEqualExpression");
        return null;
    }
    public Void visitAndExpression(MJParser.AndExpressionContext ctx) {
        if (traceVisit) traceIn("visitAndExpression");
        MJParser.ExpressionContext e = ctx.expression(0);  if (e != null) visitExpression(e);
        put("&");
        MJParser.ExpressionContext e1 = ctx.expression(1);  if (e1 != null) visitExpression(e1);
        if (traceVisit) traceOut("visitAndExpression");
        return null;
    }
    public Void visitExclExpression(MJParser.ExclExpressionContext ctx) {
        if (traceVisit) traceIn("visitExclExpression");
        MJParser.ExpressionContext e = ctx.expression(0);  if (e != null) visitExpression(e);
        put("^");
        MJParser.ExpressionContext e1 = ctx.expression(1);  if (e1 != null) visitExpression(e1);
        if (traceVisit) traceOut("visitExclExpression");
        return null;
    }
    public Void visitOrExpression(MJParser.OrExpressionContext ctx) {
        if (traceVisit) traceIn("visitOrExpression");
        MJParser.ExpressionContext e = ctx.expression(0);  if (e != null) visitExpression(e);
        put("|");
        MJParser.ExpressionContext e1 = ctx.expression(1);  if (e1 != null) visitExpression(e1);
        if (traceVisit) traceOut("visitOrExpression");
        return null;
    }
    public Void visitCondAndExpression(MJParser.CondAndExpressionContext ctx) {
        if (traceVisit) traceIn("visitCondAndExpression");
        MJParser.ExpressionContext e = ctx.expression(0);  if (e != null) visitExpression(e);
        put("&&");
        MJParser.ExpressionContext e1 = ctx.expression(1);  if (e1 != null) visitExpression(e1);
        if (traceVisit) traceOut("visitCondAndExpression");
        return null;
    }
    public Void visitCondOrExpression(MJParser.CondOrExpressionContext ctx) {
        if (traceVisit) traceIn("visitCondOrExpression");
        MJParser.ExpressionContext e = ctx.expression(0);  if (e != null) visitExpression(e);
        put("||");
        MJParser.ExpressionContext e1 = ctx.expression(1);  if (e1 != null) visitExpression(e1);
        if (traceVisit) traceOut("visitCondOrExpression");
        return null;
    }
    public Void visitAssignExpression(MJParser.AssignExpressionContext ctx) {
        if (traceVisit) traceIn("visitAssignExpression");
        //System.out.println(ctx.toStringTree(passData.parser));
        MJParser.ExpressionContext e = ctx.expression(0);  if (e != null) visitExpression(e);
        put((TerminalNode) ctx.getChild(1));
        MJParser.ExpressionContext e1 = ctx.expression(1);  if (e1 != null) visitExpression(e1);
        if (traceVisit) traceOut("visitAssignExpression");
        return null;
    }
    public Void visitExpression(MJParser.ExpressionContext ctx) {
        if (traceVisit) traceDisc("visitExpression");
        if (ctx instanceof MJParser.PrimExpressionContext) visitPrimExpression((MJParser.PrimExpressionContext) ctx);
        else if (ctx instanceof MJParser.DotExpressionContext) visitDotExpression((MJParser.DotExpressionContext) ctx);
        else if (ctx instanceof MJParser.IndexExpressionContext) visitIndexExpression((MJParser.IndexExpressionContext) ctx);
        else if (ctx instanceof MJParser.CallExpressionContext) visitCallExpression((MJParser.CallExpressionContext) ctx);
        else if (ctx instanceof MJParser.NewExpressionContext) visitNewExpression((MJParser.NewExpressionContext) ctx);
        else if (ctx instanceof MJParser.CastExpressionContext) visitCastExpression((MJParser.CastExpressionContext) ctx);
        else if (ctx instanceof MJParser.PlusExpressionContext) visitPlusExpression((MJParser.PlusExpressionContext) ctx);
        else if (ctx instanceof MJParser.NotExpressionContext) visitNotExpression((MJParser.NotExpressionContext) ctx);
        else if (ctx instanceof MJParser.MultExpressionContext) visitMultExpression((MJParser.MultExpressionContext) ctx);
        else if (ctx instanceof MJParser.AddExpressionContext) visitAddExpression((MJParser.AddExpressionContext) ctx);
        else if (ctx instanceof MJParser.ShiftExpressionContext) visitShiftExpression((MJParser.ShiftExpressionContext) ctx);
        else if (ctx instanceof MJParser.CompareExpressionContext) visitCompareExpression((MJParser.CompareExpressionContext) ctx);
        else if (ctx instanceof MJParser.EqualExpressionContext) visitEqualExpression((MJParser.EqualExpressionContext) ctx);
        else if (ctx instanceof MJParser.AndExpressionContext) visitAndExpression((MJParser.AndExpressionContext) ctx);
        else if (ctx instanceof MJParser.ExclExpressionContext) visitExclExpression((MJParser.ExclExpressionContext) ctx);
        else if (ctx instanceof MJParser.OrExpressionContext) visitOrExpression((MJParser.OrExpressionContext) ctx);
        else if (ctx instanceof MJParser.CondAndExpressionContext) visitCondAndExpression((MJParser.CondAndExpressionContext) ctx);
        else if (ctx instanceof MJParser.CondOrExpressionContext) visitCondOrExpression((MJParser.CondOrExpressionContext) ctx);
        else if (ctx instanceof MJParser.AssignExpressionContext) visitAssignExpression((MJParser.AssignExpressionContext) ctx);
        else fail("visitExpression unrecognized "+ctx.getClass().getSimpleName());
        return null;
    }
    public Void visitParenPrimary(MJParser.ParenPrimaryContext ctx) {
        if (traceVisit) traceIn("visitParenPrimary");
        put("(");
        MJParser.ExpressionContext e = ctx.expression();  if (e != null) visitExpression(e);
        put(")");
        if (traceVisit) traceOut("visitParenPrimary");
        return null;
    }
    public Void visitThisPrimary(MJParser.ThisPrimaryContext ctx) {
        if (traceVisit) traceIn("visitThisPrimary");
        put(ctx.THIS());
        if (traceVisit) traceOut("visitThisPrimary");
        return null;
    }
    public Void visitSuperPrimary(MJParser.SuperPrimaryContext ctx) {
        if (traceVisit) traceIn("visitSuperPrimary");
        put(ctx.SUPER());
        if (traceVisit) traceOut("visitSuperPrimary");
        return null;
    }
    public Void visitLiteralPrimary(MJParser.LiteralPrimaryContext ctx) {
        if (traceVisit) traceIn("visitLiteralPrimary");
        MJParser.LiteralContext l = ctx.literal();  if (l != null) visitLiteral(l);
        if (traceVisit) traceOut("visitLiteralPrimary");
        return null;
    }
    public Void visitIdentifierPrimary(MJParser.IdentifierPrimaryContext ctx) {
        if (traceVisit) traceIn("visitIdentifierPrimary");
        put(ctx.Identifier());
        if (traceVisit) traceOut("visitIdentifierPrimary");
        return null;
    }
    public Void visitPrimary(MJParser.PrimaryContext ctx) {
        if (traceVisit) traceDisc("visitPrimary");
        if (ctx instanceof MJParser.ParenPrimaryContext) visitParenPrimary((MJParser.ParenPrimaryContext) ctx);
        else if (ctx instanceof MJParser.ThisPrimaryContext) visitThisPrimary((MJParser.ThisPrimaryContext) ctx);
        else if (ctx instanceof MJParser.SuperPrimaryContext) visitSuperPrimary((MJParser.SuperPrimaryContext) ctx);
        else if (ctx instanceof MJParser.LiteralPrimaryContext) visitLiteralPrimary((MJParser.LiteralPrimaryContext) ctx);
        else if (ctx instanceof MJParser.IdentifierPrimaryContext) visitIdentifierPrimary((MJParser.IdentifierPrimaryContext) ctx);
        else fail("visitPrimary unrecognized "+ctx.getClass().getSimpleName());
        return null;
    }
    public Void visitArrayCreator(MJParser.ArrayCreatorContext ctx) {
        if (traceVisit) traceIn("visitArrayCreator");
        MJParser.CreatedNameContext c = ctx.createdName();  if (c != null) visitCreatedName(c);
        MJParser.ArrayCreatorRestContext a = ctx.arrayCreatorRest();  if (a != null) visitArrayCreatorRest(a);
        if (traceVisit) traceOut("visitArrayCreator");
        return null;
    }
    public Void visitClassCreator(MJParser.ClassCreatorContext ctx) {
        if (traceVisit) traceIn("visitClassCreator");
        MJParser.CreatedNameContext c = ctx.createdName();  if (c != null) visitCreatedName(c);
        MJParser.ClassCreatorRestContext c1 = ctx.classCreatorRest();  if (c1 != null) visitClassCreatorRest(c1);
        if (traceVisit) traceOut("visitClassCreator");
        return null;
    }
    public Void visitCreator(MJParser.CreatorContext ctx) {
        if (traceVisit) traceDisc("visitCreator");
        if (ctx instanceof MJParser.ArrayCreatorContext) visitArrayCreator((MJParser.ArrayCreatorContext) ctx);
        else if (ctx instanceof MJParser.ClassCreatorContext) visitClassCreator((MJParser.ClassCreatorContext) ctx);
        else fail("visitCreator unrecognized "+ctx.getClass().getSimpleName());
        return null;
    }
    public Void visitCreatedName(MJParser.CreatedNameContext ctx) {
        if (traceVisit) traceIn("visitCreatedName");
        MJParser.PrimitiveTypeContext p = ctx.primitiveType();
        if (p != null) visitPrimitiveType(p);
        else {
            String sep = null;
            for (TerminalNode i : ctx.Identifier()) {
            	if (sep != null) put(sep);
            	put(i);
            	sep = ".";
            }
        }
        if (traceVisit) traceOut("visitCreatedName");
        return null;
    }
    public Void visitArrayCreatorRest(MJParser.ArrayCreatorRestContext ctx) {
        if (traceVisit) traceIn("visitArrayCreatorRest");
        List<MJParser.ExpressionContext> el = ctx.expression();
        if (el.isEmpty()) {
            MJParser.ArrayDimensionContext ad = ctx.arrayDimension();  if (ad != null) visitArrayDimension(ad);
            MJParser.ArrayInitializerContext ai = ctx.arrayInitializer();  if (ai != null) visitArrayInitializer(ai);
        } else {
	        for (MJParser.ExpressionContext e : el) {
	            put("[");
	        	visitExpression(e);
	            put("]");
	        }
            MJParser.ArrayDimensionContext ad = ctx.arrayDimension();  if (ad != null) visitArrayDimension(ad);
        }
        if (traceVisit) traceOut("visitArrayCreatorRest");
        return null;
    }
    public Void visitClassCreatorRest(MJParser.ClassCreatorRestContext ctx) {
        if (traceVisit) traceIn("visitClassCreatorRest");
        MJParser.ArgumentsContext a = ctx.arguments();  if (a != null) visitArguments(a);
        MJParser.ClassBodyContext c = ctx.classBody();  if (c != null) visitClassBody(c);
        if (traceVisit) traceOut("visitClassCreatorRest");
        return null;
    }
    public Void visitArguments(MJParser.ArgumentsContext ctx) {
        if (traceVisit) traceIn("visitArguments");
        put("(");
        MJParser.ExpressionListContext e = ctx.expressionList();  if (e != null) visitExpressionList(e);
        put(")");
        if (traceVisit) traceOut("visitArguments");
        return null;
    }
}
