package me.shemplo.math.timeline.scene;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import javafx.geometry.Bounds;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.Border;
import javafx.scene.layout.BorderStroke;
import javafx.scene.layout.BorderStrokeStyle;
import javafx.scene.layout.BorderWidths;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.stage.Stage;
import me.shemplo.math.timeline.Run;
import me.shemplo.math.timeline.event.MathEvent;
import me.shemplo.math.timeline.event.MathEventNode;

public class GameScene extends StackPane {
    
    private final List <Node> NODES = new ArrayList <> ();
    private final GraphicsContext CONTEXT;
    private final Canvas CANVAS;
    private Node toMove = null;
    
    public GameScene () {
        setBackground (Run.WHITE_BG);
        setAlignment (Pos.TOP_LEFT);
        
        CANVAS = new Canvas (Run.WIDTH, Run.HEIGHT);
        CONTEXT = CANVAS.getGraphicsContext2D ();
        getChildren ().add (CANVAS);
        
        Pane gamePane = new Pane ();
        getChildren ().add (gamePane);
        
        List <Integer> sample = Run.getSample (7, MathEvent.values ().length);
        for (int i = 0; i < sample.size (); i++) {
            MathEvent event = MathEvent.values () [sample.get (i)];
            MathEventNode node = new MathEventNode (event);
            gamePane.getChildren ().add (node);
            node.setPadding (new Insets (5));
            node.setBorder (new Border (new BorderStroke (Color.BLACK, 
                BorderStrokeStyle.SOLID, CornerRadii.EMPTY, BorderWidths.DEFAULT)));
            node.setBackground (new Background (new BackgroundFill (Run.COLORS [i % Run.COLORS.length], 
                CornerRadii.EMPTY, Insets.EMPTY)));
            
            node.setOnMouseClicked (me -> {
                if (Objects.isNull (toMove)) {
                    node.setCursor (Cursor.NONE);
                    toMove = node;
                } else {
                    node.setCursor (Cursor.DEFAULT);
                    toMove = null;
                }
            });
            
            NODES.add (node);
        }
        
        Button back = new Button ("Сдаюсь");
        getChildren ().add (back);
        StackPane.setMargin (back, new Insets (10));
        back.setFocusTraversable (false);
        back.setFont (new Font (16));
        back.setOnMouseClicked (me -> {
            List <Node> nodes = new ArrayList <> (NODES);
            List <MathEvent> events = nodes.stream ()
               .map (n -> (MathEventNode) n)
               .sorted ((a, b) -> Integer.compare (a.EVENT.YEAR, b.EVENT.YEAR))
               .map (n -> n.EVENT).collect (Collectors.toList ());
            StringBuilder sb = new StringBuilder ();
            for (int i = 0; i < events.size (); i++) {
                MathEvent event = events.get (i);
                sb.append (i + 1); sb.append (". ");
                sb.append (event.EVENT);
                sb.append ("\n");
            }
            
            Pane pane = Run.getWhitePane ();
            Label label = new Label (
               "Очень жаль! Ну вы хотя бы попытались... :/\n\n"
               + "Правильный ответ:\n" + sb.toString ()
            );
            label.setPadding (new Insets (20));
            label.setFont (new Font (20));
            pane.getChildren ().add (label);
            
            int index = Run.R.nextInt (Run.CONGRATS.length);
            Stage stage = Run.showModal (pane, "Успех! " + Run.CONGRATS [index]);
            stage.setOnCloseRequest (we -> {
                Run.swtichScene (AppSceneEnum.START);
            });
        });
        
        Button check = new Button ("Проверить");
        getChildren ().add (check);
        StackPane.setMargin (check, new Insets (10, 10, 10, 95));
        check.setFocusTraversable (false);
        check.setFont (new Font (16));
        check.setOnMouseClicked (me -> {
            List <Node> nodes = new ArrayList <> (NODES);
            nodes.sort ((a, b) -> {
                Bounds aB = a.getBoundsInLocal (), bB = b.getBoundsInLocal ();
                return Double.compare (a.getLayoutX () + aB.getWidth () / 2, b.getLayoutX () + bB.getWidth () / 2);
            });
            
            int prev = ((MathEventNode) nodes.get (0)).EVENT.YEAR;
            for (int i = 1; i < nodes.size (); i++) {
                MathEventNode node = (MathEventNode) nodes.get (i);
                if (prev > node.EVENT.YEAR) {
                    Pane pane = Run.getWhitePane ();
                    Label label = new Label ("Неверный хронологический порядок :(");
                    label.setPadding (new Insets (20));
                    label.setFont (new Font (20));
                    pane.getChildren ().add (label);
                    
                    int index = Run.R.nextInt (Run.SNEERS.length);
                    Run.showModal (pane, "Ошибка: " + Run.SNEERS [index]);
                    return;
                }
                
                prev = node.EVENT.YEAR;
            }
            
            Pane pane = Run.getWhitePane ();
            Label label = new Label ("Всё верно! Правда же было не сложно? :)");
            label.setPadding (new Insets (20));
            label.setFont (new Font (20));
            pane.getChildren ().add (label);
            
            int index = Run.R.nextInt (Run.CONGRATS.length);
            Stage stage = Run.showModal (pane, "Успех! " + Run.CONGRATS [index]);
            stage.setOnCloseRequest (we -> {
                Run.swtichScene (AppSceneEnum.START);
            });
        });
        
        Scene scene = Run.getCurrentScene ();
        if (!Objects.isNull (scene)) {
            scene.setOnMouseMoved (me -> {
                if (!Objects.isNull (toMove)) {
                    double x = me.getSceneX (), y = me.getSceneY ();
                    Bounds bounds = toMove.getBoundsInLocal ();
                    
                    toMove.setLayoutY (y - bounds.getHeight () / 2);
                    toMove.setLayoutX (x - bounds.getWidth () / 2);
                }
                
                repaint ();
            });
        }
    }
    
    private void repaint () {
        double width = getWidth (), height = getHeight ();
        CONTEXT.clearRect (0, 0, width, height);
        
        CONTEXT.setFill (Color.BLACK);
        CONTEXT.fillRect (10, height - 15, width - 10, height - 10);
        
        CONTEXT.fillText ("Раньше", 10, height - 20);
        CONTEXT.fillText ("Позже", width - 47.5, height - 20);
        
        for (Node node : NODES) {
            double x = node.getLayoutX (), y = node.getLayoutY ();
            Bounds bounds = node.getBoundsInLocal ();
            
            double xOffset = bounds.getWidth () / 2;
            CONTEXT.strokeLine (x + xOffset, y, x + xOffset, height - 12.5);
        }
    }
    
}
