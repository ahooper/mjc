package ca.nevdull.mjc1.compiler;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.TerminalNode;

import ca.nevdull.mjc1.compiler.MJ1Parser.AddExpressionContext;
import ca.nevdull.mjc1.compiler.MJ1Parser.AndThenExpressionContext;
import ca.nevdull.mjc1.compiler.MJ1Parser.AssignmentContext;
import ca.nevdull.mjc1.compiler.MJ1Parser.BitAndExpressionContext;
import ca.nevdull.mjc1.compiler.MJ1Parser.BitExclExpressionContext;
import ca.nevdull.mjc1.compiler.MJ1Parser.BitOrExpressionContext;
import ca.nevdull.mjc1.compiler.MJ1Parser.BlockContext;
import ca.nevdull.mjc1.compiler.MJ1Parser.CallExpressionContext;
import ca.nevdull.mjc1.compiler.MJ1Parser.CompareExpressionContext;
import ca.nevdull.mjc1.compiler.MJ1Parser.CompilationUnitContext;
import ca.nevdull.mjc1.compiler.MJ1Parser.CompoundStatementContext;
import ca.nevdull.mjc1.compiler.MJ1Parser.EqualExpressionContext;
import ca.nevdull.mjc1.compiler.MJ1Parser.ExpressionContext;
import ca.nevdull.mjc1.compiler.MJ1Parser.ExpressionListContext;
import ca.nevdull.mjc1.compiler.MJ1Parser.ExpressionStatementContext;
import ca.nevdull.mjc1.compiler.MJ1Parser.ExternalDeclarationContext;
import ca.nevdull.mjc1.compiler.MJ1Parser.FieldDeclarationContext;
import ca.nevdull.mjc1.compiler.MJ1Parser.FormalParameterContext;
import ca.nevdull.mjc1.compiler.MJ1Parser.FormalParameterListContext;
import ca.nevdull.mjc1.compiler.MJ1Parser.IdentifierPrimaryContext;
import ca.nevdull.mjc1.compiler.MJ1Parser.IfStatementContext;
import ca.nevdull.mjc1.compiler.MJ1Parser.IndexExpressionContext;
import ca.nevdull.mjc1.compiler.MJ1Parser.LocalVariableDeclarationContext;
import ca.nevdull.mjc1.compiler.MJ1Parser.MethodDeclarationContext;
import ca.nevdull.mjc1.compiler.MJ1Parser.MultiplyExpressionContext;
import ca.nevdull.mjc1.compiler.MJ1Parser.NotExpressionContext;
import ca.nevdull.mjc1.compiler.MJ1Parser.OrElseExpressionContext;
import ca.nevdull.mjc1.compiler.MJ1Parser.ParenthesizedContext;
import ca.nevdull.mjc1.compiler.MJ1Parser.PrefixExpressionContext;
import ca.nevdull.mjc1.compiler.MJ1Parser.PrimaryContext;
import ca.nevdull.mjc1.compiler.MJ1Parser.PrimaryExpressionContext;
import ca.nevdull.mjc1.compiler.MJ1Parser.ReturnStatementContext;
import ca.nevdull.mjc1.compiler.MJ1Parser.ReturnTypeContext;
import ca.nevdull.mjc1.compiler.MJ1Parser.StatementContext;
import ca.nevdull.mjc1.compiler.MJ1Parser.TypeContext;
import ca.nevdull.mjc1.compiler.MJ1Parser.VariableDeclaratorContext;
import ca.nevdull.mjc1.compiler.MJ1Parser.VariableDeclaratorIdContext;
import ca.nevdull.mjc1.compiler.MJ1Parser.VariableDeclaratorsContext;
import ca.nevdull.mjc1.compiler.MJ1Parser.WhileStatementContext;

public class CodePass extends MJ1BaseVisitor<CodeSym> {

	private String modulePrefix;

	private CodeGen gen;

	public CodePass(String fileName) {
		int x = fileName.lastIndexOf('.');
		modulePrefix = (x > 0) ? fileName.substring(0, x)
							   : fileName;
		gen = new CodeGen(modulePrefix);
	}

	// Variable mapping
	
