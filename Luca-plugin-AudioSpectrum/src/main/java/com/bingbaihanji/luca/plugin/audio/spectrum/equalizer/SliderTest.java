package com.bingbaihanji.luca.plugin.audio.spectrum.equalizer;//package com.bingbaihanji.fxequalizer;
//
//import javafx.application.Application;
//import javafx.geometry.Orientation;
//import javafx.scene.Scene;
//import javafx.scene.control.Slider;
//import javafx.scene.layout.AnchorPane;
//import javafx.stage.Stage;
//
/// **
// *
// * @author bingbaihanji
// * @date 2026-04-08 10:56:19
// * @description //TODO
// */
//public class SliderTest extends Application {
//
//    public static void main(String[] args) {
//        launch(args);
//    }
//
//    @Override
//    public void start(Stage primaryStage) throws Exception {
//        AnchorPane root = new AnchorPane();
//        // 灰色背景
//        root.setStyle("-fx-background-color: gray;");
//        // 设置窗口标题
//        primaryStage.setTitle("Slider Test");
//
//
//        Slider slider = new Slider();
//        slider.setPrefWidth(200);
//        slider.setMin(0);
//        slider.setMax(100);
//        slider.setValue(50);
//        // 步长
//        slider.setMajorTickUnit(5);
//
//        // 启用/禁用 步长吸附（滑块自动贴合刻度）
//        slider.setSnapToTicks(true);
//        // 显示主刻度线
//        slider.setShowTickMarks(true);
//
//        // 显示刻度数值标签
//        slider.setShowTickLabels(true);
//
//        // 设置次要刻度数量（主刻度之间的小刻度）
//        slider.setMinorTickCount(5);
//
//        // 设置为垂直滑动条（默认水平）
//        slider.setOrientation(Orientation.HORIZONTAL);
//
//        slider.setStyle("""
//
//                """);
//
//
//        // 监听滑块数值变化（用户拖动/代码设置值都会触发）
//        slider.valueProperty().addListener((observable, oldValue, newValue) -> {
//            System.out.println("Slider value changed: " + newValue);
//        });
//
//
//        AnchorPane.setLeftAnchor(slider, 20.0);
//        AnchorPane.setTopAnchor(slider, 20.0);
//
//        root.getChildren().add(slider);
//        primaryStage.setScene(new Scene(root,800,600));
//        primaryStage.show();
//    }
//}
