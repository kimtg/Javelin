package javelin;

import java.util.HashMap;

import javelin.Core.node;

class Environment {
	HashMap<Integer, node> env = new HashMap<Integer, node>();
	Environment outer;

	Environment() {
		this.outer = null;
	}

	Environment(Environment outer) {
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