	HashMap<Symbol,CodeSym> symRef = new HashMap<Symbol,CodeSym>();
	
//def_globalarray(id, type, size, descr) ::= <<
//; <descr>
//@<id> = global [<size> x i32] zeroinitializer
//>>
//
//def_array(reg, id, type, size, descr) ::= <<
//; <descr>
//<size>
//%r<reg> = alloca i32, i32 %<size.reg>
//>>
//
//assign_array(sym, index, rhs, descr, tmp1, tmp2) ::= <<
//; <descr>
//<rhs>
//<index>
//<array_ptr(reg=tmp1, ...)>
//%r<tmp2> = getelementptr i32* %r<tmp1>, i32 %r<index.reg>
//store i32 %r<rhs.reg>, i32* %r<tmp2>
//>>
//
////don't leak var creation/naming into generator
//array_ptr(reg, sym) ::= <<
//; array_ptr.reg=<reg>
//%r<reg> = bitcast <sym.type:array_type()>* <if(sym.global)>@<else>%<endif><sym.name> to i32*
//>>
//
//array_type(type) ::= "[<type.sizeExpr> x i32]"

	// Parse tree visitor
	
	private Token getChildToken(ParseTree node, int index) {
		return ((TerminalNode)node.getChild(index)).getSymbol();
	}

	@Override
	public CodeSym visitCompilationUnit(CompilationUnitContext ctx) {
//declare i32 @printf(i8 *, ...)
	    // externalDeclaration+ EOF
		// all fieldDeclarations first, then all methodDeclarations
		for (ExternalDeclarationContext e : ctx.externalDeclaration()) {
			FieldDeclarationContext f = e.fieldDeclaration();
			if (f != null) visit(f);
		}
		for (ExternalDeclarationContext e : ctx.externalDeclaration()) {
			MethodDeclarationContext m = e.methodDeclaration();
			if (m != null) visit(m);
		}
		// If string table has to be positioned before code that uses it, the gathering
		// will have to move to DefinitionPass.
		gen.emitStringTable();
		return null;
	}

	@Override
	public CodeSym visitMethodDeclaration(MethodDeclarationContext ctx) {
        // returnType Identifier '(' formalParameterList? ')' dimensions block
        // don't need to visit returnType in this pass
		String fname = ctx.Identifier().getText();
        Type rtype = ctx.def.getType();
        if (rtype == null) rtype = Type.voidType;
        FormalParameterListContext fpl = ctx.formalParameterList();
	    ArrayList<CodeSym> fps = new ArrayList<CodeSym>();
        if (fpl != null) {
		    for (FormalParameterContext fp : fpl.formalParameter()) {
		    	VariableDeclaratorIdContext vdi = fp.variableDeclaratorId();
				String pid = vdi.Identifier().getSymbol().getText();
		        Type ptype = fp.type().tipe;
				CodeSym p = gen.makeArgument(pid, ptype);
		    	fps.add(p);
		    }
        }
        CodeSym def = gen.beginFunction(fname, fps, rtype);
        symRef.put(ctx.def, def);
        // store argument values in stack fields
        Iterator<CodeSym> fpit = fps.iterator();
        if (fpl != null) {
		    for (FormalParameterContext fp : fpl.formalParameter()) {
		        Type fptype = fp.type().tipe;
		    	VariableDeclaratorIdContext vdi = fp.variableDeclaratorId();
				String vid = vdi.Identifier().getSymbol().getText();
				CodeSym f = gen.emitStackField(vid, fptype);
				symRef.put(vdi.def, f);
		    	gen.emitStore(fpit.next(), f);
		    }
        }
        // method body
        BlockContext blk = ctx.block();
        visit(blk);
        // fall through return
        if (rtype == Type.voidType ) {
        	gen.emitReturn();
        } else {
        	gen.emitReturn(gen.makeLiteral(0));  // TODO other return types
        }
        gen.endFunction();
        return null;
    }
	
    @Override
	public CodeSym visitFieldDeclaration(FieldDeclarationContext ctx) {
    	// type variableDeclarators ';'
        // don't need to visit type in this pass
        VariableDeclaratorsContext d = ctx.variableDeclarators();
        for (VariableDeclaratorContext v : d.variableDeclarator()) {
    		VariableDeclaratorIdContext vdi = v.variableDeclaratorId();
	        Type vtype = vdi.def.getType();
			String vid = vdi.Identifier().getSymbol().getText();
			CodeSym f = gen.emitGlobalField(vid, vtype);
			symRef.put(vdi.def, f);
    		ExpressionContext init = v.expression();   // initializer
			if (init != null) {
				visit(init);
				//TODO store initializer
			}
        }
        return null;
	}   
    
