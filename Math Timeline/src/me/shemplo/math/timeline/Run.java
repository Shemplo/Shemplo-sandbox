package me.shemplo.math.timeline;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Random;
import java.util.Set;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import javafx.stage.Stage;
import me.shemplo.math.timeline.scene.AppSceneEnum;
import me.shemplo.math.timeline.scene.GameScene;
import me.shemplo.math.timeline.scene.StartScene;

public class Run extends Application {
 
    public static final Random R = new Random ();
    
    public static final Image ICON = new Image (Run.class.getResourceAsStream ("gfx/timeline.png"));
    public static final double WIDTH = 1000, HEIGHT = 500;
    
    public static final Background WHITE_BG 
        = new Background (new BackgroundFill (Color.WHITE, CornerRadii.EMPTY, Insets.EMPTY));
    
    public static final Color [] COLORS = {
        Color.ALICEBLUE, Color.ANTIQUEWHITE,
        Color.AZURE, Color.FLORALWHITE,
        Color.GAINSBORO, Color.HONEYDEW
    };
    
    public static final String [] SNEERS = {
        "что-то пошло не так",
        "мир - несправедливая штука",
        "возможно, стоит попробовать ещё раз",
        "я тоже не с первого раза это сделал"
    }, CONGRATS = {
        "Кто бы мог сомневаться?"
    };
    
    private static Stage stage;
    
    public static void main (String... args) {
        launch (args);
    }
    
    public static List <Integer> getSample (int need, int size) {
        if (need > size) {
            String message = "Number of needed elements more than cardinality";
            throw new IllegalStateException (message);
        }
        
        Set <Integer> sample = new HashSet <> ();
        while (sample.size () < need) {
            sample.add (R.nextInt (size));
        }
        
        return new ArrayList <> (sample);
    }
    
    public static void swtichScene (AppSceneEnum sceneEnum) {
        Parent root = null;
        switch (sceneEnum) {
            case START:
                root = new StartScene ();
                break;
                
            case GAME:
                root = new GameScene ();
                break;
        }
        
        final Parent set = root;
        if (!Objects.isNull (root)) {
            Platform.runLater (() -> stage.getScene ().setRoot (set));
        }
    }
    
    public synchronized static Scene getCurrentScene () {
        return stage.getScene ();
    }
    
    public static Stage showModal (Pane root, String title) {
        Stage stage = new Stage (); stage.setScene (new Scene (root));
        stage.initModality (Modality.APPLICATION_MODAL);
        stage.getIcons ().add (Run.ICON);
        stage.setResizable (false);
        stage.setTitle (title);
        stage.show ();
        
        return stage;
    }
    
    public static Pane getWhitePane () {
        Pane pane = new Pane ();
        pane.setBackground (Run.WHITE_BG);
        return pane;
    }
    
    @Override
    public void start (Stage stage) throws Exception {
        Run.stage = stage;
        
        Scene scene = new Scene (new StartScene (), WIDTH, HEIGHT);
        stage.setMinWidth (WIDTH); stage.setMinHeight (HEIGHT);
        stage.setTitle ("История математики");
        stage.getIcons ().add (ICON);
        stage.setResizable (false);
        stage.setScene (scene);
        stage.show ();
    }
    
}
