/* a little like a string */
class T1 : Base {
    char[] content;
    int offset, length;
    
    // return position of character in content, or -1 if not present
    int index(char c) {
    	for (int x = 0; x < this.length; x += 1) {
    	    if (this.content[this.offset+x] == c) return x;
    	}
    	return -1;
    }
    
}