package javelin;

import java.util.ArrayList;

import javelin.Core.node;

interface IFn {
	public node invoke(ArrayList<node> args, Environment env) throws Exception;
}