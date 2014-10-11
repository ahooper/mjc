package ca.nevdull.mjc.compiler;

import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.util.HashMap;
import java.util.Map;

import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.tree.ParseTreeProperty;

import ca.nevdull.mjc.compiler.MJParser.VariableDeclaratorIdContext;
import ca.nevdull.mjc.util.ListBuilder;

public class GeneratePass extends MJBaseListener {
    ParseTreeProperty<Symbol> symbols;
    ParseTreeProperty<Type> types = new ParseTreeProperty<Type>();
    ParseTreeProperty<String> rands = new ParseTreeProperty<String>();

	public GeneratePass(ParseTreeProperty<Symbol> symbols, ParseTreeProperty<Type> types) {
		super();
		this.symbols = symbols;
		this.types = types;
	}
	
    private void setType(ParserRuleContext node, Type type) {
    	types.put(node, type);
    }
    
    private Type getType(ParserRuleContext node) {
    	Type t = types.get(node);
    	assert t != null;
    	return t;
    }
      
    private void promoteType(ParserRuleContext node, ParserRuleContext subNode) {
    	setType(node, getType(subNode));
    }
    
    int reg = 1;
    public int nextreg() {
    	return reg++;
    }
    
    int stringnum = 1;
    Map<String, Integer> strings = new HashMap<String, Integer>();
    public int stringId(String s) {
    	Integer id = strings.get(s);
    	if (id == null) {
        	id = new Integer(stringnum++);
        	strings.put(s, id);
    	}
    	return id.intValue();
    }
    
    class ClassDest {
    	ClassSymbol symbol;
    	ClassDest enclosingClass;
    	ListBuilder vtable;
    	ListBuilder instance;
    	ListBuilder methods;
    	PrintStream out;
 		public ClassDest(ClassSymbol symbol, ClassDest enclosingClass) {
			super();
			this.symbol = symbol;
			this.enclosingClass = enclosingClass;
	    	this.vtable = new ListBuilder();
	    	this.instance = new ListBuilder();
	    	this.methods = new ListBuilder();
	    	System.out.println("ClassDest "+symbol);
	    	instance.add("struct ",symbol.getName(),"_struct"," {\n");
		}
		public ClassSymbol getSymbol() {
			return symbol;
		}
		private String getFileName() {
 			String thisName = symbol.getName();
			if (enclosingClass == null) return thisName;
			return enclosingClass.getFileName()+"$"+thisName;
 		}
 		
		public ClassDest close() {
	    	instance.add("};\ntypedef struct ",symbol.getName(),"_struct *",symbol.getName(),";\n");
			String fileName = getFileName()+".c";
			try {
				out = new PrintStream(fileName);
				vtable.render(out);
				instance.render(out);
				methods.render(out);
				out.close();
			} catch (FileNotFoundException excp) {
				Compiler.error(null,"Unable to open "+fileName);
			}
			return enclosingClass;
		}
    }
    ClassDest currDest = null;
    