	@Override
    public CodeSym visitLocalVariableDeclaration(LocalVariableDeclarationContext ctx) {
        // type variableDeclarators ';'
        // don't need to visit type in this pass
        VariableDeclaratorsContext d = ctx.variableDeclarators();
        for (VariableDeclaratorContext v : d.variableDeclarator()) {
    		VariableDeclaratorIdContext vdi = v.variableDeclaratorId();
	        Type vtype = vdi.def.getType();
			String vid = vdi.Identifier().getSymbol().getText();
			CodeSym f = gen.emitStackField(vid, vtype);
			symRef.put(vdi.def, f);
    		ExpressionContext init = v.expression();   // initializer
			if (init != null) {
				visit(init);
				//TODO store initializer
			}
        }
        return null;
    }
    
	@Override
	public CodeSym visitCompoundStatement(CompoundStatementContext ctx) {
    	// block
        BlockContext b = ctx.block();
		visit(b);
        return null;
    }
    
	@Override
    public CodeSym visitIfStatement(IfStatementContext ctx) {
    	// 'if' '(' expression ')' statement ('else' statement)?
    	ExpressionContext e = ctx.expression();
		CodeSym cond = visit(e);
		checkBoolean(cond,getChildToken(ctx,3));
		String tBlock = gen.nextBlock("t");
		String fBlock = gen.nextBlock("f");
		gen.emitBranch(cond, tBlock, fBlock);
    	Iterator<StatementContext> s = ctx.statement().iterator();
    	gen.beginBlock(tBlock);
    	visit(s.next());
    	if (s.hasNext()) {
    		String eBlock = gen.nextBlock("e");
    		gen.emitBranch(eBlock);
        	gen.beginBlock(fBlock);
        	visit(s.next());
        	gen.beginBlock(eBlock);
    	} else {
        	gen.beginBlock(fBlock);
    	}
        return null;
    }
    
	@Override
    public CodeSym visitWhileStatement(WhileStatementContext ctx) {
    	// 'while' '(' expression ')' statement
    	String wBlock = gen.nextBlock("w");
    	String tBlock = gen.nextBlock("t");
    	String fBlock = gen.nextBlock("f");
     	gen.emitBranch(wBlock);
     	gen.beginBlock(wBlock);
    	ExpressionContext e = ctx.expression();
    	CodeSym cond = visit(e);
		checkBoolean(cond,getChildToken(ctx,3));
		gen.emitBranch(cond, tBlock, fBlock);
    	gen.beginBlock(tBlock);
    	visit(ctx.statement());
    	gen.emitBranch(wBlock);
    	gen.beginBlock(fBlock);
        return null;
    }
    
	@Override
    public CodeSym visitReturnStatement(ReturnStatementContext ctx) {
    	// 'return' expression? ';'
    	// Find enclosing method's return type
		MethodDeclarationContext md = (MethodDeclarationContext)findAncestor(ctx, MethodDeclarationContext.class);
		Type rt = md.def.getType();
        ExpressionContext e = ctx.expression();
        if (e != null) {
        	CodeSym r = visit(e);
        	if (r.getType() != rt) {
        		// TODO coercion of return type
        		Main.error(e.start,"expected return type is "+rt);
        	}
        	gen.emitReturn(r);
        } else {
        	if (rt != null && rt != Type.voidType && rt != Type.errorType) {
        		Main.error(ctx.start,"expected return type is "+rt);
        	}
        	gen.emitReturn();
        }
        return null;
    }

	private ParserRuleContext findAncestor(ParserRuleContext ctx, Class<?> type) {
		for (ParserRuleContext ancestor = ctx;  ancestor != null;  ancestor = ancestor.getParent()) {
    		if (type.isInstance(ancestor)) {
    	        return ancestor;
    		}
    	}
		return null;
	}
    
	@Override
    public CodeSym visitNullStatement(MJ1Parser.NullStatementContext ctx) {
    	// ';'
        return null;
    }
    
