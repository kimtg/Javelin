package javelin;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

class Builtin {
	public static Object coerceNumberType(List<Object> args) {
		Object r = Integer.TYPE;
		for (Object x : args) {
			if (x instanceof Double) {
				return Double.TYPE;
			} else if (x instanceof Long) {
				r = Long.TYPE;
			}
		}
		return r;
	}

	static class _plus extends Fn {
		public Object invoke(ArrayList<Object> args) throws Throwable {
			int len = args.size();
			if (len <= 0)
				return 0;
			Object type = coerceNumberType(args);
			Object first = args.get(0);
			if (type == Integer.TYPE) {
				int acc = Core.intValue(first);
				for (int i = 1; i < len; i++) {
					acc += Core.intValue(args.get(i));
				}
				return acc;
			} else if (type == Long.TYPE) {
				long acc = Core.longValue(first);
				for (int i = 1; i < len; i++) {
					acc += Core.longValue(args.get(i));
				}
				return acc;
			} else {
				double acc = Core.doubleValue(first);
				for (int i = 1; i < len; i++) {
					acc += Core.doubleValue(args.get(i));
				}
				return acc;
			}
		}
	}

	static class _minus extends Fn {
		public Object invoke(ArrayList<Object> args) throws Throwable {
			int len = args.size();
			if (len <= 0)
				return 0;
			Object type = coerceNumberType(args);
			Object first = args.get(0);
			if (type == Integer.TYPE) {
				int acc = Core.intValue(first);
				if (len == 1) return -acc;
				for (int i = 1; i < len; i++) {
					acc -= Core.intValue(args.get(i));
				}
				return acc;
			} else if (type == Long.TYPE) {
				long acc = Core.longValue(first);
				if (len == 1) return -acc;
				for (int i = 1; i < len; i++) {
					acc -= Core.longValue(args.get(i));
				}
				return acc;
			} else {
				double acc = Core.doubleValue(first);
				if (len == 1) return -acc;
				for (int i = 1; i < len; i++) {
					acc -= Core.doubleValue(args.get(i));
				}
				return acc;
			}
		}
	}

	static class _star extends Fn {
		public Object invoke(ArrayList<Object> args) throws Throwable {
			int len = args.size();
			if (len <= 0)
				return 1;
			Object type = coerceNumberType(args);
			Object first = args.get(0);
			if (type == Integer.TYPE) {
				int acc = Core.intValue(first);
				for (int i = 1; i < len; i++) {
					acc *= Core.intValue(args.get(i));
				}
				return acc;
			} else if (type == Long.TYPE) {
				long acc = Core.longValue(first);
				for (int i = 1; i < len; i++) {
					acc *= Core.longValue(args.get(i));
				}
				return acc;
			} else {
				double acc = Core.doubleValue(first);
				for (int i = 1; i < len; i++) {
					acc *= Core.doubleValue(args.get(i));
				}
				return acc;
			}
		}
	}

	// always use doubleValue
	static class _slash extends Fn {
		public Object invoke(ArrayList<Object> args) throws Throwable {
			int len = args.size();
			if (len <= 0)
				return 1;
			Object first = args.get(0);
			double acc = Core.doubleValue(first);
			if (len == 1) return 1 / acc;
			for (int i = 1; i < len; i++) {
				acc /= Core.doubleValue(args.get(i));
			}
			return acc;
		}
	}

	// quotient
	static class quot extends Fn {
		public Object invoke(ArrayList<Object> args) throws Throwable {
			int len = args.size();
			if (len <= 0)
				return 1;
			Object first = args.get(0);
			long acc = Core.longValue(first);
			if (len == 1) return 1 / acc;
			for (int i = 1; i < len; i++) {
				acc /= Core.longValue(args.get(i));
			}
			return acc;
		}
	}

	static class mod extends Fn {
		public Object invoke(ArrayList<Object> args) throws Throwable {
			Object first = args.get(0);
			Object second = args.get(1);
			Object type = coerceNumberType(args);
			if (type == Integer.TYPE) {
				return Core.intValue(first) % Core.intValue(second);
			} else if (type == Long.TYPE) {
				return Core.longValue(first) % Core.longValue(second);
			} else {
				return Core.doubleValue(first) % Core.doubleValue(second);
			}
		}
	}

