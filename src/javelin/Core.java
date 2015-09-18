package javelin;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.TreeSet;

public class Core {
	public static final String VERSION = "0.1";

	public Core() throws Exception {
		init();
	}

	public static class node implements Cloneable {
		Object value;
		Class<?> clazz = null; // type hint

		node() {
		}

		node(Object value) {
			this.value = value;
		}

		protected node clone() {
			node r = new node(this.value);
			r.clazz = this.clazz;
			return r;
		}

		int intValue() {
			if (value instanceof Number) {
				return ((Number) value).intValue();
			} else {
				return Integer.parseInt(stringValue());
			}
		}

		double doubleValue() {
			if (value instanceof Number) {
				return ((Number) value).doubleValue();
			} else {
				return Double.parseDouble(stringValue());
			}
		}

		long longValue() {
			if (value instanceof Number) {
				return ((Number) value).longValue();
			} else {
				return Long.parseLong(stringValue());
			}
		}

		boolean booleanValue() { // null is false, other type is true.
			if (value == null)
				return false;
			if (value instanceof Boolean)
				return (Boolean) value;
			else
				return true;
		}

		String stringValue() {
			if (value == null)
				return "nil";
			return value.toString();
		}

		@SuppressWarnings("unchecked")
		ArrayList<node> arrayListValue() {
			return (ArrayList<node>) value;
		}

		String type() {
			if (value == null)
				return "nil";
			else
				return value.getClass().getName();
		}

		String str_with_type() {
			return stringValue() + " : " + type();
		}

		public String toString() {
			return stringValue();
		}

		public symbol symbolValue() {
			return (symbol) value;
		}
	}

	// frequently used constants
	static final node node_true = new node(true);
	static final node node_false = new node(false);
	static final node node_0 = new node(0);
	static final node node_1 = new node(1);
	static final node node_nil = new node();

	static class symbol {
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

		public symbol(String name) {
			code = toCode(name);
		}

		public String toString() {
			return symname.get(code);
		}
	}

	static class fn implements IFn { // anonymous function
		ArrayList<node> def; // definition
		environment outer_env;

		fn(ArrayList<node> def, environment outer_env) {
			this.def = def;
			this.outer_env = outer_env;
		}

		public String toString() {
			return "#<function:" + def.toString() + ">";
		}

		@Override
		public node invoke(ArrayList<node> args, environment env) throws Exception {
			// anonymous function application. lexical scoping
			// ((ARGUMENT ...) BODY ...)
			environment local_env = new environment(this.outer_env);
			ArrayList<node> arg_syms = this.def.get(0).arrayListValue();

			int len = arg_syms.size();
			for (int i = 0; i < len; i++) { // assign arguments
				symbol sym = arg_syms.get(i).symbolValue();
				if (sym.toString().equals("&")) { // variadic arguments
					sym = arg_syms.get(i + 1).symbolValue();
					local_env.set(sym.code, new node(new ArrayList<node>(args.subList(i, args.size()))));
					break;
				}
				node n2 = args.get(i);
				local_env.set(sym.code, n2);
			}

			len = this.def.size();
			node ret = null;
			for (int i = 1; i < len; i++) { // body
				ret = eval(this.def.get(i), local_env);
			}
			return ret;
		}
	}

	static class environment {
		HashMap<Integer, node> env = new HashMap<Integer, node>();
		environment outer;

		environment() {
			this.outer = null;
		}

		environment(environment outer) {
			this.outer = outer;
		}

		node get(int code) {
			node found = env.get(code);
			if (found != null) {
				return found;
			} else {
				if (outer != null) {
					return outer.get(code);
				} else {
					return null;
				}
			}
		}

		node set(int code, node v) {
			node v2 = v.clone();
			env.put(code, v2);
			return v2;
		}
	}

	private static class tokenizer {
		private ArrayList<String> ret = new ArrayList<String>();
		private String acc = ""; // accumulator
		private String s;
		public int unclosed = 0;