	@Override
    public CodeSym visitExpressionStatement(ExpressionStatementContext ctx) {
    	// expression ';'
        ExpressionContext e = ctx.expression();
		visit(e);
        return null;
    }
    
	@Override
    public CodeSym visitPrimaryExpression(PrimaryExpressionContext ctx) {
    	// primary
        PrimaryContext p = ctx.primary();
        CodeSym r = visit(p);
        return r;
    }
    
    private Type maxNumericType(Type left, Type right) {
    	if (left == Type.errorType || right == Type.errorType) return Type.errorType;
    	// LATER unboxing
    	if (left == Type.doubleType || right == Type.doubleType) return Type.doubleType;
    	if (left == Type.floatType || right == Type.floatType) return Type.floatType;
    	if (left == Type.longType || right == Type.longType) return Type.longType;
    	// TODO short, byte, char
    	return Type.intType;
    }
    
    private CodeSym widenNumericType(Type max, CodeSym r, Token op) {
    	if (max == Type.errorType || r.getType() == Type.errorType) return r;
    	if (r.isAddress()) r = gen.emitLoad(r);
    	if (r.getType() == max) return r;  // no widening if already required type
    	// LATER unboxing
    	if (   max == Type.doubleType
        	&& r.getType() == Type.floatType ) {
    		return gen.emitWidenFloat(max, r);
        }
    	if (   (max == Type.floatType || max == Type.doubleType)
    		&& (r.getType() == Type.intType || r.getType() == Type.longType
    		    || r.getType() == Type.shortType || r.getType() == Type.byteType) ) {
    		return gen.emitIntToFloat(max, r);
    	}
    	if (   (max == Type.intType || max == Type.longType)
        	&& r.getType() == Type.charType) {
    		return gen.emitWidenUnsigned(max, r);
        }
    	if (max == Type.intType || max == Type.longType) {
    		return gen.emitWidenInt(max, r);
        }
    	Main.error(op,r.getType()+" operand cannot be widened to "+max);
    	return r;
    }

	@Override
	public CodeSym visitIndexExpression(IndexExpressionContext ctx) {
		// expression '[' expression ']'
    	List<ExpressionContext> list = ctx.expression();
		Iterator<ExpressionContext> expit = list.iterator();
    	ExpressionContext expa = expit.next();
    	ExpressionContext expb = expit.next();
    	CodeSym ra = visit(expa);
    	CodeSym rb = visit(expb);
    	Token opToken = getChildToken(ctx,1);
    	if (!(ra.getType() instanceof ArrayType)) {
    		Main.error(opToken,"index on non-array type "+ra.getType());
    		return null;
    	}
    	rb = widenNumericType(Type.intType, rb, opToken);
    	return gen.emitIndex(ra, rb);
	}

	@Override
	public CodeSym visitCallExpression(CallExpressionContext ctx) {
		// expression '(' expressionList? ')'
		//TODO this is a mess! must be a better way. maybe treat a method as a type
		ExpressionContext me = ctx.expression();
		CodeSym meth = visit(me);
    	if (!(me instanceof PrimaryExpressionContext)) {
			Main.error(me.stop,"is not a method");
			return null;
    	}
    	PrimaryContext mp = ((PrimaryExpressionContext)me).primary();
    	if (!(mp instanceof IdentifierPrimaryContext)) {
			Main.error(me.stop,"is not a method");
			return null;
    	}
    	IdentifierPrimaryContext mi = (IdentifierPrimaryContext)mp;
    	if (mi.def == null) return null; // already flagged undefined
    	if (!(mi.def instanceof MethodSymbol)) {
			Main.error(me.stop,"is not a method");
			return null;
    	}
    	MethodSymbol ms = (MethodSymbol)mi.def;
        CodeSym m = symRef.get(mi.def);
        assert m != null;
    	System.out.println("call target class "+me.getClass().getSimpleName()+" reg "+m);
    	Iterator<Symbol> formals = ms.getParameters().iterator();
    	
        Type type = ms.getType();
        ExpressionListContext el = ctx.expressionList();
	    ArrayList<CodeSym> pl = new ArrayList<CodeSym>();
	    ArrayList<Type> fptl = new ArrayList<Type>();
        if (el != null) {
		    for (ExpressionContext e : el.expression()) {
		    	if (!formals.hasNext()) {
		    		Main.error(e.getStart(),"excess parameters provided");
		    		break;
		    	}
		    	Symbol f = formals.next();
		    	fptl.add(f.getType());
		    	CodeSym p = visit(e);
		    	// TODO check formal parameter type
				pl.add(p);
		    }
	    	if (formals.hasNext()) {
	    		Main.error(ctx.getStop(), "insufficient parameters");
	    	}
        }
        return gen.emitCall(m, type, pl);
	}

