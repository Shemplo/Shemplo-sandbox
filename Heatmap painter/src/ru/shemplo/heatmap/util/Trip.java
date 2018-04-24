package ru.shemplo.heatmap.util;


public class Trip <F, S, T> {

	public final F f;
	public final S s;
	public final T t;
	
	public Trip (F f, S s, T t) {
		this.f = f; 
		this.s = s; 
		this.t = t;
	}
	
	public String toString () {
		return "(" + f + ", " + s + ", " + t + ")";
	}
	
	public static <F, S, T> Trip <F, S, T> mt (F f, S s, T t) {
		return new Trip <F, S, T> (f, s, t);
	}
	
}
