package ca.nevdull.mjc.util;

import java.io.PrintStream;
import java.util.ArrayList;

public class ListBuilder extends OutputBuilder {
	ArrayList<OutputBuilder> list;
	String separator;

	public ListBuilder() {
		super();
		this.list = new ArrayList<OutputBuilder>();
		this.separator = null;
	}

	public ListBuilder(String separator) {
		this();
		this.separator = separator;
	}
	
	public ListBuilder add(OutputBuilder b) {
		list.add(b);
		return this;
	}
	
	public ListBuilder add(String... sList) {
		for (String s : sList) {
			list.add(new AtomBuilder(s));
		}
		return this;
	}

	@Override
	public void render(PrintStream stream) {
		for (OutputBuilder el : list) {
			el.render(stream);
		}
	}

}
