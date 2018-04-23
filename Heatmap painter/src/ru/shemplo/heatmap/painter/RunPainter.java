package ru.shemplo.heatmap.painter;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.Random;
import java.util.function.Supplier;

import javax.imageio.ImageIO;

import ru.shemplo.heatmap.reader.FileMatrixProvider;
import ru.shemplo.heatmap.reader.HeatMatrix;
import ru.shemplo.heatmap.reader.MatrixProvider;

public class RunPainter {
	
	private static final Random R = new Random ();
	private static final Supplier <Double> S = () -> {
		return 2 * Math.abs (R.nextDouble ());
	};
	
	private static final int TILE_SIZE = 32;
	
	public static void main (String... args) throws Exception {
		MatrixProvider mp = new FileMatrixProvider ("50vs50_matrix_50.txt");
		
		double [][] m = new double [10][10];
		for (int i = 0; i < m.length; i++) {
			for (int j = 0; j < m [i].length; j++) {
				m [i][j] = S.get ();
			}
		}
		HeatMatrix hm = new HeatMatrix (mp);
		
		GradientColor gc = new GradientColor (Color.WHITE, Color.RED);
		Color [][] matrix = hm.diagonalize ().getColorMatrix (gc);
		double [][] original = hm.getMatrix ();
		
		BufferedImage bi = new BufferedImage (original.length * TILE_SIZE + 40, 
												original.length * TILE_SIZE + 40, 
												BufferedImage.TYPE_INT_RGB);
		Graphics g = bi.getGraphics ();
		
		g.setColor (new Color (230, 230, 230));
		g.fillRect (0, 0, bi.getWidth (), bi.getHeight ());
		int ts = TILE_SIZE;
		
		g.setColor (Color.BLACK);
		for (int i = 0; i < original.length; i++) {
			g.drawString ("T" + (i + 1), 32 + i * ts, 22);
		}
		
		for (int i = 0; i < matrix.length; i++) {
			g.setColor (Color.BLACK);
			g.drawString ("T" + (i + 1), 4, 52 + i * ts);
			
			for (int j = 0; j < matrix [i].length; j++) {
				g.setColor (matrix [i][j]);
				g.fillRect (30 + j * ts, 30 + i * ts, ts, ts);
				
				if (original [i][j] > 0) {
					g.setColor (Color.BLACK);
					g.drawString (String.format ("%.2f", original [i][j]), 
												32 + j * ts, 52 + i * ts);
				}
			}
		}
		
		g.dispose ();
		
		File image = new File ("heatmap.jpg");
		ImageIO.write (bi, "jpg", image);
	}
	
}
