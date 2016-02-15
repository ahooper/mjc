package ca.nevdull.mjc1.compiler;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;

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

public class CodePass extends MJ1BaseVisitor<CodeReg> {

	private String modulePrefix;

	public CodePass(String fileName) {
		int x = fileName.lastIndexOf('.');
		modulePrefix = (x > 0) ? fileName.substring(0, x)
							   : fileName;
	}
	
	private static final String VOID = "void";

	// Code sequence labels
	
	private int labelSequence = 0;
	
	private String nextLabel() {
		return Integer.toHexString(++labelSequence);
	}
	
	// String tables

	private int stringIDSequence = 0;
	HashMap<String,String> stringTable = new HashMap<String,String>();
	
	private String nextStringID() {
		return "@s"+Integer.toHexString(++stringIDSequence);
	}
	
	private String stringType(String s) {
		return "["+s.length()+" x i16]";
	}
	
	private String stringID(String s) {
		String id = stringTable.get(s);
		if (id == null) {
			id = nextStringID();
			stringTable.put(s,id);
		}
		return id;
	}
	
	// Type mapping
	
	HashMap<Type,String> llvmTypeMap = new HashMap<Type,String>();
	{
		llvmTypeMap.put(Type.booleanType, "i1");
		llvmTypeMap.put(Type.byteType, "i8");
		llvmTypeMap.put(Type.charType, "i16");
		llvmTypeMap.put(Type.doubleType, "double");
		llvmTypeMap.put(Type.floatType, "float");
		llvmTypeMap.put(Type.intType, "i32");
		llvmTypeMap.put(Type.longType, "i64");
		llvmTypeMap.put(Type.shortType, "i16");
		llvmTypeMap.put(Type.stringType, "@string");
		llvmTypeMap.put(Type.voidType, CodePass.VOID);
		llvmTypeMap.put(Type.errorType, "error");
	}
	
	private String llvmType(Type type) {
		if (type instanceof ArrayType) {
			return "[0 x "+llvmType(((ArrayType) type).getBase())+"]";
		}
		String t = llvmTypeMap.get(type);
		if (t == null) return "error";
		return t;
	}
	
	// Operation mapping
	
	HashMap<String,String> llvmOpMap = new HashMap<String,String>();
	{
		llvmOpMap.put("++","add");
		llvmOpMap.put("--","sub");
		llvmOpMap.put("~","???");
		llvmOpMap.put("!","???");
		llvmOpMap.put("*","mul");
		llvmOpMap.put("/","sdiv");
		llvmOpMap.put("%","srem");
		llvmOpMap.put("+","add");
		llvmOpMap.put("-","sub");
		llvmOpMap.put("==","icmp eq");
		llvmOpMap.put("!=","icmp ne");
		llvmOpMap.put("<","icmp slt");
		llvmOpMap.put(">","icmp sgt");
		llvmOpMap.put("<=","icmp sle");
		llvmOpMap.put(">=","icmp sge");
		llvmOpMap.put("&","and");
		llvmOpMap.put("^","xor");
		llvmOpMap.put("|","or");
	}
	
	// Variable mapping
	
	HashMap<Symbol,CodeReg> symRef = new HashMap<Symbol,CodeReg>();
	
	// Code output
	
	private void emit(String... args) {
		emitn(args);
		System.out.println();
	}

	private void emitn(String... args) {
		for (String a : args) System.out.print(a);
	}
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
	public CodeReg visitCompilationUnit(CompilationUnitContext ctx) {
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
		emitStringTable();
		return null;
	}

	private void emitStringTable() {
		for (Entry<String, String> entry : stringTable.entrySet()) {
		    String s = entry.getKey();
		    String id = entry.getValue();
		    emitn(id," = internal constant ",stringType(s)," [");
		    String sep = "";
		    for (char c : s.toCharArray()) {
		    	emitn(sep,"i16 ",Integer.toString(c));
		    	sep = ",";
		    }
		    emit("]");
		}
	}

