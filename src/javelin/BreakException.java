package javelin;

public class BreakException extends Exception {
	private static final long serialVersionUID = 1L;
	public String toString() {
		return "break is used outside a loop";
	}
}
