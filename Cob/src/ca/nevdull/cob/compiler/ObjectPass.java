package ca.nevdull.cob.compiler;

// Produce the class instance structure (object fields) to the class definition file 

import java.io.FileNotFoundException;
import java.util.Map;
import java.util.Map.Entry;

import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.tree.TerminalNode;

public class ObjectPass extends PassCommon {

	private boolean trace;

	public ObjectPass(PassData data) {
		super(data);
    	trace = passData.main.trace.contains("ObjectPass");
	}

	@Override public Void visitKlass(CobParser.KlassContext ctx) {
		String name = ctx.name.getText();
		try {
			passData.defnStream = passData.openFileStream(name, Main.DEFN_SUFFIX);
		} catch (FileNotFoundException excp) {
			Main.error("Unable to open defintions stream "+excp.getMessage());
			return null;
		}
		writeDefn("// Generated at ",passData.timeStamp,"\n");
		writeDefn("// From ",passData.sourceFileName,"\n");
		writeDefn("#ifndef ",name,"_DEFN\n");
		writeDefn("#define ",name,"_DEFN\n");
		
		// Includes of dependencies
		
		ClassSymbol klass = ctx.defn;
		writeDefn("#include \"cob.h\"\n");
		writeDefn("#ifndef COB_FORWARD_CLASS_",name,"\n");
		writeDefn("#define COB_FORWARD_CLASS_",name,"\n");
		writeDefn("COB_FORWARD_CLASS(",name,")\n"); // precedes base in case of mutual dependency
		writeDefn("#endif\n");
		ClassSymbol base = klass.getBase();
		if (base != null) {
			writeDefn("#include \"",base.getName(),Main.DEFN_SUFFIX,"\"\n");
		}
		for (Symbol globSym : passData.globals.getMembers().values()) {
			if (globSym == klass) continue;
			if (globSym == base) continue;
			assert globSym instanceof ClassSymbol;
			String globName = globSym.getName();
			writeDefn("#ifndef COB_FORWARD_CLASS_",globName,"\n");
			writeDefn("#define COB_FORWARD_CLASS_",globName,"\n");
			writeDefn("COB_FORWARD_CLASS(",globName,")\n");
			writeDefn("#endif\n");
		}
		
		// The object instance
		
		writeDefn("struct ",name,"_Dispatch;\n");
		writeDefn("struct ",name,"_Fields {\n");
		if (base != null) writeDefn("  struct ",base.getName(),"_Fields _base;\n");
		visitFields(klass, null);
		writeDefn("};\n");
		writeDefn("struct ",name,"_Object {\n");
		writeDefn("  struct ",name,"_Dispatch *dispatch;\n");
		writeDefn("  struct ",name,"_Fields fields;\n");
		writeDefn("};\n");
		writeDefn("void ",PassCommon.INIT,"_",name,"();\n");
		
		// The method dispatch table
		
		writeDefn("extern struct ",name,"_Dispatch {\n");
		writeDefn("  struct ClassInit init;\n");
		visitMethods(klass, false/*staticPass*/);
		writeDefn("} ",name,"_Dispatch;\n");
		writeDefn("void ",name,"_",PassCommon.CLASSINIT,"();\n");
		writeDefn("void ",name,"_",PassCommon.INSTANCEINIT,"(",name," this);\n");
		writeDefn(name," ",name,"_",PassCommon.NEW,"();\n");
		visitMethods(klass, true/*staticPass*/);

		writeDefn("#endif /*",name,"_DEFN*/\n");

		return null;
	}
	
	private void visitFields(ClassSymbol klass, String staticName) {
		String className = klass.getName();
		for (Symbol member : klass.getMembers().values()) {
			if (member instanceof VariableSymbol) {
				VariableSymbol field = (VariableSymbol)member;
				Type type = field.getType();
				if (staticName == null) {
					if (!field.isStatic()) {
						writeDefn("  ",type.getNameString()," ",type.getArrayString(),field.getName(),";\n");
					}
				} else {
					if (field.isStatic()) {
						writeDefn(type.getNameString()," ",type.getArrayString(),staticName,"_",field.getName(),";\n");
					}
				}
			}
		}
	}
	
	private void visitMethods(ClassSymbol klass, boolean staticPass) {
		String className = klass.getName();
		Map<String, Symbol> m = klass.getMembers();
		for (Symbol member : klass.getExpandedMembers().values()) {
			if (member instanceof MethodSymbol) {
				MethodSymbol method = (MethodSymbol)member;
				String sep = "";
				Type methType = method.getType();
				Scope scope = method.getScope();
				assert scope instanceof ClassSymbol;
				String cName = ((ClassSymbol)scope).getName();
				String methName = method.getName();
				if (method.isStatic() == staticPass) {
					if (staticPass) {
						writeDefn(methType.getNameString()," ",methType.getArrayString(),cName,"_",methName,"(");
					} else {
						writeDefn("  ",methType.getNameString()," (*",methType.getArrayString(),methName,")(");
					}
					doArguments(method, sep);
				} else if (method.isNative()) {
					if (staticPass) {
						writeDefn(methType.getNameString()," ",methType.getArrayString(),cName,"_",methName,"(");
					} else {
						writeDefn("  ",methType.getNameString()," (*",methType.getArrayString(),methName,")(");
					}
					doArguments(method, sep);
				}
			}
		}
		if (klass.findMember(className) == null && staticPass) {
			// define a default constructor
			writeDefn("void ",className,"_",className,"(",className," this);\n");
		}
	}

	private void doArguments(MethodSymbol method, String sep) {
		Type type;
		Map<String, Symbol> arguments = method.getMembers();
		if (arguments != null) {
			for (Symbol argument : arguments.values()) {
				assert argument instanceof VariableSymbol;
				VariableSymbol arg = (VariableSymbol)argument;
				writeDefn(sep);  sep = ",";
				type = argument.getType();
				writeDefn(type.getNameString()," ",type.getArrayString(),argument.getName());
			}
		}
		writeDefn(");\n");
	}

}
