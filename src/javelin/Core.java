package javelin;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
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
	public static final String VERSION = "0.5.3";
	static BufferedReader defaultReader = new BufferedReader(new InputStreamReader(System.in));

	public Core() throws Exception {
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
		set("quasiquote", Special.QUASIQUOTE);
		set("unquote", Special.UNQUOTE);
		set("unquote-splicing", Special.UNQUOTE_SPLICING);
		set("macroexpand", new Builtin.macroexpand());
		set("read", new Builtin.read());

		evalString("(defmacro defn (name & body) `(def ~name (fn ~@body)))" +
				"(defmacro when (cond & body) `(if ~cond (do ~@body)))" +
				"(defn nil? (x) (= nil x))" +
				"(defmacro while (test & body) `(loop () (when ~test ~@body (recur))))" +
				"(import java.lang)"
				);
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

	static HashMap<String, Fn> macros = new HashMap<>();

	static Object macroexpand(Object n) throws Exception {
		if (n instanceof ArrayList) {
			ArrayList<Object> expr = Core.arrayListValue(n);
			if (macros.containsKey(expr.get(0).toString())) {
				Fn func = macros.get(expr.get(0).toString());

				// build arguments
				ArrayList<Object> args = new ArrayList<Object>();
				int len = expr.size();
				for (int i = 1; i < len; i++) {
					args.add(expr.get(i));
				}
				Object r = apply(func, args, globalEnv);
				return macroexpand(r); // macroexpand again
			} else {
				ArrayList<Object> r = new ArrayList<Object>();
				for (Object n2 : expr) {
					r.add(preprocess(n2));
				}
				return r;
			}
		} else {
			return n;
		}
	}

	static Object preprocess(Object n) throws Exception {
		if (n instanceof ArrayList) { // function (FUNCTION ARGUMENT ...)
			ArrayList<Object> expr = Core.arrayListValue(n);
			if (expr.size() == 0)
				return n;
			Object func = preprocess(expr.get(0));
			if (func instanceof Symbol) {
				if (func.toString().equals("defmacro")) {
					// (defmacro add (a & more) `(+ ~a ~@more)) ; define macro
					macros.put(expr.get(1).toString(), new Fn(new ArrayList<Object>(expr.subList(2, expr.size())), globalEnv));
					return null;
				} else if (func.toString().equals("quote")) {
					// skip quote
					return n;
				} else {
					return macroexpand(n);
				}
			} else {
				return macroexpand(n);
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
							else if (param instanceof Character)
								paramClass = Character.TYPE;
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
							else if (param instanceof Character)
								paramClass = Character.TYPE;
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
							try {
								for (Object n : exprs) {
									eval(n, env2);
								}
							} catch (Exception e) {
								e.printStackTrace();
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
				case QUASIQUOTE: // (quasiquote S-EXPRESSION)
				{
					return quasiquote(expr.get(1), env);
				}
				case UNQUOTE:
				case UNQUOTE_SPLICING:
				{
					throw new Exception("Invalid syntax"); // unused outside a quasiquote
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

	private static Object quasiquote(Object arg, Environment env) throws Exception {
		if (arg instanceof ArrayList && ((ArrayList<?>) arg).size() > 0) {
			@SuppressWarnings("unchecked")
			ArrayList<Object> arg2 = (ArrayList<Object>) arg;
			Object head = arg2.get(0);
			if (head instanceof Symbol && ((Symbol) head).toString().equals("unquote")) {
				return preprocessEval(arg2.get(1), env);
			}
			ArrayList<Object> ret = new ArrayList<>();
			for (Object a : arg2) {
				if (a instanceof ArrayList) {
					@SuppressWarnings("unchecked")
					ArrayList<Object> a2 = (ArrayList<Object>) a;
					if (a2.size() > 0) {
						Object head2 = a2.get(0);
						if (head2 instanceof Symbol) {
							String s = head2.toString();
							if (s.equals("unquote")) {
								ret.add(preprocessEval(a2.get(1), env));
							} else if (s.equals("unquote-splicing")) {
								ret.addAll(Core.arrayListValue(preprocessEval(a2.get(1), env)));
							} else {
								ret.add(quasiquote(a, env));
							}
						} else {
							ret.add(quasiquote(a, env));
						}
					} else {
						ret.add(quasiquote(a, env));
					}
				} else {
					ret.add(a);
				}
			}
			return ret;
		} else {
			return arg;
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

	ArrayList<Object> preprocessAll(ArrayList<Object> lst) throws Exception {
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
		//ArrayList<Object> preprocessed = preprocessAll(Core.arrayListValue(Parser.parse(s)));
		ArrayList<Object> preprocessed = preprocessAll(Core.arrayListValue(parse(new StringReader(s))));
		return evalAll(preprocessed);
	}

	void evalPrint(String s) throws Exception {
		System.out.println(Core.strWithType(evalString(s)));
	}

	static void prompt() {
		System.out.print("> ");
	}

	// read-eval-print loop
	public void repl() {
		while (true) {
			try {
				prompt();
				Object expr = parse(defaultReader);
				System.out.println(preprocessEval(expr, globalEnv));
			} catch (Exception e) {
				e.printStackTrace();
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

	public static Object preprocessEval(Object object, Environment env) throws Exception {
		return eval(preprocess(object), env);
	}
	
	public static char peek(Reader r) throws IOException {
		r.mark(1);
		char c = (char) r.read();
		r.reset();
		return c;
	}
	
	public static boolean eof(Reader r) throws IOException {
		r.mark(1);
		int i = r.read();
		r.reset();
		return i < 0;
	}
	
	public static String readToken(Reader r) throws IOException {
		final String ws = " \t\r\n,";
		final String delim = "() \t\r\n,;";
		final String prefix = "()'`";
		
		while (true) {
			StringBuilder acc = new StringBuilder(); // accumulator
			char c, p;
			
			// skip whitespaces
			while (!eof(r)) {
				p = peek(r);
				if (ws.indexOf(p) < 0) break;
				r.read();
			}
			if (eof(r)) throw new RuntimeException("EOF while reading");
			
			p = peek(r);
			if (prefix.indexOf(p) >= 0) { // prefix
				c = (char) r.read();
				return "" + c;
			} else if (p == '~') { // unquote
				c = (char) r.read();
				acc.append(c);
				if (peek(r) == '@') { // unquote-splicing
					c = (char) r.read();
					acc.append(c);
				}
				return acc.toString();
			} else if (p == '"') { // string
				acc.append(p);
				r.read();
				while (!eof(r)) {
					c = (char) r.read();
					if (c == '"') {
						break;
					}
					if (c == '\\') { // escape
						char next = (char) r.read();
						if (next == 'r')
							next = '\r';
						else if (next == 'n')
							next = '\n';
						else if (next == 't')
							next = '\t';
						acc.append(next);
					} else {
						acc.append(c);
					}
				}
				return acc.toString();
			} else if (p == ';') { // end-of-line comment
				while (!eof(r) && peek(r) != '\n') {
					r.read();
				}
				continue;
			} else { // other
				// read until delim
				while (!eof(r)) {
					p = peek(r);
					if (delim.indexOf(p) >= 0) break;
					c = (char) r.read();
					acc.append(c);
				}
				return acc.toString();
			}
		}		
	}
	
	public static Object parse(Reader r) throws IOException {
		return parse(r, readToken(r));
	}
	
	// tok is provided if already read
	public static Object parse(Reader r, String tok) throws IOException {		
		if (tok.charAt(0) == '"') { // double-quoted string
			return tok.substring(1);
		} else if (tok.equals("'")) { // quote
			ArrayList<Object> ret = new ArrayList<Object>();
			ret.add(new Symbol("quote"));
			ret.add(parse(r));
			return ret;
		} else if (tok.equals("`")) { // quasiquote
			ArrayList<Object> ret = new ArrayList<Object>();
			ret.add(new Symbol("quasiquote"));
			ret.add(parse(r));
			return ret;
		} else if (tok.equals("~")) { // unquote
			ArrayList<Object> ret = new ArrayList<Object>();
			ret.add(new Symbol("unquote"));
			ret.add(parse(r));
			return ret;
		} else if (tok.equals("~@")) { // unquote-splicing
			ArrayList<Object> ret = new ArrayList<Object>();
			ret.add(new Symbol("unquote-splicing"));
			ret.add(parse(r));
			return ret;
		} else if (tok.equals("(")) { // list
			return parseList(r);
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

	private static ArrayList<Object> parseList(Reader r) throws IOException {
		ArrayList<Object> ret = new ArrayList<Object>();
		while (!eof(r)) {
			String tok = readToken(r);
			if (tok.equals(")")) { // end of list
				break;
			} else {
				Object o = parse(r, tok);
				ret.add(o);
			}
		}
		return ret;
	}
}
