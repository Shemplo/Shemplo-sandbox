package ru.shemplo.heatmap.reader;

import java.util.List;

public interface MatrixProvider {

	public double [][] getMatrix ();
	
	public List <String> getColumnNames ();
	
	public List <String> getRowNames ();
	
}
