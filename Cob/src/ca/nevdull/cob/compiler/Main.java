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

	///// Key file name elements
	
	public static final String PATH_SEPARATOR = ":"; 
			// could use File.pathSeparator, but prefer a platform-independent separator

    public static final String IMPORT_SUFFIX = ".import";
    public static final String DEFN_SUFFIX = ".h";
    public static final String IMPL_SUFFIX = ".c";

	///// Compilation options

	public static final String BOOT_CLASS_PATH_OPTION = "-B";
	public static final String[] defaultBootClassPath = {"/Users/andy/Software/git/mjc/cob/src/ca/nevdull/cob/lib/c"};
	String[] bootClassPath = defaultBootClassPath;
	
	public static final String CLASS_PATH_OPTION = "-L";
	public static final String[] defaultClassPath = {""/*i.e. current directory*/};
	String[] classPath = defaultClassPath;

	public static final String OUTPUT_DIRECTORY_OPTION = "-d";
	public static final String defaultOutputDirectory = null/*i.e. from input file*/; 
	String outputDirectory = defaultOutputDirectory;

	public static final String TRACE_OPTION = "-t";
	HashSet<String> trace = new HashSet<String>();

    public static final String NO_BASE_OPTION = "-nobase";
    boolean no_base = false;
    
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
		} else if (arg.equals(NO_BASE_OPTION)) {
			no_base = true;
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
    	String source = t.getInputStream().getSourceName();
    	if (!source.isEmpty()) { System.err.print(source); System.err.print(" "); }
        errprintf("Line %d@%d at %s: %s - %s\n", t.getLine(), t.getCharPositionInLine()+1, t.getText(), text, caller);
    }

	public static void error(Token t, String text) {
    	String source = t.getInputStream().getSourceName();
    	if (!source.isEmpty()) { System.err.print(source); System.err.print(" "); }
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
	    	String source = recognizer.getInputStream().getSourceName();
	    	if (!source.isEmpty()) { System.err.print(source); System.err.print(" "); }
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
				input.name = "STDIN";
				unitName = "anonymous";
				passData.sourceFileName = "STDIN";
			} else {
				File inFile = new File(arg);
				input = new ANTLRInputStream(new InputStreamReader(new FileInputStream(inFile),"UTF-8"));
				input.name = arg;
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
	        if (trace.contains("Parser")) parser.setTrace(true);
	        ParseTree tree = parser.file();
	        passData.parser = parser; // to produce meaningful tree node labels

	        // Walk the syntax tree several times to produce the various output elements
	        
	        // Collect the class scope and symbol type structure, and attach it to the parse tree
	        DefinitionPass definitionPass = new DefinitionPass(passData);
	        definitionPass.visit(tree);
	        //System.out.println(DIVIDER);
 	        
 			// Produce the class instance structure (object fields) to the class definition file
//	        ObjectPass objectPass = new ObjectPass(passData);
	        NewObjectPass objectPass = new NewObjectPass(passData);
	        objectPass.visit(tree);
/*
	        // Produce the class methods list structure to the class definition file
	        MethodsPass methodsPass = new MethodsPass(passData,false**staticPass**);
	        methodsPass.visit(tree);
	        MethodsPass staticPass = new MethodsPass(passData,trues**taticPass**);
	        staticPass.visit(tree);
*/
	        // Produce the target language code to the class implementation file
	        CodePass codePass = new CodePass(passData);
	        codePass.visit(tree);
	        // Produce the class method list initialization to the class implementation file, 
//	        TablesPass tablesPass = new TablesPass(passData);
	        /*
	        NewTablesPass tablesPass = new NewTablesPass(passData);
	        tablesPass.visit(tree);
	        */
	        
		} catch (IOException excp) {
			error(excp.getMessage());
		}
		if (errorCount > 0) {
			System.err.printf("%s: %d error%s\n", arg, errorCount, (errorCount>1)?"s":"");
		}
	}

}
