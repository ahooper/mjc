package ca.nevdull.cob.compiler;

// Cob to C compiler main program

import org.antlr.v4.runtime.ANTLRInputStream;
import org.antlr.v4.runtime.BaseErrorListener;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.Parser;
import org.antlr.v4.runtime.RecognitionException;
import org.antlr.v4.runtime.Recognizer;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.tree.TerminalNode;
import org.antlr.v4.runtime.tree.ParseTree;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.ListIterator;
import java.util.regex.Pattern;

public class Main {

	///// Compilation options
	
	public static final String PATH_SEPARATOR = ":"; 
			// could use File.pathSeparator, but prefer a platform-independent separator

	public static final String BOOT_CLASS_PATH_OPTION = "-B";
	public static final String[] defaultBootClassPath = {"/Users/andy/Software/git/mjc/mjc/src/ca/nevdull/cob/lib"};
	String[] bootClassPath = defaultBootClassPath;
	
	public static final String CLASS_PATH_OPTION = "-L";
	public static final String[] defaultClassPath = {""/*i.e. current directory*/};
	String[] classPath = defaultClassPath;

	public static final String OUTPUT_DIRECTORY_OPTION = "-d";
	public static final String defaultOutputDirectory = null/*i.e. from input file*/; 
	String outputDirectory = defaultOutputDirectory;

	public static final String TRACE_OPTION = "-t";
	HashSet<String> trace = new HashSet<String>();

    public static final String IMPORT_SUFFIX = ".import";
    public static final String DEFN_SUFFIX = ".h";
    public static final String IMPL_SUFFIX = ".c";

    ///// Error
    
    static int errorCount = 0;
	private static final int ERROR_LIMIT = 100;
	private static final String ERROR_LINE_D_D_AT_S_S = "Line %d@%d at %s: %s\n";  //printf format string

	///// Compiler main program and options processing
	
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
	
	static Pattern pathPat = Pattern.compile(PATH_SEPARATOR,Pattern.LITERAL);
	private String[] pathSplit(String arg) {
		return pathPat.split(arg,-1);
	}

	private void option(String arg, ListIterator<String> argIter) {
		if (arg.equals(BOOT_CLASS_PATH_OPTION) && argIter.hasNext()) {
			bootClassPath = pathSplit(argIter.next());
		} else if (arg.equals(CLASS_PATH_OPTION) && argIter.hasNext()) {
			classPath = pathSplit(argIter.next());
		} else if (arg.equals(OUTPUT_DIRECTORY_OPTION) && argIter.hasNext()) {
			outputDirectory = argIter.next();
		} else if (arg.equals(TRACE_OPTION) && argIter.hasNext()) {
			trace.add(argIter.next());
		} else {
			error("Unrecognized option "+arg);
		}
	}

	///// Error display
	
	private static void errprintf(String format, Object... args) {
		//TODO if (errorCount >= ERROR_LIMIT) throw new Exception("too many errors");
		System.err.print("\033[1;31m");
		System.err.printf(format, args);
		System.err.print("\033[0m");
        System.err.flush();
        errorCount  += 1;
	}

	public static void error(int line, int charPositionInLine, String token, String text) {
        errprintf(Main.ERROR_LINE_D_D_AT_S_S, line, charPositionInLine, token, text);
    }
	
    public static void error(Token t, String text, String caller) {
        errprintf("Line %d@%d at %s: %s - %s\n", t.getLine(), t.getCharPositionInLine()+1, t.getText(), text, caller);
    }

	public static void error(Token t, String text) {
        error(t.getLine(), t.getCharPositionInLine()+1, t.getText(), text);
    }

    public static void error(TerminalNode tn, String text, String caller) {
    	error(tn.getSymbol(), text, caller);
    }

	public static void error(TerminalNode tn, String text) {
		error(tn.getSymbol(), text);
    }
	
    public static void error(String text) {
        errprintf("%s\n", text);
    }

