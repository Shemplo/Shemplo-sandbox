package ru.shemplo.heatmap.util;


public class Pair <F, S> {
	
	public final F f;
	public final S s;
	
	public Pair (F f, S s) {
		this.f = f; this.s = s;
	}
	
	public String toString () {
		return "(" + f + ", " + s + ")";
	}
	
	public static <F, S> Pair <F, S> mp (F f, S s) {
		return new Pair <F, S> (f, s);
	}
	
}