	@Override
	public CodeSym visitPrefixExpression(PrefixExpressionContext ctx) {
		// ('+'|'-'/*|'++'|'--'*/) expression
		// TODO Auto-generated method stub
		return super.visitPrefixExpression(ctx);
	}

	@Override
	public CodeSym visitNotExpression(NotExpressionContext ctx) {
		// ('~'|'!') expression
		// TODO Auto-generated method stub
		// ~x = x xor -1
		// !x = x xor true
		// -x = 0 - x
		return super.visitNotExpression(ctx);
	}

	private CodeSym binaryAithmeticOperation(ExpressionContext ctx, List<ExpressionContext> list, ParseTree opNode) {
		Iterator<ExpressionContext> expit = list.iterator();
    	ExpressionContext expa = expit.next();
    	ExpressionContext expb = expit.next();
    	CodeSym ra = visit(expa);
    	CodeSym rb = visit(expb);
    	Token opToken = ((TerminalNode)opNode).getSymbol();
    	Type type = maxNumericType(ra.getType(),rb.getType());
    	ra = widenNumericType(type, ra, opToken);
    	rb = widenNumericType(type, rb, opToken);
		return gen.emitBinaryOperation(opToken.getText(), ra, rb);
	}
    
	@Override
    public CodeSym visitMultiplyExpression(MultiplyExpressionContext ctx) {
    	// expression ('*'|'/'|'%') expression
    	return binaryAithmeticOperation(ctx, ctx.expression(), ctx.getChild(1));
    }
    
	@Override
    public CodeSym visitAddExpression(AddExpressionContext ctx) {
    	// expression ('+'|'-') expression
    	return binaryAithmeticOperation(ctx, ctx.expression(), ctx.getChild(1));
    }
    
	@Override
    public CodeSym visitCompareExpression(CompareExpressionContext ctx) {
    	// expression ('<=' | '>=' | '>' | '<') expression
    	List<ExpressionContext> list = ctx.expression();
		Iterator<ExpressionContext> expit = list.iterator();
    	ExpressionContext expa = expit.next();
    	ExpressionContext expb = expit.next();
    	CodeSym ra = visit(expa);
    	CodeSym rb = visit(expb);
    	Token opToken = getChildToken(ctx,1);
    	Type type = maxNumericType(ra.getType(),rb.getType());
    	ra = widenNumericType(type, ra, opToken);
    	rb = widenNumericType(type, rb, opToken);
		return gen.emitCompareOperation(opToken.getText(), ra, rb);
    }
    
	@Override
    public CodeSym visitEqualExpression(EqualExpressionContext ctx) {
    	// expression ('==' | '!=') expression
    	List<ExpressionContext> list = ctx.expression();
		Iterator<ExpressionContext> expit = list.iterator();
    	ExpressionContext expa = expit.next();
    	ExpressionContext expb = expit.next();
    	CodeSym ra = visit(expa);
    	CodeSym rb = visit(expb);
    	Token opToken = getChildToken(ctx,1);
    	// LATER reference comparison
    	Type type = maxNumericType(ra.getType(),rb.getType());
    	ra = widenNumericType(type, ra, opToken);
    	rb = widenNumericType(type, rb, opToken);
		return gen.emitCompareOperation(opToken.getText(), ra, rb);
    }

	@Override
    public CodeSym visitBitAndExpression(BitAndExpressionContext ctx) {
    	// expression '&' expression
    	return binaryAithmeticOperation(ctx, ctx.expression(), ctx.getChild(1));
    }
    
	@Override
    public CodeSym visitBitExclExpression(BitExclExpressionContext ctx) {
    	// expression '^' expression
    	return binaryAithmeticOperation(ctx, ctx.expression(), ctx.getChild(1));
    }
    