	public static void note(Token t, String text) {
        System.err.printf(Main.ERROR_LINE_D_D_AT_S_S, t.getLine(), t.getCharPositionInLine()+1, t.getText(),
        		text);
        System.err.flush();
	}

	public static void debug(String format, Object... args) {
		System.out.printf(format, args);
		System.out.println();
        System.out.flush();
	}

	public static void debugn(String format, Object... args) {
		System.out.printf(format, args);
	}
	
	// Syntax error display for ANTLR parsing
	public static class VerboseListener extends BaseErrorListener {
	    @Override
	    public void syntaxError(Recognizer<?, ?> recognizer,
	                            Object offendingSymbol,
	                            int line, int charPositionInLine,
	                            String msg,
	                            RecognitionException e) {
	        error(line,charPositionInLine+1,offendingSymbol.toString(),msg);
	        List<String> stack = ((Parser)recognizer).getRuleInvocationStack();
	        Collections.reverse(stack);
	        System.err.println("rule stack: "+stack);
	    }
	}

	// Compilation of a single input file, calling the parser, then 
	// the processing passes in sequence
	
	private static final String DIVIDER = ". . . . . . . . . . . . . . .";

	private void compile(String arg) {
		ANTLRInputStream input;
		errorCount = 0;
		try {

	        // Common data that is used in all processing passes
	        PassData passData = new PassData(this);

			// Determine variations of input file
	        
	        String unitName;
			passData.outputDir = ".";
			if (arg == null) {
				input = new ANTLRInputStream(System.in);
				unitName = "anonymous";
				passData.sourceFileName = "STDIN";
			} else {
				File inFile = new File(arg);
				input = new ANTLRInputStream(new InputStreamReader(new FileInputStream(inFile),"UTF-8"));
				int x = arg.lastIndexOf(File.separatorChar);
				if (x >= 0) {
					unitName = arg.substring(x+1);
					passData.outputDir = arg.substring(0,x);
				} else {
					unitName = arg;
				}
				x = unitName.lastIndexOf('.');
				if (x > 0) unitName = unitName.substring(0,x);
				System.out.print("---------- ");
				System.out.println(arg);
				System.out.flush();
				passData.sourceFileName = arg;
			}
			passData.unitName = unitName;
			if (outputDirectory != null) passData.outputDir = outputDirectory;  // override input file path
	        
	        // Parse the source file to produce a syntax tree
	        
	        CobLexer lexer = new CobLexer(input);
	        CommonTokenStream tokens = new CommonTokenStream(lexer);
	        CobParser parser = new CobParser(tokens);
	        parser.removeErrorListeners(); // remove ConsoleErrorListener
	        parser.addErrorListener(new VerboseListener()); // add ours
	        parser.setBuildParseTree(true);
	        ParseTree tree = parser.file();
	        passData.parser = parser; // to produce meaningful tree node labels

	        // Walk the syntax tree several times to produce the various output elements
	        
	        // Collect the class scope and symbol type structure, and attach it to the parse tree
	        DefinitionPass definitionPass = new DefinitionPass(passData);
	        definitionPass.visit(tree);
	        System.out.println(DIVIDER);
 	        // Produce the class instance structure (object fields) to the class definition file
	        ObjectPass objectPass = new ObjectPass(passData);
	        objectPass.visit(tree);
	        // Produce the class methods list structure to the class definition file
	        MethodsPass methodsPass = new MethodsPass(passData,false/*staticPass*/);
	        methodsPass.visit(tree);
	        MethodsPass staticPass = new MethodsPass(passData,true/*staticPass*/);
	        staticPass.visit(tree);
	        // Produce the target language code to the class implementation file
	        CodePass codePass = new CodePass(passData);
	        codePass.visit(tree);
	        // Produce the class method list initialization to the class implementation file, 
	        TablesPass tablesPass = new TablesPass(passData);
	        tablesPass.visit(tree);
	        
		} catch (IOException excp) {
			error(excp.getMessage());
		}
		if (errorCount > 0) {
			System.err.printf("%s: %d error%s\n", arg, errorCount, (errorCount>1)?"s":"");
		}
	}

}
