package javelin;

import java.util.ArrayList;
import java.util.List;

class UserFn extends Fn { // anonymous function
	List<Object> def; // definition
	Environment outer_env;

	UserFn(List<Object> def, Environment outer_env) {
		this.def = def;
		this.outer_env = outer_env;
	}

	public String toString() {
		List<Object> d = new ArrayList<>();
		d.add(Core.sym_fn);
		d.addAll(def);
		return Core.toReadableString(d);
	}

	@Override
	public Object invoke(List<Object> args) throws Throwable {
		// anonymous function application. lexical scoping
		// ((ARGUMENT ...) BODY ...)
		fnStart: while (true) {
			Environment local_env = new Environment(this.outer_env);
			@SuppressWarnings("unchecked")
			List<Object> arg_syms = (List<Object>) this.def.get(0);

			int len = arg_syms.size();
			for (int i = 0; i < len; i++) { // assign arguments
				Symbol sym = (Symbol) arg_syms.get(i);
				if (sym.toString().equals("&")) { // variadic arguments
					sym = Core.symbolValue(arg_syms.get(i + 1));
					local_env.def(sym.code, new ArrayList<Object>(args.subList(i, args.size())));
					break;
				}
				Object n2 = args.get(i);
				local_env.def(sym.code, n2);
			}

			len = this.def.size();
			Object ret = null;
			for (int i = 1; i < len; i++) { // body
				try {
					ret = Core.eval(this.def.get(i), local_env);
				} catch (Recur e) {
					args = e.args;
					continue fnStart; // recur this function (effectively goto)
				}
			}
			return ret;
		}
	}
}