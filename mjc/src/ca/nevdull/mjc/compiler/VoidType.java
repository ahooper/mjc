package ca.nevdull.mjc.compiler;

public class VoidType extends Symbol implements Type {

	private static final long serialVersionUID = -3467921449807277019L;

	// void is not really a type, but we pretend
	private static final VoidType INSTANCE = new VoidType();
	 
    private VoidType() {
    	super("void");
    }
 
    public static VoidType getInstance() {
        return INSTANCE;
    }

    public String toString() {
    	return "void";
    }
}
