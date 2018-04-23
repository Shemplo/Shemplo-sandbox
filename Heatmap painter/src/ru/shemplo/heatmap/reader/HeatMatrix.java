package ru.shemplo.heatmap.reader;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import ru.shemplo.heatmap.painter.GradientColor;
import ru.shemplo.heatmap.util.IntMajorValue;
import ru.shemplo.heatmap.util.Pair;

public class HeatMatrix {

	public static final int PRECISION = 3;
	
	private static final String 
		PRINT_FORMAT = " %-1." + PRECISION + "f ";
	
	private static final Comparator <Pair <Double, Integer>> 
		COMPARE_PAIR = (a, b) -> (int) Math.signum (b.f - a.f);
	
	private int height = 0, width = 0;
	private double norma = 0;
	
	private final double [][] ORIGINAL;
	private double [][] matrix;
	
	public HeatMatrix (MatrixProvider provider) {
		this (provider.getMatrix ());
	}
	
	public HeatMatrix (double [][] matrix) {
		this.width = 0;
		this.height = matrix.length;
		for (int i = 0; i < matrix.length; i++) {
			width = Math.max (width, matrix [i].length);
			for (int j = 0; j < matrix [i].length; j++) {
				norma = Math.max (norma, matrix [i][j]);
			}
		}
		
		this.ORIGINAL = new double [matrix.length][width];
		for (int i = 0; i < matrix.length; i ++) {
			int length = matrix [i].length;
			System.arraycopy (matrix [i], 0, ORIGINAL [i], 0, length);
		}
		
		this.matrix = normalize (getOriginal (), norma);
	}
	
	public String toString () {
		StringBuilder sb = new StringBuilder ();
		sb.append ("Matrix of ");
		sb.append (height);
		sb.append (" rows and ");
		sb.append (width);
		sb.append (" columns\n");
		for (int i = 0; i < height; i++) {
			sb.append ("[ ");
			for (int j = 0; j < width; j ++) {
				String formatted = String.format (Locale.ENGLISH,
									PRINT_FORMAT, matrix [i][j]);
				sb.append (formatted);
			}
			
			sb.append (" ]\n");
		}
		
		return sb.toString ();
	}
	
	public HeatMatrix reset () {
		this.matrix = normalize (getOriginal (), norma);
		return this;
	}
	
	public HeatMatrix diagonalize () {
		if (height != width) {
			String message = "Given matrix is not squared";
			throw new IllegalStateException (message);
		}
		
		List <Pair <Double, Integer>> 
			columns = new ArrayList <> (),
			rows = new ArrayList <> ();
		
		for (int i = 0; i < height; i++) {
			double maxValue = 0;
			for (int j = 0; j < width; j++) {
				maxValue = Math.max (maxValue, matrix [i][j]);
			}
			
			rows.add (Pair.mp (maxValue, i));
		}
		
		for (int i = 0; i < width; i++) {
			double maxValue = 0;
			for (int j = 0; j < height; j++) {
				maxValue = Math.max (maxValue, matrix [j][i]);
			}
			
			columns.add (Pair.mp (maxValue, i));
		}
		
		columns.sort (COMPARE_PAIR);
		rows.sort (COMPARE_PAIR);
		
		System.out.println (columns);
		System.out.println (rows);
		
		double [][] tmp = new double [height][width];
		for (int i = 0; i < height; i++) {
			int row = rows.get (i).s;
			for (int j = 0; j < width; j++) {
				int column = columns.get (j).s;
				tmp [i][j] = this.matrix [row][column];
			}
		}
		
		this.matrix = tmp;
		
		return this;
	}
	
