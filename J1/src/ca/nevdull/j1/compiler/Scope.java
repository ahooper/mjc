package ca.nevdull.j1.compiler;

public interface Scope {
	public void define(Symbol sym);
	public Symbol resolve(String name);
	public Scope getEnclosingScope();
	public String getName();
}
