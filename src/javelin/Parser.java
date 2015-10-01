package javelin;

import java.util.ArrayList;

class Parser {
	private int pos = 0;
	private ArrayList<String> tokens;

	public Parser(ArrayList<String> tokens) {
		this.tokens = tokens;
	}

	public Object parse() {
		String tok = tokens.get(pos);
		if (tok.charAt(0) == '"') { // double-quoted string
			return tok.substring(1);
		} else if (tok.equals("'")) { // quote
			pos++;
			ArrayList<Object> ret = new ArrayList<Object>();
			ret.add(new Symbol("quote"));
			ret.add(parse());
			return ret;
		} else if (tok.equals("`")) { // quasiquote
			pos++;
			ArrayList<Object> ret = new ArrayList<Object>();
			ret.add(new Symbol("quasiquote"));
			ret.add(parse());
			return ret;
		} else if (tok.equals("~")) { // unquote
			pos++;
			ArrayList<Object> ret = new ArrayList<Object>();
			ret.add(new Symbol("unquote"));
			ret.add(parse());
			return ret;
		} else if (tok.equals("~@")) { // unquote-splicing
			pos++;
			ArrayList<Object> ret = new ArrayList<Object>();
			ret.add(new Symbol("unquote-splicing"));
			ret.add(parse());
			return ret;
		} else if (tok.equals("(")) { // list
			pos++;
			return parseList();
		} else if (tok.charAt(0) == '\\') { // char
			// Characters - preceded by a backslash: \c. \newline, \space, \tab, \formfeed, \backspace, and \return yield the corresponding characters. Unicode characters are represented with \\uNNNN as in Java. Octals are represented with \\oNNN.			
			if (tok.length() == 2) {
				return tok.charAt(1);
			} else {
				switch (tok) {
				case "\\newline": return '\n';
				case "\\space": return ' ';
				case "\\tab": return '\t';
				case "\\formfeed": return '\f';
				case "\\backspace": return '\b';
				case "\\return": return '\r';
				default:
					if (tok.charAt(1) == 'u' && tok.length() == 6) { // Unicode: \\uNNNN
						int codePoint = Integer.parseInt(tok.substring(2));
						return Character.toChars(codePoint)[0];
					} else if (tok.charAt(1) == 'o' && tok.length() == 5) { // Octal: \\oNNN
						int codePoint = Integer.parseInt(tok.substring(2), 8);
						return Character.toChars(codePoint)[0];
					}
					throw new RuntimeException("Unsupported character: " + tok);
				}
			}
		} else if (Character.isDigit(tok.charAt(0)) || tok.charAt(0) == '-' && tok.length() >= 2
				&& Character.isDigit(tok.charAt(1))) { // number
			if (tok.indexOf('.') != -1 || tok.indexOf('e') != -1) { // double
				return Double.parseDouble(tok);
			} else if (tok.endsWith("L") || tok.endsWith("l")) { // long
				return Long.parseLong(tok.substring(0, tok.length() - 1));
			} else {
				return Integer.parseInt(tok);
			}
		} else { // symbol
			// other literals
			switch (tok) {
			case "true": return true;
			case "false": return false;
			case "nil": return null;
			}
			// normal symbol
			return new Symbol(tok);
		}
	}

	public ArrayList<Object> parseList() {
		ArrayList<Object> ret = new ArrayList<Object>();
		int last = tokens.size() - 1;
		for (; pos <= last; pos++) {
			String tok = tokens.get(pos);
			if (tok.equals(")")) { // end of list
				break;
			} else {
				ret.add(parse());
			}
		}
		return ret;
	}

	static Object parse(String s) {
		return new Parser(Tokenizer.tokenize(s)).parse();
	}
}