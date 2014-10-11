package ca.nevdull.mjc.util;

import java.io.PrintStream;

public class AtomBuilder extends OutputBuilder {
	String text;
	
	public AtomBuilder(String text) {
		super();
		this.text = text;
	}

	@Override
	public void render(PrintStream stream) {
		stream.print(text);
	}

}
