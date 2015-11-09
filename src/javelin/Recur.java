package javelin;

import java.util.ArrayList;

public class Recur extends Throwable {
	private static final long serialVersionUID = 1L;
	ArrayList<Object> args;
	
	public Recur(ArrayList<Object> args) {
		this.args = args;
	}
	
	// the message shown when not caught
	public String toString() {
		return "recur is used outside a fn or a loop";
	}
}
