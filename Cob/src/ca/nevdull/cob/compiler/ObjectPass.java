package ca.nevdull.cob.compiler;

// Produce the class instance structure (object fields) to the class definition file 

import java.io.FileNotFoundException;
import java.util.Map.Entry;

import org.antlr.v4.runtime.Token;

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
		}
		writeDefn("// Generated at ",passData.timeStamp,"\n");
		writeDefn("// From ",passData.sourceFileName,"\n");
		writeDefn("#ifndef ",name,"_DEFN\n");
		writeDefn("#define ",name,"_DEFN\n");
		writeDefn("#include \"cob.h\"\n");
		writeDefn("typedef struct ",name,"_Object *",name,";\n");  // precedes base in case of mutual dependency
		Token base = ctx.base;
		//include for base will be produced by the following
		for (Entry<String, Symbol> globEnt : passData.globals.getMembers().entrySet()) {
			Symbol globSym = globEnt.getValue();
			if (globSym == ctx.defn) continue;
			writeDefn("#include \"",globSym.getName(),Main.DEFN_SUFFIX,"\"\n");
		}
		writeDefn("struct ",name,"_ClassInfo;\n");
		writeDefn("struct ",name,"_Fields {\n");
		if (base != null) writeDefn("  struct ",base.getText(),"_Fields _base;\n");
		for (CobParser.MemberContext member : ctx.member()) {
			visit(member);
		}
		writeDefn("};\n");
		writeDefn("struct ",name,"_Object {\n");
		writeDefn("  struct ",name,"_ClassInfo *class;\n");
		writeDefn("  struct ",name,"_Fields fields;\n");
		writeDefn("};\n");
		writeDefn("extern void ",PassCommon.INIT,"_",name,"();\n");
		return null;
	}
	
	@Override public Void visitFieldList(CobParser.FieldListContext ctx) {
		//	'static'? type ID ( '=' expression )? ( ',' ID ( '=' expression )? )* ';'
		if (ctx.stat != null) return null;  // static fields are not included in the object instance
		for (CobParser.FieldContext field : ctx.field()) {
			visitField(field);
		}
		return null;
	}
	
	@Override public Void visitField(CobParser.FieldContext ctx) {
		//	ID ( '=' expression )?
		CobParser.FieldListContext list = (CobParser.FieldListContext)ctx.getParent();
		Type type = list.type().tipe;
		writeDefn("  ",type.getNameString()," ",type.getArrayString(),ctx.ID().getText(),";\n");
		return null;
	}
	
}
