package javelin;

import java.util.ArrayList;

interface IFn {
	public Object invoke(ArrayList<Object> args) throws Throwable;
}