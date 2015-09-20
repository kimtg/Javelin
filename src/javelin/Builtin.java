package javelin;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;

import javelin.Core.node;

class Builtin {
	static class _plus implements IFn {
		public node invoke(ArrayList<node> args, Environment env) throws Exception {
			int len = args.size();
			if (len <= 0)
				return Core.node_0;
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
		public node invoke(ArrayList<node> args, Environment env) throws Exception {
			int len = args.size();
			if (len <= 0)
				return Core.node_0;
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
		public node invoke(ArrayList<node> args, Environment env) throws Exception {
			int len = args.size();
			if (len <= 0)
				return Core.node_1;
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
		public node invoke(ArrayList<node> args, Environment env) throws Exception {
			int len = args.size();
			if (len <= 0)
				return Core.node_1;
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
		public node invoke(ArrayList<node> args, Environment env) throws Exception {
			return new node(args.get(0).intValue() % args.get(1).intValue());
		}
	}

	static class _eq implements IFn {
		public node invoke(ArrayList<node> args, Environment env) throws Exception {
			Object v1 = args.get(0).value;
			if (v1 == null) {
				for (int i = 1; i < args.size(); i++) {
					Object v2 = args.get(i).value;
					if (v2 != null) return Core.node_false;
				}
			}
			else {
				for (int i = 1; i < args.size(); i++) {
					Object v2 = args.get(i).value;
					if (!v1.equals(v2)) return Core.node_false;
				}
			}
			return Core.node_true;
		}
	}

	static class _eq_eq implements IFn {
		public node invoke(ArrayList<node> args, Environment env) throws Exception {
			node first = args.get(0);
			if (first.value instanceof Integer) {
				int firstv = first.intValue();
				for (int i = 1; i < args.size(); i++) {
					if (args.get(i).intValue() != firstv) {
						return Core.node_false;
					}
				}
			} else if (first.value instanceof Long) {
				long firstv = first.longValue();
				for (int i = 1; i < args.size(); i++) {
					if (args.get(i).longValue() != firstv) {
						return Core.node_false;
					}
				}
			} else {
				double firstv = first.doubleValue();
				for (int i = 1; i < args.size(); i++) {
					if (args.get(i).doubleValue() != firstv) {
						return Core.node_false;
					}
				}
			}
			return Core.node_true;
		}
	}

	static class Not_eq implements IFn {
		public node invoke(ArrayList<node> args, Environment env) throws Exception {
			Object v1 = args.get(0).value;
			if (v1 == null) {
				for (int i = 1; i < args.size(); i++) {
					Object v2 = args.get(i).value;
					if (v2 != null) return Core.node_true;
				}
			}
			else {
				for (int i = 1; i < args.size(); i++) {
					Object v2 = args.get(i).value;
					if (!v1.equals(v2)) return Core.node_true;
				}
			}
			return Core.node_false;
		}
	}

	static class _lt implements IFn {
		public node invoke(ArrayList<node> args, Environment env) throws Exception {
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
		public node invoke(ArrayList<node> args, Environment env) throws Exception {
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
		public node invoke(ArrayList<node> args, Environment env) throws Exception {
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
		public node invoke(ArrayList<node> args, Environment env) throws Exception {
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
		public node invoke(ArrayList<node> args, Environment env) throws Exception {
			return new node(!args.get(0).booleanValue());
		}
	}

	static class read_string implements IFn {
		public node invoke(ArrayList<node> args, Environment env) throws Exception {
			return new node(Core.parse(args.get(0).stringValue()).value);
		}
	}

	static class type implements IFn {
		public node invoke(ArrayList<node> args, Environment env) throws Exception {
			return new node(args.get(0).type());
		}
	}

	static class eval implements IFn {
		public node invoke(ArrayList<node> args, Environment env) throws Exception {
			return new node(Core.eval(args.get(0), env));
		}
	}

	static class fold implements IFn {
		public node invoke(ArrayList<node> args, Environment env) throws Exception {
			node f = args.get(0);
			ArrayList<node> lst = args.get(1).arrayListValue();
			node acc = lst.get(0);
			ArrayList<node> args2 = new ArrayList<node>(); // (ITEM1 ITEM2)
			args2.add(null); // first argument
			args2.add(null); // second argument
			for (int i = 1; i < lst.size(); i++) {
				args2.set(0, acc);
				args2.set(1, lst.get(i));
				acc = Core.apply(f, args2, env);
			}
			return acc;
		}
	}

	static class map implements IFn {
		public node invoke(ArrayList<node> args, Environment env) throws Exception {
			node f = args.get(0);
			ArrayList<node> lst = args.get(1).arrayListValue();
			ArrayList<node> acc = new ArrayList<node>();
			for (int i = 0; i < lst.size(); i++) {
				ArrayList<node> args2 = new ArrayList<node>();
				args2.add(lst.get(i));
				acc.add(Core.apply(f, args2, env));
			}
			return new node(acc);
		}
	}

	static class apply implements IFn {
		public node invoke(ArrayList<node> args, Environment env) throws Exception {
			return Core.apply(args.get(0), args.get(1).arrayListValue(), env);
		}
	}

	static class filter implements IFn {
		public node invoke(ArrayList<node> args, Environment env) throws Exception {
			node f = args.get(0);
			ArrayList<node> lst = args.get(1).arrayListValue();
			ArrayList<node> acc = new ArrayList<node>();
			for (int i = 0; i < lst.size(); i++) {
				ArrayList<node> args2 = new ArrayList<node>();
				node item = lst.get(i);
				args2.add(item);
				node ret = Core.apply(f, args2, env);
				if (ret.booleanValue()) acc.add(item);
			}
			return new node(acc);
		}
	}

	static class pr implements IFn {
		public node invoke(ArrayList<node> args, Environment env) throws Exception {
			for (int i = 0; i < args.size(); i++) {
				if (i != 0) System.out.print(" ");
				System.out.print(args.get(i));
			}
			return Core.node_nil;
		}
	}

	static class prn implements IFn {
		public node invoke(ArrayList<node> args, Environment env) throws Exception {
			for (int i = 0; i < args.size(); i++) {
				if (i != 0) System.out.print(" ");
				System.out.print(args.get(i));
			}
			System.out.println();
			return Core.node_nil;
		}
	}

	static class read_line implements IFn {
		public node invoke(ArrayList<node> args, Environment env) throws Exception {
			BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
			try {
				return new node(br.readLine());
			} catch (IOException e) {
				return Core.node_nil;
			}
		}
	}

	static class slurp implements IFn {
		public node invoke(ArrayList<node> args, Environment env) throws Exception {
			String filename = args.get(0).stringValue();
			try {
				return new node(Core.slurp(filename));
			} catch (IOException e) {
				return Core.node_nil;
			}
		}
	}

	static class spit implements IFn {
		public node invoke(ArrayList<node> args, Environment env) throws Exception {
			String filename = args.get(0).stringValue();
			String str = args.get(1).stringValue();
			return new node(Core.spit(filename, str));
		}
	}

	static class list implements IFn {
		public node invoke(ArrayList<node> args, Environment env) throws Exception {
			return new node(args);
		}
	}

	static class str implements IFn {
		public node invoke(ArrayList<node> args, Environment env) throws Exception {
			StringBuilder sb = new StringBuilder();
			for (Object x : args) {
				sb.append(x.toString());
			}
			return new node(sb.toString());
		}
	}

	static class symbol implements IFn {
		public node invoke(ArrayList<node> args, Environment env) throws Exception {
			return new node(new javelin.Symbol(args.get(0).stringValue()));
		}
	}
}