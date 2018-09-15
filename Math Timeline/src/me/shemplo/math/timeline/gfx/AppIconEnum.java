package me.shemplo.math.timeline.gfx;

import java.io.InputStream;

import javafx.scene.image.Image;
import me.shemplo.math.timeline.Run;

public enum AppIconEnum {
 
    ANGLE    ("gfx/angle.png"),
    EARTH    ("gfx/earth.png"),
    FORMULA  ("gfx/formula.png"),
    LETTER   ("gfx/letter.png"),
    SPECTRUM ("gfx/spectrum.png"),
    LOG      ("gfx/log.png"),
    LOG10    ("gfx/log10.png"),
    FRACSUM  ("gfx/fracsum.png"),
    DECFRAC  ("gfx/decfrac.png"),
    FEATHER  ("gfx/feather.png"),
    POW4     ("gfx/4pow.png"),
    PROBABIL ("gfx/4pow.png"),
    GRAPH    ("gfx/graph.png"),
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
