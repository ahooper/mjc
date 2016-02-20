package ca.nevdull.mjc1.compiler;

import java.util.HashMap;

public class CodeGen {

	public CodeGen(String modulePrefix) {
		// TODO Auto-generated constructor stub
	}
	
	// Type mapping
	
	static HashMap<Type,ca.nevdull.mjc1.compiler.llvm.Type> llvmTypeMap = new HashMap<Type,ca.nevdull.mjc1.compiler.llvm.Type>();
	{
		llvmTypeMap.put(Type.booleanType, ca.nevdull.mjc1.compiler.llvm.Type.I1);
		llvmTypeMap.put(Type.byteType, ca.nevdull.mjc1.compiler.llvm.Type.I8);
		llvmTypeMap.put(Type.charType, ca.nevdull.mjc1.compiler.llvm.Type.I16);
		llvmTypeMap.put(Type.doubleType, ca.nevdull.mjc1.compiler.llvm.Type.DOUBLE);
		llvmTypeMap.put(Type.floatType, ca.nevdull.mjc1.compiler.llvm.Type.FLOAT);
		llvmTypeMap.put(Type.intType, ca.nevdull.mjc1.compiler.llvm.Type.I32);
		llvmTypeMap.put(Type.longType, ca.nevdull.mjc1.compiler.llvm.Type.I64);
		llvmTypeMap.put(Type.shortType, ca.nevdull.mjc1.compiler.llvm.Type.I16);
		llvmTypeMap.put(Type.stringType, "@string");
		llvmTypeMap.put(Type.voidType, ca.nevdull.mjc1.compiler.llvm.Type.VOID);
		llvmTypeMap.put(Type.errorType, "error");
	}
	
	private static ca.nevdull.mjc1.compiler.llvm.Type llvmType(Type type) {
		if (type instanceof ArrayType) {
			return new ca.nevdull.mjc1.compiler.llvm.ArrayType(0, llvmType(((ArrayType) type).getBase()));
		}
		ca.nevdull.mjc1.compiler.llvm.Type t = llvmTypeMap.get(type);
		if (t == null) return "error";
		return t;
	}
	
	// Operation mapping
	
	static HashMap<String,String> llvmOpMap = new HashMap<String,String>();
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
		llvmOpMap.put("==","eq");
		llvmOpMap.put("!=","ne");
		llvmOpMap.put("<","slt");
		llvmOpMap.put(">","sgt");
		llvmOpMap.put("<=","sle");
		llvmOpMap.put(">=","sge");
		llvmOpMap.put("&","and");
		llvmOpMap.put("^","xor");
		llvmOpMap.put("|","or");
	}
	
	public CodeSym beginFuction() {
		//TODO
	}
	
	public CodeSym endFunction() {
		//TODO
	}
	
	private void emit(ca.nevdull.mjc1.compiler.llvm.Instruction inst) {
		//TODO
	}

	public CodeSym makeLiteral(boolean value) {
		return new CodeSym(Type.booleanType,
						   value ? ca.nevdull.mjc1.compiler.llvm.Constant.TRUE
								 : ca.nevdull.mjc1.compiler.llvm.Constant.FALSE,
						   false);
	}

	public CodeSym makeLiteral(byte value) {
		return new CodeSym(Type.byteType,
						   new ca.nevdull.mjc1.compiler.llvm.Constant(value),
						   false);
	}

	public CodeSym makeLiteral(short value) {
		return new CodeSym(Type.shortType,
						   new ca.nevdull.mjc1.compiler.llvm.Constant(value),
						   false);
	}

	public CodeSym makeLiteral(int value) {
		return new CodeSym(Type.intType,
						   new ca.nevdull.mjc1.compiler.llvm.Constant(value),
						   false);
	}

	public CodeSym makeLiteral(long value) {
		return new CodeSym(Type.longType,
						   new ca.nevdull.mjc1.compiler.llvm.Constant(value),
						   false);
	}

	public CodeSym makeLiteral(double value) {
		return new CodeSym(Type.doubleType,
						   new ca.nevdull.mjc1.compiler.llvm.Constant(value),
						   false);
	}

	public CodeSym makeLiteral(float value) {
		return new CodeSym(Type.floatType,
						   new ca.nevdull.mjc1.compiler.llvm.Constant(value),
						   false);
	}

	public CodeSym makeLiteral(char value) {
		return new CodeSym(Type.charType,
						   new ca.nevdull.mjc1.compiler.llvm.Constant(value),
						   false);
	}
	
	public CodeSym emitBinaryOperation(String op, Type type, CodeSym op1, CodeSym op2) {
		ca.nevdull.mjc1.compiler.llvm.Instruction inst = new ca.nevdull.mjc1.compiler.llvm.BinaryOperation(
															 llvmOpMap.get(op),
				   											 llvmType(type),
				   											 op1.operand,
				   											 op2.operand);
		emit(inst);
		return new CodeSym(type, inst, false);
	}
	
	public CodeSym emitCompareOperation(String op, Type type, CodeSym op1, CodeSym op2) {
		String t = (type == Type.floatType || type == Type.doubleType) ? "fcmp " : "icmp ";
		ca.nevdull.mjc1.compiler.llvm.Instruction inst = new ca.nevdull.mjc1.compiler.llvm.CompareOperation(
															 t+llvmOpMap.get(op),
				   											 llvmType(type),
				   											 op1.operand,
				   											 op2.operand);
		emit(inst);
		return new CodeSym(Type.booleanType, inst, false);
	}
	
	public CodeSym emitConversion(Type type1, Type type2, CodeSym op) {
		CodeSym r = new CodeSym();
		r.type = type2;
		r.operand = new ca.nevdull.mjc1.compiler.llvm.CastOperation(type1, type2, op);
		return r;
	}
}
