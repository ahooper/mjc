package ca.nevdull.cob.compiler;

import org.antlr.v4.runtime.Parser;
import org.antlr.v4.runtime.tree.TerminalNode;

public class TablesPass extends PassCommon {
	
	public TablesPass(Main main, Parser parser, String outoutDir) {
		super(main, parser, outoutDir);
	}

	@Override public Void visitKlass(CobParser.KlassContext ctx) {
		String name = ctx.name.getText();
		String parent = ctx.parent.getText();
		out("struct ",name,"_Methods ",name,"_Methods = {\n");
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
		TerminalNode id = ctx.ID();
		out("    .",id.getText(),"=&",className,"_",id.getText(),",\n");
		return null;
	}
	
}
