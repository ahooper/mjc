package ca.nevdull.mjc1.compiler;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map.Entry;

import ca.nevdull.mjc1.compiler.MJ1Parser.ExpressionContext;

public class CodeGen {

	private PrintWriter writer;

	public CodeGen(String modulePrefix) {
		writer = new PrintWriter(System.out);
		writer.print(modulePrefix);
		writer.print("::\n");
		// TODO
	}

	// Result value names
	
	private int resultCount = 0;
	private String nextResult() {
		return String.format("%%r%d", ++resultCount);
	}
	
	// Type mapping
	
	public final static String TYPE_BOOLEAN = "i1";
	public final static String TYPE_BYTE = "i8";
	public final static String TYPE_CHAR = "i16";
	public final static String TYPE_DOUBLE = "double";
	public final static String TYPE_FLOAT = "float";
	public final static String TYPE_INT = "i32";
	public final static String TYPE_LONG = "i64";
	public final static String TYPE_SHORT = "i16";
	public final static String TYPE_VOID = "void";
	public final static String TYPE_ERROR = "error";

	public final static String LITERAL_TRUE = "true";
	public final static String LITERAL_FALSE = "false";
	public final static String LITERAL_NULL = "null";
	
	static HashMap<Type,String> llvmTypeMap = new HashMap<Type,String>();
	{
		llvmTypeMap.put(Type.booleanType, TYPE_BOOLEAN);
		llvmTypeMap.put(Type.byteType, TYPE_BYTE);
		llvmTypeMap.put(Type.charType, TYPE_CHAR);
		llvmTypeMap.put(Type.doubleType, TYPE_DOUBLE);
		llvmTypeMap.put(Type.floatType, TYPE_FLOAT);
		llvmTypeMap.put(Type.intType, TYPE_INT);
		llvmTypeMap.put(Type.longType, TYPE_LONG);
		llvmTypeMap.put(Type.shortType, TYPE_SHORT);
		llvmTypeMap.put(Type.stringType, "@string");
		llvmTypeMap.put(Type.voidType, TYPE_VOID);
		llvmTypeMap.put(Type.errorType, TYPE_ERROR);
	}
	
	private static String llvmType(Type type) {
		if (type instanceof ArrayType) {
			return "[0 x "+llvmType(((ArrayType) type).getBase())+"]";
		}
		String t = llvmTypeMap.get(type);
		if (t == null) return "error";
		return t;
	}
	
	// Operation mapping
	
	static HashMap<String,String> llvmOpMap = new HashMap<String,String>();
	{
		llvmOpMap.put("++", "add");
		llvmOpMap.put("--", "sub");
		llvmOpMap.put("~", "???");
		llvmOpMap.put("!", "???");
		llvmOpMap.put("*", "mul");
		llvmOpMap.put("/", "sdiv");
		llvmOpMap.put("%", "srem");
		llvmOpMap.put("+", "add");
		llvmOpMap.put("-", "sub");
		llvmOpMap.put("==", "eq");
		llvmOpMap.put("!=", "ne");
		llvmOpMap.put("<", "slt");
		llvmOpMap.put(">", "sgt");
		llvmOpMap.put("<=", "sle");
		llvmOpMap.put(">=", "sge");
		llvmOpMap.put("&", "and");
		llvmOpMap.put("^", "xor");
		llvmOpMap.put("|", "or");
	}
	
	// Field definitions

