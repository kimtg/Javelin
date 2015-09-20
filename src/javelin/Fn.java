package javelin;

import java.util.ArrayList;

import javelin.Core.node;

class Fn implements IFn { // anonymous function
	ArrayList<node> def; // definition
	Environment outer_env;

	Fn(ArrayList<node> def, Environment outer_env) {
		this.def = def;
		this.outer_env = outer_env;
	}

	public String toString() {
		return "#<function:" + def.toString() + ">";
	}

	@Override
	public node invoke(ArrayList<node> args, Environment env) throws Exception {
		// anonymous function application. lexical scoping
		// ((ARGUMENT ...) BODY ...)
		Environment local_env = new Environment(this.outer_env);
		ArrayList<node> arg_syms = this.def.get(0).arrayListValue();

		int len = arg_syms.size();
		for (int i = 0; i < len; i++) { // assign arguments
			Symbol sym = arg_syms.get(i).symbolValue();
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
			ret = Core.eval(this.def.get(i), local_env);
		}
		return ret;
	}
}