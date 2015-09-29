package javelin;

import java.util.ArrayList;

class Tokenizer {
	private ArrayList<String> ret = new ArrayList<String>();
	private String acc = ""; // accumulator
	private String s;
	public int unclosed = 0;

	public Tokenizer(String s) {
		this.s = s;
	}

	private void emit() {
		if (acc.length() > 0) {
			ret.add(acc);
			acc = "";
		}
	}

	public ArrayList<String> tokenize() {
		int last = s.length() - 1;
		for (int pos = 0; pos <= last; pos++) {
			char c = s.charAt(pos);
			if (c == ' ' || c == '\t' || c == '\r' || c == '\n' || c == ',') { // whitespace
				emit();
			} else if (c == ';') { // end-of-line comment
				emit();
				do
					pos++;
				while (pos <= last && s.charAt(pos) != '\n');
			} else if (c == '\'' || c == '`') { // quote or quasiquote
				acc += c;
				emit();
			} else if (c == '~') { // unquote
				acc += c;
				if (s.charAt(pos + 1) == '@') { // unquote-splicing
					acc += '@';
					pos++;
				}
				emit();
			} else if (c == '"') { // beginning of string
				unclosed++;
				emit();
				acc += '"';
				pos++;
				while (pos <= last) {
					if (s.charAt(pos) == '"') {
						unclosed--;
						break;
					}
					if (s.charAt(pos) == '\\') { // escape
						char next = s.charAt(pos + 1);
						if (next == 'r')
							next = '\r';
						else if (next == 'n')
							next = '\n';
						else if (next == 't')
							next = '\t';
						acc += next;
						pos += 2;
					} else {
						acc += s.charAt(pos);
						pos++;
					}
				}
				emit();
			} else if (c == '(') {
				unclosed++;
				emit();
				acc += c;
				emit();
			} else if (c == ')') {
				unclosed--;
				emit();
				acc += c;
				emit();
			} else {
				acc += c;
			}
		}
		emit();
		return ret;
	}

	public static ArrayList<String> tokenize(String s) {
		return new Tokenizer(s).tokenize();
	}
}