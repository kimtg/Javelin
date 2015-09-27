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
		} else if (tok.equals("(")) { // list
			pos++;
			return parseList();
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