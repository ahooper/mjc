package ca.nevdull.j1.compiler;

public class ErrorType implements Type {
	static ErrorType singleton = new ErrorType();
	
	ErrorType get() {
		return singleton;
	}
	
	public String toString() {
		return "ErrorType";
	}

}