		public tokenizer(String s) {
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
				if (c == ' ' || c == '\t' || c == '\r' || c == '\n') {
					emit();
				} else if (c == ';') { // end-of-line comment
					emit();
					do
						pos++;
					while (pos <= last && s.charAt(pos) != '\n');
				} else if (c == '\'') { // quote
					acc += c;
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
	}

	public static ArrayList<String> tokenize(String s) {
		return new tokenizer(s).tokenize();
	}

	private static class parser {
		private int pos = 0;
		private ArrayList<String> tokens;

		public parser(ArrayList<String> tokens) {
			this.tokens = tokens;
		}

		public node parse() {
			String tok = tokens.get(pos);
			if (tok.charAt(0) == '"') { // double-quoted string
				return new node(tok.substring(1));
			} else if (tok.equals("'")) { // quote
				pos++;
				ArrayList<node> ret = new ArrayList<node>();
				ret.add(new node(new symbol("quote")));
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
				return new node(new symbol(tok));
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

	static node parse(String s) {
		return new parser(tokenize(s)).parse();
	}

	enum Special {
		AND, OR, IF, WHILE, SET_E, QUOTE, FN, DO, _DOT, _DOTGET, _DOTSET_E, NEW, CAST, DEFMACRO, THREAD, DEF, BREAK, DOSEQ, LET
	}

	interface IFn {
		public node invoke(ArrayList<node> args, environment env) throws Exception;
	}

	static class Builtin {
		static class _plus implements IFn {
			public node invoke(ArrayList<node> args, environment env) throws Exception {
				int len = args.size();
				if (len <= 0)
					return node_0;
				node first = args.get(0);
				if (first.value instanceof Integer) {
					int acc = first.intValue();
					for (int i = 1; i < len; i++) {
						acc += args.get(i).intValue();
					}
					return new node(acc);
				} else if (first.value instanceof Long) {
					long acc = first.longValue();
					for (int i = 1; i < len; i++) {
						acc += args.get(i).longValue();
					}
					return new node(acc);
				} else {
					double acc = first.doubleValue();
					for (int i = 1; i < len; i++) {
						acc += args.get(i).doubleValue();
					}
					return new node(acc);
				}
			}
		}

		static class _minus implements IFn {
			public node invoke(ArrayList<node> args, environment env) throws Exception {
				int len = args.size();
				if (len <= 0)
					return node_0;
				node first = args.get(0);
				if (first.value instanceof Integer) {
					int acc = first.intValue();
					if (len == 1) return new node(-acc);
					for (int i = 1; i < len; i++) {
						acc -= args.get(i).intValue();
					}
					return new node(acc);
				} else if (first.value instanceof Long) {
					long acc = first.longValue();
					if (len == 1) return new node(-acc);
					for (int i = 1; i < len; i++) {
						acc -= args.get(i).longValue();
					}
					return new node(acc);
				} else {
					double acc = first.doubleValue();
					if (len == 1) return new node(-acc);
					for (int i = 1; i < len; i++) {
						acc -= args.get(i).doubleValue();
					}
					return new node(acc);
				}
			}
		}

		static class _star implements IFn {
			public node invoke(ArrayList<node> args, environment env) throws Exception {
				int len = args.size();
				if (len <= 0)
					return node_1;
				node first = args.get(0);
				if (first.value instanceof Integer) {
					int acc = first.intValue();
					for (int i = 1; i < len; i++) {
						acc *= args.get(i).intValue();
					}
					return new node(acc);
				} else if (first.value instanceof Long) {
					long acc = first.longValue();
					for (int i = 1; i < len; i++) {
						acc *= args.get(i).longValue();
					}
					return new node(acc);
				} else {
					double acc = first.doubleValue();
					for (int i = 1; i < len; i++) {
						acc *= args.get(i).doubleValue();
					}
					return new node(acc);
				}
			}
		}

		static class _slash implements IFn {
			public node invoke(ArrayList<node> args, environment env) throws Exception {
				int len = args.size();
				if (len <= 0)
					return node_1;
				node first = args.get(0);
				if (first.value instanceof Integer) {
					int acc = first.intValue();
					if (len == 1) return new node(1 / acc);
					for (int i = 1; i < len; i++) {
						acc /= args.get(i).intValue();
					}
					return new node(acc);
				} else if (first.value instanceof Long) {
					long acc = first.longValue();
					if (len == 1) return new node(1 / acc);
					for (int i = 1; i < len; i++) {
						acc /= args.get(i).longValue();
					}
					return new node(acc);
				} else {
					double acc = first.doubleValue();
					if (len == 1) return new node(1 / acc);
					for (int i = 1; i < len; i++) {
						acc /= args.get(i).doubleValue();
					}
					return new node(acc);
				}
			}
		}

		static class mod implements IFn {
			public node invoke(ArrayList<node> args, environment env) throws Exception {
				return new node(args.get(0).intValue() % args.get(1).intValue());
			}
		}

		static class _eq implements IFn {
			public node invoke(ArrayList<node> args, environment env) throws Exception {
				Object v1 = args.get(0).value;
				if (v1 == null) {
					for (int i = 1; i < args.size(); i++) {
						Object v2 = args.get(i).value;
						if (v2 != null) return node_false;
					}
				}
				else {
					for (int i = 1; i < args.size(); i++) {
						Object v2 = args.get(i).value;
						if (!v1.equals(v2)) return node_false;
					}
				}
				return node_true;
			}
		}

		static class _eq_eq implements IFn {
			public node invoke(ArrayList<node> args, environment env) throws Exception {
				node first = args.get(0);
				if (first.value instanceof Integer) {
					int firstv = first.intValue();
					for (int i = 1; i < args.size(); i++) {
						if (args.get(i).intValue() != firstv) {
							return node_false;
						}
					}
				} else if (first.value instanceof Long) {
					long firstv = first.longValue();
					for (int i = 1; i < args.size(); i++) {
						if (args.get(i).longValue() != firstv) {
							return node_false;
						}
					}
				} else {
					double firstv = first.doubleValue();
					for (int i = 1; i < args.size(); i++) {
						if (args.get(i).doubleValue() != firstv) {
							return node_false;
						}
					}
				}
				return node_true;
			}
		}

		static class Not_eq implements IFn {
			public node invoke(ArrayList<node> args, environment env) throws Exception {
				Object v1 = args.get(0).value;
				if (v1 == null) {
					for (int i = 1; i < args.size(); i++) {
						Object v2 = args.get(i).value;
						if (v2 != null) return node_true;
					}
				}
				else {
					for (int i = 1; i < args.size(); i++) {
						Object v2 = args.get(i).value;
						if (!v1.equals(v2)) return node_true;
					}
				}
				return node_false;
			}
		}

		static class _lt implements IFn {
			public node invoke(ArrayList<node> args, environment env) throws Exception {
				node first = args.get(0);
				node second = args.get(1);
				if (first.value instanceof Integer) {
					return new node(first.intValue() < second.intValue());
				} else if (first.value instanceof Long) {
					return new node(first.longValue() < second.longValue());
				} else {
					return new node(first.doubleValue() < second.doubleValue());
				}
			}
		}

		static class _gt implements IFn {
			public node invoke(ArrayList<node> args, environment env) throws Exception {
				node first = args.get(0);
				node second = args.get(1);
				if (first.value instanceof Integer) {
					return new node(first.intValue() > second.intValue());
				} else if (first.value instanceof Long) {
					return new node(first.longValue() > second.longValue());
				} else {
					return new node(first.doubleValue() > second.doubleValue());
				}
			}
		}

		static class _lt_eq implements IFn {
			public node invoke(ArrayList<node> args, environment env) throws Exception {
				node first = args.get(0);
				node second = args.get(1);
				if (first.value instanceof Integer) {
					return new node(first.intValue() <= second.intValue());
				} else if (first.value instanceof Long) {
					return new node(first.longValue() <= second.longValue());
				} else {
					return new node(first.doubleValue() <= second.doubleValue());
				}
			}
		}

		static class _gt_eq implements IFn {
			public node invoke(ArrayList<node> args, environment env) throws Exception {
				node first = args.get(0);
				node second = args.get(1);
				if (first.value instanceof Integer) {
					return new node(first.intValue() >= second.intValue());
				} else if (first.value instanceof Long) {
					return new node(first.longValue() >= second.longValue());
				} else {
					return new node(first.doubleValue() >= second.doubleValue());
				}
			}
		}

		static class not implements IFn {
			public node invoke(ArrayList<node> args, environment env) throws Exception {
				return new node(!args.get(0).booleanValue());
			}
		}

		static class read_string implements IFn {
			public node invoke(ArrayList<node> args, environment env) throws Exception {
				return new node(parse(args.get(0).stringValue()).value);
			}
		}

		static class type implements IFn {
			public node invoke(ArrayList<node> args, environment env) throws Exception {
				return new node(args.get(0).type());
			}
		}

		static class eval implements IFn {
			public node invoke(ArrayList<node> args, environment env) throws Exception {
				return new node(eval(args.get(0), env));
			}
		}

		static class fold implements IFn {
			public node invoke(ArrayList<node> args, environment env) throws Exception {
				node f = args.get(0);
				ArrayList<node> lst = args.get(1).arrayListValue();
				node acc = lst.get(0);
				ArrayList<node> args2 = new ArrayList<node>(); // (ITEM1 ITEM2)
				args2.add(null); // first argument
				args2.add(null); // second argument
				for (int i = 1; i < lst.size(); i++) {
					args2.set(0, acc);
					args2.set(1, lst.get(i));
					acc = apply(f, args2, env);
				}
				return acc;
			}
		}

		static class map implements IFn {
			public node invoke(ArrayList<node> args, environment env) throws Exception {
				node f = args.get(0);
				ArrayList<node> lst = args.get(1).arrayListValue();
				ArrayList<node> acc = new ArrayList<node>();
				for (int i = 0; i < lst.size(); i++) {
					ArrayList<node> args2 = new ArrayList<node>();
					args2.add(lst.get(i));
					acc.add(apply(f, args2, env));
				}
				return new node(acc);
			}
		}

		static class apply implements IFn {
			public node invoke(ArrayList<node> args, environment env) throws Exception {
				return apply(args.get(0), args.get(1).arrayListValue(), env);
			}
		}

		static class filter implements IFn {
			public node invoke(ArrayList<node> args, environment env) throws Exception {
				node f = args.get(0);
				ArrayList<node> lst = args.get(1).arrayListValue();
				ArrayList<node> acc = new ArrayList<node>();
				for (int i = 0; i < lst.size(); i++) {
					ArrayList<node> args2 = new ArrayList<node>();
					node item = lst.get(i);
					args2.add(item);
					node ret = apply(f, args2, env);
					if (ret.booleanValue()) acc.add(item);
				}
				return new node(acc);
			}
		}

		static class pr implements IFn {
			public node invoke(ArrayList<node> args, environment env) throws Exception {
				for (int i = 0; i < args.size(); i++) {
					if (i != 0) System.out.print(" ");
					System.out.print(args.get(i));
				}
				return node_nil;
			}
		}

		static class prn implements IFn {
			public node invoke(ArrayList<node> args, environment env) throws Exception {
				for (int i = 0; i < args.size(); i++) {
					if (i != 0) System.out.print(" ");
					System.out.print(args.get(i));
				}
				System.out.println();
				return node_nil;
			}
		}

		static class read_line implements IFn {
			public node invoke(ArrayList<node> args, environment env) throws Exception {
				BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
				try {
					return new node(br.readLine());
				} catch (IOException e) {
					return node_nil;
				}
			}
		}

		static class slurp implements IFn {
			public node invoke(ArrayList<node> args, environment env) throws Exception {
				String filename = args.get(0).stringValue();
				try {
					return new node(slurp(filename));
				} catch (IOException e) {
					return node_nil;
				}
			}
		}

		static class spit implements IFn {
			public node invoke(ArrayList<node> args, environment env) throws Exception {
				String filename = args.get(0).stringValue();
				String str = args.get(1).stringValue();
				return new node(spit(filename, str));
			}
		}

		static class list implements IFn {
			public node invoke(ArrayList<node> args, environment env) throws Exception {
				return new node(args);
			}
		}

		static class str implements IFn {
			public node invoke(ArrayList<node> args, environment env) throws Exception {
				StringBuilder sb = new StringBuilder();
				for (Object x : args) {
					sb.append(x.toString());
				}
				return new node(sb.toString());
			}
		}

		static class symbol implements IFn {
			public node invoke(ArrayList<node> args, environment env) throws Exception {
				return new node(new Core.symbol(args.get(0).stringValue()));
			}
		}
	}

	@SuppressWarnings("unchecked")
	static node apply(node func, ArrayList<node> args, environment env) throws Exception {
		if (func.value instanceof IFn) {
			return ((IFn) func.value).invoke(args, env);
		} else {
			if (func.value instanceof List) {
				// implicit indexing
				return ((List<node>) func.value).get(args.get(0).intValue());
			}
			else {
				System.err.println("Unknown function: [" + func.value.toString() + "]");
				return node_nil;
			}
		}
	}

	environment global_env = new environment(); // variables. compile-time

	void print_collection(Collection<String> coll) {
		for (String key : new TreeSet<String>(coll)) {
			System.out.print(" " + key);
		}
		System.out.println();
	}

	public void print_logo() {
		System.out.println("Javelin " + VERSION);
		System.out.println("Predefined Symbols:");
		ArrayList<String> r = new ArrayList<String>(global_env.env.keySet().size());
		for (int x : global_env.env.keySet()) {
			r.add(symbol.symname.get(x));
		}
		print_collection(r);
		System.out.println("Macros:");
		print_collection(macros.keySet());
	}

	void init() throws Exception {
		global_env.env.put(symbol.toCode("true"), node_true);
		global_env.env.put(symbol.toCode("false"), node_false);
		global_env.env.put(symbol.toCode("nil"), new node());

		global_env.env.put(symbol.toCode("+"), new node(new Builtin._plus()));
		global_env.env.put(symbol.toCode("-"), new node(new Builtin._minus()));
		global_env.env.put(symbol.toCode("*"), new node(new Builtin._star()));
		global_env.env.put(symbol.toCode("/"), new node(new Builtin._slash()));
		global_env.env.put(symbol.toCode("mod"), new node(new Builtin.mod()));
		global_env.env.put(symbol.toCode("="), new node(new Builtin._eq()));
		global_env.env.put(symbol.toCode("=="), new node(new Builtin._eq_eq()));
		global_env.env.put(symbol.toCode("not="), new node(new Builtin.Not_eq()));
		global_env.env.put(symbol.toCode("<"), new node(new Builtin._lt()));
		global_env.env.put(symbol.toCode(">"), new node(new Builtin._gt()));
		global_env.env.put(symbol.toCode("<="), new node(new Builtin._lt_eq()));
		global_env.env.put(symbol.toCode(">="), new node(new Builtin._gt_eq()));
		global_env.env.put(symbol.toCode("and"), new node(Special.AND));
		global_env.env.put(symbol.toCode("or"), new node(Special.OR));
		global_env.env.put(symbol.toCode("not"), new node(new Builtin.not()));
		global_env.env.put(symbol.toCode("if"), new node(Special.IF));
		global_env.env.put(symbol.toCode("while"), new node(Special.WHILE));
		global_env.env.put(symbol.toCode("read-string"), new node(new Builtin.read_string()));
		global_env.env.put(symbol.toCode("type"), new node(new Builtin.type()));
		global_env.env.put(symbol.toCode("eval"), new node(new Builtin.eval()));
		global_env.env.put(symbol.toCode("quote"), new node(Special.QUOTE));
		global_env.env.put(symbol.toCode("fn"), new node(Special.FN));
		global_env.env.put(symbol.toCode("list"), new node(new Builtin.list()));
		global_env.env.put(symbol.toCode("apply"), new node(new Builtin.apply()));
		global_env.env.put(symbol.toCode("fold"), new node(new Builtin.fold()));
		global_env.env.put(symbol.toCode("map"), new node(new Builtin.map()));
		global_env.env.put(symbol.toCode("filter"), new node(new Builtin.filter()));
		global_env.env.put(symbol.toCode("do"), new node(Special.DO));
		global_env.env.put(symbol.toCode("."), new node(Special._DOT));
		global_env.env.put(symbol.toCode(".get"), new node(Special._DOTGET));
		global_env.env.put(symbol.toCode(".set!"), new node(Special._DOTSET_E));
		global_env.env.put(symbol.toCode("new"), new node(Special.NEW));
		global_env.env.put(symbol.toCode("set!"), new node(Special.SET_E));
		global_env.env.put(symbol.toCode("pr"), new node(new Builtin.pr()));
		global_env.env.put(symbol.toCode("prn"), new node(new Builtin.prn()));
		global_env.env.put(symbol.toCode("cast"), new node(Special.CAST));
		global_env.env.put(symbol.toCode("defmacro"), new node(Special.DEFMACRO));
		global_env.env.put(symbol.toCode("read-line"), new node(new Builtin.read_line()));
		global_env.env.put(symbol.toCode("slurp"), new node(new Builtin.slurp()));
		global_env.env.put(symbol.toCode("spit"), new node(new Builtin.spit()));
		global_env.env.put(symbol.toCode("thread"), new node(Special.THREAD));
		global_env.env.put(symbol.toCode("def"), new node(Special.DEF));
		global_env.env.put(symbol.toCode("break"), new node(Special.BREAK));
		global_env.env.put(symbol.toCode("doseq"), new node(Special.DOSEQ));
		global_env.env.put(symbol.toCode("str"), new node(new Builtin.str()));
		global_env.env.put(symbol.toCode("let"), new node(Special.LET));
		global_env.env.put(symbol.toCode("symbol"), new node(new Builtin.symbol()));

		eval_string("(defmacro defn (name ...) (def name (fn ...)))" +
				"(defmacro when (cond ...) (if cond (do ...)))" +
				"(defn nil? (x) (= x nil))"
				);
	}

	HashMap<String, node[]> macros = new HashMap<>();

	node apply_macro(node body, HashMap<String, node> vars) {
		if (body.value instanceof ArrayList) {
			@SuppressWarnings("unchecked")
			ArrayList<node> bvec = (ArrayList<node>) body.value;
			ArrayList<node> ret = new ArrayList<>();
			for (int i = 0; i < bvec.size(); i++) {
				node b = bvec.get(i);
				if (b.stringValue().equals("...")) { // ... is like unquote-splicing
					ret.addAll(vars.get(b.stringValue()).arrayListValue());
				} else
					ret.add(apply_macro(bvec.get(i), vars));
			}
			return new node(ret);
		} else {
			String bstr = body.stringValue();
			if (vars.containsKey(bstr))
				return vars.get(bstr);
			else
				return body;
		}
	}

	node macroexpand(node n) {
		ArrayList<node> nArrayList = n.arrayListValue();
		if (macros.containsKey(nArrayList.get(0).stringValue())) {
			node[] macro = macros.get(nArrayList.get(0).stringValue());
			HashMap<String, node> macrovars = new HashMap<>();
			ArrayList<node> argsyms = macro[0].arrayListValue();
			for (int i = 0; i < argsyms.size(); i++) {
				String argsym = argsyms.get(i).stringValue();
				if (argsym.equals("...")) {
					node n2 = new node(new ArrayList<node>());
					macrovars.put(argsym, n2);
					ArrayList<node> ellipsis = n2.arrayListValue();
					for (int i2 = i + 1; i2 < nArrayList.size(); i2++)
						ellipsis.add(nArrayList.get(i2));
					break;
				} else {
					macrovars.put(argsyms.get(i).stringValue(), nArrayList.get(i + 1));
				}
			}
			return apply_macro(macro[1], macrovars);
		} else
			return n;
	}

	node preprocess(node n) {
		if (n.value instanceof ArrayList) { // function (FUNCTION ARGUMENT ...)
			ArrayList<node> nArrayList = n.arrayListValue();
			if (nArrayList.size() == 0)
				return n;
			node func = preprocess(nArrayList.get(0));
			if (func.value instanceof symbol && func.toString().equals(("defmacro"))) {
				// (defmacro add (a b) (+ a b)) ; define macro
				macros.put(nArrayList.get(1).stringValue(), new node[] { nArrayList.get(2), nArrayList.get(3) });
				return node_nil;
			} else {
				if (macros.containsKey(nArrayList.get(0).stringValue())) { // compile
																			// macro
					return preprocess(macroexpand(n));
				} else {
					ArrayList<node> r = new ArrayList<node>();
					for (node n2 : nArrayList) {
						r.add(preprocess(n2));
					}
					return new node(r);
				}
			}
		} else {
			return n;
		}
	}

	static node eval(node n, environment env) throws Exception {
		if (n.value instanceof symbol) {
			node r = env.get(((symbol) n.value).code);
			if (r == null) {
				throw new Exception("Unable to resolve symbol: " + n.toString());
			}
			return r;
		} else if (n.value instanceof ArrayList) { // function (FUNCTION
													// ARGUMENT ...)
			ArrayList<node> nArrayList = n.arrayListValue();
			if (nArrayList.size() == 0)
				return node_nil;
			node func = eval(nArrayList.get(0), env);
			Special foundBuiltin;
			if (func.value instanceof Special) {
				foundBuiltin = (Special) func.value;
				switch (foundBuiltin) {
				case SET_E: { // (set PLACE VALUE ...) ; set the PLACE's value
					node place = null;
					int len = nArrayList.size();
					for (int i = 1; i < len; i += 2) {
						place = eval(nArrayList.get(i), env);
						node value = eval(nArrayList.get(i + 1), env);
						if (place == null) {// new variable
							throw new Exception("No such place");
							// return
							// env.set(((symbol)nArrayList.get(1).value).code,
							// value);
						} else {
							place.value = value.value;
						}
					}
					return place;
				}
				case DEF: { // (def SYMBOL VALUE ...) ; set in the current
							// environment
					node ret = null;
					int len = nArrayList.size();
					for (int i = 1; i < len; i += 2) {
						node value = eval(nArrayList.get(i + 1), env);
						ret = env.set(((symbol) nArrayList.get(i).value).code, value);
					}
					return ret;
				}
				case AND: { // (and X ...) short-circuit
					for (int i = 1; i < nArrayList.size(); i++) {
						if (!eval(nArrayList.get(i), env).booleanValue()) {
							return node_false;
						}
					}
					return node_true;
				}
				case OR: { // (or X ...) short-circuit
					for (int i = 1; i < nArrayList.size(); i++) {
						if (eval(nArrayList.get(i), env).booleanValue()) {
							return node_true;
						}
					}
					return node_false;
				}
				case IF: { // (if CONDITION THEN_EXPR [ELSE_EXPR])
					node cond = nArrayList.get(1);
					if (eval(cond, env).booleanValue()) {
						return eval(nArrayList.get(2), env);
					} else {
						if (nArrayList.size() <= 3)
							return node_nil;
						return eval(nArrayList.get(3), env);
					}
				}
				case WHILE: { // (while CONDITION EXPR ...)
					try {
						node cond = nArrayList.get(1);
						int len = nArrayList.size();
						while (eval(cond, env).booleanValue()) {
							for (int i = 2; i < len; i++) {
								eval(nArrayList.get(i), env);
							}
						}
					} catch (BreakException E) {

					}
					return node_nil;
				}
				case BREAK: { // (break)
					throw new BreakException();
				}
				case QUOTE: { // (quote X)
					return nArrayList.get(1);
				}
				case FN: {
					// anonymous function. lexical scoping
					// (fn (ARGUMENT ...) BODY ...)
					ArrayList<node> r = new ArrayList<node>();
					for (int i = 1; i < nArrayList.size(); i++) {
						r.add(nArrayList.get(i));
					}
					return new node(new fn(r, env));
				}
				case DO: { // (do X ...)
					int last = nArrayList.size() - 1;
					if (last <= 0)
						return node_nil;
					for (int i = 1; i < last; i++) {
						eval(nArrayList.get(i), env);
					}
					return eval(nArrayList.get(last), env);
				}
				case _DOT: {
					// Java interoperability
					// (. CLASS METHOD ARGUMENT ...) ; Java method invocation
					try {
						Class<?> cls;
						Object obj = null;
						String className = nArrayList.get(1).stringValue();
						// if (nArrayList.get(1).value instanceof symbol) { //
						// class's static method e.g. (. java.lang.Math floor
						// 1.5)
						if (nArrayList.get(1).value instanceof symbol
								&& env.get(((symbol) nArrayList.get(1).value).code) == null) {
							// class's static method e.g. (. java.lang.Math floor 1.5)
							cls = Class.forName(className);
						} else { // object's method e.g. (. "abc" length)
							obj = eval(nArrayList.get(1), env).value;
							cls = obj.getClass();
						}
						Class<?>[] parameterTypes = new Class<?>[nArrayList.size() - 3];
						ArrayList<Object> parameters = new ArrayList<Object>();
						int last = nArrayList.size() - 1;
						for (int i = 3; i <= last; i++) {
							node a = eval(nArrayList.get(i), env);
							Object param = a.value;
							parameters.add(param);
							Class<?> paramClass;
							if (a.clazz == null) {
								if (param instanceof Integer)
									paramClass = Integer.TYPE;
								else if (param instanceof Double)
									paramClass = Double.TYPE;
								else if (param instanceof Long)
									paramClass = Long.TYPE;
								else if (param instanceof Boolean)
									paramClass = Boolean.TYPE;
								else
									paramClass = param.getClass();
							} else {
								paramClass = a.clazz; // use hint
							}
							parameterTypes[i - 3] = paramClass;
						}
						String methodName = nArrayList.get(2).stringValue();
						Method method = cls.getMethod(methodName, parameterTypes);
						return new node(method.invoke(obj, parameters.toArray()));
					} catch (Exception e) {
						e.printStackTrace();
						return node_nil;
					}
				}
				case _DOTGET: {
					// Java interoperability
					// (.get CLASS FIELD) ; get Java field
					try {
						Class<?> cls;
						Object obj = null;
						String className = nArrayList.get(1).stringValue();
						// class's static field e.g. (.get java.lang.Math PI)
						if (nArrayList.get(1).value instanceof symbol
								&& env.get(((symbol) nArrayList.get(1).value).code) == null) {																								// static
							cls = Class.forName(className);
						} else { // object's method
							obj = eval(nArrayList.get(1), env).value;
							cls = obj.getClass();
						}
						String fieldName = nArrayList.get(2).stringValue();
						java.lang.reflect.Field field = cls.getField(fieldName);
						return new node(field.get(cls));
					} catch (Exception e) {
						e.printStackTrace();
						return node_nil;
					}
				}
				case _DOTSET_E: {
					// Java interoperability
					// (.set CLASS FIELD VALUE) ; set Java field
					try {
						Class<?> cls;
						Object obj = null;
						String className = nArrayList.get(1).stringValue();
						// if (nArrayList.get(1).value instanceof symbol) { //
						// class's static field e.g. (.get java.lang.Math PI)
						if (nArrayList.get(1).value instanceof symbol
								&& env.get(((symbol) nArrayList.get(1).value).code) == null) { // class's static method
							cls = Class.forName(className);
						} else { // object's method
							obj = eval(nArrayList.get(1), env).value;
							cls = obj.getClass();
						}
						String fieldName = nArrayList.get(2).stringValue();
						java.lang.reflect.Field field = cls.getField(fieldName);
						Object value = eval(nArrayList.get(3), env).value;
						field.set(cls, value);
						return node_nil;
					} catch (Exception e) {
						e.printStackTrace();
						return node_nil;
					}
				}
				case NEW: {
					// Java interoperability
					// (new CLASS ARG ...) ; create new Java object
					try {
						String className = nArrayList.get(1).stringValue();
						Class<?> cls = Class.forName(className);
						Class<?>[] parameterTypes = new Class<?>[nArrayList.size() - 2];
						ArrayList<Object> parameters = new ArrayList<Object>();
						int last = nArrayList.size() - 1;
						for (int i = 2; i <= last; i++) {
							node a = eval(nArrayList.get(i), env);
							Object param = a.value;
							parameters.add(param);
							Class<?> paramClass;
							if (a.clazz == null) {
								if (param instanceof Integer)
									paramClass = Integer.TYPE;
								else if (param instanceof Double)
									paramClass = Double.TYPE;
								else if (param instanceof Long)
									paramClass = Long.TYPE;
								else if (param instanceof Boolean)
									paramClass = Boolean.TYPE;
								else
									paramClass = param.getClass();
							} else {
								paramClass = a.clazz; // use hint
							}
							parameterTypes[i - 2] = paramClass;
						}
						Constructor<?> ctor = cls.getConstructor(parameterTypes);
						return new node(ctor.newInstance(parameters.toArray()));
					} catch (Exception e) {
						e.printStackTrace();
						return node_nil;
					}
				}
				case CAST: { // (cast CLASS X): Returns type-hinted object.
					node x = eval(nArrayList.get(2), env);
					try {
						x.clazz = Class.forName(nArrayList.get(1).stringValue());
					} catch (ClassNotFoundException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					return x;
				}
				case THREAD: { // (thread EXPR ...): Creates new thread and
								// starts it.
					final ArrayList<node> exprs = new ArrayList<node>(nArrayList.subList(1, nArrayList.size()));
					final environment env2 = new environment(env);
					Thread t = new Thread() {
						public void run() {
							for (node n : exprs) {
								try {
									eval(n, env2);
								} catch (Exception e) {
									// TODO Auto-generated catch block
									e.printStackTrace();
								}
							}
						}
					};
					t.start();
					return new node(t);
				}
				case DOSEQ: // (doseq (VAR SEQ) EXPR ...)
				{
					environment env2 = new environment(env);
					int varCode = nArrayList.get(1).arrayListValue().get(0).symbolValue().code;
					@SuppressWarnings("rawtypes")
					Iterable seq = (Iterable) eval(nArrayList.get(1).arrayListValue().get(1), env).value;
					int len = nArrayList.size();
					for (Object x : seq) {
						env2.set(varCode, (node) x);
						for (int i = 2; i < len; i++) {
							eval(nArrayList.get(i), env2);
						}
					}
					return node_nil;
				}
				case LET: // (let (VAR VALUE ...) BODY ...)
				{
					environment env2 = new environment(env);
					ArrayList<node> bindings = nArrayList.get(1).arrayListValue();
					for (int i = 0; i < bindings.size(); i+= 2) {
						env2.set(bindings.get(i).symbolValue().code, eval(bindings.get(i + 1), env2));
					}
					node ret = node_nil;
					for (int i = 2; i < nArrayList.size(); i++) {
						ret = eval(nArrayList.get(i), env2);
					}
					return ret;
				}
				default: {
					System.err.println("Not implemented function: [" + func.value.toString() + "]");
					return node_nil;
				}
				} // end switch(found)
			} else {
				// evaluate arguments
				ArrayList<node> args = new ArrayList<node>();
				int len = nArrayList.size();
				for (int i = 1; i < len; i++) {
					args.add(eval(nArrayList.get(i), env));
				}
				return apply(func, args, env);
			}
		} else {
			// return n.clone();
			return n;
		}
	}

	ArrayList<node> preprocess_all(ArrayList<node> lst) {
		ArrayList<node> preprocessed = new ArrayList<node>();
		int last = lst.size() - 1;
		for (int i = 0; i <= last; i++) {
			preprocessed.add(preprocess(lst.get(i)));
		}
		return preprocessed;
	}

	node eval_all(ArrayList<node> lst) throws Exception {
		int last = lst.size() - 1;
		if (last < 0)
			return node_nil;
		node ret = null;
		for (int i = 0; i <= last; i++) {
			ret = eval(lst.get(i), global_env);
		}
		return ret;
	}

	public node eval_string(String s) throws Exception {
		s = "(" + s + "\n)";
		ArrayList<node> preprocessed = preprocess_all(parse(s).arrayListValue());
		return eval_all(preprocessed);
	}

	void eval_print(String s) throws Exception {
		System.out.println(eval_string(s).str_with_type());
	}

	static void prompt() {
		System.out.print("> ");
	}

	static void prompt2() {
		System.out.print("  ");
	}

	// read-eval-print loop
	public void repl() {
		BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
		String code = "";
		while (true) {
			try {
				if (code.length() == 0)
					prompt();
				else
					prompt2();
				String line = br.readLine();
				if (line == null) { // EOF
					eval_print(code);
					break;
				}
				code += "\n" + line;
				tokenizer t = new tokenizer(code);
				t.tokenize();
				if (t.unclosed <= 0) { // no unmatched parenthesis nor quotation
					eval_print(code);
					code = "";
				}
			} catch (Exception e) {
				e.printStackTrace();
				code = "";
			}
		}
	}

	// extracts characters from filename
	public static String slurp(String fileName) throws IOException {
		return new String(java.nio.file.Files.readAllBytes(java.nio.file.Paths.get(fileName)));
	}

	// Opposite of slurp. Writes str to filename.
	public static int spit(String fileName, String str) {
		BufferedWriter bw;
		try {
			bw = new BufferedWriter(new FileWriter(fileName));
			bw.write(str);
			bw.close();
			return str.length();
		} catch (IOException e) {
			return -1;
		}
	}

	// for embedding
	public Object get(String s) {
		return global_env.get(symbol.toCode(s));
	}

	// for embedding
	public Object set(String s, Object o) {
		return global_env.set(symbol.toCode(s), new node(o));
	}

	public static Object testField;

	public static void main(String[] args) throws Exception {
		if (args.length == 0) {
			Core p = new Core();
			p.print_logo();
			p.repl();
			System.out.println();
			return;
		} else if (args.length == 1) {
			if (args[0].equals("-h")) {
				System.out.println("Usage: java javelin.Core [OPTIONS...] [FILES...]");
				System.out.println();
				System.out.println("OPTIONS:");
				System.out.println("    -h    print this screen.");
				System.out.println("    -v    print version.");
				return;
			} else if (args[0].equals("-v")) {
				System.out.println(Core.VERSION);
				return;
			}
		}

		// execute files, one by one
		for (String fileName : args) {
			Core p = new Core();
			try {
				p.eval_string(Core.slurp(fileName));
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
}
