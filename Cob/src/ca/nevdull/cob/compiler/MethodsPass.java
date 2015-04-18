package ca.nevdull.cob.compiler;

import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.tree.TerminalNode;

public class MethodsPass extends PassCommon {
	
	public MethodsPass(PassData data) {
		super(data);
	}

	@Override public Void visitKlass(CobParser.KlassContext ctx) {
		String name = ctx.name.getText();
		writeDefn("struct ",name,"_Methods {\n");
		Token parent = ctx.parent;
		if (parent != null) writeDefn("    struct ",parent.getText(),"_Methods parent;\n");
		for (CobParser.MemberContext decl : ctx.member()) {
			visit(decl);
		}
		writeDefn("};\n");
		return null;
	}
	
	@Override public Void visitMethod(CobParser.MethodContext ctx) {
		//	'static'? type ID '(' arguments? ')' '{' code '}'
		CobParser.KlassContext parent = (CobParser.KlassContext)ctx.getParent();
		String className = parent.name.getText();
		//printContextTree(parent,"    ");
		CobParser.TypeContext type = ctx.type();
		String typeName = type.typeName().getText();
		String array = "";
		if (type.getChildCount() > 1) array = "[]";
		TerminalNode id = ctx.ID();
		writeDefn("    ",typeName," (*",id.getText(),")",array,"(",className," this");
		CobParser.ArgumentsContext arguments = ctx.arguments();
		if (arguments != null) {
			String sep = ",";
			for (CobParser.ArgumentContext argument : arguments.argument()) {
				writeDefn(sep);  sep = ",";
				visit(argument);
			}
		}
		writeDefn(");\n");
		return null;
	}
	
	@Override public Void visitArgument(CobParser.ArgumentContext ctx) {
		CobParser.TypeContext type = ctx.type();
		String typeName = type.typeName().getText();
		String array = "";
		if (type.getChildCount() > 1) array = "[]";
		writeDefn(typeName," ",ctx.ID().getText(),array);
		return null;
	}
	
}
