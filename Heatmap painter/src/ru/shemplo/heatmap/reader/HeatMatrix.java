package ru.shemplo.heatmap.reader;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

import ru.shemplo.heatmap.painter.GradientColor;
import ru.shemplo.heatmap.util.Pair;

public class HeatMatrix implements MatrixProvider {

	public static final int PRECISION = 3;
	
	private static final String 
		PRINT_FORMAT = " %-1." + PRECISION + "f ";
	
	private static final Comparator <Pair <Double, Integer>> 
		COMPARE_PAIR = (a, b) -> (int) (Math.signum (b.f - a.f));
	
	private int height = 0, width = 0;
	private double norma = 0;
	
	private final List <String> ORIGINAL_CNAMES, 
								ORIGINAL_RNAMES;
	private final double [][] ORIGINAL;
	
	private List <String> cnames, rnames;
	private double [][] matrix;
	
	public HeatMatrix (MatrixProvider provider) {
		this (provider.getMatrix (), provider.getColumnNames (), 
				provider.getRowNames ());
	}
	
	public HeatMatrix (double [][] matrix, List <String> cnames, List <String> rnames) {
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
		this.ORIGINAL_CNAMES = Collections.unmodifiableList (cnames);
		this.ORIGINAL_RNAMES = Collections.unmodifiableList (rnames);
		this.cnames = ORIGINAL_CNAMES;
		this.rnames = ORIGINAL_RNAMES;
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
		this.cnames = new ArrayList <> (ORIGINAL_CNAMES);
		this.rnames = new ArrayList <> (ORIGINAL_RNAMES);
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
		
		List <Pair <Double, Integer>> same = new ArrayList <> ();
		Pair <Double, Integer> prev = rows.get (0);
		
		for (int i = 1; i < height; i++) {
			Pair <Double, Integer> cur = rows.get (i);
			if (prev.f.compareTo (cur.f) == 0) {
				if (same.size () == 0) {
					same.add (prev);
				}
				
				same.add (cur);
			} else if (same.size () > 0 
						|| i == width - 1) {
				List <Pair <Integer, Integer>> 
					inds = new ArrayList <> ();
				
				for (int j = 0; j < same.size (); j++) {
					int row = same.get (j).s;
					
					double max = 0;
					int index = -1;
					for (int k = 0; k < width; k++) {
						int column = columns.get (k).s;
						if (max < matrix [row][column]) {
							max = matrix [row][column];
							index = k;
						}
					}
					
					inds.add (Pair.mp (j, index));
				}
				
				inds.sort ((a, b) -> (int) Math.signum (a.s - b.s));
				for (int j = 0; j < inds.size (); j++) {
					rows.set (i - same.size () + j, same.get (inds.get (j).f));
				}
				
				// TODO: not optimal but not resolved
				//i += same.size () - 1;
				same.clear ();
			}
			
			prev = cur;
		}
		
		double [][] tmp = new double [height][width];
		for (int i = 0; i < height; i++) {
			int row = rows.get (i).s;
			for (int j = 0; j < width; j++) {
				int column = columns.get (j).s;
				tmp [i][j] = this.matrix [row][column];
			}
		}
		
		List <String> tnames = new ArrayList <> ();
		for (int i = 0; i < height; i++) {
			tnames.add (rnames.get (rows.get (i).s));
		}
		rnames = tnames;
		
		tnames = new ArrayList <> ();
		for (int i = 0; i < width; i++) {
			tnames.add (cnames.get (columns.get (i).s));
		}
		cnames = tnames;
		
		this.matrix = tmp;
		return this;
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
	
	@Override
	public List <String> getColumnNames () {
		return Collections.unmodifiableList (cnames);
	}

	@Override
	public List <String> getRowNames () {
		return Collections.unmodifiableList (rnames);
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
