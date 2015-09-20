package javelin;

import java.util.ArrayList;
import java.util.HashMap;

class Symbol {
	public static HashMap<String, Integer> symcode = new HashMap<String, Integer>();
	public static ArrayList<String> symname = new ArrayList<String>();
	public int code;

	public static int toCode(String name) {
		Integer r = symcode.get(name);
		if (r == null) {
			r = symcode.size();
			symcode.put(name, r);
			symname.add(name);
		}
		return r;
	}

	public Symbol(String name) {
		code = toCode(name);
	}

	public String toString() {
		return symname.get(code);
	}
}