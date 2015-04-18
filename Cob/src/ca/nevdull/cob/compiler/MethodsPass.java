package ca.nevdull.cob.compiler;

import org.antlr.v4.runtime.Parser;
import org.antlr.v4.runtime.tree.TerminalNode;

public class MethodsPass extends PassCommon {
	
	public MethodsPass(Main main, Parser parser, String outoutDir) {
		super(main, parser, outoutDir);
	}

	@Override public Void visitKlass(CobParser.KlassContext ctx) {
		String name = ctx.name.getText();
		String parent = ctx.parent.getText();
		out("struct ",name,"_Methods {\n");
		if (!parent.equals(PassCommon.NULL_PARENT)) out("    struct ",parent,"_Methods parent;\n");
		for (CobParser.MemberContext decl : ctx.member()) {
			visit(decl);
		}
		out("};\n");
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
		out("    ",typeName," (*",id.getText(),")",array,"(",className," this");
		CobParser.ArgumentsContext arguments = ctx.arguments();
		if (arguments != null) {
			String sep = ",";
			for (CobParser.ArgumentContext argument : arguments.argument()) {
				out(sep);  sep = ",";
				visit(argument);
			}
		}
		out(");\n");
		return null;
	}
	
	@Override public Void visitArgument(CobParser.ArgumentContext ctx) {
		CobParser.TypeContext type = ctx.type();
		String typeName = type.typeName().getText();
		String array = "";
		if (type.getChildCount() > 1) array = "[]";
		out(typeName," ",ctx.ID().getText(),array);
		return null;
	}
	
}
