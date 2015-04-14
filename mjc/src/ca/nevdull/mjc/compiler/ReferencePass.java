package ca.nevdull.mjc.compiler;

import java.util.ListIterator;

import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.misc.NotNull;
import org.antlr.v4.runtime.tree.TerminalNode;

import ca.nevdull.mjc.compiler.MJParser.PrimitiveTypeContext;

public class ReferencePass extends MJBaseListener {
    PassData passData;
	
	public ReferencePass(PassData passData) {
		super();
		this.passData = passData;
	}
    
    @Override 
	public void exitClassDeclaration(@NotNull MJParser.ClassDeclarationContext ctx) {
		ClassSymbol klass = ctx.defn;
		if (ctx.type() != null) {
			Type t = ctx.type().tipe;
			if (t instanceof ClassSymbol) {
				if (t.getName().equals("_NULL_")) {
					//no superclass for Object
					System.out.println(klass.getName()+" has no super class");
				} else {
					klass.setSuperClass((ClassSymbol)t);
					System.out.println(klass.getName()+" super class is "+t.toString());
				}
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
		Symbol method = ctx.defn;
		Type t;
		if (ctx.type() != null) {
			t = ctx.type().tipe;
		} else {
			t = VoidType.getInstance();
		}
		method.setType(t);
		System.out.println(method.getName()+" type is "+t.toString());
	}

	@Override
	public void exitConstructorDeclaration(@NotNull MJParser.ConstructorDeclarationContext ctx) {
		Symbol method = ctx.defn;
		Type t = VoidType.getInstance();
		method.setType(t);
		System.out.println(method.getName()+" type is "+t.toString());
	}
	@Override public void exitFieldDeclaration(MJParser.FieldDeclarationContext ctx) {
		Type t = ctx.type().tipe;
		//System.out.println("exitFieldDeclaration type "+t.toString());
		for (MJParser.VariableDeclaratorContext vd : ctx.variableDeclarators().variableDeclarator()) {
			setVariableType(vd.defn, vd.variableDeclaratorId(), t);
		}
	}
	public void setVariableType(VariableSymbol defn, MJParser.VariableDeclaratorIdContext vdi, Type t) {
		t = applyDimensions(vdi.arrayDimension(), t);
		defn.setType(t);
		System.out.println(defn.getName()+" type is "+t.toString());
	}
	@Override public void exitVariableDeclarators(MJParser.VariableDeclaratorsContext ctx) { }
	@Override public void exitVariableDeclarator(MJParser.VariableDeclaratorContext ctx) { }
	@Override public void exitVariableDeclaratorId(MJParser.VariableDeclaratorIdContext ctx) { }
	@Override public void exitSimpleVariableInitializer(MJParser.SimpleVariableInitializerContext ctx) { }
	@Override public void exitArrayVariableInitializer(MJParser.ArrayVariableInitializerContext ctx) { }
	@Override public void exitArrayInitializer(MJParser.ArrayInitializerContext ctx) { }
	@Override public void exitPrimitType(MJParser.PrimitTypeContext ctx) {
		//System.out.println("exitPrimitType "+ctx.getText());
		ctx.tipe = applyDimensions(ctx.arrayDimension(), ctx.primitiveType().tipe);
	}
	@Override public void exitObjectType(MJParser.ObjectTypeContext ctx) {
		//System.out.println("exitObjectType "+ctx.getText());
		ctx.tipe = applyDimensions(ctx.arrayDimension(), ctx.classOrInterfaceType().tipe);
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
		Scope scope = ctx.refScope;
		Token name = ctx.Identifier(0).getSymbol();
		Symbol sym = scope.resolve(name.getText());
        if (sym == null) {
        	Compiler.error(name, name.getText()+" is not defined","exitClassOrInterfaceType");
        	ctx.tipe = UnknownType.getInstance();
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
					ctx.tipe = (ClassSymbol)sym;
					System.out.println(name.getText()+" type "+sym.getName());
				} else {
					Compiler.error(name, name.getText()+" is not a class: "+sym.toString(),"ClassOrInterfaceType");
					ctx.tipe = UnknownType.getInstance();
				}
			}
        }
	}
	@Override public void exitArrayDimension(@NotNull MJParser.ArrayDimensionContext ctx) {
		//System.out.println("exitArrayDimension "+ctx.getText());
	}
	@Override public void exitBooleanType(MJParser.BooleanTypeContext ctx) {
		ctx.tipe = PrimitiveType.booleanType;
	}
	@Override public void exitDoubleType(MJParser.DoubleTypeContext ctx) {
		ctx.tipe = PrimitiveType.doubleType;
	}
	@Override public void exitCharType(MJParser.CharTypeContext ctx) {
		ctx.tipe = PrimitiveType.charType;
	}
	@Override public void exitFloatType(MJParser.FloatTypeContext ctx) {
		ctx.tipe = PrimitiveType.floatType;
	}
	@Override public void exitIntType(MJParser.IntTypeContext ctx) {
		ctx.tipe = PrimitiveType.intType;
	}
	@Override public void exitShortType(MJParser.ShortTypeContext ctx) {
		ctx.tipe = PrimitiveType.shortType;
	}
	@Override public void exitByteType(MJParser.ByteTypeContext ctx) {
		ctx.tipe = PrimitiveType.byteType;
	}
	@Override public void exitLongType(MJParser.LongTypeContext ctx) {
		ctx.tipe = PrimitiveType.longType;
	}
	@Override public void exitFormalParameters(MJParser.FormalParametersContext ctx) { }
	@Override public void exitFormalParameterList(MJParser.FormalParameterListContext ctx) { }
	@Override public void exitFormalParameter(MJParser.FormalParameterContext ctx) {
		//System.out.println("exitFormalParameter "+ctx.getText());
		Type t = ctx.type().tipe;
		//System.out.println("exitFormalParameter type "+t.toString());
		setVariableType(ctx.defn, ctx.variableDeclaratorId(), t);
	}
	@Override public void exitVariableModifier(MJParser.VariableModifierContext ctx) { }
	@Override public void exitMethodBody(MJParser.MethodBodyContext ctx) { }
	@Override public void exitConstructorBody(MJParser.ConstructorBodyContext ctx) { }
	@Override public void exitQualifiedName(MJParser.QualifiedNameContext ctx) { }
	@Override public void exitLiteral(MJParser.LiteralContext ctx) {	}
	@Override public void exitBlockStatement(MJParser.BlockStatementContext ctx) { }
	@Override public void exitLocalVariableDeclarationStatement(MJParser.LocalVariableDeclarationStatementContext ctx) { }
	@Override public void exitLocalVariableDeclaration(MJParser.LocalVariableDeclarationContext ctx) {
		//System.out.println("exitLocalVariableDeclaration type "+ctx.type().getText());
		Type t = ctx.type().tipe;
		//System.out.println("exitLocalVariableDeclaration type "+t.toString());
		for (MJParser.VariableDeclaratorContext vd : ctx.variableDeclarators().variableDeclarator()) {
			setVariableType(vd.defn, vd.variableDeclaratorId(), t);
		}
	}

