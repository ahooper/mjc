class Object : null {
	native Class getClass();
	native int hashCode();
	boolean equals(Object obj) {
		return this == obj;
	}
	String toString() {
		return new String(getClass().getName(),"@",Integer.toHexString(hashCode()));
	}
}
