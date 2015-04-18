package ca.nevdull.cob.compiler;

public interface Scope {

	public abstract String getName();

	public abstract Scope getEnclosingScope();

	public abstract Symbol find(String name);

	public abstract void add(Symbol symbol);
}