	static class _eq extends Fn {
		public Object invoke(ArrayList<Object> args) throws Throwable {
			Object v1 = args.get(0);
			if (v1 == null) {
				for (int i = 1; i < args.size(); i++) {
					Object v2 = args.get(i);
					if (v2 != null) return false;
				}
			}
			else {
				for (int i = 1; i < args.size(); i++) {
					Object v2 = args.get(i);
					if (!v1.equals(v2)) return false;
				}
			}
			return true;
		}
	}

	static class _eq_eq extends Fn {
		public Object invoke(ArrayList<Object> args) throws Throwable {
			Object first = args.get(0);
			double firstv = Core.doubleValue(first);
			for (int i = 1; i < args.size(); i++) {
				if (Core.doubleValue(args.get(i)) != firstv) {
					return false;
				}
			}
			return true;
		}
	}

	static class Not_eq extends Fn {
		public Object invoke(ArrayList<Object> args) throws Throwable {
			Object v1 = args.get(0);
			if (v1 == null) {
				for (int i = 1; i < args.size(); i++) {
					Object v2 = args.get(i);
					if (v2 != null) return true;
				}
			}
			else {
				for (int i = 1; i < args.size(); i++) {
					Object v2 = args.get(i);
					if (!v1.equals(v2)) return true;
				}
			}
			return false;
		}
	}

	static class _lt extends Fn {
		public Object invoke(ArrayList<Object> args) throws Throwable {
			for (int i = 0; i < args.size() - 1; i++) {
				Object first = args.get(i);
				Object second = args.get(i + 1);
				if (!(Core.doubleValue(first) < Core.doubleValue(second))) return false;
			}
			return true;
		}
	}

	static class _gt extends Fn {
		public Object invoke(ArrayList<Object> args) throws Throwable {
			for (int i = 0; i < args.size() - 1; i++) {
				Object first = args.get(i);
				Object second = args.get(i + 1);
				if (!(Core.doubleValue(first) > Core.doubleValue(second))) return false;
			}
			return true;
		}
	}

	static class _lt_eq extends Fn {
		public Object invoke(ArrayList<Object> args) throws Throwable {
			for (int i = 0; i < args.size() - 1; i++) {
				Object first = args.get(i);
				Object second = args.get(i + 1);
				if (!(Core.doubleValue(first) <= Core.doubleValue(second))) return false;
			}
			return true;
		}
	}

	static class _gt_eq extends Fn {
		public Object invoke(ArrayList<Object> args) throws Throwable {
			for (int i = 0; i < args.size() - 1; i++) {
				Object first = args.get(i);
				Object second = args.get(i + 1);
				if (!(Core.doubleValue(first) >= Core.doubleValue(second))) return false;
			}
			return true;
		}
	}

	static class not extends Fn {
		public Object invoke(ArrayList<Object> args) throws Throwable {
			return !Core.booleanValue(args.get(0));
		}
	}

	static class read_string extends Fn {
		public Object invoke(ArrayList<Object> args) throws Throwable {
			return Core.parse(new StringReader((String) args.get(0)));
		}
	}

	static class type extends Fn {
		public Object invoke(ArrayList<Object> args) throws Throwable {
			return Core.type(args.get(0));
		}
	}

	static class eval extends Fn {
		public Object invoke(ArrayList<Object> args) throws Throwable {
			return Core.macroexpandEval(args.get(0), Core.globalEnv);
		}
	}

	static class fold extends Fn {
		public Object invoke(ArrayList<Object> args) throws Throwable {
			Object f = args.get(0);
			Iterable<Object> iterable = Core.iterableValue(args.get(1));
			Iterator<Object> iter = iterable.iterator();
			Object acc = iter.next();
			ArrayList<Object> args2 = new ArrayList<Object>(); // (ITEM1 ITEM2)
			args2.add(null); // first argument
			args2.add(null); // second argument
			while (iter.hasNext()) {
				args2.set(0, acc);
				args2.set(1, iter.next());
				acc = Core.apply(f, args2);
			}
			return acc;
		}
	}