	@Override
	public CodeReg visitMethodDeclaration(MethodDeclarationContext ctx) {
        // returnType Identifier '(' formalParameterList? ')' dimensions block
        // don't need to visit returnType in this pass
        Type rtype = ctx.def.getType();
        String vmrt = llvmType(rtype);
        if (vmrt == null) vmrt = CodePass.VOID;
        StringBuffer refType = new StringBuffer(vmrt);
        String fName = "@"+quoteName(ctx.Identifier().getText());
        refType.append("(");
        emitn("define ",vmrt," ",fName,"(");
        FormalParameterListContext fpl = ctx.formalParameterList();
        if (fpl != null) {
		    String sep = "";
		    for (FormalParameterContext fp : fpl.formalParameter()) {
		    	VariableDeclaratorIdContext vdi = fp.variableDeclaratorId();
				String vid = vdi.Identifier().getSymbol().getText();
				String argn = "%"+quoteName("a_"+vid);
		        String argt = llvmType(fp.type().tipe);
		    	emitn(sep,argt," ",argn);
		    	refType.append(sep).append(argt);
		    	sep = ",";
		    }
        }
        emit(") {");
        refType.append(")");
		CodeReg ref = CodeReg.makeRef(rtype,refType.toString(),fName);
        symRef.put(ctx.def, ref);
        if (fpl != null) {
		    for (FormalParameterContext fp : fpl.formalParameter()) {
		        Type fptype = fp.type().tipe;
		    	VariableDeclaratorIdContext vdi = fp.variableDeclaratorId();
				String vid = vdi.Identifier().getSymbol().getText();
				String argn = "%"+quoteName("a_"+vid);
				String varn = "%"+quoteName("v_"+vid);
				String argt = llvmType(fptype);
		        ref = CodeReg.makeRef(fptype,argt,varn);
		        symRef.put(vdi.def, ref);
				emit("    ",varn," = alloca ",argt);
		    	emit("    store ",argt," ",argn,", ",argt,"* ",varn);
		    }
        }
        BlockContext blk = ctx.block();
        visit(blk);
        if (vmrt == CodePass.VOID ) {
        	emit("    ret void");
        } else {
        	emit("    ret ",vmrt," 0");
        }
        emit("}");
        return null;
    }
	
    @Override
	public CodeReg visitFieldDeclaration(FieldDeclarationContext ctx) {
    	// type variableDeclarators ';'
        // don't need to visit type in this pass
        VariableDeclaratorsContext d = ctx.variableDeclarators();
        for (VariableDeclaratorContext v : d.variableDeclarator()) {
    		VariableDeclaratorIdContext vdi = v.variableDeclaratorId();
	        Type vtype = vdi.def.getType();
			String vid = vdi.Identifier().getSymbol().getText();
			String varn = "@"+quoteName("f_"+vid);
			String vart = llvmType(vtype);
	        CodeReg ref = CodeReg.makeRef(vtype,vart,varn);
	        symRef.put(vdi.def, ref);
			emit("    ",varn," = global ",vart);
    		ExpressionContext init = v.expression();   // initializer
			if (init != null) {
				visit(init);
				//TODO store initializer
			}
        }
        return null;
	}   
    
	@Override
    public CodeReg visitLocalVariableDeclaration(LocalVariableDeclarationContext ctx) {
        // type variableDeclarators ';'
        // don't need to visit type in this pass
        VariableDeclaratorsContext d = ctx.variableDeclarators();
        for (VariableDeclaratorContext v : d.variableDeclarator()) {
    		VariableDeclaratorIdContext vdi = v.variableDeclaratorId();
	        Type vtype = vdi.def.getType();
			String vid = vdi.Identifier().getSymbol().getText();
			String varn = "%"+quoteName("v_"+vid);
			String vart = llvmType(vtype);
	        CodeReg ref = CodeReg.makeRef(vtype,vart,varn);
	        symRef.put(vdi.def, ref);
			emit("    ",varn," = alloca ",vart);
    		ExpressionContext init = v.expression();   // initializer
			if (init != null) {
				visit(init);
				//TODO store initializer
			}
        }
        return null;
    }
    
	@Override
	public CodeReg visitCompoundStatement(CompoundStatementContext ctx) {
    	// block
        BlockContext b = ctx.block();
		visit(b);
        return null;
    }
    