	@Override public void exitWhileStatement(MJParser.WhileStatementContext ctx) { }
	@Override public void exitExpressionStatement(MJParser.ExpressionStatementContext ctx) { }
	@Override public void exitEmptyStatement(MJParser.EmptyStatementContext ctx) { }
	@Override public void exitReturnStatement(MJParser.ReturnStatementContext ctx) { }
	@Override public void exitLabelStatement(MJParser.LabelStatementContext ctx) { }
	@Override public void exitBlkStatement(MJParser.BlkStatementContext ctx) { }
	@Override public void exitIfStatement(MJParser.IfStatementContext ctx) { }
	
	@Override public void exitParExpression(MJParser.ParExpressionContext ctx) {	}
	@Override public void exitExpressionList(MJParser.ExpressionListContext ctx) { }
	@Override public void exitStatementExpression(MJParser.StatementExpressionContext ctx) { }
	@Override public void exitConstantExpression(MJParser.ConstantExpressionContext ctx) { }
	@Override public void exitCompareExpression(MJParser.CompareExpressionContext ctx) { }
	@Override public void exitExclExpression(MJParser.ExclExpressionContext ctx) { }
	@Override public void exitAddExpression(MJParser.AddExpressionContext ctx) { }
	@Override public void exitAssignExpression(MJParser.AssignExpressionContext ctx) { }
	@Override public void exitNotExpression(MJParser.NotExpressionContext ctx) { }
	
