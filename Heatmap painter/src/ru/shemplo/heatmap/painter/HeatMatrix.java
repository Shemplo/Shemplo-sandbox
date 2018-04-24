package ru.shemplo.heatmap.painter;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;

import ru.shemplo.heatmap.reader.MatrixProvider;
import ru.shemplo.heatmap.util.Pair;
import ru.shemplo.heatmap.util.Trip;

public class HeatMatrix implements MatrixProvider {

	public static final int PRECISION = 3;
	
	private static final String 
		PRINT_FORMAT = " %-1." + PRECISION + "f ";
	
	@SuppressWarnings ("unused")
	private static final Comparator <Pair <Double, Integer>> 
		COMPARE_PAIR = (a, b) -> (int) Math.signum (b.f - a.f);
	private static final Comparator <Trip <Double, ?, ?>> 
		COMPARE_TRIP = (a, b) -> (int) Math.signum (b.f - a.f);
	
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
	
	public HeatMatrix (double [][] matrix) {
		this (matrix, null, null);
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
		if (!Objects.isNull (cnames)) {
			this.ORIGINAL_CNAMES = Collections.unmodifiableList (cnames);
		} else {
			this.ORIGINAL_CNAMES = new ArrayList <> ();
			for (int i = 0; i < width; i++) {
				ORIGINAL_CNAMES.add ("" + i);
			}
		}
		
		if (!Objects.isNull (rnames)) {
			this.ORIGINAL_RNAMES = Collections.unmodifiableList (rnames);
		} else {
			this.ORIGINAL_RNAMES = new ArrayList <> ();
			for (int i = 0; i < width; i++) {
				ORIGINAL_RNAMES.add ("" + i);
			}
		}

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
		
		List <Trip <Double, Integer, Integer>> 
			values = new ArrayList <> ();
		
		for (int i = 0; i < height; i++) {
			for (int j = 0; j < width; j++) {
				values.add (Trip.mt (matrix [i][j], i, j));
			}
		}
		
		values.sort (COMPARE_TRIP);
		Set <Integer> columns = new HashSet <> (),
						rows = new HashSet <> ();
		List <Integer> columnsOrder = new ArrayList <> (),
						rowsOrder = new ArrayList <> ();
		
		for (Trip <Double, Integer, Integer> trip : values) {
			int row = trip.s, col = trip.t;
			if (!columns.contains (col) 
				&& !rows.contains (row)) {
				columnsOrder.add (col);
				rowsOrder.add (row);
				
				columns.add (col);
				rows.add (row);
			}
			
			if (columns.size () == matrix.length
				&& rows.size () == matrix.length) {
				break;
			}
		}
		
		double [][] tmp = new double [height][width];
		for (int i = 0; i < height; i++) {
			int row = rowsOrder.get (i);
			for (int j = 0; j < width; j++) {
				int col = columnsOrder.get (j);
				tmp [i][j] = this.matrix [row][col];
			}
		}
		
		List <String> tnames = new ArrayList <> ();
		for (int i = 0; i < height; i++) {
			tnames.add (rnames.get (rowsOrder.get (i)));
		}
		rnames = tnames;
		
		tnames = new ArrayList <> ();
		for (int i = 0; i < width; i++) {
			tnames.add (cnames.get (columnsOrder.get (i)));
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
		double [][] tmp = new double [height][width];
		for (int i = 0; i < height; i++) {
			for (int j = 0; j < width; j++) {
				tmp [i][j] = norma * matrix [i][j];
			}
		}
		
		return tmp;
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
