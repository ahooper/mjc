package ca.nevdull.cob.compiler;

// Common elements of each processing pass 

import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.TerminalNodeImpl;

public class PassCommon extends CobBaseVisitor<Void> {

	protected static final String INSTANCEINIT = "_init_";
	protected static final String CLASSINIT = "_classinit_";
	protected static final String INIT = "__INIT";
	protected static final String NEW = "_NEW";
	protected PassData passData;

	public PassCommon(PassData data) {
		super();
		this.passData = data;
	}

	// Print syntax tree for debugging
	public void printContextTree(ParseTree t, String indent) {
		Main.debugn("%s%s",indent,t.getClass().getSimpleName());
		if (t instanceof TerminalNodeImpl) {
			Main.debugn(" %s", (TerminalNodeImpl)t);
		}
		Main.debug("");
		if (t.getChildCount() == 0) {
		} else {
			indent = "    "+indent;
			for (int i = 0; i<t.getChildCount(); i++) {
				printContextTree(t.getChild(i), indent);
			}
		}
	}

	// Write to class definitions (classname.h) output file
	protected void writeDefn(String... list) {
		for (String s : list) {
			passData.defnStream.print(s);
		}
	}

	// Write to class implementation (classname.c) output file
	protected void writeImpl(String... list) {
		for (String s : list) {
			passData.implStream.print(s);
		}
	}

}