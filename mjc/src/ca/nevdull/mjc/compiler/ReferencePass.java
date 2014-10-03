package ca.nevdull.mjc.compiler;

import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.misc.NotNull;
import org.antlr.v4.runtime.tree.ParseTreeProperty;
import org.antlr.v4.runtime.tree.TerminalNode;

public class ReferencePass extends MJBaseListener {
    ParseTreeProperty<Scope> scopes;
    GlobalScope globals;
    Scope currentScope; // resolve symbols starting in this scope
    ParseTreeProperty<Type> types;
    
    public void setType(ParserRuleContext node, Type type) {
    	types.put(node, type);
    }
    
    public Type getType(ParserRuleContext node) {
    	Type t = types.get(node);
    	assert t != null;
    	return t;
    }
      
    public void promoteType(ParserRuleContext node, ParserRuleContext subNode) {
    	setType(node, getType(subNode));
    }
	
    /**
	 * @param scopes
	 * @param globals
	 */
	public ReferencePass(ParseTreeProperty<Scope> scopes, GlobalScope globals, ParseTreeProperty<Type> types) {
		super();
		this.scopes = scopes;
		this.globals = globals;
		this.types = types;
	}
/*
 * grep 'Context extends ' MJParser.java  | sed  's/ *public static class \([a-zA-Z]*\) .*$/@Override public void exit\1(MJParser.\1Context ctx) { }/' | sed 's/Context//' | sed 's/Context//'
 */
    @Override
    public void enterCompilationUnit(@NotNull MJParser.CompilationUnitContext ctx) {
        currentScope = globals;
    }

    @Override
    public void exitCompilationUnit(@NotNull MJParser.CompilationUnitContext ctx) {
    	assert currentScope == globals;
        //TODO: report undefined names
    }
	@Override public void exitTypeDeclaration(MJParser.TypeDeclarationContext ctx) { }
	@Override public void exitModifier(MJParser.ModifierContext ctx) { }
	@Override public void exitClassOrInterfaceModifier(MJParser.ClassOrInterfaceModifierContext ctx) { }
    
	@Override 
	public void enterClassDeclaration(@NotNull MJParser.ClassDeclarationContext ctx) {
        currentScope = scopes.get(ctx);
	}
	
	@Override 
	public void exitClassDeclaration(@NotNull MJParser.ClassDeclarationContext ctx) {
		currentScope = currentScope.getEnclosingScope();	
	}
	@Override public void exitClassBody(MJParser.ClassBodyContext ctx) { }
	@Override public void exitMemberClassBodyDeclaration(MJParser.MemberClassBodyDeclarationContext ctx) { }
	@Override public void exitBlockClassBodyDeclaration(MJParser.BlockClassBodyDeclarationContext ctx) { }
	@Override public void exitEmptyClassBodyDeclaration(MJParser.EmptyClassBodyDeclarationContext ctx) { }
	@Override public void exitMemberDeclaration(MJParser.MemberDeclarationContext ctx) { }

	@Override
	public void enterMethodDeclaration(@NotNull MJParser.MethodDeclarationContext ctx) {
        currentScope = scopes.get(ctx);
	}

	@Override
	public void exitMethodDeclaration(@NotNull MJParser.MethodDeclarationContext ctx) {
		currentScope = currentScope.getEnclosingScope();	
	}

	@Override
	public void enterConstructorDeclaration(@NotNull MJParser.ConstructorDeclarationContext ctx) {
        currentScope = scopes.get(ctx);
	}

	@Override
	public void exitConstructorDeclaration(@NotNull MJParser.ConstructorDeclarationContext ctx) {
		currentScope = currentScope.getEnclosingScope();	
	}
	@Override public void exitFieldDeclaration(MJParser.FieldDeclarationContext ctx) { }
	@Override public void exitVariableDeclarators(MJParser.VariableDeclaratorsContext ctx) { }
	@Override public void exitVariableDeclarator(MJParser.VariableDeclaratorContext ctx) { }
	@Override public void exitVariableDeclaratorId(MJParser.VariableDeclaratorIdContext ctx) { }
	@Override public void exitSimpleVariableInitializer(MJParser.SimpleVariableInitializerContext ctx) { }
	@Override public void exitArrayVariableInitializer(MJParser.ArrayVariableInitializerContext ctx) { }
	@Override public void exitArrayInitializer(MJParser.ArrayInitializerContext ctx) { }
	@Override public void exitPrimitType(MJParser.PrimitTypeContext ctx) { }
	@Override public void exitObjectType(MJParser.ObjectTypeContext ctx) { }
	