	@Override
    public CodeReg visitIfStatement(IfStatementContext ctx) {
    	// 'if' '(' expression ')' statement ('else' statement)?
    	String id = nextLabel();
    	ExpressionContext e = ctx.expression();
		CodeReg cond = visit(e);
		checkBoolean(cond,getChildToken(ctx,3));
    	emit("    br ",cond.typeAndName(),", label %true",id,", label %false",id);
    	Iterator<StatementContext> s = ctx.statement().iterator();
    	emit("true",id,":");
    	visit(s.next());
    	if (s.hasNext()) {
    		emit("    br label %end",id);
        	emit("false",id,":");
        	visit(s.next());
        	emit("end",id,":");
    	} else {
    		emit("false",id,":");
    	}
        return null;
    }
    
	@Override
    public CodeReg visitWhileStatement(WhileStatementContext ctx) {
    	// 'while' '(' expression ')' statement
    	String id = nextLabel();
    	emit("    br label %loop",id," ; force basic block boundary");
    	emit("loop",id,":");
    	ExpressionContext e = ctx.expression();
    	CodeReg cond = visit(e);
		checkBoolean(cond,getChildToken(ctx,3));
    	emit("    br ",cond.typeAndName(),", label %start",id,", label %end",id);
    	emit("start",id,":");
    	visit(ctx.statement());
    	emit("    br label %loop",id);
    	emit("end",id,":");
        return null;
    }
    
