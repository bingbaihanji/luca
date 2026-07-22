package com.bingbaihanji.luca.plugin.audio.spectrum.utils.canvas;

import javafx.geometry.Point2D;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.layout.Region;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;

import java.util.List;

/**
 *
 * @author bingbaihanji
 * @date 2026-03-25 17:13:50
 * @description javafx 画布工具类
 */
public final class FXCanvasUtils {
    private FXCanvasUtils() {
        throw new UnsupportedOperationException("工具类不支持实例化");
    }

    /**
     * 绘制单个点（支持自定义颜色与可选圆环）
     */
    public static void drawPoint(GraphicsContext gc,
                                 double x,
                                 double y,
                                 double pointRadius,// 2.0 点半径
                                 double ringRadius, // 5.0 环半径
                                 double ringWidth, // 1.0 环宽度
                                 Paint paint, // 颜色
                                 boolean showRing) { // 是否显示环


        gc.setFill(paint);
        gc.fillOval(x - pointRadius, y - pointRadius, pointRadius * 2, pointRadius * 2);

        if (showRing) {
            gc.setLineWidth(ringWidth);
            // 若为 Color 则使用更暗的环色，否则直接使用原色
            gc.setStroke(paint instanceof Color color ? color.darker() : paint);
            gc.strokeOval(x - ringRadius, y - ringRadius, ringRadius * 2, ringRadius * 2);
        }
    }

    /**
     * 重载：接受 Point2D 参数
     */
    public static void drawPoint(GraphicsContext gc,
                                 Point2D point,
                                 double pointRadius,// 2.0 点半径
                                 double ringRadius, // 5.0 环半径
                                 double ringWidth, // 1.0 环宽度
                                 Paint paint,
                                 boolean showRing) {
        drawPoint(gc, point.getX(), point.getY(), pointRadius, ringRadius, ringWidth, paint, showRing);
    }

    /**
     * 批量绘制点（减少 GraphicsContext 状态切换）
     */
    public static void drawPoint(GraphicsContext gc,
                                 List<Point2D> points,
                                 double pointRadius,// 2.0 点半径
                                 double ringRadius, // 5.0 环半径
                                 double ringWidth, // 1.0 环宽度
                                 Paint paint,
                                 boolean showRing) {
        if (points == null || points.isEmpty()) {
            return;
        }

        // 统一设置填充色（所有点共用）
        gc.setFill(paint);

        // 若需要绘制环，预先设置描边样式
        if (showRing) {
            gc.setLineWidth(ringWidth);
            gc.setStroke(paint instanceof Color color ? color.darker() : paint);
        }

        for (Point2D point : points) {
            double x = point.getX();
            double y = point.getY();

            gc.fillOval(x - pointRadius, y - pointRadius, pointRadius * 2, pointRadius * 2);

            if (showRing) {
                gc.strokeOval(x - ringRadius, y - ringRadius, ringRadius * 2, ringRadius * 2);
            }
        }
    }

    // 核心绘制逻辑
    private static void drawLineImpl(GraphicsContext gc, Paint paint, double lineWidth,
                                     double x1, double y1, double x2, double y2) {
        gc.setStroke(paint);
        gc.setLineWidth(lineWidth);
        gc.strokeLine(x1, y1, x2, y2);
    }

    // 公共方法：坐标形式
    public static void drawLine(GraphicsContext gc, Paint paint, double lineWidth,
                                double x1, double y1, double x2, double y2) {
        if (gc == null || paint == null) {
            return; // 或抛出异常
        }
        drawLineImpl(gc, paint, lineWidth, x1, y1, x2, y2);
    }

    // 公共方法：Point2D 形式
    public static void drawLine(GraphicsContext gc, Paint paint, double lineWidth,
                                Point2D start, Point2D end) {
        drawLine(gc, paint, lineWidth, start.getX(), start.getY(), end.getX(), end.getY());
    }

    /**
     * 批量绘制线段（连续路径）
     */
    public static void drawLines(GraphicsContext gc,
                                 Paint paint,
                                 double lineWidth,
                                 List<Point2D> points) {

        if (gc == null || paint == null || points == null || points.size() < 2) {
            return;
        }

        gc.setStroke(paint);
        gc.setLineWidth(lineWidth);

        Point2D first = points.get(0);
        gc.beginPath();
        gc.moveTo(first.getX(), first.getY());

        for (int i = 1; i < points.size(); i++) {
            Point2D p = points.get(i);
            gc.lineTo(p.getX(), p.getY());
        }

        gc.stroke();
    }


