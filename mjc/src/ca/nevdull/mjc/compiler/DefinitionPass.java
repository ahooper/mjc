package ca.nevdull.mjc.compiler;

import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.misc.NotNull;
import org.antlr.v4.runtime.tree.ParseTreeProperty;
import org.antlr.v4.runtime.tree.TerminalNode;

public class DefinitionPass extends MJBaseListener {
    ParseTreeProperty<Scope> scopes = new ParseTreeProperty<Scope>();
    GlobalScope globals;
    Scope currentScope; // define symbols in this scope
    ParseTreeProperty<Type> types = new ParseTreeProperty<Type>();

    void saveScope(ParserRuleContext ctx, Scope s) {
    	scopes.put(ctx, s);
    }
    
    void popScope() {
	    System.out.println(currentScope);
	    currentScope = currentScope.getEnclosingScope();
    }
    
    public void setType(ParserRuleContext node, Type type) {
    	types.put(node, type);
    }
    
    public Type getType(ParserRuleContext node) {
    	Type t = types.get(node);
    	assert t != null;
    	return t;
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
		if (cdecl != null) {
			System.out.println("exitTypeDeclaration class access "+access);
			((ClassSymbol)scopes.get(cdecl)).setAccess(access);
		}
	}
	@Override public void exitModifier(@NotNull MJParser.ModifierContext ctx) { }
	@Override public void exitClassOrInterfaceModifier(@NotNull MJParser.ClassOrInterfaceModifierContext ctx) { }
    
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
	public void enterConstructorDeclaration(MJParser.ConstructorDeclarationContext ctx) {
		MethodSymbol method = new MethodSymbol(ctx.Identifier().getSymbol(), currentScope);
		currentScope.define(method);
        currentScope = method;
        saveScope(ctx, currentScope);					
	}

	@Override
	public void exitConstructorDeclaration(MJParser.ConstructorDeclarationContext ctx) {
        popScope();  // formal parameters
	}
	@Override public void exitClassBody(@NotNull MJParser.ClassBodyContext ctx) { }
	@Override public void exitMemberClassBodyDeclaration(@NotNull MJParser.MemberClassBodyDeclarationContext ctx) { }
	@Override public void exitBlockClassBodyDeclaration(@NotNull MJParser.BlockClassBodyDeclarationContext ctx) { }
	@Override public void exitEmptyClassBodyDeclaration(@NotNull MJParser.EmptyClassBodyDeclarationContext ctx) { }
	@Override public void exitMemberDeclaration(@NotNull MJParser.MemberDeclarationContext ctx) { }

	@Override
	public void enterMethodDeclaration(@NotNull MJParser.MethodDeclarationContext ctx) {
		MethodSymbol method = new MethodSymbol(ctx.Identifier().getSymbol(), currentScope);
		currentScope.define(method);
        currentScope = method;
        saveScope(ctx, currentScope);					
	}

	@Override
	public void exitMethodDeclaration(@NotNull MJParser.MethodDeclarationContext ctx) {
		MethodSymbol method = (MethodSymbol)currentScope;
		MJParser.TypeContext rt = ctx.type();
		Type t = rt != null ? getType(rt) : VoidType.getInstance();
		method.setType(t);
        popScope();  // formal parameters
	}
	
	@Override
	public void exitFieldDeclaration(@NotNull MJParser.FieldDeclarationContext ctx) {
		Type t = getType(ctx.type());
		for (MJParser.VariableDeclaratorContext vd : ctx.variableDeclarators().variableDeclarator()) {
			defineVariable(t, vd.variableDeclaratorId());
		}
	}

	/**
	 * @param t
	 * @param vdlist
	 */
	public void defineVariable(Type t, MJParser.VariableDeclaratorIdContext vdi) {
		VarSymbol var = new VarSymbol(vdi.Identifier().getSymbol());
		if (vdi.arrayDimension().getChildCount() > 0) {
			var.setType(applyDimensions(vdi.arrayDimension(), t));
		} else {
			var.setType(t);
		}
		currentScope.define(var);		
	}

	@Override public void exitVariableDeclarators(@NotNull MJParser.VariableDeclaratorsContext ctx) { }
	@Override public void exitVariableDeclarator(@NotNull MJParser.VariableDeclaratorContext ctx) { }
	@Override public void exitVariableDeclaratorId(@NotNull MJParser.VariableDeclaratorIdContext ctx) {	}
	@Override public void exitSimpleVariableInitializer(@NotNull MJParser.SimpleVariableInitializerContext ctx) { }
	@Override public void exitArrayVariableInitializer(@NotNull MJParser.ArrayVariableInitializerContext ctx) { }
	@Override public void exitArrayInitializer(@NotNull MJParser.ArrayInitializerContext ctx) { }
	