	@Override
    public CodeReg visitReturnStatement(ReturnStatementContext ctx) {
    	// 'return' expression? ';'
    	// Find enclosing method's return type
		MethodDeclarationContext md = (MethodDeclarationContext)findAncestor(ctx, MethodDeclarationContext.class);
		Type rt = md.def.getType();
        ExpressionContext e = ctx.expression();
        if (e != null) {
        	CodeReg r = visit(e);
        	if (r.getType() != rt) {
        		// TODO coercion of return type
        		Main.error(e.start,"expected return type is "+rt);
        	}
        	emit("    ret ",r.typeAndName());        	
        } else {
        	if (rt != null && rt != Type.voidType) {
        		Main.error(ctx.start,"expected return type is "+rt);
        	}
        	emit("    ret void");        	
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
    public CodeReg visitNullStatement(MJ1Parser.NullStatementContext ctx) {
    	// ';'
        return null;
    }
    
	@Override
    public CodeReg visitExpressionStatement(ExpressionStatementContext ctx) {
    	// expression ';'
        ExpressionContext e = ctx.expression();
		visit(e);
        return null;
    }
    
	@Override
    public CodeReg visitPrimaryExpression(PrimaryExpressionContext ctx) {
    	// primary
        PrimaryContext p = ctx.primary();
        CodeReg r = visit(p);
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
    
    private CodeReg widenNumericType(Type max, CodeReg r, Token op) {
    	if (max == Type.errorType || r.getType() == Type.errorType) return r;
    	if (r.isPointer()) r = doLoad(r);
    	if (r.getType() == max) return r;  // no widening if already required type
    	// LATER unboxing
    	CodeReg p = new CodeReg(max,llvmType(max),false);
    	if (   max == Type.doubleType
        	&& r.getType() == Type.floatType ) {
        	emit("    ",p.name," = fpext ",r.typeAndName()," to ",llvmType(max));
        	return p;
        }
    	if (   (max == Type.floatType || max == Type.doubleType)
    		&& (r.getType() == Type.intType || r.getType() == Type.longType
    		    || r.getType() == Type.shortType || r.getType() == Type.byteType) ) {
    		emit("    ",p.name," = sitofp ",r.typeAndName()," to ",llvmType(max));
    		return p;
    	}
    	if (   (max == Type.intType || max == Type.longType)
        	&& r.getType() == Type.charType) {
        	emit("    ",p.name," = zext ",r.typeAndName()," to ",llvmType(max));
        	return p;
        }
    	Main.error(op,r.getType()+" operand cannot be widened to "+max);
    	return r;
    }

	private CodeReg doLoad(CodeReg r) {
		CodeReg l = new CodeReg(r.getType(),llvmType(r.getType()),false);
		emit("    ",l.name," = load ",r.typeAndName());
		return l;
	}

	@Override
	public CodeReg visitIndexExpression(IndexExpressionContext ctx) {
		// expression '[' expression ']'
    	List<ExpressionContext> list = ctx.expression();
		Iterator<ExpressionContext> expit = list.iterator();
    	ExpressionContext expa = expit.next();
    	ExpressionContext expb = expit.next();
    	CodeReg ra = visit(expa);
    	CodeReg rb = visit(expb);
    	Token opToken = getChildToken(ctx,1);
    	if (!(ra.getType() instanceof ArrayType)) {
    		Main.error(opToken,"index on non-array type "+ra.getType());
    		return null;
    	}
    	Type type = ((ArrayType)ra.getType()).getBase();
    	rb = widenNumericType(Type.intType, rb, opToken);
    	CodeReg r = new CodeReg(type,llvmType(type),true);
		emit("    ",r.name," = getelementptr ",ra.typeAndName(),", ",rb.typeAndName());
		return r;
	}

	@Override
	public CodeReg visitCallExpression(CallExpressionContext ctx) {
		// expression '(' expressionList? ')'
		//TODO this is a mess! must be a better way. maybe treat a method as a type
		ExpressionContext me = ctx.expression();
		CodeReg meth = visit(me);
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
        CodeReg m = symRef.get(mi.def);
        assert m != null;
    	System.out.println("call target class "+me.getClass().getSimpleName()+" reg "+m);
    	
        Type type = ms.getType();
		CodeReg r = new CodeReg(type, llvmType(type), false);
        ExpressionListContext el = ctx.expressionList();
	    StringBuilder callArgs = new StringBuilder();
        if (el != null) {
		    ArrayList<CodeReg> pl = new ArrayList<CodeReg>(el.getChildCount());
		    String sep = "";
		    for (ExpressionContext e : el.expression()) {
		    	CodeReg p = visit(e);
		    	// TODO check formal parameter type
				pl.add(p);
				callArgs.append(sep).append(p.typeAndName());
		    }
        }
        emit("    ",r.name," = call ",m.typeAndName(),"(",callArgs.toString(),")");
        return r;
	}

	@Override
	public CodeReg visitPrefixExpression(PrefixExpressionContext ctx) {
		// ('+'|'-'/*|'++'|'--'*/) expression
		// TODO Auto-generated method stub
		return super.visitPrefixExpression(ctx);
	}

	@Override
	public CodeReg visitNotExpression(NotExpressionContext ctx) {
		// ('~'|'!') expression
		// TODO Auto-generated method stub
		return super.visitNotExpression(ctx);
	}

	private CodeReg binaryAithmeticOperation(ExpressionContext ctx, List<ExpressionContext> list, ParseTree opNode) {
		Iterator<ExpressionContext> expit = list.iterator();
    	ExpressionContext expa = expit.next();
    	ExpressionContext expb = expit.next();
    	CodeReg ra = visit(expa);
    	CodeReg rb = visit(expb);
    	Token opToken = ((TerminalNode)opNode).getSymbol();
    	Type type = maxNumericType(ra.getType(),rb.getType());
    	ra = widenNumericType(type, ra, opToken);
    	rb = widenNumericType(type, rb, opToken);
    	CodeReg r = new CodeReg(type, llvmType(type), false);
		emit("    ",r.name," = ",llvmOpMap.get(opToken.getText())," ",ra.typeAndName(),", ",rb.name);
		return r;
	}
    
	@Override
    public CodeReg visitMultiplyExpression(MultiplyExpressionContext ctx) {
    	// expression ('*'|'/'|'%') expression
    	return binaryAithmeticOperation(ctx, ctx.expression(), ctx.getChild(1));
    }
    
	@Override
    public CodeReg visitAddExpression(AddExpressionContext ctx) {
    	// expression ('+'|'-') expression
    	return binaryAithmeticOperation(ctx, ctx.expression(), ctx.getChild(1));
    }
    
	@Override
    public CodeReg visitCompareExpression(CompareExpressionContext ctx) {
    	// expression ('<=' | '>=' | '>' | '<') expression
    	List<ExpressionContext> list = ctx.expression();
		Iterator<ExpressionContext> expit = list.iterator();
    	ExpressionContext expa = expit.next();
    	ExpressionContext expb = expit.next();
    	CodeReg ra = visit(expa);
    	CodeReg rb = visit(expb);
    	Token opToken = getChildToken(ctx,1);
    	Type type = maxNumericType(ra.getType(),rb.getType());
    	ra = widenNumericType(type, ra, opToken);
    	rb = widenNumericType(type, rb, opToken);
    	CodeReg r = new CodeReg(Type.booleanType, llvmType(Type.booleanType), false);
		emit("    ",r.name," = ",llvmOpMap.get(opToken.getText())," ",ra.typeAndName(),", ",rb.name);
		return r;
    }
    
	@Override
    public CodeReg visitEqualExpression(EqualExpressionContext ctx) {
    	// expression ('==' | '!=') expression
    	List<ExpressionContext> list = ctx.expression();
		Iterator<ExpressionContext> expit = list.iterator();
    	ExpressionContext expa = expit.next();
    	ExpressionContext expb = expit.next();
    	CodeReg ra = visit(expa);
    	CodeReg rb = visit(expb);
    	Token opToken = getChildToken(ctx,1);
    	// LATER reference comparison
    	Type type = maxNumericType(ra.getType(),rb.getType());
    	ra = widenNumericType(type, ra, opToken);
    	rb = widenNumericType(type, rb, opToken);
    	CodeReg r = new CodeReg(Type.booleanType, llvmType(Type.booleanType), false);
		emit("    ",r.name," = ",llvmOpMap.get(opToken.getText())," ",ra.typeAndName(),", ",rb.name);
		return r;
    }

	@Override
    public CodeReg visitBitAndExpression(BitAndExpressionContext ctx) {
    	// expression '&' expression
    	return binaryAithmeticOperation(ctx, ctx.expression(), ctx.getChild(1));
    }
    
	@Override
    public CodeReg visitBitExclExpression(BitExclExpressionContext ctx) {
    	// expression '^' expression
    	return binaryAithmeticOperation(ctx, ctx.expression(), ctx.getChild(1));
    }
    
	@Override
    public CodeReg visitBitOrExpression(BitOrExpressionContext ctx) {
    	// expression '|' expression
    	return binaryAithmeticOperation(ctx, ctx.expression(), ctx.getChild(1));
    }

	private void checkBoolean(CodeReg cond, Token token) {
    	if (!(cond.getType() == Type.booleanType || cond.getType() == Type.errorType)) {
    		Main.error(token, "condition type must be boolean");
    	}
	}
    
	@Override
    public CodeReg visitAndThenExpression(AndThenExpressionContext ctx) {
    	// expression '&&' expression
    	String id = nextLabel();
    	List<ExpressionContext> list = ctx.expression();
		Iterator<ExpressionContext> expit = list.iterator();
    	ExpressionContext expa = expit.next();
    	ExpressionContext expb = expit.next();
    	CodeReg ra = visit(expa);
		checkBoolean(ra,expa.getStop());
    	emit("    br ",ra.typeAndName(),", label %true",id,", label %false",id);
    	emit("true",id,":");
    	CodeReg rb = visit(expb);
		checkBoolean(rb,expb.getStop());
		emit("false",id,":");
    	CodeReg r = new CodeReg(Type.booleanType, llvmType(Type.booleanType), false);
		emit("    ",r.name," = phi ",r.getRegType()," [ false, %",/*TODO*/"0"," ], [ ",rb.getName(),", %true",id," ]");
		return r;
    }
    
	@Override
    public CodeReg visitOrElseExpression(OrElseExpressionContext ctx) {
    	// expression '||' expression
		// TODO short-circuit
    	return null;
    }
        
	@Override
    public CodeReg visitAssignment(AssignmentContext ctx) {
		// <assoc=right> expression
		//      (   '='
		//      )
		//      expression
		Iterator<ExpressionContext> expit = ctx.expression().iterator();
    	ExpressionContext expa = expit.next();
    	ExpressionContext expb = expit.next();
    	CodeReg ra = visit(expa);
    	CodeReg rb = visit(expb);
    	Token opToken = getChildToken(ctx,1);
    	Type tipe = ra.getType();
    	System.out.println("assignment target class "+expa.getClass().getSimpleName()+" type "+tipe+" pointer "+ra.isPointer());
    	rb = widenNumericType(tipe, rb, opToken);
		emit("    store ",llvmType(tipe)," ",rb.getName(),", ",llvmType(tipe),"* ",ra.getName());
		return rb;
	}

	@Override
	public CodeReg visitParenthesized(ParenthesizedContext ctx) {
    	// '(' expression ')'
        ExpressionContext e = ctx.expression();
        CodeReg r = visit(e);
        return r;
    }
    
	@Override
    public CodeReg visitIdentifierPrimary(IdentifierPrimaryContext ctx) {
    	// Identifier
    	CodeReg r;
    	Token id = ctx.Identifier().getSymbol();
		ctx.def = ctx.refScope.find(id);
    	if (ctx.def == null) {
    		Main.error(id," is not defined");
        	r = new CodeReg(Type.errorType,llvmType(Type.errorType),false);
    	} else if (ctx.def.getType() == Type.errorType){
        	r = new CodeReg(Type.errorType,llvmType(Type.errorType),false);
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
    
    private String quoteName(String name) {
    	int len = name.length();
    	int x = 0;
    	while (x < len) {
    		char c = name.charAt(x);
    		if (   (c >= 'a' && c <= 'z')
    			|| (c >= 'A' && c <= 'Z')
    			|| (c >= '0' && c <= '9' && x > 0)
    			|| c == '-' || c == '$' || c == '.' || c == '_' ) {
    		} else break;
    		x++;
    	}
    	if (x == len) return name;  // no quotes needed
    	StringBuilder quoted = new StringBuilder(name.length()+2);
    	quoted.append('"');
    	quoted.append(name);
    	// should be no quotes or backslashes in a Java name to escape
    	quoted.append('"');
    	return quoted.toString();
	}

	@Override
	public CodeReg visitIntegerPrimary(MJ1Parser.IntegerPrimaryContext ctx) {
    	// IntegerLiteral
    	String v = Integer.toString(Integer.decode(ctx.IntegerLiteral().getText()));
    	CodeReg r = CodeReg.makeLiteral(Type.intType,llvmType(Type.intType),v);
        return r;
    }
    
	@Override
    public CodeReg visitFloatPrimary(MJ1Parser.FloatPrimaryContext ctx) {
    	// FloatingPointLiteral
    	String v = Float.toString(Float.parseFloat(ctx.FloatingPointLiteral().getText()));
    	CodeReg r = CodeReg.makeLiteral(Type.floatType,llvmType(Type.floatType),v);
        return r;
    }
    
	@Override
    public CodeReg visitCharPrimary(MJ1Parser.CharPrimaryContext ctx) {
    	// CharacterLiteral
    	String v = Integer.toString(Integer.decode(ctx.CharacterLiteral().getText()));
    	CodeReg r = CodeReg.makeLiteral(Type.charType,llvmType(Type.charType),v);
        return r;
    }
    
	@Override
    public CodeReg visitStringPrimary(MJ1Parser.StringPrimaryContext ctx) {
    	// StringLiteral
    	String s = ctx.StringLiteral().getText();
    	StringBuilder t = new StringBuilder(s.substring(1, s.length()-1));  // strip quotes
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
    	String u = t.toString();
    	CodeReg r = new CodeReg(Type.stringType,llvmType(Type.stringType),true);
    	emit("    ",r.name," = getelementptr ",stringType(u),"* ",stringID(u),", i32 0, i32 0");
       return r;
    }
    
	@Override
    public CodeReg visitBooleanPrimary(MJ1Parser.BooleanPrimaryContext ctx) {
    	// BooleanLiteral
    	String v = ctx.BooleanLiteral().getText().equals("true") ? "true" : "false";
    	CodeReg r = CodeReg.makeLiteral(Type.booleanType,llvmType(Type.booleanType),v);
        return r;
    }
    
	@Override
    public CodeReg visitNullPrimary(MJ1Parser.NullPrimaryContext ctx) {
    	// 'null'
    	CodeReg r = CodeReg.makeLiteral(Type.stringType,llvmType(Type.stringType),"null");
    	// TODO correct reference type
        return r;
    }

}
