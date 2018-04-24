package ru.shemplo.heatmap.reader;

import static java.lang.Double.*;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.StringTokenizer;

public class FileMatrixProvider implements MatrixProvider {

	private final File FILE;
	
	private List <String> cnames, rnames;
	private double [][] matrix; 
	
	public FileMatrixProvider (String path) {
		this (new File (path));
	}
	
	public FileMatrixProvider (File file) {
		if (!file.exists () || !file.isFile ()) {
			String message = "File `" + file.getAbsolutePath () 
								+ "` doesn't exist";
			throw new IllegalStateException (message);
		}
		
		this.FILE = file;
		this.matrix = readMatrix ();
	}
	
	private double [][] readMatrix () {
		List <List <Double>> dinamicMatrix = new ArrayList <> ();
		rnames = new ArrayList <> ();
		int width = 0;
		
		try (
			InputStream is = new FileInputStream (FILE);
			Reader r = new InputStreamReader (is, StandardCharsets.UTF_8);
			BufferedReader br = new BufferedReader (r);
		) {
			String line = br.readLine ().trim ();
			String [] names = line.split ("\\s");
			cnames = Arrays.asList (names);
			
			while ((line = br.readLine ()) != null) {
				line = line.trim ();
				if (line.length () == 0) {
					continue;
				}
				
				List <Double> row = new ArrayList <> ();
				dinamicMatrix.add (row);
				
				StringTokenizer st = new StringTokenizer (line);
				if (st.hasMoreTokens ()) {
					String name = st.nextToken ();
					rnames.add (name);
				}
				
				while (st.hasMoreTokens ()) {
					String stringNumber = st.nextToken ();
					try {
						double number = parseDouble (stringNumber);
						row.add (number);
					} catch (NumberFormatException nfe) {}
				}
				
				width = Math.max (width, row.size ());
			}
		} catch (IOException ioe) {
			System.out.println (ioe);
		}
		
		double [][] matrix = new double [dinamicMatrix.size ()][width];
		for (int i = 0; i < matrix.length; i++) {
			int rowLength = dinamicMatrix.get (i).size ();
			List <Double> row = dinamicMatrix.get (i);
			for (int j = 0; j < rowLength; j++) {
				matrix [i][j] = row.get (j);
			}
		}
		
		return matrix;
	}
	
	@Override
	public double [][] getMatrix () {
		return this.matrix;
	}

	@Override
	public List <String> getColumnNames () {
		return cnames;
	}

	@Override
	public List <String> getRowNames () {
		return rnames;
	}

}