	public CodeSym emitGlobalField(String id, Type type) {
		String name = "@"+quoteName("f_"+id);
		String irType = llvmType(type);
		CodeSym r = new CodeSym(type, irType, name, true);
		emit(name+" = global "+irType);
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

	public CodeSym emitStackField(String id, Type type) {
		String name = "%"+quoteName("v_"+id);
		String irType = llvmType(type);
		CodeSym r = new CodeSym(type, irType, name, true);
		emit(name+" = alloca "+irType);
		return r;
	}
	
	// Function definitions

	public CodeSym makeArgument(String id, Type type) {
		String name = "%"+quoteName("a_"+id);
		String irType = llvmType(type);
		CodeSym a = new CodeSym(type, irType, name);
		return a;
	}
	
	String currentBlockName;
	
	public CodeSym beginFunction(String id, ArrayList<CodeSym> args, Type type) {
        String name = "@"+quoteName(id);
        String irType = llvmType(type);
        writer.print("define ");
        writer.print(irType);
        writer.print(" ");
        writer.print(name);
        writer.print("(");
		StringBuilder fType = new StringBuilder(irType);
        fType.append("(");
        String sep = "";
        for (CodeSym arg : args) {
        	writer.print(sep);
        	writer.print(arg.getIRType());
        	writer.print(" ");
        	writer.print(arg.getIRName());
        	fType.append(sep).append(arg.getIRType());
        	sep = ", ";
        }
        writer.print(") {\n");
        fType.append(")*");
		CodeSym f = new CodeSym(type, fType.toString(), name, true);

        blockCount = 0;
		currentBlockName = "%0";

		return f;
	}
	
	public void endFunction() {
		writer.print("}\n");
	}

	public void beginBlock(String name) {
		writer.print(name);
		writer.print(":\n");
		currentBlockName = name;
	}

	public String getCurrentBlockName() {
		return currentBlockName;
	}
	
	int blockCount = 0;
	
	public String nextBlock(String prefix) {
		return String.format("%%%s%d", prefix, ++blockCount);
	}
	
	private void emit(String instruction) {
		writer.print("    ");
		writer.print(instruction);
		writer.print("\n");
		writer.flush(); // TODO temporary
	}
	
	// Literals
	
	public CodeSym makeLiteral(boolean value) {
		return new CodeSym(Type.booleanType, TYPE_BOOLEAN,
						   value ? LITERAL_TRUE : LITERAL_FALSE);
	}

	public CodeSym makeLiteral(byte value) {
		return new CodeSym(Type.byteType, TYPE_BYTE, Byte.toString(value));
	}

	public CodeSym makeLiteral(short value) {
		return new CodeSym(Type.shortType, TYPE_SHORT, Short.toString(value));
	}

	public CodeSym makeLiteral(int value) {
		return new CodeSym(Type.intType, TYPE_INT, Integer.toString(value));
	}

	public CodeSym makeLiteral(long value) {
		return new CodeSym(Type.longType, TYPE_LONG, Long.toString(value));
	}

	public CodeSym makeLiteral(double value) {
		return new CodeSym(Type.doubleType, TYPE_DOUBLE, Double.toString(value));
	}

	public CodeSym makeLiteral(float value) {
		return new CodeSym(Type.floatType, TYPE_FLOAT, Float.toString(value));
	}

	public CodeSym makeLiteral(char value) {
		return new CodeSym(Type.charType, TYPE_CHAR, Integer.toString(Character.getNumericValue(value)));
	}

	public CodeSym makeNull(Type type) {
		return new CodeSym(type, llvmType(type), "null");
	}
	
	public CodeSym makeError() {
		return new CodeSym(Type.errorType, TYPE_ERROR, "error");
	}

	// Strings
	
	HashMap<String,CodeSym> stringTable = new HashMap<String,CodeSym>();
	private int stringIDSequence = 0;
	private String nextStringID() {
		return String.format("@s%d", ++stringIDSequence);
	}
	
	private String stringType(String s) {
		return "["+s.length()+" x i16]";
	}
	
	public CodeSym makeString(String s) {
		CodeSym id = stringTable.get(s);
		if (id == null) {
			id = new CodeSym(Type.stringType, stringType(s), nextStringID(), true);
			stringTable.put(s,id);
		}
		return id;
	}

	public void emitStringTable() {
		for (Entry<String, CodeSym> entry : stringTable.entrySet()) {
		    String s = entry.getKey();
		    CodeSym id = entry.getValue();
		    writer.print(id);
		    writer.print(" = internal constant ");
		    writer.print(stringType(s));
		    writer.print(" [");
		    String sep = "";
		    for (char c : s.toCharArray()) {
		    	writer.print(sep);
			    writer.print("i16 ");
			    writer.print(Integer.toString(c));
		    	sep = ",";
		    }
		    writer.print("]\n");
		}
	}

	// Numeric operations
	
	public CodeSym emitBinaryOperation(String op, CodeSym op1, CodeSym op2) {
		Type type = op1.getType();
		assert op2.getType() == type;
		CodeSym r = new CodeSym(type, llvmType(type), nextResult());
		emit(r.getIRName()+" = "+llvmOpMap.get(op)+" "+llvmType(type)+" "+op1.getIRName()+", "+op2.getIRName());
		return r;
	}

	public CodeSym emitCompareOperation(String op, CodeSym op1, CodeSym op2) {
		Type type = op1.getType();
		assert op2.getType() == type;
		CodeSym r = new CodeSym(Type.booleanType, TYPE_BOOLEAN, nextResult());
		String t = (type == Type.floatType || type == Type.doubleType) ? "fcmp " : "icmp ";
		emit(r.getIRName()+" = "+t+llvmOpMap.get(op)+" "+llvmType(type)+" "+op1.getIRName()+", "+op2.getIRName());
		return r;
	}
	
	private CodeSym emitConversion(String op, Type type2, CodeSym op1) {
		String irType2 = llvmType(type2);
		CodeSym r = new CodeSym(type2, irType2, nextResult());
		emit(r.getIRName()+" = "+op+" "+op1.getIRType()+" "+op1.getIRName()+" to "+irType2);
		return r;
	}

	public CodeSym emitWidenFloat(Type max, CodeSym r) {
    	return emitConversion("fpext", max, r);
	}

	public CodeSym emitIntToFloat(Type max, CodeSym r) {
    	return emitConversion("sitofp", max, r);
	}

	public CodeSym emitWidenUnsigned(Type max, CodeSym r) {
    	return emitConversion("zext", max, r);
	}

	public CodeSym emitWidenInt(Type max, CodeSym r) {
       	return emitConversion("sext", max, r);
	}

	// Storage access
	
	public CodeSym emitLoad(CodeSym r) {
		assert r.isAddress();
		Type type = r.getType();
		String irType = llvmType(type);
		CodeSym l = new CodeSym(type, irType, nextResult());
		emit(l.getIRName()+" = load "+irType+" "+r.getIRName());
		return l;
	}

	public CodeSym emitIndex(CodeSym ra, CodeSym rb) {
		assert ra.isAddress();
    	Type type = ((ArrayType)ra.getType()).getBase();
		String irType = llvmType(type);
		CodeSym r = new CodeSym(type, irType, nextResult(), true);
		emit(r.getIRName()+" = getelementptr "+ra.getIRType()+" "+ra.getIRName()+", "+rb.getIRType()+" "+rb.getIRName());
		return r;
	}

	public void emitStore(CodeSym rb, CodeSym ra) {
		assert ra.isAddress();
		Type type = ra.getType();
		assert rb.getType() == type;
		String irType = llvmType(type);
		emit("store "+irType+" "+rb.getIRName()+", "+irType+"* "+ra.getIRName());
	}

	// Flow control

	public void emitBranch(CodeSym cond, String tBlock, String fBlock) {
		assert cond.getType() == Type.booleanType;
    	emit("br "+cond.getIRType()+" "+cond.getIRName()+", label "+tBlock+", label "+fBlock);
	}
	
	public void emitBranch(String dBlock) {
    	emit("br label "+dBlock);
	}

	public CodeSym emitJoin(CodeSym op1, String block1, CodeSym op2, String block2) {
		Type type = op1.getType();
		assert op2.getType() == type;
		String irType = llvmType(type);
		CodeSym r = new CodeSym(type, irType, nextResult());
		emit(r.getIRName()+" = phi "+irType+" [ "+op1.getIRName()+", %"+block1+" ], [ "+op2.getIRName()+", %"+block2+" ]");
		return r;
 	}

	public CodeSym emitCall(CodeSym f, Type rType, ArrayList<CodeSym> plist) {
		String irType = llvmType(rType);
		CodeSym r = new CodeSym(rType, irType, nextResult());
		StringBuilder call = new StringBuilder();
		call.append(r.getIRName()).append(" = call ").append(f.getIRType()).append(" ").append(f.getIRName()).append("(");
	    String sep = "";
	    for (CodeSym p : plist) {
			call.append(sep).append(p.getIRType()).append(" ").append(p.getIRName());
			sep = ", ";
	    }
	    call.append(")");
	    emit(call.toString());
		return r;
	}

	public void emitReturn(CodeSym r) {
    	emit("ret "+r.getIRType()+" "+r.getIRName());      	
	}

	public void emitReturn() {
    	emit("ret void");      	
	}
}
