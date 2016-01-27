package javelin;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.Callable;

class Fn implements Callable<Object>, Runnable, Comparator<Object> {
	public Object invoke(List<Object> args) throws Throwable {
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

	@Override
	public int compare(Object arg0, Object arg1) {
		ArrayList<Object> a = new ArrayList<Object>();
		a.add(arg0);
		a.add(arg1);
		try {
			return (int) invoke(a);
		} catch (Throwable e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return 0;
		}
	}

	@Override
	public Object call() throws Exception {
		try {
			return invoke(new ArrayList<Object>());
		} catch (Throwable e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return null;
		}
	}
}