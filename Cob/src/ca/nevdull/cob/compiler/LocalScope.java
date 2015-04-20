package ca.nevdull.cob.compiler;

public class LocalScope extends BaseScope {

	public LocalScope(int lineNumber, Scope enclosingScope) {
		super("local@"+lineNumber, enclosingScope);
	}

}