	@Override
	public void exitClassOrInterfaceType(MJParser.ClassOrInterfaceTypeContext ctx) {
		//TODO resolve type
		ReferenceType t = (ReferenceType)getType(ctx);
		Symbol sym = null;
		Token name = null;
		for (TerminalNode i : ctx.Identifier()) {
			name = i.getSymbol();
			Symbol s = null;
			if (sym == null) {
				s = currentScope.resolve(name.getText());
			} else if (sym instanceof ClassSymbol) {
				s = ((ClassSymbol)sym).resolve(name.getText());
			}
			if (s == null) {
				Compiler.error(name, name.getText()+" is not defined");
				break;
			}
			sym = s;
		}
		if (sym instanceof ClassSymbol) {
			System.out.println(name.getText()+" type "+t);
			t.resolveTo((ClassSymbol)sym);
		} else {
			Compiler.error(name, name.getText()+" is not a class");
		}

	}
	@Override public void exitBooleanType(MJParser.BooleanTypeContext ctx) { }
	@Override public void exitDoubleType(MJParser.DoubleTypeContext ctx) { }
	@Override public void exitCharType(MJParser.CharTypeContext ctx) { }
	@Override public void exitFloatType(MJParser.FloatTypeContext ctx) { }
	@Override public void exitIntType(MJParser.IntTypeContext ctx) { }
	@Override public void exitShortType(MJParser.ShortTypeContext ctx) { }
	@Override public void exitByteType(MJParser.ByteTypeContext ctx) { }
	@Override public void exitLongType(MJParser.LongTypeContext ctx) { }
	@Override public void exitFormalParameters(MJParser.FormalParametersContext ctx) { }
	@Override public void exitFormalParameterList(MJParser.FormalParameterListContext ctx) { }
	@Override public void exitFormalParameter(MJParser.FormalParameterContext ctx) { }
	@Override public void exitVariableModifier(MJParser.VariableModifierContext ctx) { }
	@Override public void exitMethodBody(MJParser.MethodBodyContext ctx) { }
	@Override public void exitConstructorBody(MJParser.ConstructorBodyContext ctx) { }
	@Override public void exitQualifiedName(MJParser.QualifiedNameContext ctx) { }
	@Override public void exitLiteral(MJParser.LiteralContext ctx) { }

	@Override 
	public void enterBlock(@NotNull MJParser.BlockContext ctx) {
        currentScope = scopes.get(ctx);
	}

	@Override public void exitBlock(@NotNull MJParser.BlockContext ctx) {
		currentScope = currentScope.getEnclosingScope();	
	}
	@Override public void exitBlockStatement(MJParser.BlockStatementContext ctx) { }
	@Override public void exitLocalVariableDeclarationStatement(MJParser.LocalVariableDeclarationStatementContext ctx) { }
	@Override public void exitLocalVariableDeclaration(MJParser.LocalVariableDeclarationContext ctx) { }
	@Override public void exitWhileStatement(MJParser.WhileStatementContext ctx) { }
	@Override public void exitExpressionStatement(MJParser.ExpressionStatementContext ctx) { }
	@Override public void exitEmnptyStatement(MJParser.EmnptyStatementContext ctx) { }
	@Override public void exitReturnStatement(MJParser.ReturnStatementContext ctx) { }
	@Override public void exitLabelStatement(MJParser.LabelStatementContext ctx) { }
	@Override public void exitBlkStatement(MJParser.BlkStatementContext ctx) { }
	@Override public void exitIfStatement(MJParser.IfStatementContext ctx) { }
	