	@Override
	public void exitPrimitType(@NotNull MJParser.PrimitTypeContext ctx) {
		Type t = getType(ctx.primitiveType());
		t = applyDimensions(ctx.arrayDimension(), t);
		setType(ctx, t);
	}
	
	@Override
	public void exitObjectType(@NotNull MJParser.ObjectTypeContext ctx) {
		Type t = getType(ctx.classOrInterfaceType());
		t = applyDimensions(ctx.arrayDimension(), t);
		setType(ctx, t);
	}

	/**
	 * @param arrayDimensionContext
	 * @param t
	 * @return
	 */
	public Type applyDimensions(MJParser.ArrayDimensionContext arrayDimensionContext, Type t) {
		for (int dims = arrayDimensionContext.getChildCount() / 2;
			 dims > 0;
			 dims -= 1) {
			t = new ArrayType(t);
		}
		return t;
	}
	
	@Override
	public void exitClassOrInterfaceType(@NotNull MJParser.ClassOrInterfaceTypeContext ctx) {
		Token last = null;
		for (TerminalNode i : ctx.Identifier()) {
			last = i.getSymbol();
		}
		setType(ctx, new ReferenceType(last));
	}

	@Override
	public void exitBooleanType(@NotNull MJParser.BooleanTypeContext ctx) {
		setType(ctx, PrimitiveType.booleanType);
	}

	@Override
	public void exitDoubleType(@NotNull MJParser.DoubleTypeContext ctx) {
		setType(ctx, PrimitiveType.doubleType);
	}

	@Override
	public void exitCharType(@NotNull MJParser.CharTypeContext ctx) {
		setType(ctx, PrimitiveType.charType);
	}

	@Override
	public void exitFloatType(@NotNull MJParser.FloatTypeContext ctx) {
		setType(ctx, PrimitiveType.floatType);
	}
	@Override
	public void exitIntType(@NotNull MJParser.IntTypeContext ctx) {
		setType(ctx, PrimitiveType.intType);
	}
	@Override
	public void exitShortType(@NotNull MJParser.ShortTypeContext ctx) {
		setType(ctx, PrimitiveType.shortType);
	}

	@Override
	public void exitByteType(@NotNull MJParser.ByteTypeContext ctx) {
		setType(ctx, PrimitiveType.byteType);
	}

	@Override
	public void exitLongType(@NotNull MJParser.LongTypeContext ctx) {
		setType(ctx, PrimitiveType.longType);
	}

	@Override 
	public void enterFormalParameters(@NotNull MJParser.FormalParametersContext ctx) {
	}

	@Override 
	public void exitFormalParameters(@NotNull MJParser.FormalParametersContext ctx) {
	}
	@Override public void exitFormalParameterList(@NotNull MJParser.FormalParameterListContext ctx) { }
	
	@Override
	public void exitFormalParameter(@NotNull MJParser.FormalParameterContext ctx) {
		Type t = getType(ctx.type());
		defineVariable(t, ctx.variableDeclaratorId());
		//TODO variableModifiers
	}
	@Override public void exitVariableModifier(@NotNull MJParser.VariableModifierContext ctx) { }
	@Override public void exitMethodBody(@NotNull MJParser.MethodBodyContext ctx) { }
	@Override public void exitConstructorBody(@NotNull MJParser.ConstructorBodyContext ctx) { }
	@Override public void exitQualifiedName(@NotNull MJParser.QualifiedNameContext ctx) { }
	
