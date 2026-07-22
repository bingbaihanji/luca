package com.bingbaihanji.luca.plugin.audio.spectrum.core.visualizer.builder;

import com.bingbaihanji.luca.plugin.audio.spectrum.core.visualizer.core.AudioVisualizer;
import javafx.scene.paint.Color;

import javax.sound.sampled.*;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

/**
 * 音频可视化器构建器
 * <p>
 * 提供流畅的链式 API 来构建和配置音频可视化器。
 * 支持通过类名或 Class 对象动态创建 AudioVisualizer 的任意实现类。
 * </p>
 *
 * <p>使用示例：</p>
 * <pre>{@code
 * // 使用类名创建
 * VisualizerKit kit = VisualizerKitBuilder.create()
 *     .audioFile(new File("audio.wav"))
 *     .visualizerClass("com.bingbaihanji.audio.visualizer.effects.CircularSpectrumVisualizer")
 *     .size(800, 600)
 *     .targetFPS(100)
 *     .backgroundColor(Color.rgb(20, 20, 30))
 *     .property("innerRadius", 0.618)
 *     .property("rainbowMode", true)
 *     .build();
 *
 * // 使用 Class 对象创建
 * VisualizerKit kit = VisualizerKitBuilder.create()
 *     .audioFile(new File("audio.wav"))
 *     .visualizerClass(WaveformVisualizer.class)
 *     .size(800, 600)
 *     .property("stereoMode", true)
 *     .property("rainbowMode", true)
 *     .build();
 * }</pre>
 *
 * @author bingbaihanji
 * @date 2026-03-21
 */
public class VisualizerKitBuilder {

    // 自定义属性（用于配置特定可视化器的属性）
    private final Map<String, Object> customProperties = new HashMap<>();
    // 必需参数
    private File audioFile;
    private Class<? extends AudioVisualizer> visualizerClass;
    private String visualizerClassName;
    // 可选的音频控制器
    private AudioController controller;
    // 通用可视化器配置
    private double width = 400;
    private double height = 400;
    private double minWidth = 50;
    private double minHeight = 50;
    private int targetFPS = 60;
    private Color backgroundColor = Color.BLACK;
    private double backgroundOpacity = 1.0;

    /**
     * 创建一个新的构建器实例
     */
    private VisualizerKitBuilder() {
    }

    /**
     * 创建构建器的入口方法
     *
     * @return 新的构建器实例
     */
    public static VisualizerKitBuilder create() {
        return new VisualizerKitBuilder();
    }

    /**
     * 设置音频文件
     *
     * @param audioFile 音频文件
     * @return 当前构建器实例
     */
    public VisualizerKitBuilder audioFile(File audioFile) {
        this.audioFile = audioFile;
        return this;
    }

    /**
     * 通过文件路径设置音频文件
     *
     * @param audioFilePath 音频文件路径
     * @return 当前构建器实例
     */
    public VisualizerKitBuilder audioFile(String audioFilePath) {
        this.audioFile = new File(audioFilePath);
        return this;
    }

    /**
     * 设置可视化器类（通过 Class 对象）
     *
     * @param visualizerClass AudioVisualizer 实现类的 Class 对象
     * @return 当前构建器实例
     */
    public VisualizerKitBuilder visualizerClass(Class<? extends AudioVisualizer> visualizerClass) {
        this.visualizerClass = visualizerClass;
        this.visualizerClassName = null;
        return this;
    }

    /**
     * 设置可视化器类（通过类名字符串）
     *
     * @param className AudioVisualizer 实现类的完全限定类名
     * @return 当前构建器实例
     */
    public VisualizerKitBuilder visualizerClass(String className) {
        this.visualizerClassName = className;
        this.visualizerClass = null;
        return this;
    }

    /**
     * 设置音频控制器
     *
     * @param controller 音频控制器
     * @return 当前构建器实例
     */
    public VisualizerKitBuilder controller(AudioController controller) {
        this.controller = controller;
        return this;
    }

