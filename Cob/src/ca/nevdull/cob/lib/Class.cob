class Class : Object {
	Class() {
		fatal("A Class may not be dynamically created.");
	}
	String toString() {
		return new String("class ",getName());
	}
	native String getName();
	native boolean isInstance(Object obj);
}
