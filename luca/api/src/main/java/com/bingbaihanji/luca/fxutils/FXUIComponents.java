package com.bingbaihanji.luca.fxutils;

import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Dialog;
import javafx.scene.control.DialogPane;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.HeaderBar;
import javafx.scene.layout.HeaderDragType;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.stage.Window;

import java.util.Objects;

/**
 *
 * @author bingbaihanji
 * @date 2026-05-18 11:51:05
 * @description //TODO
 */
public final class FXUIComponents {
    private FXUIComponents() {
        throw new IllegalStateException("Utility class");
    }


    @SuppressWarnings("deprecation")
    public static void createlucaAlert(Alert alert) {
        DialogPane pane = alert.getDialogPane();

        // initStyle 必须在 hasBeenVisible 被置 true 之前调用，越早越好。
        Scene scene = pane.getScene();
        if (scene != null) {
            applyInitStyleToScene(scene);
        } else {
            pane.sceneProperty().addListener((obs, o, newScene) -> {
                if (newScene != null) {
                    applyInitStyleToScene(newScene);
                }
            });
        }
        // scene.setRoot 替换在 showingProperty 中执行：
        //   HeavyweightDialog.showAndWait 内部先调 scene.setRoot(dialogPane)，
        //   此时 dialogPane 是独立 root（无 parent）。
        //   showingProperty 监听器在 stage.show() 内部同步触发，仍在首帧渲染前。
        alert.showingProperty().addListener((obs, oldVal, showing) -> {
            if (showing) {
                Scene paneScene = pane.getScene();
                if (paneScene == null) {
                    return;
                }

                var url = FXUIComponents.class.getResource("/dark-theme.css");
                if (url != null) {
                    String cssUrl = url.toExternalForm();
                    if (!paneScene.getStylesheets().contains(cssUrl)) {
                        paneScene.getStylesheets().add(cssUrl);
                    }
                }

                Label titleLabel = new Label(alert.getTitle() != null ? alert.getTitle() : "");

                HBox lead = new HBox(10);

                switch (alert.getAlertType()) {
                    case CONFIRMATION -> {

                    }
                    case INFORMATION -> {

                    }
                    case WARNING -> {
                        loadLogo(alert, lead, "/icon/warning.png");
                    }

                    case ERROR -> {
                        loadLogo(alert, lead, "/icon/error.png");
                    }

                    default -> {

                    }
                }


                lead.getChildren().addAll(titleLabel);

                HeaderBar headerBar = new HeaderBar();
                headerBar.setLeading(lead);
                HeaderBar.setMargin(lead, new Insets(10, 0, 0, 12));
                HeaderBar.setDragType(lead, HeaderDragType.DRAGGABLE_SUBTREE);// 可拖拽


                BorderPane customRoot = new BorderPane();
                customRoot.setTop(headerBar);
                customRoot.setCenter(pane);
                paneScene.setRoot(customRoot);
            }
        });
    }

    private static void loadLogo(Alert alert, HBox lead, String iconPath) {

        Image image = new Image(Objects.requireNonNull(FXUIComponents.class.getResource(iconPath)).toExternalForm());
        ImageView imageView = new ImageView(image);
        imageView.setFitHeight(16);
        imageView.setFitWidth(16);
        lead.getChildren().add(imageView);

        setDialogIcon(alert, iconPath, null);
    }


    @SuppressWarnings("deprecation")
    private static void applyInitStyleToScene(Scene scene) {
        if (scene.getWindow() instanceof Stage stage) {
            if (!stage.isShowing()) {
                stage.initStyle(StageStyle.EXTENDED);
            }
        } else {
            scene.windowProperty().addListener((obs, o, w) -> {
                if (w instanceof Stage stage && !stage.isShowing()) {
                    stage.initStyle(StageStyle.EXTENDED);
                }
            });
        }
    }

    /**
     * 为 Dialog 设置图标(通用方法)
     * <p>
     * 使用监听器在对话框显示后获取 Stage 并设置图标
     *
     * @param dialog        对话框
     * @param iconPath      图标路径(相对于 resources 目录,例如："/icon/setting.png")
     * @param resourceClass 用于加载资源的类(如果为 null,使用 FXUIComponents.class)
     */
    public static void setDialogIcon(Dialog<?> dialog, String iconPath, Class<?> resourceClass) {
        dialog.showingProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue) { // 对话框正在显示
                Window window = dialog.getDialogPane().getScene().getWindow();
                if (window instanceof Stage stage) {
                    try {
                        Class<?> loader = resourceClass != null ? resourceClass : FXUIComponents.class;
                        var iconUrl = loader.getResource(iconPath);
                        if (iconUrl != null) {
                            stage.getIcons().add(new Image(iconUrl.toExternalForm()));
                        }
                    } catch (Exception e) {

                    }
                }
            }
        });
    }

}
