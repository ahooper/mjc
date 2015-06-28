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
		} else if (t instanceof CobParser.PrimaryContext) {
			printContextTipe(((CobParser.PrimaryContext)t).tipe);
			printContextExp(((CobParser.PrimaryContext)t).exp);
		} else if (t instanceof CobParser.ExpressionContext) {
			printContextTipe(((CobParser.ExpressionContext)t).tipe);
			printContextExp(((CobParser.ExpressionContext)t).exp);
		} else if (t instanceof CobParser.AssignmentContext) {
			printContextTipe(((CobParser.AssignmentContext)t).tipe);
			printContextExp(((CobParser.AssignmentContext)t).exp);
		} else if (t instanceof CobParser.SequenceContext) {
			printContextTipe(((CobParser.SequenceContext)t).tipe);
			printContextExp(((CobParser.SequenceContext)t).exp);
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

	private void printContextTipe(Type tipe) {
		Main.debugn(" type=%s", tipe==null?"null":tipe.toString());
	}

	private void printContextExp(Exp exp) {
		Main.debugn(" exp=%s", exp==null?"null":exp.toString());
	}

	// Write to class definitions (classname.h) output file
	protected void writeDefn(String s) {
		passData.defnStream.print(s);
	}

	protected void writeDefn(String... list) {
		for (String s : list) {
			passData.defnStream.print(s);
		}
	}

	// Write to class implementation (classname.c) output file
	protected void writeImpl(String s) {
		passData.implStream.print(s);
	}

	protected void writeImpl(String... list) {
		for (String s : list) {
			passData.implStream.print(s);
		}
	}

	// Write to class implementation (classname.c) output file
	protected void writeImpl(Exp exp) {
		exp.write(passData.implStream);
	}

}