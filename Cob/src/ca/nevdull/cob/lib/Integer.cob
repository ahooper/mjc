class Integer : Object {
  int value;
  static int MIN_VALUE = 0x80000000;
  static int MAX_VALUE = 0x7fffffff;
  
  static String toHexString(int i) {
    return toUnsignedString(i, 4);
  }
  static String toOctalString(int i) {
    return toUnsignedString(i, 3);
  }
  static String toBinaryString(int i) {
    return toUnsignedString(i, 1);
  }
  static String toUnsignedString(int num, int exp) {
    // Use an array large enough for a binary number.
    int mask = (1 << exp) - 1;
    char[] buffer = new char[32];
    int i = 32;
    char d;
    do {
    	d = (num & mask) + 0x30;
    	if (d > 0x39) d += 0x21;
        buffer[--i] = d;
        num >>>= exp;
    } while (num != 0);
    return new String(buffer, i, 32 - i, true);
  }
}
