package ca.nevdull.cob.compiler;

import org.antlr.v4.runtime.ANTLRInputStream;
import org.antlr.v4.runtime.BaseErrorListener;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.Parser;
import org.antlr.v4.runtime.RecognitionException;
import org.antlr.v4.runtime.Recognizer;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.tree.TerminalNode;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.ParseTreeWalker;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.ListIterator;
import java.util.regex.Pattern;

public class Main {

	public static final String PATH_SEPARATOR = ":"; // or File.pathSeparator

	public static final String BOOT_CLASS_PATH_OPTION = "-B";
	public static final String[] defaultBootClassPath = {"/Users/andy/Software/git/mjc/mjc/src/ca/nevdull/cob/lib"};
	String[] bootClassPath = defaultBootClassPath;
	
	public static final String CLASS_PATH_OPTION = "-L";
	public static final String[] defaultClassPath = {""/*i.e. current directory*/};
	String[] classPath = defaultClassPath;

	public static final String OUTPUT_DIRECTORY_OPTION = "-L";
	public static final String defaultOutputDirectory = null/*i.e. from input file*/; 
	String outputDirectory = defaultOutputDirectory;

	public static final String TRACE_OPTION = "-t";
	HashSet<String> trace = new HashSet<String>();

    public static final String IMPORT_SUFFIX = ".import";

    static int errorCount = 0;
	private static final int ERROR_LIMIT = 100;

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

	private static void errprintf(String format, Object... args) {
		//TODO if (errorCount >= ERROR_LIMIT) throw new Exception("too many errors");
		System.err.print("\033[1;31m");
		System.err.printf(format, args);
		System.err.print("\033[0m");
        System.err.flush();
        errorCount  += 1;
	}
	
    public static void error(Token t, String text, String caller) {
        errprintf("Line %d@%d at %s: %s - %s\n", t.getLine(), t.getCharPositionInLine()+1, t.getText(), text, caller);
    }

	public static void error(Token t, String text) {
        errprintf("Line %d@%d at %s: %s\n", t.getLine(), t.getCharPositionInLine()+1, t.getText(), text);
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
        System.err.printf("Line %d@%d at %s: %s\n", t.getLine(), t.getCharPositionInLine()+1, t.getText(),
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
	                            RecognitionException e) {
	        errprintf("line %d@%d at %s:%s\n",line,charPositionInLine,offendingSymbol,msg);
	        List<String> stack = ((Parser)recognizer).getRuleInvocationStack();
	        Collections.reverse(stack);
	        System.err.println("rule stack: "+stack);
	    }

	}

	private static final String DIVIDER = "\n------------------------------------------------------------\n";

	private void compile(String arg) {
		ANTLRInputStream input;
		errorCount = 0;
		try {

			String unitName, codePath = ".";;
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
				x = unitName.lastIndexOf('.');
				if (x > 0) unitName = unitName.substring(0,x);
				System.out.print("---------- ");
				System.out.println(arg);
				System.out.flush();
			}
	        String outputDir = (outputDirectory != null) ?  outputDirectory : codePath;
	        CobLexer lexer = new CobLexer(input);
	        CommonTokenStream tokens = new CommonTokenStream(lexer);
	        CobParser parser = new CobParser(tokens);
	        parser.removeErrorListeners(); // remove ConsoleErrorListener
	        parser.addErrorListener(new VerboseListener()); // add ours
	        parser.setBuildParseTree(true);
	        ParseTree tree = parser.file();
	        PassCommon objectPass = new ObjectPass(this,parser,outputDir);
	        objectPass.visit(tree);
	        MethodsPass methodsPass = new MethodsPass(this,parser,outputDir);
	        methodsPass.visit(tree);
	        CodePass codePass = new CodePass(this,parser,outputDir);
	        codePass.visit(tree);
	        TablesPass tablesPass = new TablesPass(this,parser,outputDir);
	        tablesPass.visit(tree);
	        //System.out.println(DIVIDER);
		} catch (IOException excp) {
			error(excp.getMessage());
		}
		if (errorCount > 0) {
			System.err.printf("%s: %d error%s\n", arg, errorCount, (errorCount>1)?"s":"");
		}
	}

}
