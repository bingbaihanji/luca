package com.bingbaihanji.luca.test;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.BorderPane;
import javafx.stage.Popup;
import javafx.stage.Stage;

/**
 *
 * @author bingbaihanji
 * @date 2026-05-15 18:58:15
 * @description //TODO
 */
public class FXTest extends Application {


    static void main() {
        launch(FXTest.class);
    }

    @Override
    public void start(Stage stage) throws Exception {

        BorderPane root = new BorderPane();

        AnchorPane pane = new AnchorPane();
        root.setCenter(pane);

        Button button = new Button("弹窗");

        pane.getChildren().add(button);

        Scene scene = new Scene(root);
        stage.setScene(scene);
        stage.show();
        button.setOnAction(event -> {
            System.out.println("按下按钮");

            Popup popup = new Popup();


            popup.show(stage, 0, 0);


        });
    }
}
