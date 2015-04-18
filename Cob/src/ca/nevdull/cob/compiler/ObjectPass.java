package ca.nevdull.cob.compiler;

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
		writeDefn("#ifndef ",name,"_DEFN\n");
		writeDefn("#define ",name,"_DEFN\n");
		writeDefn("#include \"cob.h\"\n");
		Token parent = ctx.parent;
		if (parent != null) writeDefn("#include \"",parent.getText(),".h\"\n");
		writeDefn("typedef struct ",name,"_Object *",name,";\n");
		writeDefn("extern struct ClassTable ",name,"_Class;\n");
		writeDefn("struct ",name,"_Object {\n");
		if (parent != null) writeDefn("    struct ",parent.getText(),"_Object parent;\n");
		for (CobParser.MemberContext decl : ctx.member()) {
			visit(decl);
		}
		writeDefn("};\n");
		writeDefn("#endif /*",name,"_DEFN*/\n");
		return null;
	}
	
	@Override public Void visitField(CobParser.FieldContext ctx) {
		//	'static'? type ID ( '=' code )? ( ',' ID ( '=' code )? )* ';'
		CobParser.TypeContext type = ctx.type();
		String typeName = type.typeName().getText();
		String array = "";
		if (type.getChildCount() > 1) array = "[]";
		for (TerminalNode id : ctx.ID()) {
			writeDefn("    ",typeName," ",id.getText(),array,";\n");
		}
		return null;
	}
	
}
