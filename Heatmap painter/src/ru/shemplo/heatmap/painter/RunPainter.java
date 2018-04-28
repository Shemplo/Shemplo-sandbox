package ru.shemplo.heatmap.painter;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.List;
import java.util.Random;
import java.util.function.Supplier;

import javax.imageio.ImageIO;

import ru.shemplo.heatmap.reader.FileMatrixProvider;
import ru.shemplo.heatmap.reader.MatrixProvider;
import ru.shemplo.heatmap.util.Trip;

public class RunPainter {
	
	private static final Random R = new Random ();
	private static final Supplier <Double> S = () -> {
		return 2 * Math.abs (R.nextDouble ());
	};
	
	private static final int TILE_SIZE = 36;
	
	public static void main (String... args) throws Exception {
		String file = "matrix.txt";
		if (args != null && args.length > 0) {
			file = args [0];
		}
		
		MatrixProvider mp = new FileMatrixProvider (file);
		
		double [][] m = new double [10][10];
		for (int i = 0; i < m.length; i++) {
			for (int j = 0; j < m [i].length; j++) {
				m [i][j] = S.get ();
			}
		}
		HeatMatrix hm = new HeatMatrix (mp);
		
		GradientColor gc = new GradientColor (Color.RED, Color.WHITE);
		Color [][] matrix = hm.diagonalize ().getColorMatrix (gc);
		double [][] original = hm.getMatrix ();
		
		List <String> cnames = hm.getColumnNames (), 
						rnames = hm.getRowNames ();
		int maxWidth = 0, maxHeight = 0;
		for (int i = 0; i < cnames.size (); i++) {
			maxHeight = Math.max (maxHeight, cnames.get (i).length ());
		}
		for (int i = 0; i < rnames.size (); i++) {
			maxWidth = Math.max (maxWidth, rnames.get (i).length ());
		}
		
		int hOff = 10 + maxWidth * 6, vOff = 10 + maxHeight * 6;
		BufferedImage bi = new BufferedImage (original.length * TILE_SIZE + hOff + 10, 
												original.length * TILE_SIZE + vOff + 10, 
												BufferedImage.TYPE_INT_RGB);
		Graphics2D g = (Graphics2D) bi.getGraphics ();
		AffineTransform at = g.getTransform ();
		
		g.setColor (new Color (230, 230, 230));
		g.fillRect (0, 0, bi.getWidth (), bi.getHeight ());
		int ts = TILE_SIZE;
		
		g.setColor (Color.BLACK);
		g.rotate (-Math.PI / 2);
		for (int i = 0; i < original.length; i++) {
			int y =  hOff + 4 + ts / 2 + i * ts;
			g.drawString (cnames.get (i), 4 - vOff, y);
		}
		
		g.setTransform (at);
		for (int i = 0; i < matrix.length; i++) {
			g.setColor (Color.BLACK);
			int y = vOff + 4 + ts / 2 + i * ts;
			g.drawString (rnames.get (i), 6, y);
			
			for (int j = 0; j < matrix [i].length; j++) {
				g.setColor (matrix [i][j]);
				g.fillRect (hOff + j * ts, vOff + i * ts, ts, ts);
				
				if (original [i][j] > 0) {
					g.setColor (Color.BLACK);
					g.drawString (String.format ("%3.0f", original [i][j]), 
												hOff + 2 + j * ts, y);
				}
			}
		}
		
		g.dispose ();
		
		File image = new File ("heatmap.jpg");
		ImageIO.write (bi, "jpg", image);
		
		System.out.println ("Image saved");
		
		File matches = new File ("matches.txt");
		
		try (
			OutputStream os = new FileOutputStream (matches);
			PrintWriter pw = new PrintWriter (os, true);
		) {
			List <Trip <String, Double, String>> trips = hm.getBestMatches ();
			trips.sort ((a, b) -> {
				char [] f = a.f.toCharArray (),
						s = b.f.toCharArray ();
				if (f.length != s.length) {
					return f.length - s.length;
				}
				
				for (int i = 0; i < f.length; i++) {
					if (f [i] != s [i]) {
						return f [i] - s [i];
					}
				}
				
				return 0;
			});
			
			for (Trip <String, Double, String> trip : trips) {
				pw.println (String.format ("%10s %4.0f %s", trip.f, trip.s, trip.t));
			}
		} catch (IOException ioe) {
			System.out.println (ioe.toString ());
		}
		
		System.out.println ("Matches saved");
	}
	
}
