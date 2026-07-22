package com.bingbaihanji.luca.plugin.audio.spectrum.core.visualizer.view;

import com.bingbaihanji.luca.plugin.audio.spectrum.core.visualizer.core.AudioData;
import com.bingbaihanji.luca.plugin.audio.spectrum.core.visualizer.core.AudioVisualizer;
import javafx.beans.property.*;
import javafx.geometry.Orientation;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

/**
 * 立体声电平表可视化器（VU 表）。
 *
 * <p>分别显示左右声道的平均绝对值电平，带指数衰减平滑动画。
 * 支持水平和垂直两种布局方向，可选显示 L/R 标签。
 *
 * @author bingbaihanji
 */
public class StereoMeterVisualizer extends AudioVisualizer {

    // 配置属性
    private final ObjectProperty<Orientation> orientation = new SimpleObjectProperty<>(Orientation.HORIZONTAL);
    private final DoubleProperty decay = new SimpleDoubleProperty(0.02);
    private final BooleanProperty showScale = new SimpleBooleanProperty(true);
    private final ObjectProperty<Color> meterColor = new SimpleObjectProperty<>(Color.LIME);

    // 电平平滑状态（指数衰减）
    private float smoothLeft = 0;
    private float smoothRight = 0;

    public StereoMeterVisualizer() {
        super();
    }

    @Override
    protected void render(GraphicsContext gc, AudioData data) {
        double width = getCanvasWidth();
        double height = getCanvasHeight();

        if (width <= 0 || height <= 0) {
            return;
        }

        // 计算左右声道平均绝对值电平，归一化到 [0, 1]
        float rawLeft = computeLevel(data.getLeftChannel());
        float rawRight = computeLevel(data.getRightChannel());

        // 指数衰减：新值大则跟随上升，新值小则缓慢下降
        float decayAmount = (float) decay.get();
        smoothLeft = rawLeft >= smoothLeft - decayAmount
                ? rawLeft : Math.max(0, smoothLeft - decayAmount);
        smoothRight = rawRight >= smoothRight - decayAmount
                ? rawRight : Math.max(0, smoothRight - decayAmount);

        if (orientation.get() == Orientation.HORIZONTAL) {
            drawHorizontalMeters(gc, width, height);
        } else {
            drawVerticalMeters(gc, width, height);
        }
    }

    /**
     * 计算声道的平均绝对值电平，结果限制在 [0, 1]。
     */
    private float computeLevel(float[] samples) {
        float sum = 0;
        for (float s : samples) {
            sum += Math.abs(s);
        }
        return Math.min((sum * 2.0f) / samples.length, 1.0f);
    }

    // ── 绘制方法 ──────────────────────────────────────────────────

    private void drawHorizontalMeters(GraphicsContext gc, double width, double height) {
        int barHeight = (int) (height / 2) - 5;
        int leftY = 2;
        int rightY = barHeight + 8;

        drawHorizontalBar(gc, 2, leftY, (int) (smoothLeft * (width - 4)), barHeight);
        drawHorizontalBar(gc, 2, rightY, (int) (smoothRight * (width - 4)), barHeight);

        if (showScale.get()) {
            gc.setFill(Color.WHITE);
            gc.fillText("L", 4, leftY + 12);
            gc.fillText("R", 4, rightY + 12);
        }
    }

    private void drawVerticalMeters(GraphicsContext gc, double width, double height) {
        int barWidth = (int) (width / 2) - 5;
        int leftX = 4;
        int rightX = barWidth + 8;

        drawVerticalBar(gc, leftX, (int) height, barWidth, (int) (smoothLeft * height));
        drawVerticalBar(gc, rightX, (int) height, barWidth, (int) (smoothRight * height));

        if (showScale.get()) {
            gc.setFill(Color.WHITE);
            gc.fillText("L", leftX + 2, height - 4);
            gc.fillText("R", rightX + 2, height - 4);
        }
    }

    /**
     * 绘制水平电平条（分段式，每段 2px 宽，带刻度线）。
     */
    private void drawHorizontalBar(GraphicsContext gc, int x, int y, int barWidth, int barHeight) {
        if (barWidth <= 0) {
            return;
        }
        gc.setStroke(meterColor.get());
        for (int xPos = x; xPos < x + barWidth; xPos += 2) {
            gc.strokeRect(xPos, y, 2, barHeight);
        }
        if (showScale.get()) {
            gc.setStroke(Color.BLACK);
            for (int xPos = x; xPos < x + barWidth; xPos += 15) {
                gc.strokeLine(xPos, y, xPos, y + barHeight);
            }
        }
    }

    /**
     * 绘制垂直电平条（分段式，每段 2px 高，带刻度线）。
     */
    private void drawVerticalBar(GraphicsContext gc, int x, int y, int barWidth, int barHeight) {
        if (barHeight <= 0) {
            return;
        }
        gc.setStroke(meterColor.get());
        for (int yPos = y; yPos > y - barHeight; yPos -= 2) {
            gc.strokeRect(x, yPos, barWidth, 2);
        }
        if (showScale.get()) {
            gc.setStroke(Color.BLACK);
            for (int yPos = y; yPos > y - barHeight; yPos -= 15) {
                gc.strokeLine(x, yPos, x + barWidth, yPos);
            }
        }
    }

    // ── 属性访问器 ────────────────────────────────────────────────

    public ObjectProperty<Orientation> orientationProperty() {
        return orientation;
    }

    public Orientation getOrientation() {
        return orientation.get();
    }

    public void setOrientation(Orientation orient) {
        if (orient != null) {
            orientation.set(orient);
        }
    }

    public DoubleProperty decayProperty() {
        return decay;
    }

    public double getDecay() {
        return decay.get();
    }

    public void setDecay(double rate) {
        decay.set(Math.max(0.0, Math.min(1.0, rate)));
    }

    public BooleanProperty showScaleProperty() {
        return showScale;
    }

    public boolean isShowScale() {
        return showScale.get();
    }

    public void setShowScale(boolean show) {
        showScale.set(show);
    }

    public ObjectProperty<Color> meterColorProperty() {
        return meterColor;
    }

    public Color getMeterColor() {
        return meterColor.get();
    }

    public void setMeterColor(Color color) {
        if (color != null) {
            meterColor.set(color);
        }
    }
}
