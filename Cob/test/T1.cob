class T1 : Base {
    char[] content;
    int offset, length;
    static int serial;
    
    // return position of character in content, or -1 if not present
    int index(char c) {
        for (int x = 0; x < this.length; x += 1) {
    	    if (this.content[this.offset+x] == c) return x;
        }
        return -1;
    }
    static T1 copy(T1 model) {
        int x = serial+1;
    	return T1.new();
    }
}