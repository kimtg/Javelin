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
	public static final String VERSION = "0.1.1";

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

		public Symbol symbolValue() {
			return (Symbol) value;
		}
	}

	// frequently used constants
	static final node node_true = new node(true);
	static final node node_false = new node(false);
	static final node node_0 = new node(0);
	static final node node_1 = new node(1);
	static final node node_nil = new node();

	public static ArrayList<String> tokenize(String s) {
		return new Tokenizer(s).tokenize();
	}

	static node parse(String s) {
		return new Parser(tokenize(s)).parse();
	}

	@SuppressWarnings("unchecked")
	static node apply(node func, ArrayList<node> args, Environment env) throws Exception {
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

	Environment global_env = new Environment(); // variables. compile-time

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
			r.add(Symbol.symname.get(x));
		}
		print_collection(r);
		System.out.println("Macros:");
		print_collection(macros.keySet());
	}

	void init() throws Exception {
		global_env.env.put(Symbol.toCode("true"), node_true);
		global_env.env.put(Symbol.toCode("false"), node_false);
		global_env.env.put(Symbol.toCode("nil"), new node());

		global_env.env.put(Symbol.toCode("+"), new node(new Builtin._plus()));
		global_env.env.put(Symbol.toCode("-"), new node(new Builtin._minus()));
		global_env.env.put(Symbol.toCode("*"), new node(new Builtin._star()));
		global_env.env.put(Symbol.toCode("/"), new node(new Builtin._slash()));
		global_env.env.put(Symbol.toCode("mod"), new node(new Builtin.mod()));
		global_env.env.put(Symbol.toCode("="), new node(new Builtin._eq()));
		global_env.env.put(Symbol.toCode("=="), new node(new Builtin._eq_eq()));
		global_env.env.put(Symbol.toCode("not="), new node(new Builtin.Not_eq()));
		global_env.env.put(Symbol.toCode("<"), new node(new Builtin._lt()));
		global_env.env.put(Symbol.toCode(">"), new node(new Builtin._gt()));
		global_env.env.put(Symbol.toCode("<="), new node(new Builtin._lt_eq()));
		global_env.env.put(Symbol.toCode(">="), new node(new Builtin._gt_eq()));
		global_env.env.put(Symbol.toCode("and"), new node(Special.AND));
		global_env.env.put(Symbol.toCode("or"), new node(Special.OR));
		global_env.env.put(Symbol.toCode("not"), new node(new Builtin.not()));
		global_env.env.put(Symbol.toCode("if"), new node(Special.IF));
		global_env.env.put(Symbol.toCode("while"), new node(Special.WHILE));
		global_env.env.put(Symbol.toCode("read-string"), new node(new Builtin.read_string()));
		global_env.env.put(Symbol.toCode("type"), new node(new Builtin.type()));
		global_env.env.put(Symbol.toCode("eval"), new node(new Builtin.eval()));
		global_env.env.put(Symbol.toCode("quote"), new node(Special.QUOTE));
		global_env.env.put(Symbol.toCode("fn"), new node(Special.FN));
		global_env.env.put(Symbol.toCode("list"), new node(new Builtin.list()));
		global_env.env.put(Symbol.toCode("apply"), new node(new Builtin.apply()));
		global_env.env.put(Symbol.toCode("fold"), new node(new Builtin.fold()));
		global_env.env.put(Symbol.toCode("map"), new node(new Builtin.map()));
		global_env.env.put(Symbol.toCode("filter"), new node(new Builtin.filter()));
		global_env.env.put(Symbol.toCode("do"), new node(Special.DO));
		global_env.env.put(Symbol.toCode("."), new node(Special._DOT));
		global_env.env.put(Symbol.toCode(".get"), new node(Special._DOTGET));
		global_env.env.put(Symbol.toCode(".set!"), new node(Special._DOTSET_E));
		global_env.env.put(Symbol.toCode("new"), new node(Special.NEW));
		global_env.env.put(Symbol.toCode("set!"), new node(Special.SET_E));
		global_env.env.put(Symbol.toCode("pr"), new node(new Builtin.pr()));
		global_env.env.put(Symbol.toCode("prn"), new node(new Builtin.prn()));
		global_env.env.put(Symbol.toCode("cast"), new node(Special.CAST));
		global_env.env.put(Symbol.toCode("defmacro"), new node(Special.DEFMACRO));
		global_env.env.put(Symbol.toCode("read-line"), new node(new Builtin.read_line()));
		global_env.env.put(Symbol.toCode("slurp"), new node(new Builtin.slurp()));
		global_env.env.put(Symbol.toCode("spit"), new node(new Builtin.spit()));
		global_env.env.put(Symbol.toCode("thread"), new node(Special.THREAD));
		global_env.env.put(Symbol.toCode("def"), new node(Special.DEF));
		global_env.env.put(Symbol.toCode("break"), new node(Special.BREAK));
		global_env.env.put(Symbol.toCode("doseq"), new node(Special.DOSEQ));
		global_env.env.put(Symbol.toCode("str"), new node(new Builtin.str()));
		global_env.env.put(Symbol.toCode("let"), new node(Special.LET));
		global_env.env.put(Symbol.toCode("symbol"), new node(new Builtin.symbol()));

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
			if (func.value instanceof Symbol && func.toString().equals(("defmacro"))) {
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

	static node eval(node n, Environment env) throws Exception {
		if (n.value instanceof Symbol) {
			node r = env.get(((Symbol) n.value).code);
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
						ret = env.set(((Symbol) nArrayList.get(i).value).code, value);
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
					return new node(new Fn(r, env));
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
						if (nArrayList.get(1).value instanceof Symbol
								&& env.get(((Symbol) nArrayList.get(1).value).code) == null) {
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
						if (nArrayList.get(1).value instanceof Symbol
								&& env.get(((Symbol) nArrayList.get(1).value).code) == null) {																								// static
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
						if (nArrayList.get(1).value instanceof Symbol
								&& env.get(((Symbol) nArrayList.get(1).value).code) == null) { // class's static method
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
					final Environment env2 = new Environment(env);
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
					Environment env2 = new Environment(env);
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
					Environment env2 = new Environment(env);
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
				Tokenizer t = new Tokenizer(code);
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
		return global_env.get(Symbol.toCode(s));
	}

	// for embedding
	public Object set(String s, Object o) {
		return global_env.set(Symbol.toCode(s), new node(o));
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
