package importer;

/**
 * Note: This class was directly copied from yoyo package
 */
public class StringStream {

    String input;
    int current = 0;
	
    public StringStream(String in) {
	input = in;
	current = 0;
    }
	
    public void reset() {
	current = 0;
    }

    public boolean empty() {
	if (current >= input.length()) return true;
	return false;
    }

    public char peek() {
	if (current < input.length()) return input.charAt(current);
	return (char)-1;
    }

    public char peekpeek() {
	if (current < input.length()-1) return input.charAt(current+1);
	return (char)-1;
    }
	
    public char read() {
	if (current < input.length()) {
	    //System.out.println("read byte: " + input.charAt(current));
	    return input.charAt(current++);
	}
	return (char)-1;
    }

	
}
