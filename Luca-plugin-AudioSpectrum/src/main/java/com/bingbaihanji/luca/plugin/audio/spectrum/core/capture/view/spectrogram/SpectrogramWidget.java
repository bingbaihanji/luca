package com.bingbaihanji.luca.plugin.audio.spectrum.core.capture.view.spectrogram;

import com.bingbaihanji.luca.plugin.audio.spectrum.core.capture.audio.AudioBuffer;
import com.bingbaihanji.luca.plugin.audio.spectrum.core.capture.view.base.AudioWidget;
import com.bingbaihanji.luca.plugin.audio.spectrum.core.capture.view.base.WidgetViewModel;
import com.bingbaihanji.luca.plugin.audio.spectrum.utils.color.CMRMap;
import com.bingbaihanji.luca.plugin.audio.spectrum.utils.math.fft.CooleyTukeyFFT;
import com.bingbaihanji.luca.plugin.audio.spectrum.utils.signal.SignalProcessing;
import javafx.scene.image.PixelFormat;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.Region;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.IntBuffer;

/**
 * 频谱图 Widget - 性能优化版本
 * <p>
 * 使用更高效的像素操作方法
 */
public class SpectrogramWidget implements AudioWidget {

    private static final Logger logger = LoggerFactory.getLogger(SpectrogramWidget.class);
    private static final double OVERLAP = 0.75;
    // dB 范围
    // 调整范围以更好显示实际音频信号
    private static final double MIN_DB = -60.0;   // 噪声 floor (蓝紫色)
    private static final double MAX_DB = 0.0;     // 强信号 (白色)
    private static final int BACKGROUND_ARGB = 0xFF000000;  // 纯黑色背景
    // 频谱数据下采样/插值到固定高度
    private static final int DISPLAY_HEIGHT = 256;  // 固定显示高度，避免布局问题
    private final SpectrogramViewModel viewModel = new SpectrogramViewModel();
    private final SpectrogramView view;
    // 颜色缓存
    private final int[] colorCache = new int[256];
    private AudioBuffer audioBuffer;
    private CooleyTukeyFFT fft;
    private boolean isPaused = false;
    // FFT 配置
    private int fftSize = 2048;
    private long oldIndex = 0;
    // 滚动图像缓冲区 - 动态尺寸
    private WritableImage spectrogramImage;
    private PixelWriter pixelWriter;
    private int imageWidth = 600;  // 默认值，将根据容器大小动态调整
    private int imageHeight = 256;  // 默认高度，使用合理的固定值
    // 性能优化：使用 IntBuffer 批量操作像素
    private int[] imageBuffer;
    private int currentColumn = 0;  // 当前写入列

    public SpectrogramWidget() {
        fft = new CooleyTukeyFFT(fftSize);
        initializeColorCache();
        initializeImage();

        view = new SpectrogramView(viewModel);

        // 初始设置图像到视图
        view.setImage(spectrogramImage);

        // 设置视图宽度变化回调
        view.setOnWidthChanged(newWidth -> {
            if (newWidth > 0) {
                resizeImage(newWidth);
            }
        });
    }

    /**
     * 初始化滚动图像
     */
    private void initializeImage() {
        imageHeight = DISPLAY_HEIGHT;
        spectrogramImage = new WritableImage(imageWidth, imageHeight);
        pixelWriter = spectrogramImage.getPixelWriter();
        imageBuffer = new int[imageWidth * imageHeight];
        currentColumn = 0;
        clearImage();
        viewModel.updateImage(spectrogramImage);
    }

    /**
     * 调整图像宽度以适应容器
     */
    public void resizeImage(int newWidth) {
        if (newWidth != imageWidth && newWidth > 0) {
            imageWidth = Math.max(200, Math.min(2000, newWidth));  // 限制合理范围
            initializeImage();
            view.setImage(spectrogramImage);
            logger.info("频谱图图像宽度调整为 {}", imageWidth);
        }
    }

    /**
     * 预计算颜色缓存（使用 CMRMap）
     */
    private void initializeColorCache() {
        int[] argbTable = CMRMap.generateArgbLookupTable();
        System.arraycopy(argbTable, 0, colorCache, 0, 256);
    }

    @Override
    public WidgetViewModel getViewModel() {
        return viewModel;
    }

    @Override
    public Region getView() {
        return view;
    }

    @Override
    public void setBuffer(AudioBuffer buffer) {
        this.audioBuffer = buffer;

        buffer.getNewDataProperty().addListener((obs, oldVal, newVal) -> {
            if (!isPaused) {
                handleNewData();
            }
        });

        logger.info("SpectrogramWidget 已连接到 AudioBuffer");
    }

