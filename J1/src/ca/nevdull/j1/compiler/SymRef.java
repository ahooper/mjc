package ca.nevdull.j1.compiler;

public class SymRef extends ValRef {
	Symbol sym;
	
	public SymRef(Symbol sym) {
		this.sym = sym;
	}
	
	public Type getType() {
		return sym.getType();
	}
	
	public String toString() {
		return sym+"@"+sym.location;
	}

}
