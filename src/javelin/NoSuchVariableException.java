package javelin;

public class NoSuchVariableException extends Exception {
	private static final long serialVersionUID = 1L;
	private String message = "";
	
	public NoSuchVariableException(String message) {
		this.message = message;
	}
	
	public String toString() {
		return message;
	}
}
