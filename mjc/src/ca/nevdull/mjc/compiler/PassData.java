package ca.nevdull.mjc.compiler;

import java.io.File;

import joptsimple.OptionSet;
import org.antlr.v4.runtime.tree.ParseTreeProperty;

public class PassData {
	public OptionSet options;
	public static final char PATH_SEPARATOR_CHAR = ':';
	public static final String BOOTCLASSPATH_OPTION = "bootclasspath";
	public static final String[] defaultBootClassPath = {"src/ca/nevdull/mjc/lib"};
	public static final String CLASSPATH_OPTION = "classpath";
	public static final String[] defaultClassPath = {""/*i.e. current directory*/};

    public static final String IMPORT_SUFFIX = ".import";

    File inputDir;
	public ParseTreeProperty<Scope> scopes = new ParseTreeProperty<Scope>();
	public GlobalScope globals;
	public ParseTreeProperty<Symbol> symbols = new ParseTreeProperty<Symbol>();
    ParseTreeProperty<Type> types = new ParseTreeProperty<Type>();
}