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
        
        Label title = new Label ("������� ����������");
        vertical.getChildren ().add (title);
        VBox.setMargin (title, new Insets (50, 10, 10, 10));
        title.setTextAlignment (TextAlignment.CENTER);
        title.setFont (new Font (36));
        
        Label intro = new Label (
           "���������� - ������� �����, ������� ��������� ��������� ����������� �����. "
           + "�� ��� ����� ��������� ����� ��������, �����������, ������������� � �.�. "
           + "� ���� ���� ��� ����� ���������� ��������� ����� ������� ��� �������, "
           + "� ��� ���������� ����� ����������� �� � ��������������� �������.\n\n"
           + "������ ����� ������� ����� ���������� �� ����� ������, ������� ����� "
           + "�������� ������� �� ������� �������. � ������ ����� ������ ����� ���������� "
           + "��������� �����, ������� ������� ������������ � ������ ������������� (���������)");
        vertical.getChildren ().add (intro);
        VBox.setMargin (intro, new Insets (10, 115, 10, 115));
        intro.setTextAlignment (TextAlignment.CENTER);
        intro.setFont (new Font (20));
        intro.setWrapText (true);
        
        Button button = new Button ("�� �������, ��������");
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