    @Override
    public void handleNewData() {
        try {
            long currentIndex = audioBuffer.getCurrentOffset();
            long available = currentIndex - oldIndex;
            int needed = (int) (fftSize * (1.0 - OVERLAP));

            // 检查是否有足够的数据进行 FFT
            if (available >= needed && currentIndex >= fftSize) {
                //    logger.debug("处理频谱图数据: available={}, needed={}", available, needed);

                // 获取样本
                double[] samples = audioBuffer.dataIndexed(oldIndex, fftSize);

                // FFT 分析
                double[] spectrum = fft.calculate(samples);
                double[] dBSpectrum = SignalProcessing.spectrumToDecibels(spectrum);

                // 转换为颜色列（使用缓存）
                int[] colorColumn = dBToColorIndices(dBSpectrum);

                // 使用环形缓冲方式添加新列
                addColumnOptimized(colorColumn);

                oldIndex += needed;

                // 标记需要更新
                view.markDirty();
            }
        } catch (Exception e) {
            logger.error("处理频谱图数据时出错", e);
        }
    }

    @Override
    public void canvasUpdate() {

        // 触发视图刷新
        view.render();

        // 更新图像到视图
        WritableImage img = viewModel.getImage();
        if (img != null && view.getImageView().getImage() != img) {
            view.setImage(img);
        }
    }

    @Override
    public void pause() {
        isPaused = true;
        logger.info("SpectrogramWidget 已暂停");
    }

    @Override
    public void restart() {
        isPaused = false;
        logger.info("SpectrogramWidget 已重启");
    }

    /**
     * 将 dB 频谱转换为颜色索引（使用预计算的颜色）
     * 使用对数频率刻度以适应人耳感知和音频特性
     */
    private int[] dBToColorIndices(double[] dBSpectrum) {
        int[] result = new int[DISPLAY_HEIGHT];

        // 使用对数频率刻度重采样
        double minFreq = 20.0;
        double maxFreq = 22050.0;
        double logMin = Math.log(minFreq);
        double logMax = Math.log(maxFreq);

        for (int i = 0; i < DISPLAY_HEIGHT; i++) {
            // 对数刻度映射：从显示位置 i 映射到频率
            double logFreq = logMin + (logMax - logMin) * i / DISPLAY_HEIGHT;
            double freq = Math.exp(logFreq);

            // 频率映射到 FFT bin
            int binIndex = (int) ((freq / maxFreq) * (dBSpectrum.length - 1));
            binIndex = Math.max(0, Math.min(dBSpectrum.length - 1, binIndex));

            // 获取该频率的 dB 值并转换为颜色
            double dbValue = dBSpectrum[binIndex];
            double normalized = ((dbValue - MIN_DB) / (MAX_DB - MIN_DB));
            normalized = Math.max(0.0, Math.min(1.0, normalized));
            int colorIndex = (int) (normalized * 255);
            colorIndex = Math.max(0, Math.min(255, colorIndex));
            result[i] = colorCache[colorIndex];
        }

        return result;
    }

    /**
     * 优化的列添加方法：使用环形缓冲，只更新一列
     */
    private void addColumnOptimized(int[] colorColumn) {
        // 写入新列到 imageBuffer
        for (int y = 0; y < colorColumn.length; y++) {
            int invertedY = imageHeight - y - 1;
            imageBuffer[invertedY * imageWidth + currentColumn] = colorColumn[y];
        }

        // 移动到下一列（环形）
        currentColumn = (currentColumn + 1) % imageWidth;

        // 批量更新图像
        updateImageFromBuffer();
    }

    /**
     * 从缓冲区更新图像
     * 使用滚动显示：将 currentColumn 之后的部分显示在左边
     */
    private void updateImageFromBuffer() {
        PixelFormat<IntBuffer> pixelFormat = PixelFormat.getIntArgbInstance();

        // 分两次写入，形成滚动效果
        // 1. 写入 currentColumn 之后的部分到图像左侧
        if (currentColumn < imageWidth) {
            int remainingWidth = imageWidth - currentColumn;
            for (int y = 0; y < imageHeight; y++) {
                pixelWriter.setPixels(
                        0, y,
                        remainingWidth, 1,
                        pixelFormat,
                        imageBuffer,
                        y * imageWidth + currentColumn,
                        imageWidth
                );
            }

            // 2. 写入 currentColumn 之前的部分到图像右侧
            if (currentColumn > 0) {
                for (int y = 0; y < imageHeight; y++) {
                    pixelWriter.setPixels(
                            remainingWidth, y,
                            currentColumn, 1,
                            pixelFormat,
                            imageBuffer,
                            y * imageWidth,
                            imageWidth
                    );
                }
            }
        }
    }

    /**
     * 清空图像
     */
    private void clearImage() {
        java.util.Arrays.fill(imageBuffer, BACKGROUND_ARGB);
        for (int x = 0; x < imageWidth; x++) {
            for (int y = 0; y < imageHeight; y++) {
                pixelWriter.setArgb(x, y, BACKGROUND_ARGB);
            }
        }
    }

    public int getFftSize() {
        return fftSize;
    }

    /**
     * 设置 FFT 大小
     */
    public void setFftSize(int newSize) {
        if (newSize <= 0 || (newSize & (newSize - 1)) != 0) {
            throw new IllegalArgumentException("FFT 大小必须是 2 的幂");
        }

        fftSize = newSize;
        fft = new CooleyTukeyFFT(newSize);
        initializeImage();
        logger.info("频谱图 FFT 大小设置为 {}", fftSize);
    }
}
