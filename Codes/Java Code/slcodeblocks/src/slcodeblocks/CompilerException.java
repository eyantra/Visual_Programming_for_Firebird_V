package slcodeblocks;

public class CompilerException extends Exception {

	private static final long serialVersionUID = 1L;

	public static enum Error {
        CUSTOM, INVALID_STACK, EMPTY_SOCKET, INVALID_NUMBER,
        INVALID_ARG, INVALID_VAR, OUTPUT_STACK, YIELD_STACK, ORPHAN_STUB
    }
	
	public Error errorType;
	public Long blockID;
	private String info;
	
	/** The stack this error originated from. Optionally used by compiler. */
	Long stack = null;
	
	public CompilerException(Error type, Long id, String s) {
	    errorType = type;
        blockID = id;
        info = (s == null ? "" : s);
	}
	
	@Override
	public String getMessage() {
		switch (errorType) {
		case INVALID_STACK:
			return "Cannot execute " + info + " code by double-clicking";
		case EMPTY_SOCKET:
			return "Empty "+ info + (info.length() == 0 ? "socket" : " socket");
		case INVALID_NUMBER:
		    return "Invalid number: " + info;
		case INVALID_ARG:
		    return "Invalid procedure argument: " + info;
		case INVALID_VAR:
		    return "Invalid variable: " + info;
		case OUTPUT_STACK:
		    if (info.length() == 0)
		        return "Output blocks can only appear in procedure stacks";
		    else return "Output blocks cannot appear in " + info + " stacks";
		case YIELD_STACK:
		    return "Yield blocks cannot appear in " + info;
		case ORPHAN_STUB:
		    return "Missing declaration block for " + info;
		    
		default:
			// In the default case, we display the custom message passed 
		    // in info.
		    return info;
		}
	}
}
