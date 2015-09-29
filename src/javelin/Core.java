package javelin;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.TreeSet;

public class Core {
	public static final String VERSION = "0.4.8";

	public Core() throws Exception {
		init();
	}

	static int intValue(Object value) {
		if (value instanceof Number) {
			return ((Number) value).intValue();
		} else {
			return Integer.parseInt(value.toString());
		}
	}

	static double doubleValue(Object value) {
		if (value instanceof Number) {
			return ((Number) value).doubleValue();
		} else {
			return Double.parseDouble(value.toString());
		}
	}

	static long longValue(Object value) {
		if (value instanceof Number) {
			return ((Number) value).longValue();
		} else {
			return Long.parseLong(value.toString());
		}
	}

	static boolean booleanValue(Object value) { // null is false, other type is true.
		if (value == null)
			return false;
		if (value instanceof Boolean)
			return (Boolean) value;
		else
			return true;
	}

	@SuppressWarnings("unchecked")
	static ArrayList<Object> arrayListValue(Object value) {
		return (ArrayList<Object>) value;
	}

	static String type(Object value) {
		if (value == null)
			return "nil";
		else
			return value.getClass().getName();
	}

	static String strWithType(Object value) {
		String s = "";
		if (value == null) s = "nil";
		else if (value instanceof String) s = "\"" + value + "\"";
		else s = value.toString();
		return s + " : " + type(value);
	}

	static public String toString(Object value) {
		return value.toString();
	}

	static public Symbol symbolValue(Object value) {
		return (Symbol) value;
	}

	@SuppressWarnings("unchecked")
	static Object apply(Object func, ArrayList<Object> args, Environment env) throws Exception {
		if (func instanceof IFn) {
			return ((IFn) func).invoke(args, env);
		} else {
			if (func instanceof List) {
				// implicit indexing
				return ((List<Object>) func).get(Core.intValue(args.get(0)));
			}
			else {
				System.err.println("Unknown function: [" + func.toString() + "]");
				return null;
			}
		}
	}

	static Environment globalEnv = new Environment(); // variables. compile-time
	static ArrayList<String> imports = new ArrayList<String>();

	static void printCollection(Collection<String> coll) {
		for (String key : new TreeSet<String>(coll)) {
			System.out.print(" " + key);
		}
		System.out.println();
	}

	public static void printLogo() {
		System.out.println("Javelin " + VERSION);
		System.out.println("Predefined Symbols:");
		ArrayList<String> r = new ArrayList<String>(globalEnv.env.keySet().size());
		for (int x : globalEnv.env.keySet()) {
			r.add(Symbol.symname.get(x));
		}
		printCollection(r);
		System.out.println("Macros:");
		printCollection(macros.keySet());
	}

	void init() throws Exception {
		set("true", true);
		set("false", false);
		set("nil", null);

		set("+", new Builtin._plus());
		set("-", new Builtin._minus());
		set("*", new Builtin._star());
		set("/", new Builtin._slash());
		set("mod", new Builtin.mod());
		set("=", new Builtin._eq());
		set("==", new Builtin._eq_eq());
		set("not=", new Builtin.Not_eq());
		set("<", new Builtin._lt());
		set(">", new Builtin._gt());
		set("<=", new Builtin._lt_eq());
		set(">=", new Builtin._gt_eq());
		set("and", Special.AND);
		set("or", Special.OR);
		set("not", new Builtin.not());
		set("if", Special.IF);
		set("read-string", new Builtin.read_string());
		set("type", new Builtin.type());
		set("eval", new Builtin.eval());
		set("quote", Special.QUOTE);
		set("fn", Special.FN);
		set("list", new Builtin.list());
		set("apply", new Builtin.apply());
		set("fold", new Builtin.fold());
		set("map", new Builtin.map());
		set("filter", new Builtin.filter());
		set("do", Special.DO);
		set(".", Special._DOT);
		set(".get", Special._DOTGET);
		set(".set!", Special._DOTSET_E);
		set("new", Special.NEW);
		set("set!", Special.SET_E);
		set("pr", new Builtin.pr());
		set("prn", new Builtin.prn());
		set("defmacro", Special.DEFMACRO);
		set("read-line", new Builtin.read_line());
		set("slurp", new Builtin.slurp());
		set("spit", new Builtin.spit());
		set("thread", Special.THREAD);
		set("def", Special.DEF);
		set("doseq", Special.DOSEQ);
		set("str", new Builtin.str());
		set("let", Special.LET);
		set("symbol", new Builtin.symbol());
		set("import", Special.IMPORT);
		set("reify", Special.REIFY);
		set("recur", Special.RECUR);
		set("loop", Special.LOOP);

		evalString("(defmacro defn (name ...) (def name (fn ...)))" +
				"(defmacro when (cond ...) (if cond (do ...)))" +
				"(defn nil? (x) (= nil x))" +
				"(defmacro while (test ...) (loop () (when test ... (recur))))" +
				"(import java.lang)"
				);
	}