	static class map extends Fn {
		public Object invoke(ArrayList<Object> args) throws Throwable {
			Object f = args.get(0);
			Iterable<Object> iterable = Core.iterableValue(args.get(1));
			ArrayList<Object> acc = new ArrayList<Object>();
			ArrayList<Object> args2 = new ArrayList<Object>();
			args2.add(null);
			for (Object x : iterable) {
				args2.set(0, x);
				acc.add(Core.apply(f, args2));
			}
			return acc;
		}
	}

	static class apply extends Fn {
		public Object invoke(ArrayList<Object> args) throws Throwable {
			Object args2 = args.get(1);
			ArrayList<Object> argsList;
			if (args2 instanceof ArrayList<?>) {
				argsList = Core.arrayListValue(args2);
			} else {
				argsList = new ArrayList<Object>();
				for (Object x : Core.iterableValue(args2)) {
					argsList.add(x);
				}
			}
			return Core.apply(args.get(0), argsList);
		}
	}

	static class filter extends Fn {
		public Object invoke(ArrayList<Object> args) throws Throwable {
			Object f = args.get(0);
			Iterable<Object> iterable = Core.iterableValue(args.get(1));
			ArrayList<Object> acc = new ArrayList<Object>();
			ArrayList<Object> args2 = new ArrayList<Object>();
			args2.add(null);
			for (Object x : iterable) {
				args2.set(0, x);
				Object ret = Core.apply(f, args2);
				if (Core.booleanValue(ret)) acc.add(x);
			}
			return acc;
		}
	}

	static class pr extends Fn {
		public Object invoke(ArrayList<Object> args) throws Throwable {
			for (int i = 0; i < args.size(); i++) {
				if (i != 0) System.out.print(" ");
				System.out.print(Core.toReadableString(args.get(i)));
			}
			return null;
		}
	}

	static final pr pr1 = new pr();

	static class prn extends Fn {
		public Object invoke(ArrayList<Object> args) throws Throwable {
			pr1.invoke(args);
			System.out.println();
			return null;
		}
	}

	static class print extends Fn {
		public Object invoke(ArrayList<Object> args) throws Throwable {
			for (int i = 0; i < args.size(); i++) {
				if (i != 0) System.out.print(" ");
				System.out.print(args.get(i));
			}
			return null;
		}
	}

	static final print print1 = new print();

	static class println extends Fn {
		public Object invoke(ArrayList<Object> args) throws Throwable {
			print1.invoke(args);
			System.out.println();
			return null;
		}
	}

	static class read_line extends Fn {
		public Object invoke(ArrayList<Object> args) throws Throwable {
			BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
			try {
				return br.readLine();
			} catch (IOException e) {
				return null;
			}
		}
	}

	// (slurp filename [encoding]) default encoding: UTF-8
	static class slurp extends Fn {
		public Object invoke(ArrayList<Object> args) throws Throwable {
			String filename = Core.toString(args.get(0));
			String charset = args.size() >= 2 ? args.get(1).toString() : "UTF-8";
			return Core.slurp(filename, charset);
		}
	}

	static class spit extends Fn {
		public Object invoke(ArrayList<Object> args) throws Throwable {
			String filename = Core.toString(args.get(0));
			String str = Core.toString(args.get(1));
			return Core.spit(filename, str);
		}
	}

	static class list extends Fn {
		public Object invoke(ArrayList<Object> args) throws Throwable {
			return args;
		}
	}

	static class str extends Fn {
		public Object invoke(ArrayList<Object> args) throws Throwable {
			StringBuilder sb = new StringBuilder();
			for (Object x : args) {
				if (x != null) sb.append(x.toString());
			}
			return sb.toString();
		}
	}

	static class symbol extends Fn {
		public Object invoke(ArrayList<Object> args) throws Throwable {
			return new javelin.Symbol(Core.toString(args.get(0)));
		}
	}

	// (macroexpand X)
	static class macroexpand extends Fn {
		public Object invoke(ArrayList<Object> args) throws Throwable {
			return Core.macroexpand(args.get(0));
		}
	}

	// (read [Reader])
	static class read extends Fn {
		public Object invoke(ArrayList<Object> args) throws Throwable {
			switch (args.size()) {
			case 0: return Core.parse(Core.defaultReader);
			case 1: return Core.parse((Reader) args.get(1));
			default: throw new IllegalArgumentException();
			}
		}
	}
}
