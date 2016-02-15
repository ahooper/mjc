package ca.nevdull.mjc1.compiler;

import ca.nevdull.mjc1.compiler.MJ1Parser.BlockContext;
import ca.nevdull.mjc1.compiler.MJ1Parser.BlockStatementContext;
import ca.nevdull.mjc1.compiler.MJ1Parser.CompilationUnitContext;
import ca.nevdull.mjc1.compiler.MJ1Parser.DimensionsContext;
import ca.nevdull.mjc1.compiler.MJ1Parser.ExpressionContext;
import ca.nevdull.mjc1.compiler.MJ1Parser.ExternalDeclarationContext;
import ca.nevdull.mjc1.compiler.MJ1Parser.FieldDeclarationContext;
import ca.nevdull.mjc1.compiler.MJ1Parser.FormalParameterContext;
import ca.nevdull.mjc1.compiler.MJ1Parser.FormalParameterListContext;
import ca.nevdull.mjc1.compiler.MJ1Parser.LocalVariableDeclarationContext;
import ca.nevdull.mjc1.compiler.MJ1Parser.MethodDeclarationContext;
import ca.nevdull.mjc1.compiler.MJ1Parser.PrimitiveTypeContext;
import ca.nevdull.mjc1.compiler.MJ1Parser.ReturnTypeContext;
import ca.nevdull.mjc1.compiler.MJ1Parser.ReturnTypeTypeContext;
import ca.nevdull.mjc1.compiler.MJ1Parser.ReturnVoidContext;
import ca.nevdull.mjc1.compiler.MJ1Parser.TypeBooleanContext;
import ca.nevdull.mjc1.compiler.MJ1Parser.TypeCharContext;
import ca.nevdull.mjc1.compiler.MJ1Parser.TypeContext;
import ca.nevdull.mjc1.compiler.MJ1Parser.TypeFloatContext;
import ca.nevdull.mjc1.compiler.MJ1Parser.TypeIntContext;
import ca.nevdull.mjc1.compiler.MJ1Parser.VariableDeclaratorContext;
import ca.nevdull.mjc1.compiler.MJ1Parser.VariableDeclaratorIdContext;
import ca.nevdull.mjc1.compiler.MJ1Parser.VariableDeclaratorsContext;

public class DefinitionPass extends MJ1BaseVisitor<Void> {
	
	Scope currentScope;

	public DefinitionPass(String fileName) {
	}

	@Override
	public Void visitCompilationUnit(CompilationUnitContext ctx) {
		currentScope = new Scope("globals", null);
		for (ExternalDeclarationContext e : ctx.externalDeclaration()) visit(e);
		currentScope.close();
		return null;
	}

	@Override
	public Void visitMethodDeclaration(MethodDeclarationContext ctx) {
        // returnType Identifier '(' formalParameterList? ')' dimensions block
        ReturnTypeContext rt = ctx.returnType();
        visit(rt);
        Type tipe = rt.tipe;
        // don't need to visit dimensions, since it is all terminals
		int dim = ctx.dimensions().getChildCount() / 2;
		while (dim-- > 0) tipe = new ArrayType(tipe);
		MethodSymbol msym = new MethodSymbol(ctx.Identifier().getSymbol(), currentScope);
		msym.setType(rt.tipe);
		currentScope.add(msym);
		currentScope = msym;
		ctx.def = msym;
        FormalParameterListContext fpl = ctx.formalParameterList();
        if (fpl != null) visit(fpl);
        BlockContext blk = ctx.block();
        visit(blk);
        currentScope = currentScope.close();		
        return null;
    }
	
	@Override
    public Void visitReturnTypeType(ReturnTypeTypeContext ctx) {
        TypeContext t = ctx.type();
        visit(t);
        ctx.tipe = t.tipe;
        return null;
    }

	@Override
    public Void visitReturnVoid(ReturnVoidContext ctx) {
    	ctx.tipe = Type.voidType;
        return null;
    }
    
	@Override
    public Void visitFormalParameter(FormalParameterContext ctx) {
        // type variableDeclaratorId
        TypeContext t = ctx.type();
        visit(t);
		variableDeclaration(t, ctx.variableDeclaratorId());
        return null;
    }
	
    @Override
	public Void visitFieldDeclaration(FieldDeclarationContext ctx) {
        TypeContext t = ctx.type();
        visit(t);
        VariableDeclaratorsContext d = ctx.variableDeclarators();
        for (VariableDeclaratorContext v : d.variableDeclarator()) {
    		variableDeclaration(t, v.variableDeclaratorId());
    		ExpressionContext init = v.expression();   // initializer
			if (init != null) visit(init);
        }
        return null;
	}

	private void variableDeclaration(TypeContext t, VariableDeclaratorIdContext i) {
		FieldSymbol fsym = new FieldSymbol(i.Identifier().getSymbol());
		Type tipe = t.tipe;
		int dim = i.dimensions().getChildCount() / 2;
		//System.out.println(fsym.getName()+" dimensions "+dim);
		while (dim-- > 0) tipe = new ArrayType(tipe);
		fsym.setType(tipe);
		//currentScope.allocate(fsym);
		currentScope.add(fsym);
		i.def = fsym;
	}
    
	@Override
    public Void visitTypePrimitive(MJ1Parser.TypePrimitiveContext ctx) {
        PrimitiveTypeContext p = ctx.primitiveType();
        visit(p);
        ctx.tipe = p.tipe;
        return null;
    }
    
	@Override
    public Void visitTypeBoolean(TypeBooleanContext ctx) {
    	ctx.tipe = Type.booleanType;
        return null;
    }
    
	@Override
    public Void visitTypeChar(TypeCharContext ctx) {
    	ctx.tipe = Type.charType;
        return null;
    }
    
	@Override
    public Void visitTypeFloat(TypeFloatContext ctx) {
    	ctx.tipe = Type.floatType;
        return null;
    }
    
	@Override
    public Void visitTypeInt(TypeIntContext ctx) {
    	ctx.tipe = Type.intType;
        return null;
    }
    
	@Override
    public Void visitTypeString(MJ1Parser.TypeStringContext ctx) {
    	ctx.tipe = Type.stringType;
        return null;
    }
    
	@Override
    public Void visitBlock(MJ1Parser.BlockContext ctx) {
		currentScope = new BlockScope("block@"+ctx.start.getLine(), currentScope);
        for (BlockStatementContext s : ctx.blockStatement()) visit(s);
		currentScope = currentScope.close();
        return null;
    }

	@Override
    public Void visitLocalVariableDeclaration(LocalVariableDeclarationContext ctx) {
        // type variableDeclarators ';'
        TypeContext t = ctx.type();
        visit(t);
        VariableDeclaratorsContext d = ctx.variableDeclarators();
        for (VariableDeclaratorContext v : d.variableDeclarator()) {
    		variableDeclaration(t, v.variableDeclaratorId());
    		ExpressionContext init = v.expression();   // initializer
			if (init != null) visit(init);
        }
        return null;
    }

	@Override
    public Void visitIdentifierPrimary(MJ1Parser.IdentifierPrimaryContext ctx) {
    	ctx.refScope = currentScope;
        return null;
    }
}
