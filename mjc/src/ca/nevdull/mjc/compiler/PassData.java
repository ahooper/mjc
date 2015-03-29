package ca.nevdull.mjc.compiler;

import java.io.File;

import org.antlr.v4.runtime.tree.ParseTreeProperty;

public class PassData {

	Compiler options;
    File inputDir;
	public MJParser parser;
	public GlobalScope globals;
}