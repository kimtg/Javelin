package javelin;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.EOFException;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.TreeSet;
import java.util.Vector;
import java.util.regex.Pattern;

public final class Core {
	public static final String VERSION = "0.15";

	// no instance
	private Core() {
		
	}
	
	static BufferedReader defaultReader = new BufferedReader(new InputStreamReader(System.in));
	static final Symbol sym_set_e = new Symbol("set!");
	static final Symbol sym_def = new Symbol("def");
	static final Symbol sym_and = new Symbol("and");
	static final Symbol sym_or = new Symbol("or");
	static final Symbol sym_if = new Symbol("if");
	static final Symbol sym_quote = new Symbol("quote");
	static final Symbol sym_fn = new Symbol("fn");
	static final Symbol sym_do = new Symbol("do");
	static final Symbol sym__dot = new Symbol(".");
	static final Symbol sym_new = new Symbol("new");
	static final Symbol sym_doseq = new Symbol("doseq");
	static final Symbol sym_let = new Symbol("let");
	static final Symbol sym_import = new Symbol("import");
	static final Symbol sym_reify = new Symbol("reify");
	static final Symbol sym_recur = new Symbol("recur");
	static final Symbol sym_loop = new Symbol("loop");
	static final Symbol sym_quasiquote = new Symbol("quasiquote");
	static final Symbol sym_unquote = new Symbol("unquote");
	static final Symbol sym_unquote_splicing = new Symbol("unquote-splicing");
	static final Symbol sym_try = new Symbol("try");
	static final Symbol sym_catch = new Symbol("catch");
	static final Symbol sym_finally = new Symbol("finally");
	static final Symbol sym_defmacro = new Symbol("defmacro");

	static Environment globalEnv = new Environment(); // variables. compile-time
	static ArrayList<String> imports = new ArrayList<String>();
	static HashMap<String, UserFn> macros = new HashMap<>();
	static HashMap<String, Class<?>> getClassCache = new HashMap<>();
	
	public static Object testField;
	
