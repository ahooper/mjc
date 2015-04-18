package ca.nevdull.cob.compiler;

import org.antlr.v4.runtime.Parser;
import org.antlr.v4.runtime.tree.TerminalNode;

public class ObjectPass extends PassCommon {

	public ObjectPass(Main main, Parser parser, String outoutDir) {
		super(main, parser, outoutDir);
	}

	@Override public Void visitKlass(CobParser.KlassContext ctx) {
		String name = ctx.name.getText();
		out("#ifndef ",name,"_DEFN\n");
		out("#define ",name,"_DEFN\n");
		out("#include \"cob.h\"\n");
		String parent = ctx.parent.getText();
		if (!parent.equals(PassCommon.NULL_PARENT)) out("#include \"",parent,".h\"\n");
		out("typedef struct ",name,"_Object *",name,";\n");
		out("extern struct ClassTable ",name,"_Class;\n");
		out("struct ",name,"_Object {\n");
		if (!parent.equals(PassCommon.NULL_PARENT)) out("    struct ",parent,"_Object parent;\n");
		for (CobParser.MemberContext decl : ctx.member()) {
			visit(decl);
		}
		out("};\n");
		out("#endif /*",name,"_DEFN*/\n");
		return null;
	}
	
	@Override public Void visitField(CobParser.FieldContext ctx) {
		//	'static'? type ID ( '=' code )? ( ',' ID ( '=' code )? )* ';'
		CobParser.TypeContext type = ctx.type();
		String typeName = type.typeName().getText();
		String array = "";
		if (type.getChildCount() > 1) array = "[]";
		for (TerminalNode id : ctx.ID()) {
			out("    ",typeName," ",id.getText(),array,";\n");
		}
		return null;
	}
	
}