    // 根据坐标位置按顺序绘制平滑曲线 使用三次贝塞尔曲线平滑绘制
    public static void drawLinesBezierListPoint(GraphicsContext gc, Paint paint,
                                                double lineWidth, List<Point2D> pointList) {
        if (pointList.size() < 2) {
            return;
        }
        gc.setStroke(paint);
        gc.setLineWidth(lineWidth);

        // 开始绘制
        gc.beginPath();
        Point2D first = pointList.get(0);
        gc.moveTo(first.getX(), first.getY());

        for (int i = 0; i < pointList.size() - 1; i++) {
            Point2D p0 = i > 0 ? pointList.get(i - 1) : pointList.get(i);
            Point2D p1 = pointList.get(i);
            Point2D p2 = pointList.get(i + 1);
            Point2D p3 = (i + 2 < pointList.size()) ? pointList.get(i + 2) : p2;
            drawLineBezier(
                    gc,
                    p0.getX(), p0.getY(),
                    p1.getX(), p1.getY(),
                    p2.getX(), p2.getY(),
                    p3.getX(), p3.getY()
            );

        }
        gc.stroke();
    }

    // 绘制背景Region
    public static void drawBackground(GraphicsContext gc, Region region, Paint paint) {
        gc.setFill(paint);
        gc.fillRect(0, 0, region.getWidth(), region.getHeight());
    }


    // 绘制背景，默认颜色为灰色
    public static void drawBackground(GraphicsContext gc, Region region) {
        drawBackground(gc, region, Color.GRAY);
    }


    /**
     * 在指定区域绘制网格（支持子网格）
     *
     * @param gc          图形上下文
     * @param paint       网格颜色
     * @param region      绘制区域
     * @param gridWidth   主网格宽度
     * @param gridHeight  主网格高度
     * @param showSubGrid 是否绘制子网格（每格细分为5份）
     */
    public static void drawGrid(GraphicsContext gc,
                                Paint paint,
                                Region region,
                                boolean showSubGrid,
                                double gridWidth,
                                double gridHeight
    ) {

        double width = region.getWidth();
        double height = region.getHeight();

        //主网格
        gc.setStroke(paint);
        gc.setLineWidth(1.0);

        for (double x = 0; x < width; x += gridWidth) {
            gc.strokeLine(x, 0, x, height);
        }
        for (double y = 0; y < height; y += gridHeight) {
            gc.strokeLine(0, y, width, y);
        }

        // 子网格
        if (showSubGrid) {
            double subGridWidth = gridWidth / 5.0;
            double subGridHeight = gridHeight / 5.0;

            // 可选：子网格更浅（如果 paint 是 Color）
            if (paint instanceof Color color) {
                gc.setStroke(new Color(
                        color.getRed(),
                        color.getGreen(),
                        color.getBlue(),
                        0.3 // 透明度降低
                ));
            } else {
                gc.setStroke(paint);
            }

            gc.setLineWidth(0.5);

            // 垂直子网格（跳过主网格线位置）
            for (double x = 0; x < width; x += subGridWidth) {
                if (Math.abs(x % gridWidth) < 0.0001) {
                    continue;
                }
                gc.strokeLine(x, 0, x, height);
            }

            // 水平子网格
            for (double y = 0; y < height; y += subGridHeight) {
                if (Math.abs(y % gridHeight) < 0.0001) {
                    continue;
                }
                gc.strokeLine(0, y, width, y);
            }
        }
    }


    // 使用 Catmull-Rom 转 Bezier 绘制平滑曲线
    private static void drawLineBezier(GraphicsContext gc,
                                       double x0, double y0,
                                       double x1, double y1,
                                       double x2, double y2,
                                       double x3, double y3) {

        double cp1x = x1 + (x2 - x0) / 6.0;
        double cp1y = y1 + (y2 - y0) / 6.0;

        double cp2x = x2 - (x3 - x1) / 6.0;
        double cp2y = y2 - (y3 - y1) / 6.0;

        gc.bezierCurveTo(
                cp1x, cp1y,
                cp2x, cp2y,
                x2, y2
        );
    }


}
