package javelin;

import java.util.ArrayList;

class Fn implements Runnable {
	public Object invoke(ArrayList<Object> args) throws Throwable {
		return null;
	}

	@Override
	public void run() {
		try {
			invoke(new ArrayList<Object>());
		} catch (Throwable e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}