    /**
     * 设置可视化器的尺寸
     *
     * @param width  宽度
     * @param height 高度
     * @return 当前构建器实例
     */
    public VisualizerKitBuilder size(double width, double height) {
        this.width = width;
        this.height = height;
        return this;
    }

    /**
     * 设置可视化器的最小尺寸
     *
     * @param minWidth  最小宽度
     * @param minHeight 最小高度
     * @return 当前构建器实例
     */
    public VisualizerKitBuilder minSize(double minWidth, double minHeight) {
        this.minWidth = minWidth;
        this.minHeight = minHeight;
        return this;
    }

    /**
     * 设置目标帧率
     *
     * @param targetFPS 目标帧率
     * @return 当前构建器实例
     */
    public VisualizerKitBuilder targetFPS(int targetFPS) {
        this.targetFPS = targetFPS;
        return this;
    }

    /**
     * 设置背景颜色
     *
     * @param backgroundColor 背景颜色
     * @return 当前构建器实例
     */
    public VisualizerKitBuilder backgroundColor(Color backgroundColor) {
        this.backgroundColor = backgroundColor;
        return this;
    }

    /**
     * 设置背景透明度
     *
     * @param opacity 透明度 (0.0 到 1.0)
     * @return 当前构建器实例
     */
    public VisualizerKitBuilder backgroundOpacity(double opacity) {
        this.backgroundOpacity = opacity;
        return this;
    }

    /**
     * 设置自定义属性（用于配置特定可视化器的属性）
     * <p>
     * 例如：property("innerRadius", 0.618) 会调用 setInnerRadius(0.618)
     * </p>
     *
     * @param propertyName  属性名（对应 setter 方法名去掉 "set" 前缀）
     * @param propertyValue 属性值
     * @return 当前构建器实例
     */
    public VisualizerKitBuilder property(String propertyName, Object propertyValue) {
        customProperties.put(propertyName, propertyValue);
        return this;
    }

    /**
     * 构建 VisualizerKit 实例
     *
     * @return 配置好的 VisualizerKit
     * @throws IllegalStateException 如果必需参数未设置
     * @throws RuntimeException      如果构建过程中发生错误
     */
    public VisualizerKit build() {
        // 验证必需参数
        if (audioFile == null) {
            throw new IllegalStateException("音频文件未设置");
        }
        if (visualizerClass == null && visualizerClassName == null) {
            throw new IllegalStateException("可视化器类未设置");
        }

        try {
            // 解析可视化器类
            Class<? extends AudioVisualizer> clazz = resolveVisualizerClass();

            // 创建可视化器实例
            AudioVisualizer visualizer = clazz.getDeclaredConstructor().newInstance();

            // 配置通用属性
            configureCommonProperties(visualizer);

            // 配置自定义属性
            configureCustomProperties(visualizer);

            // 准备音频系统
            AudioInputStream audioInputStream = prepareAudioStream();
            AudioFormat format = audioInputStream.getFormat();
            SourceDataLine audioLine = prepareAudioLine(format);

            // 创建控制器（如果未提供）
            if (controller == null) {
                controller = new DefaultAudioController();
            }

            // 返回 VisualizerKit
            return new VisualizerKit(
                    visualizer,
                    audioInputStream,
                    audioLine,
                    controller
            );

        } catch (Exception e) {
            throw new RuntimeException("构建 VisualizerKit 失败: " + e.getMessage(), e);
        }
    }

    /**
     * 解析可视化器类
     */
    @SuppressWarnings("unchecked")
    private Class<? extends AudioVisualizer> resolveVisualizerClass() throws ClassNotFoundException {
        if (visualizerClass != null) {
            return visualizerClass;
        }

        // 通过类名加载
        Class<?> clazz = Class.forName(visualizerClassName);
        if (!AudioVisualizer.class.isAssignableFrom(clazz)) {
            throw new IllegalArgumentException(
                    "类 " + visualizerClassName + " 不是 AudioVisualizer 的子类"
            );
        }
        return (Class<? extends AudioVisualizer>) clazz;
    }

