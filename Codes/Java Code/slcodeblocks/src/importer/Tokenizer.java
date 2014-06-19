package importer;

//import java.io.IOException;
//import java.io.PushbackReader;
//import java.io.StringReader;
import java.util.ArrayList;
//import java.util.Enumeration;

/**
 * Note: This class was directly copied from package yoyo.
 */
public class Tokenizer {

  StringStream in;
  
  public Tokenizer(String input) {
    in = new StringStream(input);
  }
  
  public ArrayList<String> tokenize() {
    ArrayList<String> v = new ArrayList<String>();
    while(true) {
      if (in.empty()) return v;
      String o = getNextToken();
      //System.out.println("finished getNextToken");
      if (o != null) v.add(o);
    }
  }

  public boolean balancedp(String open, String close) {
    int level = 0;
    while(!in.empty()) {
      String o = getNextToken();
      if (o == null) break;
      //System.out.println("finished getNextToken");
      if (o.equals(open)) level++;
      if (o.equals(close)) level--;
    }
    return (level == 0);
  }

  public char eatWhiteSpace() {
    //System.out.println("eat white space");
    char t = in.read();
    while(true) {
      if (!(whitespace(t) || comment(t))) break;
      while(whitespace(t)) {
	if (in.empty()) return (char)-1;
	t = in.read();
      }
      //System.out.println("look for comment");		
      if (comment(t)) {
	while(true) {
	  //System.out.println("parsing comment");
	  if (in.empty()) return (char)-1;
	  t = in.read();
	  if (t == '\n' || t == '\r') {
	    if (in.empty()) return (char)-1;
	    t = in.peek();
	    if (t == '\n' || t == '\r') continue; //dumbass /n/r and \r\n!
	    t = in.read();
	    break;
	  }
	}
      }
    }
    //System.out.println("done eating whitespace, not comment nor whitespace");
    return t;
  }

  public String getNextToken() {
    //System.out.println("get next token");
    StringBuffer sb = new StringBuffer();
    
    char t = eatWhiteSpace();
    //System.out.println("getNextToken after whitespace: " + (int)t);
    if (t == 65535 || t < 0) return null;
    //System.out.println("didn't return, t !< 0");
    sb.append(t);
    if (isNumber(t)) return getNextNumber(t);
    if (delimiter(t)) return sb.toString();
    if (string(t)) return getNextString();
    
    while(true) {
      if (in.empty()) return sb.toString();
      char p = in.peek();
      if (delimiter(p) || string(p) || 
	  whitespace(p) || comment(p)) return sb.toString();
      char c = in.read();
      if (escapeChar(c)) c = readEscape();
      if (c == 65535 || c < 0) return null;
      sb.append(c);
    }
  }
  
  public String getNextNumber(char item) {
    StringBuffer sb = new StringBuffer();
    sb.append(item);
    while(true) {
      if (in.empty()) break;
      char p = in.peek();
      if (delimiternotdot(p) || string(p) || 
	  whitespace(p) || comment(p)) break;
      char c = in.read();
      sb.append(c);
    }
    return sb.toString();
  }
  
  
  public String getNextString() {
      boolean lispstring = false;
    StringBuffer sb = new StringBuffer();
    sb.append('"');

    if (in.empty()) { sb.append('"'); return sb.toString(); }
    char c = in.read();
    if (c == '"') { sb.append('"'); return sb.toString(); }
    if (c == '|') {
	lispstring = true;
    } else {
	if (escapeChar(c)) c = readEscape();
	if (c == 65535 || c < 0) { sb.append('"'); return sb.toString(); }
	sb.append(c);
    }
    while(true) {
      if (in.empty()) { sb.append('"'); return sb.toString(); }
      c = in.read();
      if (c == '"' || (lispstring && c == '|')) 
	  { sb.append('"'); return sb.toString(); }
      if (escapeChar(c)) c = readEscape();
      if (c == 65535 || c < 0) { sb.append('"'); return sb.toString(); }
      sb.append(c);
    }
  }
  
  public boolean number(char item) {
    return (((item >= '0') && (item <= '9')));
  }

  public static boolean hexDigit(char item)
  {
    return (((item >= '0') && (item <= '9')) || ((item >= 'a') && (item <= 'f')) || ((item >= 'A') && (item <= 'F')));
  }

	public static int hexDigitToNumber(char item)
	{
		if((item >= '0') && (item <= '9'))
			return (item - '0');
		else if((item >= 'a') && (item <= 'f'))
			return (item - 'a' + 10);
		else if((item >= 'A') && (item <= 'F'))
			return (item - 'A' + 10);
		else
			return -1;
	}
  
  public boolean isNumber(char item) {
    boolean num = number(item);
    if (num) return true;
    char p = in.peek();
    boolean next = (number(p) && (item == '.' || item == '-')); 
    if (next) return true;
    char pp = in.peekpeek();
    return ((item == '-') && (p == '.') && number(pp));
  }
  
  public boolean string(char item) {
    return (item == '"');
  }
  
  public char readEscape() {
    if (in.empty()) return 65535;
    char p = in.read();
    switch(p) {
    case 'n': return '\n';
    case 't': return '\t';
    case 'b': return '\b';
    case 'r': return '\r';
    case 'f': return '\f';
    case '\\': return '\\';
    case '\'': return '\'';
    case '\"': return '\"';
	case 'u':
		char digit1 = in.peek();
		if(!hexDigit(digit1))
			return 65535;
		in.read();
		char digit2 = in.peek();
		if(!hexDigit(digit2))
			return hexToChar(0, 0, 0, hexDigitToNumber(digit1));
		in.read();
		char digit3 = in.peek();
		if(!hexDigit(digit3))
			return hexToChar(0, 0, hexDigitToNumber(digit1), hexDigitToNumber(digit2));
		in.read();
		char digit4 = in.peek();
		if(!hexDigit(digit4))
			return hexToChar(0, hexDigitToNumber(digit1), hexDigitToNumber(digit2), hexDigitToNumber(digit3));
		in.read();
		return hexToChar(hexDigitToNumber(digit1), hexDigitToNumber(digit2), hexDigitToNumber(digit3), hexDigitToNumber(digit4));
    }
    return 65535;
  }

	// Construct a character (16 bits) from 4 digits in the range 0-15 (4 bits each)
	public static char hexToChar(int digit1, int digit2, int digit3, int digit4)
	{
		return (char)(((digit1 & 0xF) << 12) | ((digit2 & 0xF) << 8) | ((digit3 & 0xF) << 4) | (digit4 & 0xF));
	}

  public boolean escapeChar(char item) {
    return (item == '\\');
  }

  public boolean delimiter(char item) {
    return ((item == '[') || (item == ']') ||
	    (item == '(') || (item == ')') ||
	    (item == ':') || (item == '.'));
  }
  
  public boolean delimiternotdot(char item) {
    return ((item == '[') || (item == ']') ||
	    (item == '(') || (item == ')') ||
	    (item == ':'));
  }
  
  public boolean whitespace(char item) {
    return ((item == ' ') || (item == '\n') || 
	    (item == '\r') || (item == '\t'));
  }
  
  public boolean comment(char item) {
    return (item == ';');
  }
}
