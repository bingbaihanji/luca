package com.bingbaihanji.luca.plugin.audio.spectrum.core.capture.view.spectrogram;

import com.bingbaihanji.luca.plugin.audio.spectrum.utils.color.CMRMap;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.canvas.Canvas;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;

import java.util.Arrays;
import java.util.List;
import java.util.function.IntConsumer;

/**
 * Spectrogram Widget 的 View（渲染层）
 * 包含频谱图主体和右侧颜色指示条
 * dB 值      颜色
 * ━━━━━━━━━━━━━━━━━━━━━━━━━━━
 * -60dB   深蓝紫色（静音） minDB
 * -40dB   紫色
 * -20dB   红色/橙色
 * 0dB     黄色/白色（峰值）
 */
public class SpectrogramView extends BorderPane {

    private final ImageView imageView = new ImageView();
    private final Canvas colorbarCanvas = new Canvas(60, 100);
    private final javafx.scene.canvas.GraphicsContext colorbarGC = colorbarCanvas.getGraphicsContext2D();

    private volatile boolean isDirty = true;

    // 宽度变化回调（由 Widget 设置）
    private IntConsumer onWidthChanged = (width) -> {
    };

    public SpectrogramView(SpectrogramViewModel viewModel) {
        // 使用更灵活的高度设置
        setMinHeight(200);
        setPrefHeight(280);

        setPadding(new Insets(10));
        setBackground(new Background(
                new BackgroundFill(Color.BLACK, new CornerRadii(8), Insets.EMPTY)
        ));

        // 主频谱图区域 - 使用 StackPane 确保图像完全填充
        StackPane imageContainer = new StackPane(imageView);
        HBox.setHgrow(imageContainer, Priority.ALWAYS);
        VBox.setVgrow(imageContainer, Priority.ALWAYS);
        imageContainer.setMinHeight(0);
        imageContainer.setBackground(new Background(
                new BackgroundFill(Color.BLACK, new CornerRadii(6), Insets.EMPTY)
        ));

        // 颜色指示条容器
        VBox colorbarContainer = new VBox();
        colorbarContainer.getChildren().add(colorbarCanvas);
        colorbarContainer.setAlignment(Pos.CENTER_RIGHT);
        colorbarContainer.setPadding(new Insets(4, 10, 4, 8));
        colorbarContainer.setMinHeight(0);

        // 布局：左侧主图，右侧 colorbar
        setCenter(imageContainer);
        setRight(colorbarContainer);

        // 图像填充整个区域，拉伸适应容器
        imageView.setPreserveRatio(false);
        imageView.setSmooth(false);  // 关闭平滑以提高性能
        imageView.fitWidthProperty().bind(imageContainer.widthProperty());
        imageView.fitHeightProperty().bind(imageContainer.heightProperty());

        // colorbar 高度绑定到 imageContainer 高度
        colorbarCanvas.heightProperty().bind(imageContainer.heightProperty().subtract(8));

        // 监听高度变化，重绘 colorbar
        colorbarCanvas.heightProperty().addListener((obs, oldVal, newVal) -> drawColorbar());

        // 监听容器宽度变化，通知 Widget 调整图像宽度
        imageContainer.widthProperty().addListener((obs, oldVal, newVal) -> {
            isDirty = true;
            if (newVal.doubleValue() > 100) {
                onWidthChanged.accept(newVal.intValue());
            }
        });

        // 监听高度变化
        heightProperty().addListener((obs, oldVal, newVal) -> {
            isDirty = true;
            render();
        });
    }

    /**
     * 设置宽度变化回调
     */
    public void setOnWidthChanged(IntConsumer callback) {
        this.onWidthChanged = callback;
    }

    /**
     * 绘制右侧颜色指示条
     */
    private void drawColorbar() {
        double width = colorbarCanvas.getWidth();
        double height = colorbarCanvas.getHeight();

        if (height < 50) {
            return;  // 高度太小时不绘制
        }

        double minDB = -60.0;
        double maxDB = 0.0;

        // 清空画布
        colorbarGC.clearRect(0, 0, width, height);

        // 绘制颜色渐变条
        double barWidth = 30;
        double barX = 5;
        double margin = 10;
        double barY = margin;
        double barHeight = height - 2 * margin;

        // 从上到下绘制颜色条（高 dB 在上，低 dB 在下）
        for (int i = 0; i < (int) barHeight; i++) {
            double value = 1.0 - i / barHeight;  // 从 1.0 (top) 到 0.0 (bottom)
            Color color = CMRMap.getColor(value);

            colorbarGC.setStroke(color);
            colorbarGC.strokeLine(barX, barY + i, barX + barWidth, barY + i);
        }

        // 绘制边框
        colorbarGC.setStroke(Color.WHITE);
        colorbarGC.setLineWidth(1);
        colorbarGC.strokeRect(barX, barY, barWidth, barHeight);

        // 绘制刻度标签
        colorbarGC.setFill(Color.WHITE);
        colorbarGC.setFont(new Font("Monospace", 10));

        List<Double> ticks = Arrays.asList(maxDB, -20.0, -40.0, minDB);
        for (double db : ticks) {
            double normalized = (db - minDB) / (maxDB - minDB);
            double y = barY + barHeight * (1.0 - normalized);

            // 刻度线
            colorbarGC.strokeLine(barX + barWidth, y, barX + barWidth + 5, y);

            // 刻度文字
            colorbarGC.fillText(String.valueOf((int) db), barX + barWidth + 8, y + 4);
        }
    }

    public void render() {
        // 图像更新由 Widget 控制，这里只返回脏标记状态
        // 实际图像设置通过 setImage() 方法
    }

    public void setImage(javafx.scene.image.Image image) {
        imageView.setImage(image);
        isDirty = false;
    }

    public void markDirty() {
        isDirty = true;
    }

    public ImageView getImageView() {
        return imageView;
    }

    public boolean isDirty() {
        return isDirty;
    }

    public void clearDirty() {
        isDirty = false;
    }
}
