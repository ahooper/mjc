package ca.nevdull.mjc.compiler;

import java.util.ListIterator;

import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.misc.NotNull;
import org.antlr.v4.runtime.tree.ParseTreeProperty;
import org.antlr.v4.runtime.tree.TerminalNode;

public class ReferencePass extends MJBaseListener {
    ParseTreeProperty<Scope> scopes;
    GlobalScope globals;
    ParseTreeProperty<Symbol> symbols;
    ParseTreeProperty<Type> types = new ParseTreeProperty<Type>();
    
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
	public ReferencePass(ParseTreeProperty<Scope> scopes, GlobalScope globals, ParseTreeProperty<Symbol> symbols) {
		super();
		this.scopes = scopes;
		this.globals = globals;
		this.symbols = symbols;
	}
/*
 * grep 'Context extends ' MJParser.java  | sed  's/ *public static class \([a-zA-Z]*\) .*$/@Override public void exit\1(MJParser.\1Context ctx) { }/' | sed 's/Context//' | sed 's/Context//'
 */
	
	@Override 
	public void exitClassDeclaration(@NotNull MJParser.ClassDeclarationContext ctx) {
		ClassSymbol klass = (ClassSymbol)symbols.get(ctx);
		if (ctx.type() != null) {
			Type t = getType(ctx.type());
			if (t instanceof ClassSymbol) {
				klass.setSuperClass((ClassSymbol)t);
				System.out.println(klass.getName()+" super class is "+t.toString());
			} else {
				Compiler.error(ctx.Identifier().getSymbol(), " super must be a class");
			}
		}
	}
	@Override public void exitClassBody(MJParser.ClassBodyContext ctx) { }
	@Override public void exitMemberClassBodyDeclaration(MJParser.MemberClassBodyDeclarationContext ctx) { }
	@Override public void exitBlockClassBodyDeclaration(MJParser.BlockClassBodyDeclarationContext ctx) { }
	@Override public void exitEmptyClassBodyDeclaration(MJParser.EmptyClassBodyDeclarationContext ctx) { }
	@Override public void exitMemberDeclaration(MJParser.MemberDeclarationContext ctx) { }

	@Override
	public void exitMethodDeclaration(@NotNull MJParser.MethodDeclarationContext ctx) {
		Symbol method = symbols.get(ctx);
		Type t;
		if (ctx.type() != null) {
			t = getType(ctx.type());
		} else {
			t = VoidType.getInstance();
		}
		method.setType(t);
		System.out.println(method.getName()+" type is "+t.toString());
	}

	@Override
	public void exitConstructorDeclaration(@NotNull MJParser.ConstructorDeclarationContext ctx) {
	}
	@Override public void exitFieldDeclaration(MJParser.FieldDeclarationContext ctx) {
		Type t = getType(ctx.type());
		//System.out.println("exitFieldDeclaration type "+t.toString());
		for (MJParser.VariableDeclaratorContext vd : ctx.variableDeclarators().variableDeclarator()) {
			setVariableType(vd.variableDeclaratorId(), t);
		}
	}
	public void setVariableType(MJParser.VariableDeclaratorIdContext vdi, Type t) {
		Symbol var = symbols.get(vdi);
		t = applyDimensions(vdi.arrayDimension(), t);
		var.setType(t);
		System.out.println(var.getName()+" type is "+t.toString());
	}
	@Override public void exitVariableDeclarators(MJParser.VariableDeclaratorsContext ctx) { }
	@Override public void exitVariableDeclarator(MJParser.VariableDeclaratorContext ctx) { }
	@Override public void exitVariableDeclaratorId(MJParser.VariableDeclaratorIdContext ctx) { }
	@Override public void exitSimpleVariableInitializer(MJParser.SimpleVariableInitializerContext ctx) { }
	@Override public void exitArrayVariableInitializer(MJParser.ArrayVariableInitializerContext ctx) { }
	@Override public void exitArrayInitializer(MJParser.ArrayInitializerContext ctx) { }
	@Override public void exitPrimitType(MJParser.PrimitTypeContext ctx) {
		//System.out.println("exitPrimitType "+ctx.getText());
		setType(ctx, applyDimensions(ctx.arrayDimension(), getType(ctx.primitiveType())));
	}
	@Override public void exitObjectType(MJParser.ObjectTypeContext ctx) {
		//System.out.println("exitObjectType "+ctx.getText());
		setType(ctx, applyDimensions(ctx.arrayDimension(), getType(ctx.classOrInterfaceType())));
	}
	
	public Type applyDimensions(MJParser.ArrayDimensionContext ctx, Type t) {
		//System.out.println("applyDimensions "+ctx.getText());
		for (int dims = ctx.getChildCount() / 2;
			 dims > 0;
			 dims -= 1) {
			t = new ArrayType(t);
		}
		//System.out.println("applyDimensions "+t.toString());
		return t;
	}
	