	static HashMap<String, Object[]> macros = new HashMap<>();

	static Object applyMacro(Object body, HashMap<String, Object> vars) {
		if (body instanceof ArrayList) {
			@SuppressWarnings("unchecked")
			ArrayList<Object> bvec = (ArrayList<Object>) body;
			ArrayList<Object> ret = new ArrayList<>();
			for (int i = 0; i < bvec.size(); i++) {
				Object b = bvec.get(i);
				if (b.toString().equals("...")) { // ... is like unquote-splicing
					ret.addAll(Core.arrayListValue(vars.get(b.toString())));
				} else
					ret.add(applyMacro(bvec.get(i), vars));
			}
			return ret;
		} else {
			String bstr = body.toString();
			if (vars.containsKey(bstr))
				return vars.get(bstr);
			else
				return body;
		}
	}

	static Object macroexpand(Object n) {
		ArrayList<Object> nArrayList = Core.arrayListValue(n);
		if (macros.containsKey(nArrayList.get(0).toString())) {
			Object[] macro = macros.get(nArrayList.get(0).toString());
			HashMap<String, Object> macrovars = new HashMap<>();
			ArrayList<Object> argsyms = Core.arrayListValue(macro[0]);
			for (int i = 0; i < argsyms.size(); i++) {
				String argsym = argsyms.get(i).toString();
				if (argsym.equals("...")) {
					ArrayList<Object> n2 = new ArrayList<Object>();
					macrovars.put(argsym, n2);
					ArrayList<Object> ellipsis = n2;
					for (int i2 = i + 1; i2 < nArrayList.size(); i2++)
						ellipsis.add(nArrayList.get(i2));
					break;
				} else {
					macrovars.put(argsyms.get(i).toString(), nArrayList.get(i + 1));
				}
			}
			return applyMacro(macro[1], macrovars);
		} else
			return n;
	}

	static Object preprocess(Object n) {
		if (n instanceof ArrayList) { // function (FUNCTION ARGUMENT ...)
			ArrayList<Object> nArrayList = Core.arrayListValue(n);
			if (nArrayList.size() == 0)
				return n;
			Object func = preprocess(nArrayList.get(0));
			if (func instanceof Symbol && func.toString().equals(("defmacro"))) {
				// (defmacro add (a b) (+ a b)) ; define macro
				macros.put(nArrayList.get(1).toString(), new Object[] { nArrayList.get(2), nArrayList.get(3) });
				return null;
			} else {
				if (macros.containsKey(nArrayList.get(0).toString())) { // compile
																			// macro
					return preprocess(macroexpand(n));
				} else {
					ArrayList<Object> r = new ArrayList<Object>();
					for (Object n2 : nArrayList) {
						r.add(preprocess(n2));
					}
					return r;
				}
			}
		} else {
			return n;
		}
	}

