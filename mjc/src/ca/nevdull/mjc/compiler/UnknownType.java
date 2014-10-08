package ca.nevdull.mjc.compiler;

public class UnknownType extends Symbol implements Type {
	private static final UnknownType INSTANCE = new UnknownType();
	 
    private UnknownType() {
    	super("unknown");
    }
 
    public static UnknownType getInstance() {
        return INSTANCE;
    }
    
	public String toString() {
		return "unknown";
	}
}
