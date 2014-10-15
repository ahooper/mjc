package ca.nevdull.mjc.util;

import java.io.PrintStream;

public class OutputAtom extends OutputItem {
	String text;
	
	public OutputAtom(String text) {
		super();
		this.text = text;
	}

	@Override
	public void print(PrintStream stream) {
		stream.print(text);
	}

}