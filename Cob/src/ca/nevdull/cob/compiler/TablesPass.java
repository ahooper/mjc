package ca.nevdull.cob.compiler;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Map.Entry;

import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.tree.TerminalNode;

public class TablesPass extends PassCommon {
	
	public TablesPass(PassData data) {
		super(data);
	}

	@Override public Void visitKlass(CobParser.KlassContext ctx) {
		String name = ctx.name.getText();
		Token parent = ctx.parent;
		writeImpl("struct ",name,"_Methods ",name,"_Methods = {\n");
		for (CobParser.MemberContext decl : ctx.member()) {
			visit(decl);
		}
		writeImpl("};\n");
		// Save symbols for import
        try {
			ClassSymbol klass = ctx.defn;
			PrintWriter impWriter = passData.openFileWriter(klass.getName(),Main.IMPORT_SUFFIX);
			for (Entry<String, Symbol> globEnt : passData.globals.getMembers().entrySet()) {
				Symbol globSym = globEnt.getValue();
				if (globSym == klass) continue;
				impWriter.append("import ").append(globSym.getName()).append(";\n");
			}
	        klass.writeImport(impWriter);
	        impWriter.close();
		} catch (IOException excp) {
			Main.error("Unable to write import symbols "+excp.getMessage());
		}
		return null;
	}
	
	@Override public Void visitMethod(CobParser.MethodContext ctx) {
		//	'static'? type ID '(' arguments? ')' '{' code '}'
		CobParser.KlassContext parent = (CobParser.KlassContext)ctx.getParent();
		String className = parent.name.getText();
		TerminalNode id = ctx.ID();
		writeImpl("    .",id.getText(),"=&",className,"_",id.getText(),",\n");
		return null;
	}
	
}