	@Override public void exitCompilationUnit(MJParser.CompilationUnitContext ctx) { }
	@Override public void exitTypeDeclaration(MJParser.TypeDeclarationContext ctx) { }
	@Override public void exitModifier(MJParser.ModifierContext ctx) { }
	@Override public void exitClassOrInterfaceModifier(MJParser.ClassOrInterfaceModifierContext ctx) { }
	@Override public void enterClassDeclaration(MJParser.ClassDeclarationContext ctx) {
		Symbol s = symbols.get(ctx);
		currDest = new ClassDest((ClassSymbol)symbols.get(ctx), currDest);
	}
	@Override public void exitClassDeclaration(MJParser.ClassDeclarationContext ctx) {
		currDest = currDest.close();
	}
	@Override public void exitClassBody(MJParser.ClassBodyContext ctx) { }
	@Override public void exitMemberClassBodyDeclaration(MJParser.MemberClassBodyDeclarationContext ctx) { }
	@Override public void exitBlockClassBodyDeclaration(MJParser.BlockClassBodyDeclarationContext ctx) { }
	@Override public void exitEmptyClassBodyDeclaration(MJParser.EmptyClassBodyDeclarationContext ctx) { }
	@Override public void exitMemberDeclaration(MJParser.MemberDeclarationContext ctx) { }
	@Override public void enterMethodDeclaration(MJParser.MethodDeclarationContext ctx) {
		//TODO type
		currDest.methods.add(currDest.getSymbol().getName(),"_",symbols.get(ctx).getName());
	}
	@Override public void exitMethodDeclaration(MJParser.MethodDeclarationContext ctx) {
		currDest.methods.add("\n");
	}
	@Override public void exitConstructorDeclaration(MJParser.ConstructorDeclarationContext ctx) { }
	@Override public void exitFieldDeclaration(MJParser.FieldDeclarationContext ctx) {	
		currDest.methods.add(";\n");
	}
	@Override public void exitVariableDeclarators(MJParser.VariableDeclaratorsContext ctx) { }
	@Override public void exitVariableDeclarator(MJParser.VariableDeclaratorContext ctx) { }
	@Override public void exitVariableDeclaratorId(MJParser.VariableDeclaratorIdContext ctx) {
		Symbol var = symbols.get(ctx);
		defineVariable(var, var.getType());
	}
	private void defineVariable(Symbol var, Type t) {
		if (t instanceof ArrayType) {
			defineVariable(var, ((ArrayType)t).getElementType());
			currDest.methods.add("[]");
		} else if (t instanceof ClassSymbol) {
			currDest.methods.add(t.getName()," ",var.getName());
		} else if (t instanceof PrimitiveType) {
			currDest.methods.add(t.getName()," ",var.getName());
		} else {
			currDest.methods.add("errorType ",var.getName());
		}
	}
	@Override public void exitSimpleVariableInitializer(MJParser.SimpleVariableInitializerContext ctx) { }
	@Override public void exitArrayVariableInitializer(MJParser.ArrayVariableInitializerContext ctx) { }
	@Override public void exitArrayInitializer(MJParser.ArrayInitializerContext ctx) { }
	@Override public void exitPrimitType(MJParser.PrimitTypeContext ctx) { }
	@Override public void exitObjectType(MJParser.ObjectTypeContext ctx) { }
	@Override public void exitArrayDimension(MJParser.ArrayDimensionContext ctx) { }
	@Override public void exitClassOrInterfaceType(MJParser.ClassOrInterfaceTypeContext ctx) { }
	@Override public void exitBooleanType(MJParser.BooleanTypeContext ctx) { }
	@Override public void exitDoubleType(MJParser.DoubleTypeContext ctx) { }
	@Override public void exitCharType(MJParser.CharTypeContext ctx) { }
	@Override public void exitFloatType(MJParser.FloatTypeContext ctx) { }
	@Override public void exitIntType(MJParser.IntTypeContext ctx) { }
	@Override public void exitShortType(MJParser.ShortTypeContext ctx) { }
	@Override public void exitByteType(MJParser.ByteTypeContext ctx) { }
	@Override public void exitLongType(MJParser.LongTypeContext ctx) { }
	@Override public void enterFormalParameters(MJParser.FormalParametersContext ctx) {
		currDest.methods.add("(",currDest.getSymbol().getName()," ","this");
	}
	@Override public void exitFormalParameters(MJParser.FormalParametersContext ctx) {
		currDest.methods.add(") ");
	}
	@Override public void exitFormalParameterList(MJParser.FormalParameterListContext ctx) { }
	@Override public void enterFormalParameter(MJParser.FormalParameterContext ctx) {
		currDest.methods.add(", ");
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
		String rand = "l"+nextreg();
		currDest.methods.add(rand,"=",text,";\n");
		setType(ctx, t);
	}
	@Override public void enterBlock(MJParser.BlockContext ctx) {
		currDest.methods.add("{\n");
	}
	@Override public void exitBlock(MJParser.BlockContext ctx) {
		currDest.methods.add("}\n");
	}
	@Override public void exitBlockStatement(MJParser.BlockStatementContext ctx) { }
	@Override public void exitLocalVariableDeclarationStatement(MJParser.LocalVariableDeclarationStatementContext ctx) { }
	@Override public void exitLocalVariableDeclaration(MJParser.LocalVariableDeclarationContext ctx) {
		currDest.methods.add(";\n");
	}
	@Override public void exitWhileStatement(MJParser.WhileStatementContext ctx) { }
	@Override public void exitExpressionStatement(MJParser.ExpressionStatementContext ctx) { }
	@Override public void exitEmnptyStatement(MJParser.EmnptyStatementContext ctx) { }
	@Override public void exitReturnStatement(MJParser.ReturnStatementContext ctx) { }
	@Override public void exitLabelStatement(MJParser.LabelStatementContext ctx) { }
	@Override public void exitBlkStatement(MJParser.BlkStatementContext ctx) { }
	@Override public void exitIfStatement(MJParser.IfStatementContext ctx) { }
	@Override public void exitParExpression(MJParser.ParExpressionContext ctx) { }
	@Override public void exitExpressionList(MJParser.ExpressionListContext ctx) { }
	@Override public void exitStatementExpression(MJParser.StatementExpressionContext ctx) { }
	@Override public void exitConstantExpression(MJParser.ConstantExpressionContext ctx) { }
	@Override public void exitCompareExpression(MJParser.CompareExpressionContext ctx) { }
	@Override public void exitExclExpression(MJParser.ExclExpressionContext ctx) {
		String op = ctx.getChild(1).getText();
		binaryOperator(ctx, ctx.expression(0), op, ctx.expression(1)); 
	}
	@Override public void exitAddExpression(MJParser.AddExpressionContext ctx) {
		String op = ctx.getChild(1).getText();
		binaryOperator(ctx, ctx.expression(0), op, ctx.expression(1)); 
	}

