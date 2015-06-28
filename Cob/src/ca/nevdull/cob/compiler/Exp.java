package ca.nevdull.cob.compiler;

import java.io.PrintStream;
import java.util.ArrayList;

// Representation of expression code
public class Exp {
	ArrayList<Exp> list;
	
	// Leaf element of structure
	class ExpString extends Exp {
		// list field is unused
		String string;
		public ExpString(String string) {
			this.string = string;
		}
		public void write(PrintStream stream) {
			stream.print(string);
		}
		public String toString() {
			return string;
		}
	}

	public Exp() {
		list = new ArrayList<Exp>();
	}

	public Exp(String string) {
		//System.out.println("Yes, the single-arg Exp constructor does get called!");
		list = new ArrayList<Exp>();
		list.add(new ExpString(string));
	}

	public Exp(String... strings) {
		list = new ArrayList<Exp>();
		add(strings);
	}

	public Exp add(String string) {
		list.add(new ExpString(string));
		return this;  // for "fluent" interface
	}

	public Exp add(String... strings) {
		int n = strings.length;
		for (int i = 0;  i < n;  i++) list.add(new ExpString(strings[i]));
		return this;  // for "fluent" interface
	}

	public Exp add(Exp exp) {
		assert exp != null;
		list.add(exp);
		return this;  // for "fluent" interface
	}
	
	public void write(PrintStream stream) {
		for (Exp e : list) e.write(stream);
	}
	
	public String toString() {
		return list.toString();
	}

}