	@Override
    public CodeSym visitBitOrExpression(BitOrExpressionContext ctx) {
    	// expression '|' expression
    	return binaryAithmeticOperation(ctx, ctx.expression(), ctx.getChild(1));
    }

	private void checkBoolean(CodeSym cond, Token token) {
    	if (!(cond.getType() == Type.booleanType || cond.getType() == Type.errorType)) {
    		Main.error(token, "condition type must be boolean");
    	}
	}
    
	@Override
    public CodeSym visitAndThenExpression(AndThenExpressionContext ctx) {
    	// expression '&&' expression
    	List<ExpressionContext> list = ctx.expression();
		Iterator<ExpressionContext> expit = list.iterator();
    	ExpressionContext expa = expit.next();
    	ExpressionContext expb = expit.next();
    	CodeSym ra = visit(expa);
		checkBoolean(ra,expa.getStop());
		String cBlock = gen.getCurrentBlockName();
		String aBlock = gen.nextBlock("a");
		String eBlock = gen.nextBlock("e");
		gen.emitBranch(ra, aBlock, eBlock);
    	gen.beginBlock(aBlock);
    	CodeSym rb = visit(expb);
		checkBoolean(rb,expb.getStop());
   		gen.emitBranch(eBlock);
       	gen.beginBlock(eBlock);
       	return gen.emitJoin(gen.makeLiteral(false), cBlock, rb, aBlock);
    }
    
	@Override
    public CodeSym visitOrElseExpression(OrElseExpressionContext ctx) {
    	// expression '||' expression
    	List<ExpressionContext> list = ctx.expression();
		Iterator<ExpressionContext> expit = list.iterator();
    	ExpressionContext expa = expit.next();
    	ExpressionContext expb = expit.next();
    	CodeSym ra = visit(expa);
		checkBoolean(ra,expa.getStop());
		String cBlock = gen.getCurrentBlockName();
		String oBlock = gen.nextBlock("o");
		String eBlock = gen.nextBlock("e");
		gen.emitBranch(ra, eBlock, oBlock);
    	gen.beginBlock(oBlock);
    	CodeSym rb = visit(expb);
		checkBoolean(rb,expb.getStop());
   		gen.emitBranch(eBlock);
       	gen.beginBlock(eBlock);
       	return gen.emitJoin(gen.makeLiteral(true), cBlock, rb, oBlock);
    }
        
	@Override
    public CodeSym visitAssignment(AssignmentContext ctx) {
		// <assoc=right> expression
		//      (   '='
		//      )
		//      expression
		Iterator<ExpressionContext> expit = ctx.expression().iterator();
    	ExpressionContext expa = expit.next();
    	ExpressionContext expb = expit.next();
    	CodeSym ra = visit(expa);
    	CodeSym rb = visit(expb);
    	Token opToken = getChildToken(ctx,1);
    	Type tipe = ra.getType();
    	System.out.println("assignment target class "+expa.getClass().getSimpleName()+" type "+tipe+" address "+ra.isAddress());
    	rb = widenNumericType(tipe, rb, opToken);
    	// TODO other assignment conversions
    	gen.emitStore(rb, ra);
		return rb;
	}

	@Override
	public CodeSym visitParenthesized(ParenthesizedContext ctx) {
    	// '(' expression ')'
        ExpressionContext e = ctx.expression();
        CodeSym r = visit(e);
        return r;
    }
    
	@Override
    public CodeSym visitIdentifierPrimary(IdentifierPrimaryContext ctx) {
    	// Identifier
    	CodeSym r;
    	Token id = ctx.Identifier().getSymbol();
		ctx.def = ctx.refScope.find(id);
    	if (ctx.def == null) {
    		Main.error(id," is not defined");
        	r = gen.makeError();
    	} else if (ctx.def.getType() == Type.errorType){
        	r = gen.makeError();
    	} else {
    		Scope defScope = ctx.def.getDefiningScope();
    		// if block scope, token index of definition must be before token
    		// index of reference
            if ( ctx.refScope instanceof BlockScope
                && defScope instanceof BlockScope
                && id.getTokenIndex() < ctx.def.token.getTokenIndex() ) {
            	Main.error(id, "forward reference to local variable");
            }
    		System.out.println(ctx.def.getName()+" type "+ctx.def.getType().toString());
            r = symRef.get(ctx.def);
            assert r != null;
    	}
        return r;
    }

