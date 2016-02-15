package ca.nevdull.mjc1.compiler;

import org.antlr.v4.runtime.ANTLRInputStream;
import org.antlr.v4.runtime.BaseErrorListener;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.Parser;
import org.antlr.v4.runtime.RecognitionException;
import org.antlr.v4.runtime.Recognizer;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.tree.TerminalNode;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.ListIterator;

public class Main {

	public static void main(String[] args) {
		Main compiler = new Main();
		boolean any = false;
		for (ListIterator<String> argIter = Arrays.asList(args).listIterator();
			 argIter.hasNext(); ) {
			String arg = argIter.next();
			if (arg.startsWith("-")) {
				compiler.option(arg,argIter);
			} else {
        		compiler.compile(arg);
        		any = true;
        	}
        }
        if (!any) compiler.compile(null);
	}

	private void option(String arg, ListIterator<String> argIter) {
		if (arg.equals(TRACE_OPTION) && argIter.hasNext()) {
			trace.add(argIter.next());
		} else {
			error("Unrecognized option "+arg);
		}
	}

	public static final String TRACE_OPTION = "-t";
	HashSet<String> trace = new HashSet<String>();

	// Compilation of a single input file
	
	private void compile(String arg) {
		ANTLRInputStream input;
		try {

			File inFile = new File(arg);
			input = new ANTLRInputStream(new InputStreamReader(new FileInputStream(inFile),"UTF-8"));
			input.name = arg;
			System.out.print("---------- ");
			System.out.println(arg);
			System.out.flush();
	        
	        MJ1Lexer lexer = new MJ1Lexer(input);
	        CommonTokenStream tokens = new CommonTokenStream(lexer);
	        MJ1Parser parser = new MJ1Parser(tokens);
	        parser.removeErrorListeners(); // replace ConsoleErrorListener
	        parser.addErrorListener(new VerboseListener()); // with ours
	        parser.setBuildParseTree(true);
	        if (trace.contains("Parser")) parser.setTrace(true);
	        MJ1Parser.CompilationUnitContext parse = parser.compilationUnit();
	        DefinitionPass definitions = new DefinitionPass(arg);
	        definitions.visit(parse);
	        CodePass code = new CodePass(arg);
	        code.visit(parse);
	        
		} catch (IOException excp) {
			error(excp.getMessage());
		}
	}

	// Error display
	
	private static void errprintf(String format, Object... args) {
		//TODO if (errorCount >= ERROR_LIMIT) throw new Exception("too many errors");
		System.err.printf(format, args);
        System.err.flush();
	}

	private static final String ERROR_D_D_S_S = "%d:%d '%s': %s\n";

	public static void error(int line, int charPositionInLine, String token, String text) {
        errprintf(Main.ERROR_D_D_S_S, line, charPositionInLine, token, text);
    }

	public static void error(Token t, String text) {
    	String source = t.getInputStream().getSourceName();
    	if (!source.isEmpty()) { System.err.print(source); System.err.print(":"); }
        error(t.getLine(), t.getCharPositionInLine()+1, t.getText(), text);
    }

	public static void error(TerminalNode tn, String text) {
		error(tn.getSymbol(), text);
    }
	
    public static void error(String text) {
        errprintf("%s\n", text);
    }

    public static void note(Token t, String text) {
        errprintf(Main.ERROR_D_D_S_S, t.getLine(), t.getCharPositionInLine(), t.getText(), text);
    }

	public static void debug(String format, Object... args) {
		System.out.printf(format, args);
		System.out.println();
        System.out.flush();
	}
	
	// Syntax error display for ANTLR parsing

	public static class VerboseListener extends BaseErrorListener {
	    @Override
	    public void syntaxError(Recognizer<?, ?> recognizer,
	                            Object offendingSymbol,
	                            int line, int charPositionInLine,
	                            String msg,
	                            RecognitionException e) {
	        error((Token)offendingSymbol,msg);
	        List<String> stack = ((Parser)recognizer).getRuleInvocationStack();
	        Collections.reverse(stack);
	        System.err.println("    rule stack: "+stack);
	    }
	}

}
