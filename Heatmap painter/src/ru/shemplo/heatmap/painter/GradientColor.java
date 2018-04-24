package ru.shemplo.heatmap.painter;

import java.awt.Color;

public class GradientColor {

	private final Color FIRST, SECOND;
	
	public GradientColor (Color first, Color second) {
		this.FIRST = first; this.SECOND = second;
	}
	
	public Color getGradient (double part) {
		int [] color = new int [] {
			SECOND.getRed   () + (int) ((FIRST.getRed   () - SECOND.getRed   ()) * part),
			SECOND.getGreen () + (int) ((FIRST.getGreen () - SECOND.getGreen ()) * part),
			SECOND.getBlue  () + (int) ((FIRST.getBlue  () - SECOND.getBlue  ()) * part),
			SECOND.getAlpha () + (int) ((FIRST.getAlpha () - SECOND.getAlpha ()) * part)
		};
		
		return new Color (color [0], color [1], color [2], color [3]);
	}
	
}
