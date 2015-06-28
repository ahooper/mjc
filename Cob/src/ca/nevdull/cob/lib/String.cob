class String : Object {
  char[] value;
  int count;
  int cachedHashCode;
  int offset;
  
  String toString() {
	return this;
  }
  String(char[] data, int offset, int count) {
    this(data, offset, count, false);
  }
  int length() {
    return count;
  }
  
}
