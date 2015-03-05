package ca.nevdull.j1.compiler;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.ListIterator;
import java.util.regex.Pattern;

import org.antlr.v4.runtime.ANTLRInputStream;
import org.antlr.v4.runtime.BaseErrorListener;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.Parser;
import org.antlr.v4.runtime.RecognitionException;
import org.antlr.v4.runtime.Recognizer;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.TerminalNode;

public class Compiler {
	
	private static final String sourceSuffix = ".j";
	static String[] systemPath = {"lib"};
	static String[] usePath = {"."};
	static int errorCount = 0;
	private static final int ERROR_LIMIT = 100;

	public static void main(String[] args) {
		boolean any = false;
		for (ListIterator<String> argIter = Arrays.asList(args).listIterator();
			 argIter.hasNext(); ) {
			String arg = argIter.next();
			if (arg.startsWith("-")) {
				option(arg,argIter);
			} else {
        		compile(arg);
        		any = true;
        	}
        }
        if (!any) compile(null);
	}
	
	static Pattern pathPat = Pattern.compile(File.pathSeparator,Pattern.LITERAL);
	private static String[] pathSplit(String arg) {
		return pathPat.split(arg,-1);
	}

	private static void option(String arg, ListIterator<String> argIter) {
		if (arg.equals("-S") && argIter.hasNext()) {
			systemPath = pathSplit(argIter.next());
		} else if (arg.equals("-L") && argIter.hasNext()) {
				usePath = pathSplit(argIter.next());;
		} else {
			error("Unrecognized option "+arg);
		}
	}
	
	private static void errprintf(String format, Object... args) {
		//TODO if (errorCount >= ERROR_LIMIT) throw new Exception("too many errors");
		System.err.printf(format, args);
        System.err.flush();
        errorCount += 1;
	}
	
    public static void error(Token t, String text, String caller) {
        errprintf("line %d@%d at %s: %s - %s\n", t.getLine(), t.getCharPositionInLine()+1, t.getText(), text, caller);
    }

	
    public static void error(TerminalNode tn, String text, String caller) {
    	error(tn.getSymbol(), text, caller);
    }

	public static void error(Token t, String text) {
        errprintf("line %d@%d at %s: %s\n", t.getLine(), t.getCharPositionInLine()+1, t.getText(), text);
    }

	public static void error(TerminalNode tn, String text) {
		error(tn.getSymbol(), text);
    }
	
    public static void error(String text) {
        errprintf("%s\n", text);
    }

	public static void note(Token t, String text) {
        System.err.printf("line %d@%d at %s: %s\n", t.getLine(), t.getCharPositionInLine()+1, t.getText(),
        		text);
        System.err.flush();
	}

	public static void debug(String text) {
        System.out.println(text);
        System.out.flush();
	}
	
	public static class VerboseListener extends BaseErrorListener {
	    @Override
	    public void syntaxError(Recognizer<?, ?> recognizer,
	                            Object offendingSymbol,
	                            int line, int charPositionInLine,
	                            String msg,
	                            RecognitionException e)
	    {
	        List<String> stack = ((Parser)recognizer).getRuleInvocationStack();
	        Collections.reverse(stack);
	        errprintf("line %d@%d at %s: %s\n", line, charPositionInLine+1, offendingSymbol, msg); 
	        System.err.printf("..rule stack: %s\n", stack);
	    }

	}

	private static void compile(String arg) {
		ANTLRInputStream input;
		errorCount = 0;
		try {
			String unitName, codePath = ".";
			if (arg == null) {
				input = new ANTLRInputStream(System.in);
				unitName = "anonymous";
			} else {
				File inFile = new File(arg);
				input = new ANTLRInputStream(new FileInputStream(inFile));
				int x = arg.lastIndexOf(File.separatorChar);
				if (x >= 0) {
					unitName = arg.substring(x+1);
					codePath = arg.substring(0,x);
				} else {
					unitName = arg;
				}
				if (unitName.endsWith(Compiler.sourceSuffix)) {
					unitName = unitName.substring(0, unitName.length()-Compiler.sourceSuffix.length());
				}
				System.out.print("---------- ");
				System.out.println(unitName);
				System.out.flush();
			}
	        J1Lexer lexer = new J1Lexer(input);
	        CommonTokenStream tokens = new CommonTokenStream(lexer);
	        J1Parser parser = new J1Parser(tokens);
	        parser.removeErrorListeners();
	        parser.addErrorListener(new VerboseListener());
	        parser.setBuildParseTree(true);
	        ParseTree tree = parser.compilationUnit();
	        CompilationUnit unit = new CompilationUnit(unitName);
	        DefVisitor defs = new DefVisitor(unit);
	        defs.visit(tree);
	        DeclRefVisitor declRefVisitor = new DeclRefVisitor(unit);
	        declRefVisitor.visit(tree);
	        ExprTypeVisitor exprTypeVisitor = new ExprTypeVisitor(unit);
	        exprTypeVisitor.visit(tree);
		} catch (IOException excp) {
			error(excp.getMessage());
		}
		if (errorCount > 0) {
			System.err.printf("%s: %d error%s\n", arg, errorCount, (errorCount>1)?"s":"");
		}
	}

}