	@Override
	public void exitParExpression(MJParser.ParExpressionContext ctx) {
		promoteType(ctx, ctx.expression());
	}
	@Override public void exitExpressionList(MJParser.ExpressionListContext ctx) { }
	@Override public void exitStatementExpression(MJParser.StatementExpressionContext ctx) { }
	@Override public void exitConstantExpression(MJParser.ConstantExpressionContext ctx) { }
	@Override public void exitCompareExpression(MJParser.CompareExpressionContext ctx) { }
	@Override public void exitExclExpression(MJParser.ExclExpressionContext ctx) { }
	@Override public void exitAddExpression(MJParser.AddExpressionContext ctx) { }
	@Override public void exitAssignExpression(MJParser.AssignExpressionContext ctx) { }
	@Override public void exitNotExpression(MJParser.NotExpressionContext ctx) { }
	
	@Override
	public void exitCallExpression(MJParser.CallExpressionContext ctx) {
		Type type = getType(ctx.expression());
		
		
	}
	@Override public void exitOrExpression(MJParser.OrExpressionContext ctx) { }
	@Override public void exitIndexExpression(MJParser.IndexExpressionContext ctx) { }
	@Override public void exitEqualExpression(MJParser.EqualExpressionContext ctx) { }
	@Override public void exitMultExpression(MJParser.MultExpressionContext ctx) { }
	@Override public void exitCondAndExpression(MJParser.CondAndExpressionContext ctx) { }
	@Override public void exitAndExpression(MJParser.AndExpressionContext ctx) { }
	
	@Override
	public void exitPrimExpression(MJParser.PrimExpressionContext ctx) {
		promoteType(ctx, ctx.primary());
	}
	@Override public void exitCondOrExpression(MJParser.CondOrExpressionContext ctx) { }
	@Override public void exitCastExpression(MJParser.CastExpressionContext ctx) { }
	
	@Override
	public void exitDotExpression(MJParser.DotExpressionContext ctx) {
		Type t = getType(ctx.expression());
    	Token token = ctx.Identifier().getSymbol();
        String name = token.getText();
        if (t instanceof ReferenceType) {
        	System.out.println("dotExpression "+t);
        	ClassSymbol klass = ((ReferenceType) t).referredClass;
        	Symbol sym = klass.resolve(name);
        	if (sym == null) {
        		Compiler.error(token, name+" is not defined in "+klass.getName());
        		t = UnknownType.getInstance();
        	} else {
        		t = sym.getType();
        	}
        } else {
        	Compiler.error(token, "not a reference: "+t.toString());
        }
		setType(ctx, t);
	}
	@Override public void exitShiftExpression(MJParser.ShiftExpressionContext ctx) { }
	@Override public void exitPlusExpression(MJParser.PlusExpressionContext ctx) { }
	@Override public void exitNewExpression(MJParser.NewExpressionContext ctx) { }
	
	@Override
	public void exitLiteralPrimary(MJParser.LiteralPrimaryContext ctx) {
		promoteType(ctx, ctx.literal());
	}
	@Override public void exitSuperPrimary(MJParser.SuperPrimaryContext ctx) { }
	
	@Override
	public void exitIdentifierPrimary(@NotNull MJParser.IdentifierPrimaryContext ctx) {
    	Token token = ctx.Identifier().getSymbol();
        String name = token.getText();
        Symbol sym = currentScope.resolve(name);
        Type t;
        if (sym == null) {
        	Compiler.error(token, name+" is not defined");
        	t = UnknownType.getInstance();
        } else {
        	t = sym.getType();
        }
		System.out.println(name+" type "+t);
        setType(ctx, t);
	}
	@Override public void exitThisPrimary(MJParser.ThisPrimaryContext ctx) { }
	
	@Override
	public void exitParenPrimary(MJParser.ParenPrimaryContext ctx) {
		promoteType(ctx, ctx.expression());
	}
	@Override public void exitCreator(MJParser.CreatorContext ctx) { }
	@Override public void exitCreatedName(MJParser.CreatedNameContext ctx) { }
	@Override public void exitArrayCreatorRest(MJParser.ArrayCreatorRestContext ctx) { }
	@Override public void exitClassCreatorRest(MJParser.ClassCreatorRestContext ctx) { }
	@Override public void exitArguments(MJParser.ArgumentsContext ctx) { }
}
