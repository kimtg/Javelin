package javelin;

import java.util.HashMap;

class Environment {
	HashMap<Integer, Object> env = new HashMap<Integer, Object>();
	Environment outer;

	Environment() {
		this.outer = null;
	}

	Environment(Environment outer) {
		this.outer = outer;
	}

	Object get(int code) throws Exception {
		if (env.containsKey(code)) {
			return env.get(code);
		} else {
			if (outer != null) {
				return outer.get(code);
			} else {
				try {
					return Core.getClass(Symbol.symname.get(code));
				} catch (ClassNotFoundException e) {
					throw new Exception("Unable to resolve symbol: " + Symbol.symname.get(code));
				}
			}
		}
	}
	
	// change an existing variable
	Object set(int code, Object v) throws Exception {
		if (env.containsKey(code)) {
			env.put(code, v);
			return v;
		} else {
			if (outer != null) {
				return outer.set(code, v);
			} else {
				throw new Exception("Unable to resolve symbol: " + Symbol.symname.get(code));
			}
		}
	}	

	// define a new variable
	Object def(int code, Object v) {
		env.put(code, v);
		return v;
	}
}