	@Override
	public CodeSym visitIntegerPrimary(MJ1Parser.IntegerPrimaryContext ctx) {
    	// IntegerLiteral
		String text = ctx.IntegerLiteral().getText();
		int length = text.length();
		char last = text.charAt(length-1);
		if (last == 'l' || last == 'L') {
			return gen.makeLiteral(Long.decode(text.substring(0,length-1)));
		} else {
			return gen.makeLiteral(Integer.decode(text));
		}
    }
    
	@Override
    public CodeSym visitFloatPrimary(MJ1Parser.FloatPrimaryContext ctx) {
    	// FloatingPointLiteral
 		String text = ctx.FloatingPointLiteral().getText();
		int length = text.length();
		char last = text.charAt(length-1);
		if (last == 'f' || last == 'F') {
			return gen.makeLiteral(Float.parseFloat(text.substring(0,length-1)));
		} else if (last == 'd' || last == 'D') {
			return gen.makeLiteral(Double.parseDouble(text.substring(0,length-1)));
		} else {
			return gen.makeLiteral(Float.parseFloat(text));
		}
    }
    
	@Override
    public CodeSym visitCharPrimary(MJ1Parser.CharPrimaryContext ctx) {
    	// CharacterLiteral
		String text = ctx.CharacterLiteral().getText();
    	String c = decodeEscapes(text.substring(1, text.length()-1)/*strip quotes*/);
		return gen.makeLiteral(c.charAt(1));
    }
    
	@Override
    public CodeSym visitStringPrimary(MJ1Parser.StringPrimaryContext ctx) {
    	// StringLiteral
    	String text = ctx.StringLiteral().getText();
    	String s = decodeEscapes(text.substring(1, text.length()-1)/*strip quotes*/);
    	CodeSym r = gen.makeString(s);
    	// TODO create String object
    	return r;
    }

	private String decodeEscapes(String s) {
		StringBuilder t = new StringBuilder(s);
    	for (int x = 0; x < t.length()-1; x += 1) {
    		if (t.charAt(x) == '\\') {
    			int y = x+1, e, d;
    			char c = t.charAt(y++);
    			switch (c) {
    			case 'b':
    				c = '\b';  break;
    			case 't':
    				c = '\t';  break;
    			case 'n':
    				c = '\n';  break;
    			case 'f':
    				c = '\f';  break;
    			case 'r':
    				c = '\r';  break;
    			case '"':
    			case '\'':
    			case '\\':
    				/* c = c; */  break;
    			case '0': case '1': case '2': case '3':
    			case '4': case '5': case '6': case '7':
    				e = Character.digit(c, 8);
    				while (y < t.length() && y-x < 4) {
    					c = t.charAt(y);
    					d = Character.digit(c, 8);
    					if (d < 0) break; // not an octal digit
    					y++;
    	    			e = (e << 3) + d;
						//Main.debug("escape oct %c %d %d",c,d,e);
    				}
    				//Main.debug("escape %s %c %d",t.substring(x,y),c,e);
    				c = (char)e;
    				break;
    			case 'u':
    				e = 0;
    				while (y < t.length() && y-x < 6) {
    					c = t.charAt(y++);
    					d = Character.digit(c, 16);
    					if (d < 0) break; // not a hexadecimal digit
						e = (e << 4) + d;
						//Main.debug("escape hex %c %d %d",c,d,e);
    				}
    				//Main.debug("escape %s %c %d",t.substring(x,y),c,e);
    				c = (char)e;
    				break;
    			default:
    			}
				t.delete(x+1,y).setCharAt(x,c);
    		}
    	}
    	return t.toString();
	}
    
	@Override
    public CodeSym visitBooleanPrimary(MJ1Parser.BooleanPrimaryContext ctx) {
    	// BooleanLiteral
    	String text = ctx.BooleanLiteral().getText();
		return gen.makeLiteral(text.equals("true"));
    }
    
	@Override
    public CodeSym visitNullPrimary(MJ1Parser.NullPrimaryContext ctx) {
    	// 'null'
    	CodeSym r = gen.makeNull(Type.stringType);
    	// TODO correct reference type
        return r;
    }

}
