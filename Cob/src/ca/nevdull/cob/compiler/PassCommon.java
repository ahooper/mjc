package ca.nevdull.cob.compiler;

import org.antlr.v4.runtime.Parser;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.TerminalNodeImpl;

public class PassCommon extends CobBaseVisitor<Void> {

	protected Main main;
	protected Parser parser;
	protected String outoutDir;
	
	protected static final String NULL_PARENT = "null";

	public PassCommon(Main main, Parser parser, String outoutDir) {
		super();
		this.main = main;
		this.parser = parser;
		this.outoutDir = outoutDir;
	}

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

	protected void out(String... list) {
		for (String s : list) {
			System.out.print(s);
		}
	}

}