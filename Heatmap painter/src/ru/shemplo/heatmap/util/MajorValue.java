package ru.shemplo.heatmap.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class MajorValue <T> {

	private final List <Integer> ENTRIES;
	private final List <T> KEYS;
	private int totalNumber;
	
	public MajorValue () {
		this.ENTRIES = new ArrayList <> ();
		this.KEYS = new ArrayList <> ();
		this.totalNumber = 0;
	}
	
	public void reset () {
		ENTRIES.clear ();
		totalNumber = 0;
		KEYS.clear ();
	}
	
	public MajorValue <T> addKey (T key) {
		int index = KEYS.indexOf (key);
		if (index == -1) {
			ENTRIES.add (1);
			KEYS.add (key);
		} else {
			int ents = ENTRIES.get (index) + 1;
			ENTRIES.set (index, ents);
		}
		
		totalNumber++;
		return this;
	}
	
	public List <T> getMajor () {
		return getMajor (0);
	}
	
	public List <T> getMajor (int threshold) {
		List <T> list = new ArrayList <> ();
		if (ENTRIES.size () == 0) {
			return list;
		}
		
		int max = Collections.max (ENTRIES);
		if (max < threshold) {
			return list;
		}
		
		for (int i = 0; i < KEYS.size (); i++) {
			if (ENTRIES.get (i) == max) {
				list.add (KEYS.get (i));
			}
		}
		
		return list;
	}
	
	public List <T> getMajor (double threshold) {
		int limit = (int) (totalNumber * threshold);
		return getMajor (limit);
	}
	
	public List <T> getJustSorted () {
		List <Pair <Integer, T>> zip = new ArrayList <> ();
		for (int i = 0; i < KEYS.size (); i++) {
			zip.add (Pair.mp (ENTRIES.get (i), KEYS.get (i)));
		}
		zip.sort ((a, b) -> (int) Math.signum (b.f - a.f));
		List <T> sorted = new ArrayList <> ();
		for (int i = 0; i < zip.size (); i++) {
			sorted.add (zip.get (i).s);
		}
		
		return sorted;
	}
	
}
