package ru.shemplo.heatmap.painter;

import java.awt.Color;

public class GradientColor {

	private final Color FIRST, SECOND;
	
	public GradientColor (Color first, Color second) {
		this.FIRST = first; this.SECOND = second;
	}
	
	public Color getGradient (double part) {
		int [] color = new int [] {
			FIRST.getRed   () + (int) ((SECOND.getRed   () - FIRST.getRed   ()) * part),
			FIRST.getGreen () + (int) ((SECOND.getGreen () - FIRST.getGreen ()) * part),
			FIRST.getBlue  () + (int) ((SECOND.getBlue  () - FIRST.getBlue  ()) * part),
			FIRST.getAlpha () + (int) ((SECOND.getAlpha () - FIRST.getAlpha ()) * part)
		};
		
		return new Color (color [0], color [1], color [2], color [3]);
	}
	
}