	static {
		set("+", new Builtin._plus());
		set("-", new Builtin._minus());
		set("*", new Builtin._star());
		set("/", new Builtin._slash());
		set("quot", new Builtin.quot());
		set("mod", new Builtin.mod());
		set("=", new Builtin._eq());
		set("==", new Builtin._eq_eq());
		set("not=", new Builtin.Not_eq());
		set("<", new Builtin._lt());
		set(">", new Builtin._gt());
		set("<=", new Builtin._lt_eq());
		set(">=", new Builtin._gt_eq());
		set("not", new Builtin.not());
		set("read-string", new Builtin.read_string());
		set("type", new Builtin.type());
		set("eval", new Builtin.eval());
		set("list", new Builtin.list());
		set("apply", new Builtin.apply());
		set("fold", new Builtin.fold());
		set("map", new Builtin.map());
		set("filter", new Builtin.filter());
		set("pr", Builtin.pr1);
		set("prn", new Builtin.prn());
		set("print", Builtin.print1);
		set("println", new Builtin.println());
		set("read-line", new Builtin.read_line());
		set("slurp", new Builtin.slurp());
		set("spit", new Builtin.spit());
		set("str", new Builtin.str());
		set("symbol", new Builtin.symbol());
		set("macroexpand", new Builtin.macroexpand());
		set("read", new Builtin.read());
		set("load-string", new Builtin.load_string());
		set("nth", new Builtin.nth());
		set("instance?", new Builtin.instance_q());
		
		try {
			load_string(
					"(import java.lang)\n" +
					"(defmacro defn (name & body) `(def ~name (fn ~@body)))\n" +
					"(defmacro when (cond & body) `(if ~cond (do ~@body)))\n" +
					"(defn nil? (x) (= nil x))\n" +
					"(defmacro while (test & body) `(loop () (when ~test ~@body (recur))))\n" +
					"(def gensym\n"+
					"  (let (gs-counter 0)\n" +
					"    (fn ()\n" +
					"      (symbol (str \"G__\" (set! gs-counter (+ gs-counter 1)))))))\n" +
					"(defmacro dotimes (binding & body)\n" +
					"  (let (g (gensym), var (binding 0), limit (binding 1))\n" +
					"    `(let (~g ~limit) (loop (~var 0) (when (< ~var ~g) ~@body (recur (+ ~var 1)))))))\n" +
					"(defn load-file (file) (load-string (slurp file)))\n" +
					"(defn range (& args)\n  (let (size (. args size))\n    (if (== 0 size) (range 0 (/ 1 0) 1)\n      (if (== 1 size) (range 0 (args 0) 1)\n        (if (== 2 size) (range (args 0) (args 1) 1)\n          (let (start (args 0)\n                end (args 1)\n                step (args 2))\n            (reify java.lang.Iterable\n              (iterator (this)\n                (let (x start)\n                  (reify java.util.Iterator\n                    (hasNext (this)\n                      (< x end))\n                    (next (this)\n                      (let (cur x)\n                        (set! x (+ x step))\n                        cur)))))\n              (toString (this) (str \"(range \" start \" \" end \" \" step \")\")))))))))\n"
					);			
		} catch (Throwable e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
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
	static List<Object> listValue(Object value) {
		return (List<Object>) value;
	}

	@SuppressWarnings("unchecked")
	static Iterable<Object> iterableValue(Object value) {
		return (Iterable<Object>) value;
	}

	static Class<?> type(Object value) {
		if (value == null)
			return null;
		else
			return value.getClass();
	}

	static String toReadableString(Object value) {
		if (value == null) return "nil";
		else if (value instanceof String) return "\"" + escape((String) value) + "\"";
		else if (value instanceof Pattern) return "#\"" + escape(((Pattern) value).pattern()) + "\"";
		else if (value instanceof List) {
			String openParen = "(", closeParen = ")";
			if (value instanceof Vector) {
				openParen = "[";
				closeParen = "]";
			}
			StringBuilder sb = new StringBuilder();
			sb.append(openParen);
			boolean first = true;
			for (Object o : (List<?>) value) {
				if (!first) {
					sb.append(" ");
				}
				sb.append(toReadableString(o));
				first = false;
			}
			sb.append(closeParen);
			return sb.toString();
		}
		else if (value instanceof Character)
		{
			switch ((char)value)
			{
				case '\n':
					return "\\newline";
				case ' ':
					return "\\space";
				case '\t':
					return "\\tab";
				case '\f':
					return "\\formfeed";
				case '\b':
					return "\\backspace";
				case '\r':
					return "\\return";
				default:
					return "\\" + value.toString();
			}
		}

		else if (value instanceof Class<?>) return ((Class<?>) value).getName();
		else return value.toString();
	}

	static String escape(String value) {
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < value.length(); i++) {
			char c = value.charAt(i);
			switch (c) {
			case '\b': sb.append("\\b"); break;
			case '\f': sb.append("\\f"); break;
			case '\n': sb.append("\\n"); break;
			case '\r': sb.append("\\r"); break;
			case '\t': sb.append("\\t"); break;
			default: sb.append(c); break;
			}
		}
		return sb.toString();
	}

	static public String toString(Object value) {
		return value.toString();
	}

	static public Symbol symbolValue(Object value) {
		return (Symbol) value;
	}

	static Object apply(Object func, List<Object> args) throws Throwable {
		if (func instanceof Fn) {
			return ((Fn) func).invoke(args);
		} else {
			// implicit indexing
			if (func instanceof List<?>) {
				return ((List<?>) func).get(Core.intValue(args.get(0)));
			} else if (func.getClass().isArray()) {
				return Array.get(func, Core.intValue(args.get(0)));
			}
			else {
				System.err.println("Unknown function: [" + func.toString() + "]");
				return null;
			}
		}
	}

	static void printCollection(Collection<String> coll) {
		for (String key : new TreeSet<String>(coll)) {
			System.out.print(" " + key);
		}
		System.out.println();
	}

	public static void printLogo() {
		System.out.println("Javelin " + VERSION);
		System.out.println("Special forms:");
		ArrayList<String> fields = new ArrayList<String>();
		for (Field f : Core.class.getDeclaredFields()) {
			if (f.getType().equals(Symbol.class)) {
				try {
					fields.add(f.get(null).toString());
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
		printCollection(fields);

		ArrayList<String> functions = new ArrayList<String>();
		System.out.println("Defined symbols:");
		for (int x : globalEnv.env.keySet()) {
			functions.add(Symbol.symname.get(x));
		}
		printCollection(functions);

		System.out.println("Macros:");
		printCollection(macros.keySet());
	}

	static Object macroexpand(Object n) throws Throwable {
		if (n instanceof ArrayList) {
			List<Object> expr = Core.listValue(n);
			if (expr.size() == 0) return n;
			Object prefix = expr.get(0);
			String ps = prefix.toString();
			if (prefix instanceof Symbol && ps.equals("quote")) return n;
			if (prefix instanceof Symbol && ps.length() >= 3 && ps.contains("/")) { // e.g. (Math/cos 0)
				// (. Math cos 0)
				int sepPos = ps.indexOf('/');
				String head = ps.substring(0, sepPos);
				String tail = ps.substring(sepPos + 1);
				ArrayList<Object> newForm = new ArrayList<Object>();
				newForm.add(sym__dot);
				newForm.add(new Symbol(head));
				newForm.add(new Symbol(tail));
				for (int i = 1; i < expr.size(); i++) {
					newForm.add(expr.get(i));
				}
				return macroexpand(newForm);
			}
			if (prefix instanceof Symbol && ps.length() >= 2 && ps.startsWith(".")) { // e.g. (.length "abc")
				// (. "abc" length)		
				String tail = ps.substring(1);
				ArrayList<Object> newForm = new ArrayList<Object>();
				newForm.add(sym__dot);
				newForm.add(expr.get(1));
				newForm.add(new Symbol(tail));
				for (int i = 2; i < expr.size(); i++) {
					newForm.add(expr.get(i));
				}
				return macroexpand(newForm);
			}
			if (prefix instanceof Symbol && ps.length() >= 2 && ps.endsWith(".")) { // e.g. (Date.)
				// (new Date)
				String head = ps.substring(0, ps.length() - 1);
				ArrayList<Object> newForm = new ArrayList<Object>();
				newForm.add(sym_new);
				newForm.add(new Symbol(head));								
				for (int i = 1; i < expr.size(); i++) {
					newForm.add(expr.get(i));
				}
				return macroexpand(newForm);
			}			
			if (prefix instanceof Symbol && macros.containsKey(ps)) {
				UserFn func = macros.get(ps);

				// build arguments
				ArrayList<Object> args = new ArrayList<Object>();
				int len = expr.size();
				for (int i = 1; i < len; i++) {
					args.add(expr.get(i));
				}
				Object r = apply(func, args);
				return macroexpand(r); // macroexpand again
			} else {
				// macroexpand elements
				ArrayList<Object> r = new ArrayList<Object>();
				for (Object n2 : expr) {
					r.add(macroexpand(n2));
				}
				return r;
			}
		} else if (n instanceof Vector) {
			// macroexpand elements
			List<Object> expr = Core.listValue(n);
			Vector<Object> r = new Vector<Object>();
			for (Object n2 : expr) {
				r.add(macroexpand(n2));
			}
			return r;
		} else if (n instanceof Symbol) {
			String ns = n.toString();
			if (ns.length() >= 3 && ns.contains("/")) { // e.g. Math/PI
				// (. Math -PI)
				int sepPos = ns.indexOf('/');
				String head = ns.substring(0, sepPos);
				String tail = ns.substring(sepPos + 1);
				ArrayList<Object> newForm = new ArrayList<Object>();
				newForm.add(sym__dot);
				newForm.add(new Symbol(head));
				newForm.add(new Symbol("-" + tail));
				return macroexpand(newForm);
			}
		}
		// no expansion
		return n;
	}

	static Object eval(Object n, Environment env) throws Throwable {
		if (n instanceof Symbol) {
			Object r = env.get(((Symbol) n).code);
			return r;
		} else if (n instanceof Vector) {
			ArrayList<Object> r = new ArrayList<Object>();
			for (Object x : (Vector<?>) n) {
				r.add(eval(x, env));
			}
			return r;
		} else if (n instanceof ArrayList) { // function (FUNCTION
													// ARGUMENT ...)
			List<Object> expr = Core.listValue(n);
			if (expr.size() == 0)
				return null;
			Object e0 = expr.get(0);
			if (e0 instanceof Symbol) {
				final int code = ((Symbol) e0).code;
				if (code == sym_set_e.code) { // (set! SYMBOL-OR-FIELD VALUE) ; set the SYMBOL-OR-FIELD's value
					Object dest = expr.get(1);
					if (dest instanceof Symbol) {
						Object value = eval(expr.get(2), env);
						env.set(((Symbol) dest).code, value);
						return value;
					} else { // field
						// Java interoperability
						// (set! (. CLASS-OR-OBJECT -FIELD) VALUE) ; set Java field
						try {
							@SuppressWarnings("unchecked")
							List<Object> dl = (ArrayList<Object>) dest;
							// get class
							Class<?> cls = tryGetClass(dl.get(1).toString());
							Object obj = null;
							if (cls != null) {
								// class's static method e.g. (. java.lang.Math floor 1.5)
							} else {
								// object's method e.g. (. "abc" length)
								obj = eval(dl.get(1), env);
								cls = obj.getClass();
							}

							String fieldName = dl.get(2).toString().substring(1);
							java.lang.reflect.Field field = cls.getField(fieldName);
							Object value = eval(expr.get(2), env);
							field.set(obj, value);
							return value;
						} catch (Exception e) {
							e.printStackTrace();
							return null;
						}
					}
				}
				else if (code == sym_def.code) { // (def SYMBOL VALUE ...) ; set in the global
							// environment
					Object ret = null;
					int len = expr.size();
					for (int i = 1; i < len; i += 2) {
						Object value = eval(expr.get(i + 1), env);
						ret = globalEnv.def(((Symbol) expr.get(i)).code, value);
					}
					return ret;
				}
				else if (code == sym_and.code) { // (and X ...) short-circuit
					for (int i = 1; i < expr.size(); i++) {
						if (!Core.booleanValue(eval(expr.get(i), env))) {
							return false;
						}
					}
					return true;
				}
				else if (code == sym_or.code) { // (or X ...) short-circuit
					for (int i = 1; i < expr.size(); i++) {
						if (Core.booleanValue(eval(expr.get(i), env))) {
							return true;
						}
					}
					return false;
				}
				else if (code == sym_if.code) { // (if CONDITION THEN_EXPR [ELSE_EXPR])
					Object cond = expr.get(1);
					if (Core.booleanValue(eval(cond, env))) {
						return eval(expr.get(2), env); // Tail call optimization is not faster. I tried it.
					} else {
						if (expr.size() <= 3)
							return null;
						return eval(expr.get(3), env);
					}
				}
				else if (code == sym_quote.code) { // (quote X)
					return expr.get(1);
				}
				else if (code == sym_fn.code) {
					// anonymous function. lexical scoping
					// (fn (ARGUMENT ...) BODY ...)
					ArrayList<Object> r = new ArrayList<Object>();
					for (int i = 1; i < expr.size(); i++) {
						r.add(expr.get(i));
					}
					return new UserFn(r, env);
				}
				else if (code == sym_do.code) { // (do X ...)
					int last = expr.size() - 1;
					if (last <= 0)
						return null;
					for (int i = 1; i < last; i++) {
						eval(expr.get(i), env);
					}
					return eval(expr.get(last), env);
				}
				else if (code == sym__dot.code) {
					String methodName = expr.get(2).toString();
					if (methodName.startsWith("-")) {
						// Java interoperability
						// (. CLASS-OR-OBJECT -FIELD) ; get Java field
						try {
							// get class
							Class<?> cls = tryGetClass(expr.get(1).toString());
							Object obj = null;
							if (cls != null) {
								// class's static method e.g. (. java.lang.Math floor 1.5)
							} else {
								// object's method e.g. (. "abc" length)
								obj = eval(expr.get(1), env);
								cls = obj.getClass();
							}

							String fieldName = methodName.substring(1);
							java.lang.reflect.Field field = cls.getField(fieldName);
							return field.get(obj);
						} catch (Exception e) {
							e.printStackTrace();
							return null;
						}
					}
					// Java interoperability
					// (. CLASS-OR-OBJECT METHOD ARGUMENT ...) ; Java method invocation
					try {
						// get class
						Class<?> cls = tryGetClass(expr.get(1).toString());
						Object obj = null;
						if (cls != null) {
							// class's static method e.g. (. java.lang.Math floor 1.5)
						} else {
							// object's method e.g. (. "abc" length)
							obj = eval(expr.get(1), env);
							cls = obj.getClass();
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
				else if (code == sym_new.code) {
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
				else if (code == sym_doseq.code) // (doseq (VAR SEQ) EXPR ...)
				{
					Environment env2 = new Environment(env);
					int varCode = Core.symbolValue(Core.listValue(expr.get(1)).get(0)).code;
					@SuppressWarnings("rawtypes")
					Iterable seq = (Iterable) eval(Core.listValue(expr.get(1)).get(1), env);
					int len = expr.size();
					for (Object x : seq) {
						env2.def(varCode, (Object) x);
						for (int i = 2; i < len; i++) {
							eval(expr.get(i), env2);
						}
					}
					return null;
				}
				else if (code == sym_let.code) // (let (VAR VALUE ...) BODY ...)
				{
					Environment env2 = new Environment(env);
					List<Object> bindings = Core.listValue(expr.get(1));
					for (int i = 0; i < bindings.size(); i+= 2) {
						env2.def(Core.symbolValue(bindings.get(i)).code, eval(bindings.get(i + 1), env2));
					}
					Object ret = null;
					for (int i = 2; i < expr.size(); i++) {
						ret = eval(expr.get(i), env2);
					}
					return ret;
				}
				else if (code == sym_import.code) // (import & import-symbols-or-lists-or-prefixes)
				{
					Class<?> lastImport = null;
					for (int i = 1; i < expr.size(); i++) {
						Object x = expr.get(i);
						if (x instanceof Symbol) { // e.g. java.util.Date
							String s = x.toString();
							try {								
								lastImport = getClass(s);
								getClassCache.put(lastImport.getSimpleName(), lastImport);
							} catch (ClassNotFoundException cnfe) {
								if (!imports.contains(s)) imports.add(s);
							}
						} else if (x instanceof List) { // e.g. (java.util Date ArrayList)
							List<Object> xl = listValue(x);
							String prefix = xl.get(0).toString();
							for (int j = 1; j < xl.size(); j++) {
								String s = xl.get(j).toString();
								lastImport = getClass(prefix + "." + s);
								getClassCache.put(s, lastImport);
							}
						} else {
							throw new RuntimeException("Syntax error");
						}
					}
					return lastImport;
				}
				else if (code == sym_reify.code) // (reify INTERFACE (METHOD (ARGS ...) BODY ...) ...)
				{
// Note that the first parameter must be supplied to
// correspond to the target object ('this' in Java parlance). Thus
// methods for interfaces will take one more argument than do the
// interface declarations.
//
// Example:
//   (import javax.swing java.awt java.awt.event)
//   
//   (def frame (new JFrame))
//   (def button (new Button "Hello"))
//   (. button addActionListener
//   	(reify java.awt.event.ActionListener
//   	  (actionPerformed (this e)
//   		(. javax.swing.JOptionPane showMessageDialog nil (str "Hello, World!\nthis=" this "\ne=" e)))))
//   (. frame setDefaultCloseOperation (. JFrame -EXIT_ON_CLOSE))
//   (. frame add button (. BorderLayout -NORTH))
//   (. frame pack)
//   (. frame setVisible true)

					Class<?> cls = getClass(expr.get(1).toString());
					ClassLoader cl = cls.getClassLoader();
					HashMap<String, UserFn> methods = new HashMap<String, UserFn>();
					for (int i = 2; i < expr.size(); i++) {
						@SuppressWarnings("unchecked")
						ArrayList<Object> methodDef = (ArrayList<Object>) expr.get(i);
						methods.put(methodDef.get(0).toString(), new UserFn(new ArrayList<Object>(methodDef.subList(1, methodDef.size())), env));
					}
					class MyHandler implements InvocationHandler {
						HashMap<String, UserFn> methods;

						public MyHandler(HashMap<String, UserFn> methods, Environment env) {
							this.methods = methods;
						}

						@Override
						public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
							ArrayList<Object> args2 = new ArrayList<Object>();
							args2.add(this);
							if (args != null) args2.addAll(Arrays.asList(args));
							return apply(methods.get(method.getName()), args2);
						}
					}
					InvocationHandler handler = new MyHandler(methods, env);
					Object ret = Proxy.newProxyInstance(cl, new Class[] {cls}, handler);
					return ret;
				}
				else if (code == sym_recur.code) // (recur ARG ...)
				{
					ArrayList<Object> args = new ArrayList<Object>();
					for (int i = 1; i < expr.size(); i++) {
						args.add(eval(expr.get(i), env));
					}
					throw new Recur(args);
				}
				else if (code == sym_loop.code) // (loop (VAR VALUE ...) BODY ...)
				{
					// separate formal and actual parameters
					List<Object> bindings = Core.listValue(expr.get(1));
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
							} catch (Recur e) {
								actualParams = e.args;
								continue loopStart; // recur this loop (effectively goto)
							}
						}
						return ret;
					}
				}
				else if (code == sym_quasiquote.code) // (quasiquote S-EXPRESSION)
				{
					return quasiquote(expr.get(1), env);
				}
				else if (code == sym_try.code) // (try EXPR ... (catch CLASS VAR EXPR ...) ... (finally EXPR ...))
				{
					int i = 1, len = expr.size();
					Object ret = null;
					try {
						for (; i < len; i++) {
							Object e = expr.get(i);
							if (e instanceof ArrayList) {
								Object prefix = ((ArrayList<?>) e).get(0);
								if (prefix.equals(sym_catch) || prefix.equals(sym_finally)) break;
							}
							ret = eval(e, env);
						}
					} catch (Throwable t) {
						for (; i < len; i++) {
							Object e = expr.get(i);
							if (e instanceof ArrayList) {
								ArrayList<?> exprs = (ArrayList<?>) e;
								if (exprs.get(0).equals(sym_catch) && getClass(exprs.get(1).toString()).isInstance(t)) {
									Environment env2 = new Environment(env);
									env2.def(Symbol.toCode(exprs.get(2).toString()), t);
									for (int j = 3; j < exprs.size(); j++) {
										ret = eval(exprs.get(j), env2);
									}
									return ret;
								}
							}
						}
						throw t;
					} finally {
						for (; i < len; i++) {
							Object e = expr.get(i);
							if (e instanceof ArrayList && ((ArrayList<?>) e).get(0).equals(sym_finally)) {
								ArrayList<?> exprs = (ArrayList<?>) e;
								for (int j = 1; j < exprs.size(); j++) {
									eval(exprs.get(j), env);
								}
							}
						}
					}
					return ret;
				}
				else if (code == sym_defmacro.code) // (defmacro add (a & more) `(+ ~a ~@more)) ; define macro
				{
					macros.put(expr.get(1).toString(), new UserFn(new ArrayList<Object>(expr.subList(2, expr.size())), globalEnv));
					return null;
				}
			}
			// evaluate arguments
			Object func = eval(expr.get(0), env);
			ArrayList<Object> args = new ArrayList<Object>();
			int len = expr.size();
			for (int i = 1; i < len; i++) {
				args.add(eval(expr.get(i), env));
			}
			return apply(func, args);
		} else {
			// return n.clone();
			return n;
		}
	}

	private static Object quasiquote(Object arg, Environment env) throws Throwable {
		if (arg instanceof ArrayList && ((ArrayList<?>) arg).size() > 0) {
			ArrayList<?> arg2 = (ArrayList<?>) arg;
			Object head = arg2.get(0);
			if (head instanceof Symbol && ((Symbol) head).code == sym_unquote.code) {
				return macroexpandEval(arg2.get(1), env);
			}
			ArrayList<Object> ret = new ArrayList<>();
			for (Object a : arg2) {
				if (a instanceof ArrayList) {
					ArrayList<?> a2 = (ArrayList<?>) a;
					if (a2.size() > 0) {
						Object head2 = a2.get(0);
						if (head2 instanceof Symbol && ((Symbol) head2).code == sym_unquote_splicing.code) {
							ret.addAll(Core.listValue(macroexpandEval(a2.get(1), env)));
							continue;
						}
					}
				}
				ret.add(quasiquote(a, env));
			}
			return ret;
		} else {
			return arg;
		}
	}

	// cached
	static Class<?> getClass(String className) throws ClassNotFoundException {
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
	
	static Class<?> tryGetClass(String className) {
		try {
			return getClass(className);
		} catch (ClassNotFoundException e) {
			return null;
		}
	}

	public static Object load_string(String s) throws Throwable {
		s = "(" + s + "\n)";
		Object result = null;
		for (Object o : Core.listValue(parse(new StringReader(s))))  {
			result = macroexpandEval(o, globalEnv);
		}
		return result;
	}

	static void prompt() {
		System.out.print("> ");
	}

	// read-eval-print loop
	public static void repl() {
		while (true) {
			try {
				prompt();
				Object expr;
				try {
					expr = parse(defaultReader);
				} catch (EOFException e) {
					break;
				}
				System.out.println(toReadableString(macroexpandEval(expr, globalEnv)));
			} catch (Throwable e) {
				e.printStackTrace();
			}
		}
	}

	// extracts characters from URL or filename
	public static String slurp(String urlOrFileName, String charsetName) throws IOException {
		InputStream is = null;
		try {
			// try URL
			URL url = new URL(urlOrFileName);
			is = url.openStream();
		} catch (MalformedURLException e) {
			is = new FileInputStream(urlOrFileName);
		}
		BufferedReader br = new BufferedReader(new InputStreamReader(is, Charset.forName(charsetName)));
		StringBuilder sb = new StringBuilder();
		while (true) {
			int c = br.read();
			if (c < 0) break;
			sb.append((char) c);
		}
		br.close();
		return sb.toString();
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
	public static Object get(String s) throws Exception {
		return globalEnv.get(Symbol.toCode(s));
	}

	// for embedding
	public static Object set(String s, Object o) {
		return globalEnv.def(Symbol.toCode(s), o);
	}

	public static void main(String[] args) throws Throwable {
		if (args.length == 0) {
			List<String> argsList = new ArrayList<String>();
			set("*command-line-args*", argsList);
			printLogo();
			repl();
			System.out.println();
			return;
		} else if (args.length >= 1) {
			if (args[0].equals("-h")) {
				System.out.println("Usage: java javelin.Core [OPTION] [ARGS...]");
				System.out.println();
				System.out.println("Options:");
				System.out.println("    FILE  run a script.");
				System.out.println("    -h    print this screen.");
				System.out.println("    -r    run a REPL.");
				System.out.println("    -v    print version.");
				System.out.println("Operation:");
				System.out.println("    Binds *command-line-args* to a list of strings containing command line args that appear after FILE.");
				return;
			} else if (args[0].equals("-r")) {
				List<String> argsList = new ArrayList<String>();
				for (int i = 1; i < args.length; i++) argsList.add(args[i]);
				set("*command-line-args*", argsList);
				printLogo();
				repl();
				System.out.println();
				return;
			} else if (args[0].equals("-v")) {
				System.out.println(Core.VERSION);
				return;
			}
		}

		// execute the file
		List<String> argsList = new ArrayList<String>();
		for (int i = 1; i < args.length; i++) argsList.add(args[i]);
		set("*command-line-args*", argsList);
		try {			
			load_file(args[0]);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static Object load_file(String file) throws IOException, Throwable {
		return load_string(Core.slurp(file, "UTF-8"));
	}

	public static Object macroexpandEval(Object object, Environment env) throws Throwable {
		return eval(macroexpand(object), env);
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
		final String delim = "()[] \t\r\n,;\"";
		final String prefix = "()[]'`\"#";

		while (true) {			
			char c, p;

			// skip whitespaces
			while (true) {
				if (eof(r)) throw new EOFException("EOF while reading");
				p = peek(r);
				if (ws.indexOf(p) < 0) break;
				r.read();
			}

			p = peek(r);
			if (prefix.indexOf(p) >= 0) { // prefix
				StringBuilder acc = new StringBuilder(); // accumulator
				if (p == '#') {
					r.read();
					acc.append(p);
					p = peek(r);
				}
				if (p == '"') { // string
					r.read();
					acc.append(p);					
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
							else if (next == 'b')
								next = '\b';
							else if (next == 'f')
								next = '\f';
							else if (next == '\\')
								next = '\\';							
							else
								acc.append('\\'); // Unsupported escape character: do not escape
							acc.append(next);
						} else {
							acc.append(c);
						}
					}
					return acc.toString();
				} else {				
					c = (char) r.read();
					return "" + c;
				}
			} else if (p == '~') { // unquote
				StringBuilder acc = new StringBuilder(); // accumulator
				c = (char) r.read();
				acc.append(c);
				if (peek(r) == '@') { // unquote-splicing
					c = (char) r.read();
					acc.append(c);
				}
				return acc.toString();
			} else if (p == ';') { // end-of-line comment
				while (!eof(r) && peek(r) != '\n') {
					r.read();
				}
				continue;
			} else { // other
				StringBuilder acc = new StringBuilder(); // accumulator
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
		} else if (tok.charAt(0) == '#' && tok.charAt(1) == '"') { // regex
			return Pattern.compile(tok.substring(2));
		} else if (tok.equals("'")) { // quote
			ArrayList<Object> ret = new ArrayList<Object>();
			ret.add(sym_quote);
			ret.add(parse(r));
			return ret;
		} else if (tok.equals("`")) { // quasiquote
			ArrayList<Object> ret = new ArrayList<Object>();
			ret.add(sym_quasiquote);
			ret.add(parse(r));
			return ret;
		} else if (tok.equals("~")) { // unquote
			ArrayList<Object> ret = new ArrayList<Object>();
			ret.add(sym_unquote);
			ret.add(parse(r));
			return ret;
		} else if (tok.equals("~@")) { // unquote-splicing
			ArrayList<Object> ret = new ArrayList<Object>();
			ret.add(sym_unquote_splicing);
			ret.add(parse(r));
			return ret;
		} else if (tok.equals("(")) { // list
			return parseList(r);
		} else if (tok.equals("[")) {
			return parseVector(r);
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
				try {
					return Integer.parseInt(tok);
				} catch (NumberFormatException nfe) {
					return Long.parseLong(tok); // parse big number to long
				}
			}
		} else { // symbol
			// other literals
			switch (tok) {
			case "true": return true;
			case "false": return false;
			case "nil": return null;
			}
			if (tok.startsWith(":")) { // keyword
				return new Keyword(tok);
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
	
	private static Vector<Object> parseVector(Reader r) throws IOException {
		Vector<Object> ret = new Vector<Object>();
		while (!eof(r)) {
			String tok = readToken(r);
			if (tok.equals("]")) { // end of list
				break;
			} else {
				Object o = parse(r, tok);
				ret.add(o);
			}
		}
		return ret;
	}	
}
