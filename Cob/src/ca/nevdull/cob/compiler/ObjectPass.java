package ca.nevdull.cob.compiler;

// Produce the class instance structure (object fields) to the class definition file 

import java.io.FileNotFoundException;

import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.tree.TerminalNode;

public class ObjectPass extends PassCommon {

	public ObjectPass(PassData data) {
		super(data);
	}

	@Override public Void visitKlass(CobParser.KlassContext ctx) {
		String name = ctx.name.getText();
		try {
			passData.defnStream = passData.openFileStream(name, ".h");
		} catch (FileNotFoundException excp) {
			Main.error("Unable to open defintions stream "+excp.getMessage());
		}
		writeDefn("// Generated at ",passData.timeStamp,"\n");
		writeDefn("// From ",passData.sourceFileName,"\n");
		writeDefn("#ifndef ",name,"_DEFN\n");
		writeDefn("#define ",name,"_DEFN\n");
		writeDefn("#include \"cob.h\"\n");
		Token base = ctx.base;
		if (base != null) writeDefn("#include \"",base.getText(),".h\"\n");
		writeDefn("typedef struct ",name,"_Object *",name,";\n");
		writeDefn("struct ",name,"_Class;\n");
		writeDefn("struct ",name,"_Fields {\n");
		if (base != null) writeDefn("  struct ",base.getText(),"_Fields _base;\n");
		for (CobParser.MemberContext decl : ctx.member()) {
			visit(decl);
		}
		writeDefn("};\n");
		writeDefn("struct ",name,"_Object {\n");
		writeDefn("  struct ",name,"_Class *class;\n");
		writeDefn("  struct ",name,"_Fields fields;\n");
		writeDefn("};\n");
		return null;
	}
	
	@Override public Void visitField(CobParser.FieldContext ctx) {
		//	'static'? type ID ( '=' code )? ( ',' ID ( '=' code )? )* ';'
		if (ctx.stat != null) return null;  // static fields are not included in the object instance
		CobParser.TypeContext type = ctx.type();
		String typeName = type.typeName().getText();
		String array = "";
		if (type.getChildCount() > 1) array = "*";
		for (TerminalNode id : ctx.ID()) {
			writeDefn("  ",typeName," ",array,id.getText(),";\n");
		}
		return null;
	}
	
}