	public void binaryOperator(MJParser.ExpressionContext ctx, MJParser.ExpressionContext left, String op, MJParser.ExpressionContext right) {
		String rand = "e"+nextreg();
		currDest.methods.add(rand,"=",rands.get(left),op,rands.get(right),";\n");
		rands.put(ctx, rand);
	}

	@Override public void exitAssignExpression(MJParser.AssignExpressionContext ctx) {
		currDest.methods.add(rands.get(ctx.expression(0)),"=",rands.get(ctx.expression(1)),";\n");
		rands.put(ctx, rands.get(ctx.expression(0)));
	}
	@Override public void exitNotExpression(MJParser.NotExpressionContext ctx) { }
	@Override public void exitCallExpression(MJParser.CallExpressionContext ctx) {
		System.out.println("exitCallExpression "+ctx.getText());
		Type type = getType(ctx.expression());
		System.out.println("CallExpression "+type.toString());
	}
	@Override public void exitOrExpression(MJParser.OrExpressionContext ctx) {
		//TODO types
		binaryOperator(ctx, ctx.expression(0), "|", ctx.expression(1));
	}
	@Override public void exitIndexExpression(MJParser.IndexExpressionContext ctx) { }
	@Override public void exitEqualExpression(MJParser.EqualExpressionContext ctx) { }
	@Override public void exitMultExpression(MJParser.MultExpressionContext ctx) {
		//TODO types
		String op = ctx.getChild(1).getText();
		binaryOperator(ctx, ctx.expression(0), op, ctx.expression(1)); 
	}
	@Override public void exitCondAndExpression(MJParser.CondAndExpressionContext ctx) { }
	@Override public void exitAndExpression(MJParser.AndExpressionContext ctx) {
		//TODO types
		binaryOperator(ctx, ctx.expression(0), "&", ctx.expression(1)); 
	}
	@Override public void exitPrimExpression(MJParser.PrimExpressionContext ctx) {
		promoteType(ctx, ctx.primary());
		rands.put(ctx, rands.get(ctx.primary()));
	}
	@Override public void exitCondOrExpression(MJParser.CondOrExpressionContext ctx) { }
	@Override public void exitCastExpression(MJParser.CastExpressionContext ctx) { }
	@Override public void exitDotExpression(MJParser.DotExpressionContext ctx) {
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
	@Override public void exitLiteralPrimary(MJParser.LiteralPrimaryContext ctx) {
		promoteType(ctx, ctx.literal());
		rands.put(ctx, rands.get(ctx.literal()));
	}
	@Override public void exitSuperPrimary(MJParser.SuperPrimaryContext ctx) { }
	@Override public void exitIdentifierPrimary(MJParser.IdentifierPrimaryContext ctx) {
        Symbol sym = symbols.get(ctx);
		Type t = null;
        if (sym != null) {
        	t = sym.getType();
    		System.out.println(sym.getName()+" type "+t);
            setType(ctx, t);
    		rands.put(ctx, sym.getName());
        } else {
	        if (t == null) t = UnknownType.getInstance();
	        setType(ctx, t);
			rands.put(ctx, "null");
        }
	}
	@Override public void exitThisPrimary(MJParser.ThisPrimaryContext ctx) {
		setType(ctx, currDest.getSymbol());
		rands.put(ctx, "this");
	}
	@Override public void exitParenPrimary(MJParser.ParenPrimaryContext ctx) {
		promoteType(ctx, ctx.expression());
		rands.put(ctx, rands.get(ctx.expression()));
	}
	@Override public void exitCreator(MJParser.CreatorContext ctx) { }
	@Override public void exitCreatedName(MJParser.CreatedNameContext ctx) { }
	@Override public void exitArrayCreatorRest(MJParser.ArrayCreatorRestContext ctx) { }
	@Override public void exitClassCreatorRest(MJParser.ClassCreatorRestContext ctx) { }
	@Override public void exitArguments(MJParser.ArgumentsContext ctx) { }


}
