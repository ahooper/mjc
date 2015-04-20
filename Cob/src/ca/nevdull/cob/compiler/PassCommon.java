package ca.nevdull.cob.compiler;

// Common elements of each processing pass 

import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.TerminalNodeImpl;

public class PassCommon extends CobBaseVisitor<Void> {

	protected PassData passData;

	public PassCommon(PassData data) {
		super();
		this.passData = data;
	}

	// Print syntax tree for debugging
	public void printContextTree(ParseTree t, String indent) {
		System.out.print(indent);
		System.out.print(t.getClass().getSimpleName());
		if (t instanceof TerminalNodeImpl) {
			System.out.print(" ");
			System.out.print((TerminalNodeImpl)t);
		}
		System.out.println();
		if (t.getChildCount() == 0) {
		} else {
			indent = "    "+indent;
			for (int i = 0; i<t.getChildCount(); i++) {
				printContextTree(t.getChild(i), indent);
			}
		}
	}

	// Write code to class definitions (classname.h) output file
	protected void writeDefn(String... list) {
		for (String s : list) {
			passData.defnStream.print(s);
		}
	}

	// Write code to class implementation (classname.c) output file
	protected void writeImpl(String... list) {
		for (String s : list) {
			passData.implStream.print(s);
		}
	}

}