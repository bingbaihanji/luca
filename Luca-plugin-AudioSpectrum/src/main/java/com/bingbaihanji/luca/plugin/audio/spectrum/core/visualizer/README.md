# 音频可视化组件库

独立的、可复用的 JavaFX 音频可视化组件，完全基于原 XR3Player 项目重构而成。

## 项目概述

这是一套完全独立的音频可视化组件，无需依赖原项目即可在任何 JavaFX 应用中使用。

### 核心特性

- ✅ **完全独立** - 无外部依赖，可在任何项目中使用
- ✅ **JavaFX 原生** - 继承 `Region`，完美支持 FXML、CSS、属性绑定
- ✅ **简洁 API** - 直接传入 `float[]` 音频数据
- ✅ **高性能** - FPS 控制、预计算表、对象复用
- ✅ **5种可视化效果** - 覆盖常见使用场景

## 包结构

```
com.audio.visualizer/
├── core/                          # 核心基础设施
│   ├── AudioData.java             # 音频数据封装
│   └── AudioVisualizer.java       # 抽象基类
│
├── processor/                     # 数据处理层
│   ├── FFTProcessor.java          # FFT 接口
│   ├── SimpleFFT.java             # FFT 实现
│   └── SpectrumAnalyzer.java      # 频谱分析器
│
├── view/                       # 可视化效果
│   ├── WaveformVisualizer.java    # 波形（示波器）
│   ├── SpectrumBarsVisualizer.java # 频谱条形图
│   ├── CircularSpectrumVisualizer.java # 圆形频谱
│   ├── StereoMeterVisualizer.java # 立体声电平表
│   └── SpiralVisualizer.java      # 螺旋粒子
│
├── util/                          # 工具类
│   ├── ColorGradient.java         # 颜色渐变
│   └── PeakDetector.java          # 峰值检测
│
└── test/                          # 测试代码
    └── AudioVisualizerTest.java   # 综合测试
```

## 快速开始

### 1. 基本用法

```java
// 创建可视化器
SpectrumBarsVisualizer visualizer = new SpectrumBarsVisualizer();
visualizer.

setPrefHeight(300);

// 配置参数
visualizer.

setBandCount(50);
visualizer.

setShowPeaks(true);

// 添加到界面
pane.

getChildren().

add(visualizer);

// 启动可视化
visualizer.

start();

// 更新音频数据（在音频线程中调用）
visualizer.

updateAudioData(leftChannel, rightChannel);
```

### 2. FXML 集成

```xml
<?xml version="1.0" encoding="UTF-8"?>
<?import com.audio.visualizer.view.*?>
<?import javafx.scene.layout.*?>

<VBox xmlns:fx="http://javafx.com/fxml">
    <WaveformVisualizer fx:id="waveform"
                        prefHeight="150"
                        stereoMode="true"/>

    <SpectrumBarsVisualizer fx:id="spectrum"
                            prefHeight="300"
                            bandCount="50"
                            showPeaks="true"/>
</VBox>
```

### 3. 属性绑定

```java
// 绑定到滑块
Slider fpsSlider = new Slider(30, 120, 60);
visualizer.

targetFPSProperty().

bind(fpsSlider.valueProperty());

// 绑定到颜色选择器
ColorPicker colorPicker = new ColorPicker(Color.CYAN);
waveform.

waveColorProperty().

bind(colorPicker.valueProperty());

// 绑定到复选框
CheckBox peaksCheck = new CheckBox("显示峰值");
spectrum.

showPeaksProperty().

bind(peaksCheck.selectedProperty());
```

## 5种可视化效果

### 1. WaveformVisualizer（波形可视化）

最直观的音频表示，显示实时波形。

```java
WaveformVisualizer waveform = new WaveformVisualizer();
waveform.setStereoMode(true);          // 立体声模式
waveform.setWaveColor(Color.CYAN);     // 波形颜色
waveform.setLineWidth(1.5);            // 线宽
waveform.setRainbowMode(false);        // 彩虹模式
```

**适用场景**：音频监控、调试、录音软件

### 2. SpectrumBarsVisualizer（频谱条形图）

经典的频谱分析仪，带峰值指示和渐变色。

```java
SpectrumBarsVisualizer spectrum = new SpectrumBarsVisualizer();
spectrum.

setBandCount(50);             // 频段数
spectrum.

setFFTSize(512);              // FFT 大小
spectrum.

setDecay(0.05);               // 衰减率
spectrum.

setShowPeaks(true);           // 显示峰值
spectrum.

setBarSpacing(2);             // 条形间距
```

**适用场景**：音乐播放器、DJ 软件、音频编辑器

### 3. CircularSpectrumVisualizer（圆形频谱）

炫酷的圆形放射状频谱。

```java
CircularSpectrumVisualizer circular = new CircularSpectrumVisualizer();
circular.setBarCount(360);             // 线条数量
circular.setInnerRadius(0.3);          // 内圈半径
circular.setRainbowMode(true);         // 彩虹模式
circular.setBidirectional(true);       // 双向显示
```