	@Override
	public void exitClassOrInterfaceType(MJParser.ClassOrInterfaceTypeContext ctx) {
		Scope scope = scopes.get(ctx.getParent());
		Token name = ctx.Identifier(0).getSymbol();
		Symbol sym = scope.resolve(name.getText());
        if (sym == null) {
        	Compiler.error(name, name.getText()+" is not defined","exitClassOrInterfaceType");
			setType(ctx, UnknownType.getInstance());
        } else {
			for (ListIterator<TerminalNode> iter = ctx.Identifier().listIterator(1);
				 iter.hasNext(); ) {
				TerminalNode id = iter.next();
				name = id.getSymbol();
				if (sym instanceof ScopingSymbol) {
					System.out.println("ClassOrInterfaceType lookup "+name.getText()+" in "+sym.toString());
					Symbol s = ((ScopingSymbol)sym).resolveMember(name.getText());
					if (s == null) {
						Compiler.error(name, name.getText()+" is not defined in "+sym.toString(),"ClassOrInterfaceType");
						sym = null;  // lookup failed
						break;
					}
					sym = s;
				} else {
					Compiler.error(name, sym.getName()+" is not a scope: "+sym.toString(),"ClassOrInterfaceType");
					sym = null;  // lookup failed
					break;
				}
			}
			if (sym != null) {  // lookup succeeded
				if (sym instanceof ClassSymbol) {
					setType(ctx, (ClassSymbol)sym);
					System.out.println(name.getText()+" type "+sym.getName());
				} else {
					Compiler.error(name, name.getText()+" is not a class: "+sym.toString(),"ClassOrInterfaceType");
					setType(ctx, UnknownType.getInstance());
				}
			}
        }
	}
	@Override public void exitArrayDimension(@NotNull MJParser.ArrayDimensionContext ctx) {
		//System.out.println("exitArrayDimension "+ctx.getText());
	}
	@Override public void exitBooleanType(MJParser.BooleanTypeContext ctx) {
		setType(ctx, PrimitiveType.booleanType);
	}
	@Override public void exitDoubleType(MJParser.DoubleTypeContext ctx) {
		setType(ctx, PrimitiveType.doubleType);
	}
	@Override public void exitCharType(MJParser.CharTypeContext ctx) {
		setType(ctx, PrimitiveType.charType);
	}
	@Override public void exitFloatType(MJParser.FloatTypeContext ctx) {
		setType(ctx, PrimitiveType.floatType);
	}
	@Override public void exitIntType(MJParser.IntTypeContext ctx) {
		setType(ctx, PrimitiveType.intType);
	}
	@Override public void exitShortType(MJParser.ShortTypeContext ctx) {
		setType(ctx, PrimitiveType.shortType);
	}
	@Override public void exitByteType(MJParser.ByteTypeContext ctx) {
		setType(ctx, PrimitiveType.byteType);
	}
	@Override public void exitLongType(MJParser.LongTypeContext ctx) {
		setType(ctx, PrimitiveType.longType);
	}
	@Override public void exitFormalParameters(MJParser.FormalParametersContext ctx) { }
	@Override public void exitFormalParameterList(MJParser.FormalParameterListContext ctx) { }
	@Override public void exitFormalParameter(MJParser.FormalParameterContext ctx) {
		//System.out.println("exitFormalParameter "+ctx.getText());
		Type t = getType(ctx.type());
		//System.out.println("exitFormalParameter type "+t.toString());
		setVariableType(ctx.variableDeclaratorId(), t);
	}
	@Override public void exitVariableModifier(MJParser.VariableModifierContext ctx) { }
	@Override public void exitMethodBody(MJParser.MethodBodyContext ctx) { }
	@Override public void exitConstructorBody(MJParser.ConstructorBodyContext ctx) { }
	@Override public void exitQualifiedName(MJParser.QualifiedNameContext ctx) { }
	@Override public void exitLiteral(MJParser.LiteralContext ctx) {
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
	@Override public void exitBlockStatement(MJParser.BlockStatementContext ctx) { }
	@Override public void exitLocalVariableDeclarationStatement(MJParser.LocalVariableDeclarationStatementContext ctx) { }
	@Override public void exitLocalVariableDeclaration(MJParser.LocalVariableDeclarationContext ctx) {
		//System.out.println("exitLocalVariableDeclaration type "+ctx.type().getText());
		Type t = getType(ctx.type());
		//System.out.println("exitLocalVariableDeclaration type "+t.toString());
		for (MJParser.VariableDeclaratorContext vd : ctx.variableDeclarators().variableDeclarator()) {
			setVariableType(vd.variableDeclaratorId(), t);
		}
	}

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
		System.out.println("exitCallExpression "+ctx.getText());
		Type type = getType(ctx.expression());
		System.out.println("CallExpression "+type.toString());
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
		Type type = getType(ctx.expression());
    	Token token = ctx.Identifier().getSymbol();
        String name = token.getText();
        if (type instanceof ScopingSymbol) {
        	Symbol sym = ((ScopingSymbol) type).resolveMember(name);
        	if (sym == null) {
        		Compiler.error(token, name+" is not defined in "+type.getName(),"DotExpression");
        		type = UnknownType.getInstance();
        	} else {
        		type = sym.getType();
        	}
        } else if (!(type instanceof UnknownType)) {
        	Compiler.error(token, "not a reference: "+type.toString(),"DotExpression");
        }
		setType(ctx, type);
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
        Symbol sym = scopes.get(ctx).resolve(name);
        Type t;
        if (sym == null) {
        	Compiler.error(token, name+" is not defined","IdentifierPrimary");
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
