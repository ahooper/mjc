package ca.nevdull.mjc.compiler;

import org.antlr.v4.runtime.ANTLRInputStream;
import org.antlr.v4.runtime.BaseErrorListener;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.RecognitionException;
import org.antlr.v4.runtime.Recognizer;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.tree.*;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

public class Compiler {
	
	/***
	 * Excerpted from "The Definitive ANTLR 4 Reference",
	 * published by The Pragmatic Bookshelf.
	 * Copyrights apply to this code. It may not be used to create training material, 
	 * courses, books, articles, and the like. Contact us if you are in doubt.
	 * We make no guarantees that this code is fit for any purpose. 
	 * Visit http://www.pragmaticprogrammer.com/titles/tpantlr2 for more book information.
	***/
	public static class UnderlineErrorListener extends BaseErrorListener {
		public void syntaxError(Recognizer<?, ?> recognizer,
					Object offendingSymbol,
					int line, int charPositionInLine,
					String msg,
					RecognitionException e)
	    {
	        System.err.println(line+":"+charPositionInLine+" "+msg);
	        underlineError(recognizer,(Token)offendingSymbol,
	                       line, charPositionInLine);
System.err.flush();
	    }

	    protected void underlineError(Recognizer<?, ?> recognizer,
	                                  Token offendingToken, int line,
	                                  int charPositionInLine) {
	        CommonTokenStream tokens =
	            (CommonTokenStream)recognizer.getInputStream();
	        String input = tokens.getTokenSource().getInputStream().toString();
	        String[] lines = input.split("\n");
	        String errorLine = lines[line - 1];
	        System.err.println(errorLine);
	        for (int i=0; i<charPositionInLine; i++) {
	        	if (errorLine.charAt(i) == '\t') System.err.print('\t');
	        	else System.err.print(' ');
	        }
	        int start = offendingToken.getStartIndex();
	        int stop = offendingToken.getStopIndex();
	        if ( start>=0 && stop>=0 ) {
	            for (int i=start; i<=stop; i++) System.err.print('^');
	        }
	        System.err.println();
	    }
	}
	
    public static void error(Token t, String msg) {
        System.err.printf("line %d@%d %s\n", t.getLine(), t.getCharPositionInLine(),
                          msg);
System.err.flush();
    }
	
    public static void error(Token t, String msg, String caller) {
        System.err.printf("line %d@%d %s - %s\n", t.getLine(), t.getCharPositionInLine(),
                          msg, caller);
System.err.flush();
    }

    public void process(String[] args) throws Exception {
        if ( args.length>0 ) {
        	for (String inputFile : args) {
	            InputStream is = new FileInputStream(inputFile);
	            process1(is);
        	}
        } else {
        	process1(System.in);
        }
    }

	/**
	 * @param is
	 * @throws IOException
	 * @throws RecognitionException
	 */
	public void process1(InputStream is) throws IOException,
			RecognitionException {
		ANTLRInputStream input = new ANTLRInputStream(is);
        MJLexer lexer = new MJLexer(input);
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        MJParser parser = new MJParser(tokens);
        parser.removeErrorListeners(); // remove ConsoleErrorListener
        parser.addErrorListener(new UnderlineErrorListener());
        parser.setBuildParseTree(true);
        ParseTree tree = parser.compilationUnit();
        // show tree in text form
        //System.out.println(tree.toStringTree(parser));

        ParseTreeWalker walker = new ParseTreeWalker();
        DefinitionPass def = new DefinitionPass();
        walker.walk(def, tree);
        System.out.println("\n------------------------------------------------------------\n");
        ReferencePass ref = new ReferencePass(def.scopes, def.globals, def.symbols);
        walker.walk(ref, tree);
        System.out.println("\n------------------------------------------------------------\n");
/*
        EmitVisit visitor = new EmitVisit();
        visitor.visit(tree);
*/
	}

	public static void main(String[] args) {
        try {
			new Compiler().process(args);
		} catch (Exception excp) {
			excp.printStackTrace();
		}
	}

}
