package ca.nevdull.cob.compiler;

// Common data that is used in all processing passes

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.antlr.v4.runtime.Parser;

public class PassData {
	public Main main;
	public String unitName;
	public String sourceFileName;
	public Parser parser;
	public String outputDir;
	public BaseScope globals;
	public PrintStream defnStream;
	public PrintStream implStream;
	public String timeStamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss Z").format(new Date());

	public PassData(Main main) {
		super();
		this.main = main;
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