	static Object eval(Object n, Environment env) throws Exception {
		if (n instanceof Symbol) {
			Object r = env.get(((Symbol) n).code);
			return r;
		} else if (n instanceof ArrayList) { // function (FUNCTION
													// ARGUMENT ...)
			ArrayList<Object> expr = Core.arrayListValue(n);
			if (expr.size() == 0)
				return null;
			Object func = eval(expr.get(0), env);
			if (func instanceof Special) {
				switch ((Special) func) {
				case SET_E: { // (set! SYMBOL VALUE ...) ; set the SYMBOL's value
					Object value = null;
					int len = expr.size();
					for (int i = 1; i < len; i += 2) {
						value = eval(expr.get(i + 1), env);
						env.set(((Symbol)expr.get(i)).code, value);
					}
					return value;
				}
				case DEF: { // (def SYMBOL VALUE ...) ; set in the current
							// environment
					Object ret = null;
					int len = expr.size();
					for (int i = 1; i < len; i += 2) {
						Object value = eval(expr.get(i + 1), env);
						ret = env.def(((Symbol) expr.get(i)).code, value);
					}
					return ret;
				}
				case AND: { // (and X ...) short-circuit
					for (int i = 1; i < expr.size(); i++) {
						if (!Core.booleanValue(eval(expr.get(i), env))) {
							return false;
						}
					}
					return true;
				}
				case OR: { // (or X ...) short-circuit
					for (int i = 1; i < expr.size(); i++) {
						if (Core.booleanValue(eval(expr.get(i), env))) {
							return true;
						}
					}
					return false;
				}
				case IF: { // (if CONDITION THEN_EXPR [ELSE_EXPR])
					Object cond = expr.get(1);
					if (Core.booleanValue(eval(cond, env))) {
						return eval(expr.get(2), env);
					} else {
						if (expr.size() <= 3)
							return null;
						return eval(expr.get(3), env);
					}
				}
				case QUOTE: { // (quote X)
					return expr.get(1);
				}
				case FN: {
					// anonymous function. lexical scoping
					// (fn (ARGUMENT ...) BODY ...)
					ArrayList<Object> r = new ArrayList<Object>();
					for (int i = 1; i < expr.size(); i++) {
						r.add(expr.get(i));
					}
					return new Fn(r, env);
				}
				case DO: { // (do X ...)
					int last = expr.size() - 1;
					if (last <= 0)
						return null;
					for (int i = 1; i < last; i++) {
						eval(expr.get(i), env);
					}
					return eval(expr.get(last), env);
				}
				case _DOT: {
					// Java interoperability
					// (. CLASS-OR-OBJECT METHOD ARGUMENT ...) ; Java method invocation
					try {
						// get class
						Class<?> cls;
						Object obj = null;
						try {
							// object's method e.g. (. "abc" length)
							obj = eval(expr.get(1), env);
							cls = obj.getClass();
						} catch (NoSuchVariableException e) {
							// class's static method e.g. (. java.lang.Math floor 1.5)
							String className = expr.get(1).toString();
							cls = getClass(className);
						}

						Class<?>[] parameterTypes = new Class<?>[expr.size() - 3];
						ArrayList<Object> parameters = new ArrayList<Object>();
						int last = expr.size() - 1;
						for (int i = 3; i <= last; i++) {
							Object a = eval(expr.get(i), env);
							Object param = a;
							parameters.add(param);
							Class<?> paramClass;
							if (param instanceof Integer)
								paramClass = Integer.TYPE;
							else if (param instanceof Double)
								paramClass = Double.TYPE;
							else if (param instanceof Long)
								paramClass = Long.TYPE;
							else if (param instanceof Boolean)
								paramClass = Boolean.TYPE;
							else {
								if (param == null) paramClass = null;
								else paramClass = param.getClass();
							}
							parameterTypes[i - 3] = paramClass;
						}
						String methodName = expr.get(2).toString();
						try {
							Method m = cls.getMethod(methodName, parameterTypes);
							return m.invoke(obj, parameters.toArray());
						} catch (NoSuchMethodException e) {
							for (Method m : cls.getMethods()) {
								// find a method with the same number of parameters
								if (m.getName().equals(methodName) && m.getParameterTypes().length == expr.size() - 3) {
									try {
										return m.invoke(obj, parameters.toArray());
									} catch (IllegalArgumentException iae) {
										// try next method
										continue;
									}
								}
							}
						}
						throw new IllegalArgumentException(expr.toString());
					} catch (Exception e) {
						e.printStackTrace();
						return null;
					}
				}
				case _DOTGET: {
					// Java interoperability
					// (.get CLASS-OR-OBJECT FIELD) ; get Java field
					try {
						// get class
						Class<?> cls;
						Object obj = null;
						try {
							// object's method e.g. (. "abc" length)
							obj = eval(expr.get(1), env);
							cls = obj.getClass();
						} catch (NoSuchVariableException e) {
							// class's static method e.g. (. java.lang.Math floor 1.5)
							String className = expr.get(1).toString();
							cls = getClass(className);
						}

						String fieldName = expr.get(2).toString();
						java.lang.reflect.Field field = cls.getField(fieldName);
						return field.get(cls);
					} catch (Exception e) {
						e.printStackTrace();
						return null;
					}
				}
				case _DOTSET_E: {
					// Java interoperability
					// (.set! CLASS-OR-OBJECT FIELD VALUE) ; set Java field
					try {
						// get class
						Class<?> cls;
						Object obj = null;
						try {
							// object's method e.g. (. "abc" length)
							obj = eval(expr.get(1), env);
							cls = obj.getClass();
						} catch (NoSuchVariableException e) {
							// class's static method e.g. (. java.lang.Math floor 1.5)
							String className = expr.get(1).toString();
							cls = getClass(className);
						}

						String fieldName = expr.get(2).toString();
						java.lang.reflect.Field field = cls.getField(fieldName);
						Object value = eval(expr.get(3), env);
						field.set(cls, value);
						return null;
					} catch (Exception e) {
						e.printStackTrace();
						return null;
					}
				}
				case NEW: {
					// Java interoperability
					// (new CLASS ARG ...) ; create new Java object
					try {
						String className = expr.get(1).toString();
						Class<?> cls = getClass(className);
						Class<?>[] parameterTypes = new Class<?>[expr.size() - 2];
						ArrayList<Object> parameters = new ArrayList<Object>();
						int last = expr.size() - 1;
						for (int i = 2; i <= last; i++) {
							Object a = eval(expr.get(i), env);
							Object param = a;
							parameters.add(param);
							Class<?> paramClass;
							if (param instanceof Integer)
								paramClass = Integer.TYPE;
							else if (param instanceof Double)
								paramClass = Double.TYPE;
							else if (param instanceof Long)
								paramClass = Long.TYPE;
							else if (param instanceof Boolean)
								paramClass = Boolean.TYPE;
							else {
								if (param == null) paramClass = null;
								else paramClass = param.getClass();
							}
							parameterTypes[i - 2] = paramClass;
						}

						try {
							Constructor<?> c = cls.getConstructor(parameterTypes);
							return c.newInstance(parameters.toArray());
						} catch (NoSuchMethodException e) {
							for (Constructor<?> c : cls.getConstructors()) {
								// find a constructor with the same number of parameters
								if (c.getParameterTypes().length == expr.size() - 2) {
									try {
										return c.newInstance(parameters.toArray());
									} catch (IllegalArgumentException iae) {
										// try next constructor
										continue;
									}
								}
							}
						}
						throw new IllegalArgumentException(expr.toString());
					} catch (Exception e) {
						e.printStackTrace();
						return null;
					}
				}
				case THREAD: { // (thread EXPR ...): Creates new thread and
								// starts it.
					final ArrayList<Object> exprs = new ArrayList<Object>(expr.subList(1, expr.size()));
					final Environment env2 = new Environment(env);
					Thread t = new Thread() {
						public void run() {
							for (Object n : exprs) {
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
					return t;
				}
				case DOSEQ: // (doseq (VAR SEQ) EXPR ...)
				{
					Environment env2 = new Environment(env);
					int varCode = Core.symbolValue(Core.arrayListValue(expr.get(1)).get(0)).code;
					@SuppressWarnings("rawtypes")
					Iterable seq = (Iterable) eval(Core.arrayListValue(expr.get(1)).get(1), env);
					int len = expr.size();
					for (Object x : seq) {
						env2.def(varCode, (Object) x);
						for (int i = 2; i < len; i++) {
							eval(expr.get(i), env2);
						}
					}
					return null;
				}
				case LET: // (let (VAR VALUE ...) BODY ...)
				{
					Environment env2 = new Environment(env);
					ArrayList<Object> bindings = Core.arrayListValue(expr.get(1));
					for (int i = 0; i < bindings.size(); i+= 2) {
						env2.def(Core.symbolValue(bindings.get(i)).code, eval(bindings.get(i + 1), env2));
					}
					Object ret = null;
					for (int i = 2; i < expr.size(); i++) {
						ret = eval(expr.get(i), env2);
					}
					return ret;
				}
				case IMPORT: // (import CLASS-PREFIX ...)
				{
					for (int i = 1; i < expr.size(); i++) {
						String s = expr.get(i).toString();
						if (!imports.contains(s)) imports.add(s);
					}
					return imports;
				}
				case REIFY: // (reify INTERFACE (METHOD (ARGS ...) BODY ...) ...)
				{
// Note that the first parameter must be supplied to
// correspond to the target object ('this' in Java parlance). Thus
// methods for interfaces will take one more argument than do the
// interface declarations.
//
// Example:
//					(import javax.swing java.awt java.awt.event)
//
//					(def frame (new JFrame))
//					(def button (new Button "Hello"))
//					(. button addActionListener
//						(reify java.awt.event.ActionListener
//						  (actionPerformed (this e)
//							(. javax.swing.JOptionPane showMessageDialog nil (str "Hello, World!\nthis=" this "\ne=" e)))))
//					(. frame setDefaultCloseOperation (.get JFrame EXIT_ON_CLOSE))
//					(. frame add button (.get BorderLayout NORTH))
//					(. frame pack)
//					(. frame setVisible true)

					Class<?> cls = getClass(expr.get(1).toString());
					ClassLoader cl = cls.getClassLoader();
					HashMap<String, Fn> methods = new HashMap<String, Fn>();
					for (int i = 2; i < expr.size(); i++) {
						@SuppressWarnings("unchecked")
						ArrayList<Object> methodDef = (ArrayList<Object>) expr.get(i);
						methods.put(methodDef.get(0).toString(), new Fn(new ArrayList<Object>(methodDef.subList(1, methodDef.size())), env));
					}
					class MyHandler implements InvocationHandler {
						HashMap<String, Fn> methods;
						Environment env;

						public MyHandler(HashMap<String, Fn> methods, Environment env) {
							this.methods = methods;
							this.env = env;
						}

						@Override
						public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
							ArrayList<Object> args2 = new ArrayList<Object>();
							args2.add(this);
							args2.addAll(Arrays.asList(args));
							return apply(methods.get(method.getName()), args2, env);
						}
					}
					InvocationHandler handler = new MyHandler(methods, env);
					Object ret = Proxy.newProxyInstance(cl, new Class[] {cls}, handler);
					return ret;
				}
				case RECUR: // (recur ARG ...)
				{
					ArrayList<Object> args = new ArrayList<Object>();
					for (int i = 1; i < expr.size(); i++) {
						args.add(eval(expr.get(i), env));
					}
					throw new RecurException(args);
				}
				case LOOP: // (loop (VAR VALUE ...) BODY ...)
				{
					// separate formal and actual parameters
					ArrayList<Object> bindings = Core.arrayListValue(expr.get(1));
					ArrayList<Object> formalParams = new ArrayList<Object>();
					ArrayList<Object> actualParams = new ArrayList<Object>();
					Environment env2 = new Environment(env);
					for (int i = 0; i < bindings.size(); i+= 2) {
						formalParams.add(bindings.get(i));
						actualParams.add(eval(bindings.get(i + 1), env2));
					}

					loopStart: while (true) {
						// fill the environment
						for (int i = 0; i < formalParams.size(); i++) {
							env2.def(Core.symbolValue(formalParams.get(i)).code, actualParams.get(i));
						}
						// evaluate body
						Object ret = null;
						for (int i = 2; i < expr.size(); i++) {
							try {
								ret = eval(expr.get(i), env2);
							} catch (RecurException e) {
								actualParams = e.args;
								continue loopStart; // recur this loop (effectively goto)
							}
						}
						return ret;
					}
				}
				default: {
					System.err.println("Not implemented function: [" + func.toString() + "]");
					return null;
				}
				} // end switch(found)
			} else {
				// evaluate arguments
				ArrayList<Object> args = new ArrayList<Object>();
				int len = expr.size();
				for (int i = 1; i < len; i++) {
					args.add(eval(expr.get(i), env));
				}
				return apply(func, args, env);
			}
		} else {
			// return n.clone();
			return n;
		}
	}

	static HashMap<String, Class<?>> getClassCache = new HashMap<>();

	// cached
	private static Class<?> getClass(String className) throws ClassNotFoundException {
		if (getClassCache.containsKey(className)) {
			return getClassCache.get(className);
		} else {
			try {
				Class<?> value = Class.forName(className);
				getClassCache.put(className, value);
				return value;
			} catch (ClassNotFoundException cnfe) {
				for (String prefix : imports) {
					try {
						Class<?> value = Class.forName(prefix + "." + className);
						getClassCache.put(className, value);
						return value;
					} catch (ClassNotFoundException e) {
						// try next import prefix
						continue;
					}
				}
				throw new ClassNotFoundException(className);
			}
		}
	}

	ArrayList<Object> preprocessAll(ArrayList<Object> lst) {
		ArrayList<Object> preprocessed = new ArrayList<Object>();
		int last = lst.size() - 1;
		for (int i = 0; i <= last; i++) {
			preprocessed.add(preprocess(lst.get(i)));
		}
		return preprocessed;
	}

	Object evalAll(ArrayList<Object> lst) throws Exception {
		int last = lst.size() - 1;
		if (last < 0)
			return null;
		Object ret = null;
		for (int i = 0; i <= last; i++) {
			ret = eval(lst.get(i), globalEnv);
		}
		return ret;
	}

	public Object evalString(String s) throws Exception {
		s = "(" + s + "\n)";
		ArrayList<Object> preprocessed = preprocessAll(Core.arrayListValue(Parser.parse(s)));
		return evalAll(preprocessed);
	}

	void evalPrint(String s) throws Exception {
		System.out.println(Core.strWithType(evalString(s)));
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
					evalPrint(code);
					break;
				}
				code += "\n" + line;
				Tokenizer t = new Tokenizer(code);
				t.tokenize();
				if (t.unclosed <= 0) { // no unmatched parenthesis nor quotation
					evalPrint(code);
					code = "";
				}
			} catch (Exception e) {
				e.printStackTrace();
				code = "";
			}
		}
	}

	// extracts characters from URL or filename
	public static String slurp(String urlOrFileName) throws IOException {
		try {
			// try URL
			URL url = new URL(urlOrFileName);
			InputStream is = url.openStream();
			try (BufferedReader br = new BufferedReader(new InputStreamReader(is))) {
				String line;
				StringBuffer sb = new StringBuffer();
				while ((line = br.readLine()) != null) {
					sb.append(line);
				}
				return sb.toString();
			}
		} catch (MalformedURLException e) {
			return new String(java.nio.file.Files.readAllBytes(java.nio.file.Paths.get(urlOrFileName)));
		}
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
	public Object get(String s) throws Exception {
		return globalEnv.get(Symbol.toCode(s));
	}

	// for embedding
	public Object set(String s, Object o) {
		return globalEnv.def(Symbol.toCode(s), o);
	}

	public static Object testField;

	public static void main(String[] args) throws Exception {
		if (args.length == 0) {
			Core p = new Core();
			printLogo();
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
				p.evalString(Core.slurp(fileName));
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	public static Object preprocess_eval(Object object, Environment env) throws Exception {
		return eval(preprocess(object), env);
	}
}