**适用场景**：桌面小部件、可视化背景、嵌入式显示

### 4. StereoMeterVisualizer（立体声电平表）

专业的 VU 表，清晰显示左右声道电平。

```java
StereoMeterVisualizer meter = new StereoMeterVisualizer();
meter.

setOrientation(Orientation.HORIZONTAL); // 方向
meter.

setDecay(0.02);                  // 衰减速度
meter.

setShowScale(true);              // 显示刻度
meter.

setMeterColor(Color.LIME);       // 电平表颜色
```

**适用场景**：录音软件、混音器、专业音频工具

### 5. SpiralVisualizer（螺旋可视化）

艺术化的粒子和玫瑰线效果。

```java
SpiralVisualizer spiral = new SpiralVisualizer();
spiral.setParticleDensity(50);         // 粒子密度
spiral.setRosetteVertices(6);          // 玫瑰线顶点数
spiral.setRainbowMode(true);           // 彩虹模式
```

**适用场景**：可视化背景、装饰动画、艺术展示

## 运行测试

### 方法一：命令行运行

```bash
cd D:\javaProject\bingbaihanji\javaui\XR3Player
mvn compile exec:java -Dexec.mainClass="com.audio.visualizer.test.AudioVisualizerTest"
```

### 方法二：IDE 运行

1. 打开 `AudioVisualizerTest.java`
2. 运行 `main` 方法

### 方法三：直接运行编译后的类

```bash
java -cp target/classes com.audio.visualizer.test.AudioVisualizerTest
```

## 实际使用示例

### 集成到音频播放器

```java
public class MyAudioPlayer {
    private SpectrumBarsVisualizer visualizer;
    private SourceDataLine audioLine;

    public void setupVisualizer() {
        visualizer = new SpectrumBarsVisualizer();
        visualizer.start();

        // 在音频播放线程中
        new Thread(() -> {
            byte[] buffer = new byte[2048];
            while (playing) {
                int bytesRead = audioLine.read(buffer, 0, buffer.length);

                // 转换为 float 数组
                float[] left = convertToFloat(buffer, 0);   // 左声道
                float[] right = convertToFloat(buffer, 1);  // 右声道

                // 更新可视化
                visualizer.updateAudioData(left, right);
            }
        }).start();
    }

    private float[] convertToFloat(byte[] buffer, int channel) {
        // PCM 转换逻辑
        // ...
    }
}
```

### 集成到录音软件

```java
public class AudioRecorder {
    private WaveformVisualizer waveform;
    private StereoMeterVisualizer meter;

    public void startRecording() {
        TargetDataLine line = AudioSystem.getTargetDataLine(format);
        line.open();
        line.start();

        new Thread(() -> {
            byte[] buffer = new byte[1024];
            while (recording) {
                int bytesRead = line.read(buffer, 0, buffer.length);

                float[] left = convertToFloat(buffer, 0);
                float[] right = convertToFloat(buffer, 1);

                // 同时更新波形和电平表
                waveform.updateAudioData(left, right);
                meter.updateAudioData(left, right);
            }
        }).start();
    }
}
```

## 性能优化建议

1. **控制更新频率**：不要超过 120 FPS
2. **合理的 FFT 大小**：512 或 1024 通常足够
3. **适当的频段数**：30-80 个频段效果最佳
4. **使用对象池**：高频更新时可考虑复用数组

## 常见问题

### Q: 如何从 WAV/MP3 文件获取音频数据？

A: 使用 Java Sound API 或第三方库（如 JLayer）：

```java
AudioInputStream stream = AudioSystem.getAudioInputStream(file);
byte[] buffer = new byte[2048];
int bytesRead = stream.read(buffer);
// 转换为 float[] 并传入可视化器
```

### Q: 可视化器不显示？

A: 检查以下几点：

1. 是否调用了 `start()` 方法
2. 是否持续调用 `updateAudioData()`
3. 画布尺寸是否正确设置
4. 是否将可视化器添加到场景图

### Q: 如何自定义颜色？

A: 所有可视化器都支持颜色自定义：

```java
// 使用内置颜色渐变
spectrum.setColorGradient(ColorGradient.createFireGradient());

// 自定义渐变
ColorGradient custom = ColorGradient.createGradient(
        Color.BLUE, Color.RED, 256
);
spectrum.

setColorGradient(custom);
```

## 技术细节

### FFT 实现

使用 Cooley-Tukey 基-2 FFT 算法，支持任意 2^n 大小的采样。

### 衰减算法

采用平滑衰减确保动画流畅：

```java
if (newValue >= oldValue - decay)
    oldValue = newValue;
else
    oldValue -= decay;
```

### 峰值检测

峰值会保持一定帧数后缓慢下降，提供更好的视觉反馈。
