package javelin;

import java.util.ArrayList;

import javelin.Core.node;

class Parser {
	private int pos = 0;
	private ArrayList<String> tokens;

	public Parser(ArrayList<String> tokens) {
		this.tokens = tokens;
	}

	public node parse() {
		String tok = tokens.get(pos);
		if (tok.charAt(0) == '"') { // double-quoted string
			return new node(tok.substring(1));
		} else if (tok.equals("'")) { // quote
			pos++;
			ArrayList<node> ret = new ArrayList<node>();
			ret.add(new node(new Symbol("quote")));
			ret.add(parse());
			return new node(ret);
		} else if (tok.equals("(")) { // list
			pos++;
			return new node(parseList());
		} else if (Character.isDigit(tok.charAt(0)) || tok.charAt(0) == '-' && tok.length() >= 2
				&& Character.isDigit(tok.charAt(1))) { // number
			if (tok.indexOf('.') != -1 || tok.indexOf('e') != -1) { // double
				return new node(Double.parseDouble(tok));
			} else if (tok.endsWith("L") || tok.endsWith("l")) { // long
				return new node(Long.parseLong(tok.substring(0, tok.length() - 1)));
			} else {
				return new node(Integer.parseInt(tok));
			}
		} else { // symbol
			return new node(new Symbol(tok));
		}
	}

	public ArrayList<node> parseList() {
		ArrayList<node> ret = new ArrayList<node>();
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
}