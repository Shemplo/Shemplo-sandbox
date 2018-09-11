package me.shemplo.math.timeline.gfx;

import java.io.InputStream;

import javafx.scene.image.Image;
import me.shemplo.math.timeline.Run;

public enum AppIconEnum {
 
    ANGLE   ("gfx/angle.png"),
    EARTH   ("gfx/earth.png"),
    FORMULA ("gfx/formula.png"),
    LETTER  ("gfx/letter.png"),
    ;
    
    public final Image IMAGE;
    
    private AppIconEnum (String path) {
        this (Run.class.getResourceAsStream (path));
    }
    
    private AppIconEnum (InputStream is) {
        this (new Image (is));
    }
    
    private AppIconEnum (Image image) {
        this.IMAGE = image;
    }
    
}
