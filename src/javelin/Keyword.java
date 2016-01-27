package javelin;

public class Keyword {
	Symbol symbol;
	
	public Keyword(String x) {
		this.symbol = new Symbol(x);
	}
	
	@Override
	public boolean equals(Object x) {
		return x instanceof Keyword && symbol.equals(((Keyword) x).symbol);
	}
	
	@Override
	public String toString() {
		return symbol.toString();
	}
}
