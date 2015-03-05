package ca.nevdull.j1.compiler;

public class CompilationUnit extends BaseScope {
	String name;
	ClassSymbol stringClass;
	
	public CompilationUnit(String name) {
		super(/*enclosingScope:*/null);
		this.name = name;
		this.stringClass = new ClassSymbol(this,"String");
		this.define(this.stringClass);
	}

	public String getName() {
		return "unit "+name;
	}

}