	@Override public void exitCallExpression(MJParser.CallExpressionContext ctx) {	}
	@Override public void exitOrExpression(MJParser.OrExpressionContext ctx) { }
	@Override public void exitIndexExpression(MJParser.IndexExpressionContext ctx) { }
	@Override public void exitEqualExpression(MJParser.EqualExpressionContext ctx) { }
	@Override public void exitMultExpression(MJParser.MultExpressionContext ctx) { }
	@Override public void exitCondAndExpression(MJParser.CondAndExpressionContext ctx) { }
	@Override public void exitAndExpression(MJParser.AndExpressionContext ctx) { }
	
	@Override public void exitPrimExpression(MJParser.PrimExpressionContext ctx) {	}
	@Override public void exitCondOrExpression(MJParser.CondOrExpressionContext ctx) { }
	@Override public void exitCastExpression(MJParser.CastExpressionContext ctx) { }
	
	@Override public void exitDotExpression(MJParser.DotExpressionContext ctx) {	}
	@Override public void exitShiftExpression(MJParser.ShiftExpressionContext ctx) { }
	@Override public void exitPlusExpression(MJParser.PlusExpressionContext ctx) { }
	@Override public void exitNewExpression(MJParser.NewExpressionContext ctx) { }
	
	@Override public void exitLiteralPrimary(MJParser.LiteralPrimaryContext ctx) {	}
	@Override public void exitSuperPrimary(MJParser.SuperPrimaryContext ctx) { }
	
	@Override
	public void exitIdentifierPrimary(@NotNull MJParser.IdentifierPrimaryContext ctx) {
    	Token token = ctx.Identifier().getSymbol();
        String name = token.getText();
        Scope refScope = ctx.refScope;
        Symbol sym = refScope.resolve(name);
        if (sym == null) {
        	Compiler.error(token, name+" is not defined","IdentifierPrimary");
        } else {
        	// check for forward local reference forbidden
        	int refLocation = token.getTokenIndex();
        	Scope defScope = sym.getScope();
        	int defLocation = sym.getToken().getTokenIndex();
        	System.out.println(name+" ref@"+refLocation+" def@"+defLocation);
        	if (   refScope instanceof BaseScope
        		&& defScope instanceof BaseScope
        		&& refLocation < defLocation ) {
        		Compiler.error(token, name+" is forward local variable reference","IdentifierPrimary");
        		sym = null;
        	} else {
        		ctx.defn = sym;
        	}
        }
	}
	@Override public void exitThisPrimary(MJParser.ThisPrimaryContext ctx) { }
	
	@Override public void exitParenPrimary(MJParser.ParenPrimaryContext ctx) {	}
	@Override public void exitArrayCreator(MJParser.ArrayCreatorContext ctx) { }
	@Override public void exitClassCreator(MJParser.ClassCreatorContext ctx) { }
	@Override public void exitCreatedName(MJParser.CreatedNameContext ctx) {
        PrimitiveTypeContext ptc = ctx.primitiveType();
        if (ptc != null) {
        	ctx.tipe = ctx.primitiveType().tipe;
		} else {
	        Scope refScope = ctx.refScope;
			Symbol sym = null;
			for (TerminalNode id : ctx.Identifier()) {
		    	Token token = id.getSymbol();
		        String name = token.getText();
		        sym = refScope.resolve(name);
		        if (sym == null) {
		        	Compiler.error(token, name+" is not defined","CreatedName");
		        } else if (sym instanceof ScopingSymbol) {
		        	refScope = (Scope)sym;
		        }
			}
			if (sym instanceof ClassSymbol) {
	    		ctx.defn = sym;
			} else if (sym != null) {
	        	Compiler.error(sym.getToken(), sym.getName()+" is not a class","CreatedName");
			}
		}
	}
	@Override public void exitArrayCreatorRest(MJParser.ArrayCreatorRestContext ctx) { }
	@Override public void exitClassCreatorRest(MJParser.ClassCreatorRestContext ctx) { }
	@Override public void exitArguments(MJParser.ArgumentsContext ctx) { }
}
