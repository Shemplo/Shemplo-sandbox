package me.shemplo.math.timeline.scene;

import java.util.Objects;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.TextAlignment;
import me.shemplo.math.timeline.Run;

public class StartScene extends StackPane {
    
    public StartScene () {
        VBox vertical = new VBox ();
        getChildren ().add (vertical);
        vertical.setBackground (Run.WHITE_BG);
        vertical.setAlignment (Pos.TOP_CENTER);
        
        Label title = new Label ("»стори€ математики");
        vertical.getChildren ().add (title);
        VBox.setMargin (title, new Insets (50, 10, 10, 10));
        title.setTextAlignment (TextAlignment.CENTER);
        title.setFont (new Font (36));
        
        Label intro = new Label (
           "ћатематика - древн€€ наука, котора€ по€вилась несколько тыс€чалетий назад. "
           + "«а это врем€ произошло много открытий, изобретений, доказательств и т.д. "
           + "¬ этой игре ¬ам будет предложено несколько таких событий или €влений, "
           + "и ¬ам необходимо будет расположить их в хронологическом пор€дке.\n\n"
           + " аждое такое €вление будет находитьс€ на своей плитке, которые можно "
           + "свободно двигать по видимой области. ¬ нижней части экрана будет находитьс€ "
           + "временна€ шкала, котора€ поможет определитьс€ с верным расположением (визуально)");
        vertical.getChildren ().add (intro);
        VBox.setMargin (intro, new Insets (10, 115, 10, 115));
        intro.setTextAlignment (TextAlignment.CENTER);
        intro.setFont (new Font (20));
        intro.setWrapText (true);
        
        Button button = new Button ("¬сЄ пон€тно, начинаем");
        vertical.getChildren ().add (button);
        VBox.setMargin (button, new Insets (20, 115, 30, 115));
        button.setFont (new Font (16));
        button.setOnMouseClicked (me -> {
            Run.swtichScene (AppSceneEnum.GAME);
        });
        
        Scene scene = Run.getCurrentScene ();
        if (!Objects.isNull (scene)) {
            scene.setOnMouseMoved (null);
        }
    }
    
}
