package ca.nevdull.cob.compiler;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.io.PrintWriter;

import org.antlr.v4.runtime.Parser;

public class PassData {
	public Main main;
	public Parser parser;
	public String outputDir;
	public BaseScope globals;
	public PrintStream defnStream;
	public PrintStream implStream;

	public PassData(Main main, Parser parser, String outputDir) {
		super();
		this.main = main;
		this.parser = parser;
		this.outputDir = outputDir;
	}

	public File makeFileName(String className, String suffix) {
		String name = className+suffix;
		return this.outputDir == null ? new File(name)
									  : new File(this.outputDir,name);
	}
	
	public PrintStream openFileStream(String className, String suffix) throws FileNotFoundException {
		FileOutputStream fos = new FileOutputStream(makeFileName(className,suffix));
	    return new PrintStream(fos);
	}
	
	public PrintWriter openFileWriter(String className, String suffix) throws FileNotFoundException {
		FileOutputStream fos = new FileOutputStream(makeFileName(className,suffix));
	    return new PrintWriter(fos);
	}
}