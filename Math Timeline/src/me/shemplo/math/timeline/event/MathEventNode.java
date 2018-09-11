package me.shemplo.math.timeline.event;

import java.util.Objects;

import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Border;
import javafx.scene.layout.BorderStroke;
import javafx.scene.layout.BorderStrokeStyle;
import javafx.scene.layout.BorderWidths;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.TextAlignment;
import me.shemplo.math.timeline.Run;

public class MathEventNode extends HBox {
    
    public final MathEvent EVENT;
    
    public MathEventNode (MathEvent event) {
        setLayoutY (Run.R.nextInt ((int) Run.HEIGHT - 200) + 75);
        setLayoutX (Run.R.nextInt ((int) Run.WIDTH - 400) + 200);
        //setAlignment (Pos.TOP_CENTER);
        this.EVENT = event;
        
        if (!Objects.isNull (event.ICON)) {
            VBox pane = new VBox ();
            getChildren ().add (pane);
            HBox.setMargin (pane, new Insets (0, 10, 0, 0));
            pane.setBorder (new Border (new BorderStroke (Color.BLACK, 
                BorderStrokeStyle.SOLID, CornerRadii.EMPTY, BorderWidths.DEFAULT)));
            pane.setBackground (Run.WHITE_BG);
            
            ImageView image = new ImageView (event.ICON.IMAGE);
            pane.getChildren ().add (image);
            image.setFitWidth (24); image.setFitHeight (24);
            
            int offsets = 10;
            VBox.setMargin (image, new Insets (offsets));
            pane.setMaxHeight (24 + 2 * offsets);
        }
        
        Label label = new Label (event.EVENT);
        getChildren ().add (label);
        label.setTextAlignment (TextAlignment.CENTER);
        label.setFont (new Font (17));
        label.setWrapText (true);
        label.setMaxWidth (200);
    }
    
}