	@Override public void exitLiteral(@NotNull MJParser.LiteralContext ctx) {
		String text = ctx.getText(); 
		Type t = null;
		if (ctx.IntegerLiteral() != null) {
			t = PrimitiveType.intType;
			if (text.endsWith("l") || text.endsWith("L")) {
				t = PrimitiveType.longType;
			}
		} else if (ctx.FloatingPointLiteral() != null) {
			t = PrimitiveType.floatType;
			if (text.endsWith("d") || text.endsWith("D")) {
				t = PrimitiveType.doubleType;
			}
		} else if (ctx.CharacterLiteral() != null) {
			t = PrimitiveType.charType;
		} else if (ctx.BooleanLiteral() != null) {
			t = PrimitiveType.booleanType;
		} else if (ctx.StringLiteral() != null) {
			//TODO t = String;
		} else {
			assert text.equals("null");
			//TODO t = Object; 
		}
		setType(ctx, t);
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
	@Override public void exitBlockStatement(@NotNull MJParser.BlockStatementContext ctx) { }
	@Override public void exitLocalVariableDeclarationStatement(@NotNull MJParser.LocalVariableDeclarationStatementContext ctx) { }
	
	@Override
	public void exitLocalVariableDeclaration(@NotNull MJParser.LocalVariableDeclarationContext ctx) {
		Type t = getType(ctx.type());
		for (MJParser.VariableDeclaratorContext vd : ctx.variableDeclarators().variableDeclarator()) {
			defineVariable(t, vd.variableDeclaratorId());
		}
		//TODO variableModifiers
		//TODO ensure no declaration of same name in enclosing blocks 
	}
	@Override public void exitWhileStatement(@NotNull MJParser.WhileStatementContext ctx) { }
	@Override public void exitExpressionStatement(@NotNull MJParser.ExpressionStatementContext ctx) { }
	@Override public void exitEmnptyStatement(@NotNull MJParser.EmnptyStatementContext ctx) { }
	@Override public void exitReturnStatement(@NotNull MJParser.ReturnStatementContext ctx) { }
	@Override public void exitLabelStatement(@NotNull MJParser.LabelStatementContext ctx) { }
	@Override public void exitBlkStatement(@NotNull MJParser.BlkStatementContext ctx) { }
	@Override public void exitIfStatement(@NotNull MJParser.IfStatementContext ctx) { }
	@Override public void exitParExpression(@NotNull MJParser.ParExpressionContext ctx) { }
	@Override public void exitExpressionList(@NotNull MJParser.ExpressionListContext ctx) { }
	@Override public void exitStatementExpression(@NotNull MJParser.StatementExpressionContext ctx) { }
	@Override public void exitConstantExpression(@NotNull MJParser.ConstantExpressionContext ctx) { }
	@Override public void exitCompareExpression(@NotNull MJParser.CompareExpressionContext ctx) { }
	@Override public void exitExclExpression(@NotNull MJParser.ExclExpressionContext ctx) { }
	@Override public void exitAddExpression(@NotNull MJParser.AddExpressionContext ctx) { }
	@Override public void exitAssignExpression(@NotNull MJParser.AssignExpressionContext ctx) { }
	@Override public void exitNotExpression(@NotNull MJParser.NotExpressionContext ctx) { }
	@Override public void exitCallExpression(@NotNull MJParser.CallExpressionContext ctx) { }
	@Override public void exitOrExpression(@NotNull MJParser.OrExpressionContext ctx) { }
	@Override public void exitIndexExpression(@NotNull MJParser.IndexExpressionContext ctx) { }
	@Override public void exitEqualExpression(@NotNull MJParser.EqualExpressionContext ctx) { }
	@Override public void exitMultExpression(@NotNull MJParser.MultExpressionContext ctx) { }
	@Override public void exitCondAndExpression(@NotNull MJParser.CondAndExpressionContext ctx) { }
	@Override public void exitAndExpression(@NotNull MJParser.AndExpressionContext ctx) { }
	@Override public void exitPrimExpression(@NotNull MJParser.PrimExpressionContext ctx) { }
	@Override public void exitCondOrExpression(@NotNull MJParser.CondOrExpressionContext ctx) { }
	@Override public void exitCastExpression(@NotNull MJParser.CastExpressionContext ctx) { }
	@Override public void exitDotExpression(@NotNull MJParser.DotExpressionContext ctx) { }
	@Override public void exitShiftExpression(@NotNull MJParser.ShiftExpressionContext ctx) { }
	@Override public void exitPlusExpression(@NotNull MJParser.PlusExpressionContext ctx) { }
	@Override public void exitNewExpression(@NotNull MJParser.NewExpressionContext ctx) { }
	@Override public void exitLiteralPrimary(@NotNull MJParser.LiteralPrimaryContext ctx) { }
	@Override public void exitSuperPrimary(@NotNull MJParser.SuperPrimaryContext ctx) { }
	@Override public void exitIdentifierPrimary(@NotNull MJParser.IdentifierPrimaryContext ctx) { }
	@Override public void exitThisPrimary(@NotNull MJParser.ThisPrimaryContext ctx) { }
	@Override public void exitParenPrimary(@NotNull MJParser.ParenPrimaryContext ctx) { }
	@Override public void exitCreator(@NotNull MJParser.CreatorContext ctx) { }
	@Override public void exitCreatedName(@NotNull MJParser.CreatedNameContext ctx) { }
	@Override public void exitArrayCreatorRest(@NotNull MJParser.ArrayCreatorRestContext ctx) { }
	@Override public void exitClassCreatorRest(@NotNull MJParser.ClassCreatorRestContext ctx) { }
	@Override public void exitArguments(@NotNull MJParser.ArgumentsContext ctx) { }

}
