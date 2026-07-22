# com.bingbaihanji.utils - 通用工具包

此包包含音频可视化项目中使用的通用工具类和算法。

## 包结构

### 1. com.bingbaihanji.utils.math.fft - FFT 快速傅里叶变换

提供 FFT 算法的接口和实现：

- `FFTProcessor` - FFT 处理器接口
- `CooleyTukeyFFT` - Cooley-Tukey 算法的 FFT 实现

**使用示例：**

```java
// 创建 FFT 处理器（样本大小必须是 2 的幂）
FFTProcessor fft = new CooleyTukeyFFT(1024);

// 执行 FFT
float[] samples = ...; // 时域音频数据
float[] spectrum = fft.calculate(samples); // 频域数据
```

### 2. com.bingbaihanji.utils.signal - 信号处理算法

提供各种信号处理功能：

- `SpectrumAnalyzer` - 频谱分析器（FFT + 频带分组 + 平滑）
- `PeakDetector` - 峰值检测器
- `SignalProcessing` - 信号处理工具类（平滑、归一化、dB 转换等）

**使用示例：**

```java
// 创建频谱分析器
SpectrumAnalyzer analyzer = new SpectrumAnalyzer(1024, 64, 0.3f, 48000.0);

// 分析音频样本
float[] bands = analyzer.analyze(samples);

// 查找峰值频率
float[] fftData = ...;
double peakFreq = analyzer.findPeakFrequency(fftData, 20.0, 20000.0);
```

### 3. com.bingbaihanji.utils.audio - 音频处理工具

提供音频相关的工具方法：

- `AudioFormatUtils` - PCM 数据解析、音频格式转换、RMS 计算

**使用示例：**

```java
// 解析 PCM 数据
byte[] pcmData = ...;
AudioFormat format = ...;
float[] left = new float[sampleCount];
float[] right = new float[sampleCount];
AudioFormatUtils.

decodeStereo(pcmData, pcmData.length, format, left, right);

// 计算 RMS
float rms = AudioFormatUtils.calculateRMS(samples);

// 字节数组转 float
float[] samples = AudioFormatUtils.bytesToFloatArray(pcmData, pcmData.length);
```

### 4. com.bingbaihanji.utils.color - 颜色处理工具

提供颜色相关的工具类：

- `ColorGradient` - 颜色渐变
- `CMRMap` - CMRMap 配色方案（热图样式）

**使用示例：**

```java
// 创建渐变
ColorGradient fire = ColorGradient.createFireGradient();
ColorGradient rainbow = ColorGradient.createRainbowGradient(360);
ColorGradient blue = ColorGradient.createBlueGradient();

// 获取颜色
Color c = fire.getColor(0.5); // 位置 0.5 处的颜色

// CMRMap（热图）
Color heat = CMRMap.getColor(0.8);
```

### 5. com.bingbaihanji.utils.datastructure - 数据结构

提供通用的数据结构实现：

- `RingBuffer` - 线程安全的环形缓冲区（双缓冲优化）

**使用示例：**

```java
// 创建环形缓冲区
RingBuffer buffer = new RingBuffer(4096);

// 写入数据
buffer.

push(samples, timestamp);

// 读取最新数据
double[] latest = buffer.data(1024);

// 读取历史数据（延迟 100 个样本）
double[] delayed = buffer.dataOlder(1024, 100);
```

## 迁移指南

旧包中的类已标记为 `@Deprecated`，但保留了向后兼容性：

| 旧包                                                             | 新包                                                                                                  |
|----------------------------------------------------------------|-----------------------------------------------------------------------------------------------------|
| `com.bingbaihanji.audio.visualizer.processor.FFTProcessor`     | `com.bingbaihanji.utils.math.fft.FFTProcessor`                                                      |
| `com.bingbaihanji.audio.visualizer.processor.SimpleFFT`        | `com.bingbaihanji.utils.math.fft.CooleyTukeyFFT`                                                    |
| `com.bingbaihanji.audio.visualizer.processor.SpectrumAnalyzer` | `com.bingbaihanji.utils.signal.SpectrumAnalyzer`                                                    |
| `com.bingbaihanji.audio.visualizer.util.PcmAudioParser`        | `com.bingbaihanji.utils.audio.AudioFormatUtils`                                                     |
| `com.bingbaihanji.audio.visualizer.util.ColorGradient`         | `com.bingbaihanji.utils.color.ColorGradient`                                                        |
| `com.bingbaihanji.audio.visualizer.util.PeakDetector`          | `com.bingbaihanji.utils.signal.PeakDetector`                                                        |
| `com.bingbaihanji.capture.audio.AudioProcessor`                | `com.bingbaihanji.utils.math.fft.CooleyTukeyFFT` + `com.bingbaihanji.utils.signal.SpectrumAnalyzer` |
| `com.bingbaihanji.capture.audio.RingBuffer`                    | `com.bingbaihanji.utils.datastructure.RingBuffer`                                                   |
| `com.bingbaihanji.capture.utils.CMRMap`                        | `com.bingbaihanji.utils.color.CMRMap`                                                               |

## 性能优化

1. **FFT 优化**：使用预计算的三角函数表和位反转表
2. **RingBuffer 优化**：使用双缓冲技术避免取模运算
3. **复用工作数组**：FFT 和 SpectrumAnalyzer 内部复用数组

## 线程安全

- `RingBuffer`：线程安全，使用 ReentrantLock
- `CooleyTukeyFFT`：线程安全（无共享可变状态）
- `SpectrumAnalyzer`：非线程安全（建议使用独立实例）
- `PeakDetector`：非线程安全

## 依赖

- JavaFX (javafx.scene.paint.Color)
- Java Sound API (javax.sound.sampled)
- SLF4J (用于日志)
