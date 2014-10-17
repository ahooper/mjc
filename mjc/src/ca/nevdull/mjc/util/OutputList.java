package ca.nevdull.mjc.util;

import java.io.PrintStream;
import java.util.ArrayList;

public class OutputList extends OutputItem {
	ArrayList<OutputItem> list;

	public OutputList() {
		super();
		this.list = new ArrayList<OutputItem>();
	}
	
	public OutputList add(OutputItem b) {
		list.add(b);
		return this;
	}
	
	public OutputList add(String... sList) {
		for (String s : sList) {
			list.add(new OutputAtom(s));
		}
		return this;
	}

	@Override
	public void print(PrintStream stream) {
		for (OutputItem el : list) {
			if (el == null) {
				stream.print("!NULL!");  // highlight for debugging
			} else {
				el.print(stream);
			}
		}
	}

	@Override
	public String toString() {
		return "OutputList [list=" + list + "]";
	}

}
