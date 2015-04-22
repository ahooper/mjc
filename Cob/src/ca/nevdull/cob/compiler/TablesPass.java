package ca.nevdull.cob.compiler;

// Produce the class method list initialization to the class implementation file,
// and the class symbol outline to the import file

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
		Token base = ctx.base;
		writeImpl("struct ",name,"_Class ",name,"_Class={\n");
		writeImpl("  .class={\n");
		writeImpl("    .classInitialized=0,\n");
		writeImpl("    .className=\"",name,"\",\n");
		writeImpl("  //.packageName=\n");
		writeImpl("  //.enclosingClassName=\n");
		writeImpl("  //.enclosingMethodName=\n");
		if (base != null) {
			writeImpl("    .baseType=&",base.getText(),"_Class,\n");
		} else {
			writeImpl("    .baseType=0,\n");
		}
		writeImpl("  //.arrayType=\n");
		writeImpl("  },\n");
		writeImpl("  .methods={\n");
		if (base != null) {
			writeImpl("    ._base=&",base.getText(),"_Methods,\n");
		}
		for (CobParser.MemberContext member : ctx.member()) {
			visit(member);
		}
		writeImpl("  }\n");
		writeImpl("};\n");
		// Save symbols for import
        try {
			ClassSymbol klass = ctx.defn;
			PrintWriter impWriter = passData.openFileWriter(klass.getName(),Main.IMPORT_SUFFIX);
			impWriter.append("// Generated at ").append(passData.timeStamp).append("\n");
			impWriter.append("// From ").append(passData.sourceFileName).append("\n");
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
		//	'static'? type ID '(' arguments? ')' compoundStatement
		if (ctx.stat != null) return null;
		CobParser.KlassContext parent = (CobParser.KlassContext)ctx.getParent();
		String className = parent.name.getText();
		TerminalNode id = ctx.ID();
		writeImpl("    .",id.getText(),"=&",className,"_",id.getText(),",\n");
		return null;
	}
	
}
