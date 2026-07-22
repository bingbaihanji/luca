package com.bingbaihanji.luca.core.ui;

import com.bingbaihanji.luca.fxutils.FXUIComponents;
import javafx.application.Application;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;

/**
 *
 * @author bingbaihanji
 * @date 2026-05-18 14:09:40
 * @description //TODO
 */
public class Test extends Application {

    static void main() {
        Application.launch();
    }

    @Override
    public void start(Stage primaryStage) throws Exception {
        BorderPane root = new BorderPane();
        root.setStyle("-fx-background-color: #33353b");
        Button error = new Button("Error");
        Button info = new Button("Info");
        Button warn = new Button("Warn");

        HBox hBox = new HBox();
        hBox.setStyle("-fx-background-color: #191b1d;");
        hBox.setAlignment(Pos.BOTTOM_CENTER);
        hBox.setSpacing(10);
        hBox.getChildren().addAll(error, info, warn);
        root.setCenter(hBox);
        Scene scene = new Scene(root);
        // 加载 IDEA 深色主题 CSS
        String css = LucaApp.class.getResource("/css/idea-dark.css").toExternalForm();
        scene.getStylesheets().add(css);

        primaryStage.setScene(scene);
        primaryStage.show();

        error.setOnAction(e -> {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Error");
            FXUIComponents.createlucaAlert(alert);
            alert.showAndWait();
        });

        warn.setOnAction(e -> {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("这是警告");
            FXUIComponents.createlucaAlert(alert);
            alert.showAndWait();

        });

    }
}
