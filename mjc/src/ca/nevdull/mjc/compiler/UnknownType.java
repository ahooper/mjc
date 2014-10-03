package ca.nevdull.mjc.compiler;

public class UnknownType extends Type {
	private static final UnknownType INSTANCE = new UnknownType();
	 
    private UnknownType() {}
 
    public static UnknownType getInstance() {
        return INSTANCE;
    }
    
	public String toString() {
		return "unknown";
	}
}