    /**
     * 配置通用属性
     */
    private void configureCommonProperties(AudioVisualizer visualizer) {
        visualizer.setPrefSize(width, height);
        visualizer.setMinWidth(minWidth);
        visualizer.setMinHeight(minHeight);
        visualizer.setTargetFPS(targetFPS);
        visualizer.setBackgroundColor(backgroundColor);
        visualizer.setBackgroundOpacity(backgroundOpacity);
    }

    /**
     * 配置自定义属性（通过反射调用 setter 方法）
     */
    private void configureCustomProperties(AudioVisualizer visualizer) {
        for (Map.Entry<String, Object> entry : customProperties.entrySet()) {
            String propertyName = entry.getKey();
            Object propertyValue = entry.getValue();

            try {
                // 构造 setter 方法名
                String setterName = "set" + Character.toUpperCase(propertyName.charAt(0))
                        + propertyName.substring(1);

                // 查找并调用 setter 方法
                Method setter = findSetter(visualizer.getClass(), setterName, propertyValue);
                if (setter != null) {
                    setter.invoke(visualizer, propertyValue);
                } else {
                    System.err.println("警告: 未找到属性 '" + propertyName + "' 的 setter 方法");
                }

            } catch (Exception e) {
                System.err.println("警告: 设置属性 '" + propertyName + "' 失败: " + e.getMessage());
            }
        }
    }

    /**
     * 查找合适的 setter 方法
     */
    private Method findSetter(Class<?> clazz, String setterName, Object value) {
        Class<?> valueClass = value.getClass();

        // 尝试精确匹配
        try {
            return clazz.getMethod(setterName, valueClass);
        } catch (NoSuchMethodException e) {
            // 继续尝试其他匹配方式
        }

        // 尝试基本类型匹配（例如 Integer -> int）
        try {
            switch (value) {
                case Integer i -> {
                    return clazz.getMethod(setterName, int.class);
                }
                case Double v -> {
                    return clazz.getMethod(setterName, double.class);
                }
                case Boolean b -> {
                    return clazz.getMethod(setterName, boolean.class);
                }
                case Float v -> {
                    return clazz.getMethod(setterName, float.class);
                }
                case Long l -> {
                    return clazz.getMethod(setterName, long.class);
                }
                default -> {
                }
            }
        } catch (NoSuchMethodException e) {
            // 继续尝试其他匹配方式
        }

        // 尝试查找任何名称匹配的单参数方法
        for (Method method : clazz.getMethods()) {
            if (method.getName().equals(setterName) && method.getParameterCount() == 1) {
                Class<?> paramType = method.getParameterTypes()[0];
                if (paramType.isAssignableFrom(valueClass)) {
                    return method;
                }
            }
        }

        return null;
    }

    /**
     * 准备音频流
     */
    private AudioInputStream prepareAudioStream() throws UnsupportedAudioFileException, IOException {
        AudioInputStream original = AudioSystem.getAudioInputStream(audioFile);
        AudioFormat format = original.getFormat();

        // 转换为标准 PCM 格式 (16-bit, little-endian)
        AudioFormat decodedFormat = new AudioFormat(
                AudioFormat.Encoding.PCM_SIGNED,
                format.getSampleRate(),
                16,
                format.getChannels(),
                format.getChannels() * 2,
                format.getSampleRate(),
                false
        );

        return AudioSystem.getAudioInputStream(decodedFormat, original);
    }

    /**
     * 准备音频输出线路
     */
    private SourceDataLine prepareAudioLine(AudioFormat format) throws LineUnavailableException {
        DataLine.Info info = new DataLine.Info(SourceDataLine.class, format);
        SourceDataLine line = (SourceDataLine) AudioSystem.getLine(info);
        line.open(format);
        line.start();
        return line;
    }
}