	public HeatMatrix diagonalize (int a) {
		if (height != width) {
			String message = "Given matrix is not squared";
			throw new IllegalStateException (message);
		}
		
		List <IntMajorValue> majors = new ArrayList <> ();
		List <Integer> order = new ArrayList <> ();
		for (int i = 0; i < width; i++) {
			majors.add (new IntMajorValue ());
			order.add (-1);
		}
		
		Set <Integer> used = new HashSet <> ();
		List <Pair <Double, Integer>> line;
		
		for (int i = 0; i < height; i++) {
			line = new ArrayList <> ();
			for (int j = 0; j < width; j++) {
				line.add (Pair.mp (matrix [i][j], j));
			}
			
			line.sort (COMPARE_PAIR);
			
			//line = makePolarList (line, COMPARE_PAIR, i);
			//System.out.println (line);
			//System.out.println ();
			
			/*
			for (int j = 0; j < width; j++) {
				majors.get (j).addKey (line.get (j).s);
			}
			*/
			
			/*
			 * THIS WORKS DON'T TOUCH
			 */ 
			for (int j = 0; j < width; j++) {
				int column = line.get (j).s;
				if (!used.contains (column)) {
					order.set (i, column);
					used.add (column);
					break;
				}
			}
		}
		
		/*
		int iter = 0;
		while (iter < height) {
			for (int i = 0; i < width; i++) {
				if (order.get (i) != -1) { continue; }
				
				IntMajorValue imv = majors.get (i);
				List <Integer> major = imv.getMajor ();
				major = major.stream ()
							 .filter (v -> !used.contains (v))
							 .collect (Collectors.toList ());
				
				if (major.size () == 1) {
					int value = major.get (0);
					order.set (i, value);
					used.add (value);
				}
			}
			
			iter++;
		}
		
		for (int i = 0; i < width; i++) {
			if (order.get (i) != -1) { continue; }
			
			IntMajorValue imv = majors.get (i);
			List <Integer> major = imv.getJustSorted ();
			major = major.stream ()
						 .filter (v -> !used.contains (v))
						 .collect (Collectors.toList ());
			
			if (major.size () > 0) {
				int value = major.get (0);
				order.set (i, value);
				used.add (value);
			} else {
				String message = "Can't diagonalize matrix (index " + i + ")";
				throw new IllegalStateException (message);
			}
		}
		*/
		
		System.out.println ("Order: " + order);
		double [][] tmp = new double [height][width];
		for (int i = 0; i < height; i++) {
			for (int j = 0; j < width; j++) {
				tmp [i][j] = this.matrix [i][Math.max (order.get (j), 0)];
			}
		}
		
		this.matrix = tmp;
		return this;
	}
	
	@SuppressWarnings ("unused")
	private <T> List <T> makePolarList (List <T> list, Comparator <T> compare, int polar) {
		if (list == null) {
			return list;
		}
		
		List <T> polarList = new ArrayList <> (list);
		//list.sort (compare);
		
		/*
		 * ASSUMED THAT `list` IS SORTED DESCENDING
		 */
		for (int i = 0, l = polar, r = polar + 1, side = 0; 
				i < list.size (); i++, side = (side + 1) & 1) {
			T value = list.get (i);
			// Sides:  * 0 - left
			//         * 1 - right
			// Action: * set value
			//         * move carriage to 1 position
			
			// Result will look like:
			// 5 3 1 2 4 6 7 8 9 ...
			// For the situation when polar at 2 position
			if (side == 0) {
				if (l >= 0) {
					polarList.set (l, value);
					l -= 1; // Move 1 left
				} else {
					polarList.set (r, value);
					r += 1; // Move 1 right
				}
			} else if (side == 1) {
				if (r < polarList.size ()) {
					polarList.set (r, value);
					r += 1; // Move 1 right
				} else {
					polarList.set (l, value);
					l -= 1; // Move 1 left
				}
			}
		}
		
		//System.out.println (polar + " " + polarList);
		return polarList;
	}
	
	public double [][] getOriginal () {
		double [][] matrix = new double [height][width];
		for (int i = 0; i < height; i++) {
			System.arraycopy (ORIGINAL [i], 0, 
						matrix [i], 0, width);
		}
		
		return matrix;
	}
	
	public double [][] getMatrix () {
		return this.matrix.clone ();
	}
	
	public Color [][] getColorMatrix (GradientColor gc) {
		Color [][] matrix = new Color [height][width];
		for (int i = 0; i < height; i++) {
			for (int j = 0; j < width; j++) {
				matrix [i][j] = gc.getGradient (this.matrix [i][j]);
			}
		}
		
		return matrix;
	}
	
	@SuppressWarnings ("unused")
	private double [][] normalize (double [][] matrix) {
		double norma = 0;
		for (int i = 0; i < height; i++) {
			for (int j = 0; j < width; j ++) {
				norma = Math.max (norma, matrix [i][j]);
			}
		}
		
		return normalize (matrix, norma);
	}
	
	private double [][] normalize (double [][] matrix, double norma) {
		if (norma > 1) {
			for (int i = 0; i < height; i++) {
				for (int j = 0; j < width; j ++) {
					matrix [i][j] /= norma;
				}
			}
		}
		
		return matrix;
	}
	
}
