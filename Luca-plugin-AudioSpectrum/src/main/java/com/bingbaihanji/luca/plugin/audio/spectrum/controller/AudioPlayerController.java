package com.bingbaihanji.luca.plugin.audio.spectrum.controller;

import com.bingbaihanji.luca.plugin.audio.spectrum.core.spectrum.AudioSpectrumVisualizer;
import com.bingbaihanji.luca.plugin.audio.spectrum.core.visualizer.core.AudioVisualizer;
import com.bingbaihanji.luca.plugin.audio.spectrum.decoder.AudioDecoderFactory;
import com.bingbaihanji.luca.plugin.audio.spectrum.utils.audio.AudioFormatUtils;
import javafx.application.Platform;
import javafx.beans.property.*;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.util.Duration;
import lombok.Getter;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import java.io.File;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

/**
 * 单例音频播放器 — 持有唯一一个 MediaPlayer，向所有已注册的可视化器广播频谱数据。
 * 两个视图（圆形布局 / 发光试管）共享同一播放状态和进度属性，天然保持同步。
 * <p>
 * <b>线程安全说明：</b>所有公共方法均应在 JavaFX 应用线程调用（属性绑定与 UI 更新要求）。
 */
public class AudioPlayerController {

    // 静态内部类持有者 — JVM 类加载机制保证懒加载和线程安全，无需加锁

    /**
     * 当前原始文件名显示
     */
    public final SimpleStringProperty currentFileName = new SimpleStringProperty("未选择文件");
    /**
     * 当前播放时间文本
     */
    public final SimpleStringProperty currentTime = new SimpleStringProperty("0:00");
    /**
     * 总时长文本
     */
    public final SimpleStringProperty totalTime = new SimpleStringProperty("0:00");
    /**
     * 播放进度 0.0~1.0，可直接绑定进度条
     */
    public final SimpleObjectProperty<Double> progress = new SimpleObjectProperty<>(0.0);

    // ── 可观察属性（视图直接绑定）────────────────────────────────
    /**
     * 播放状态标识，按钮等可通过此属性改变样式或文字
     */
    public final SimpleObjectProperty<Boolean> isPlaying = new SimpleObjectProperty<>(false);
    /**
     * 音量 0.0~1.0，自动绑定到当前及后续所有 MediaPlayer
     */
    public final SimpleDoubleProperty volume = new SimpleDoubleProperty(1.0);
    /**
     * 播放速率，默认 1.0，自动绑定到当前及后续所有 MediaPlayer
     */
    public final SimpleDoubleProperty playbackRate = new SimpleDoubleProperty(1.0);
    private final Set<String> supportedAudioExtensions = new HashSet<>();
    /**
     * 是否已加载媒体（只读）
     */
    private final ReadOnlyBooleanWrapper hasMediaWrapper = new ReadOnlyBooleanWrapper(false);
    public final ReadOnlyBooleanProperty hasMedia = hasMediaWrapper.getReadOnlyProperty();
    /**
     * 所有已注册的可视化器，频谱数据广播给全部。线程安全写，FX 线程读。
     */
    private final List<AudioSpectrumVisualizer> visualizers = new CopyOnWriteArrayList<>();
    /**
     * 所有已注册的 PCM 可视化器，原始采样数据广播给全部。线程安全写，FX 线程读。
     */
    private final List<AudioVisualizer> pcmVisualizers = new CopyOnWriteArrayList<>();
    /**
     * 每次新 MediaPlayer 就绪时调用，用于重新绑定均衡器等外部组件。
     */
    private final List<Consumer<MediaPlayer>> onPlayerReadyCallbacks = new CopyOnWriteArrayList<>();
    private final Object pcmLock = new Object();
    private volatile Thread pcmThread;
    private volatile AudioInputStream pcmStream;

    // ── PCM 数据读取线程状态 ───────────────────────────────────────
    private volatile boolean pcmPlaying = false;
    private volatile boolean pcmStopped = true;
    private volatile double pcmTimeOffset = 0.0;
    /**
     * 当前持有的音频播放器实例
     */
    @Getter
    private MediaPlayer mediaPlayer = null;
    /**
     * 当前解码后的标准 WAV 文件（临时文件）
     */
    @Getter
    private File currentDecodedWavFile;
    /**
     * 当前加载的原始音频文件
     */
    @Getter
    private File currentSourceFile;

    // ── 内部状态 ──────────────────────────────────────────────────

    private AudioPlayerController() {
    }

    /**
     * 获取全局单例，插件内所有视图共享同一实例。
     */
    public static AudioPlayerController getInstance() {
        return Holder.INSTANCE;
    }

    /**
     * 返回所有支持的文件扩展名（不可修改）。
     * 首次调用时（或集合为空时）从 AudioDecoderFactory 懒加载，
     * 确保在 DecoderPluginLoader.loadBuiltInDecoders() 执行后才填充。
     */
    public Set<String> getSupportedAudioExtensions() {
        if (supportedAudioExtensions.isEmpty()) {
            AudioDecoderFactory.getInstance().getAllDecoders().forEach(decoder -> {
                String[] extensions = decoder.getSupportedExtensions();
                if (extensions != null) {
                    Collections.addAll(supportedAudioExtensions, extensions);
                }
            });
        }
        return Collections.unmodifiableSet(supportedAudioExtensions);
    }

    /**
     * 注册一个可视化器，后续每次频谱数据更新都会通知它。
     */
    public void addVisualizer(AudioSpectrumVisualizer v) {
        if (v != null && !visualizers.contains(v)) {
            visualizers.add(v);
        }
    }

    // ── 可视化器注册 ──────────────────────────────────────────────

    /**
     * 移除已注册的可视化器，停止向其推送频谱数据。
     */
    public void removeVisualizer(AudioSpectrumVisualizer v) {
        visualizers.remove(v);
    }

    /**
     * 注册一个回调，每当新 MediaPlayer 就绪（READY 状态）时触发一次。
     * 用于均衡器等需要持有 MediaPlayer 引用的组件，无需关心何时换歌。
     */
    public void addOnPlayerReady(Consumer<MediaPlayer> callback) {
        if (callback != null) {
            onPlayerReadyCallbacks.add(callback);
        }
    }

    /**
     * 移除已注册的播放器就绪回调。
     */
    public void removeOnPlayerReady(Consumer<MediaPlayer> callback) {
        onPlayerReadyCallbacks.remove(callback);
    }

    /**
     * 注册一个 PCM 可视化器，后续每次 PCM 数据更新都会通知它。
     * 若当前正在播放，会自动启动可视化器。
     */
    public void addPCMVisualizer(AudioVisualizer v) {
        if (v != null && !pcmVisualizers.contains(v)) {
            pcmVisualizers.add(v);
            if (isPlaying.get() && !v.isRunning()) {
                javafx.application.Platform.runLater(v::start);
            }
        }
    }

    // ── PCM 可视化器注册 ──────────────────────────────────────────

    /**
     * 移除已注册的 PCM 可视化器，停止向其推送原始采样数据。
     */
    public void removePCMVisualizer(AudioVisualizer v) {
        pcmVisualizers.remove(v);
    }

    /**
     * 启动 PCM 数据读取线程，从解码后的 WAV 文件读取原始采样并广播给所有 PCM 可视化器。
     *
     * @param wavFile    解码后的 WAV 文件
     * @param timeOffset 起始时间偏移（0.0~1.0 的百分比）
     */
    private void startPCMThread(File wavFile, double timeOffset) {
        stopPCMThread();
        if (wavFile == null || !wavFile.exists()) {
            return;
        }

        pcmStopped = false;
        pcmPlaying = true;
        pcmTimeOffset = timeOffset;

        pcmThread = new Thread(() -> {
            AudioInputStream originalAis = null;
            try {
                originalAis = AudioSystem.getAudioInputStream(wavFile);
                AudioFormat originalFormat = originalAis.getFormat();
                AudioFormat decodedFormat = new AudioFormat(
                        AudioFormat.Encoding.PCM_SIGNED,
                        originalFormat.getSampleRate(),
                        16,
                        originalFormat.getChannels(),
                        originalFormat.getChannels() * 2,
                        originalFormat.getSampleRate(),
                        false
                );
                pcmStream = AudioSystem.getAudioInputStream(decodedFormat, originalAis);

                // 按时间偏移跳转到对应位置
                if (timeOffset > 0) {
                    skipToTimeOffset(pcmStream, decodedFormat, timeOffset, wavFile.length());
                }

                byte[] buffer = new byte[4096];
                int bytesRead;
                float[] left = new float[1024];
                float[] right = new float[1024];

                while (!pcmStopped && (bytesRead = pcmStream.read(buffer)) != -1) {
                    synchronized (pcmLock) {
                        while (!pcmPlaying && !pcmStopped) {
                            pcmLock.wait(50);
                        }
                    }
                    if (pcmStopped) {
                        break;
                    }

                    AudioFormatUtils.decodeStereo(buffer, bytesRead, decodedFormat, left, right);

                    final float[] leftCopy = Arrays.copyOf(left, left.length);
                    final float[] rightCopy = Arrays.copyOf(right, right.length);

                    Platform.runLater(() -> {
                        for (AudioVisualizer viz : pcmVisualizers) {
                            viz.updateAudioData(leftCopy, rightCopy);
                        }
                    });

                    // 限速：按实际采样率休眠，使读取速度与实时播放同步
                    int frames = bytesRead / decodedFormat.getFrameSize();
                    long sleepNanos = (long) (frames * 1_000_000_000.0 / decodedFormat.getSampleRate());
                    if (sleepNanos > 0) {
                        Thread.sleep(sleepNanos / 1_000_000, (int) (sleepNanos % 1_000_000));
                    }
                }
            } catch (Exception e) {
                if (!pcmStopped) {
                    System.err.println("PCM 可视化线程错误: " + e.getMessage());
                    e.printStackTrace();
                }
            } finally {
                closePCMStream();
                if (originalAis != null) {
                    try {
                        originalAis.close();
                    } catch (Exception ignored) {
                    }
                }
            }
        }, "PCM-Visualizer");
        pcmThread.setDaemon(true);
        pcmThread.start();
    }

    // ── PCM 后台线程管理 ──────────────────────────────────────────

    /**
     * 停止 PCM 数据读取线程并清理资源。
     */
    private void stopPCMThread() {
        pcmStopped = true;
        pcmPlaying = false;
        synchronized (pcmLock) {
            pcmLock.notifyAll();
        }
        if (pcmThread != null && pcmThread.isAlive()) {
            try {
                pcmThread.join(1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        pcmThread = null;
        closePCMStream();
    }

    /**
     * 安全关闭 PCM 音频流。
     */
    private void closePCMStream() {
        if (pcmStream != null) {
            try {
                pcmStream.close();
            } catch (Exception e) {
                // 静默处理
            } finally {
                pcmStream = null;
            }
        }
    }

    /**
     * 根据时间百分比跳转到 AudioInputStream 的对应位置。
     */
    private void skipToTimeOffset(AudioInputStream stream, AudioFormat format,
                                  double timeOffset, long fileSize) throws Exception {
        long totalFrames = stream.getFrameLength();
        long skipBytes;
        if (totalFrames != AudioSystem.NOT_SPECIFIED && totalFrames > 0) {
            long targetFrame = (long) (timeOffset * totalFrames);
            skipBytes = targetFrame * format.getFrameSize();
        } else {
            // 无法获取总帧数，用文件大小估算
            skipBytes = (long) (timeOffset * fileSize);
        }
        if (skipBytes > 0) {
            long skipped = 0;
            while (skipped < skipBytes) {
                long s = stream.skip(skipBytes - skipped);
                if (s <= 0) {
                    break;
                }
                skipped += s;
            }
        }
    }

    /**
     * 加载并播放新的音频文件。
     * 会先停止当前播放、释放旧资源，然后创建新 MediaPlayer 并设置频谱监听、进度同步等。
     *
     * @param audioFile 原始音频文件
     */
    public void loadAudioFile(File audioFile) {
        if (audioFile == null || !audioFile.exists()) {
            Platform.runLater(() -> currentFileName.set("文件不存在"));
            return;
        }

        File decodedWavFile = null;
        try {
            decodedWavFile = decodeAudioFile(audioFile);
            if (decodedWavFile == null) {
                Platform.runLater(() -> currentFileName.set("不支持的格式: " + audioFile.getName()));
                return;
            }

            // 1. 释放旧播放器及旧临时文件
            releaseInternal();

            // 2. 基于解码后的文件创建 Media 及 MediaPlayer
            Media media = new Media(decodedWavFile.toURI().toString());
            MediaPlayer player = new MediaPlayer(media);

            // 3. 将音量和速率属性自动绑定到新播放器
            player.volumeProperty().bind(volume);
            player.rateProperty().bind(playbackRate);

            // 4. 配置频谱监听 — 将频谱幅值广播给所有已注册的可视化器
            player.setAudioSpectrumListener((ts, dur, magnitudes, phases) -> {
                for (AudioSpectrumVisualizer viz : visualizers) {
                    viz.updateSpectrum(magnitudes);
                }
            });
            player.setAudioSpectrumInterval(0.05);
            player.setAudioSpectrumNumBands(128);
            player.setAudioSpectrumThreshold(-60);

            // 5. 进度同步 — 监听当前时间变化，更新可观察属性
            player.currentTimeProperty().addListener((obs, oldVal, newVal) ->
                    Platform.runLater(() -> {
                        currentTime.set(formatTime(newVal));
                        Duration total = player.getTotalDuration();
                        if (total != null && !total.isUnknown() && total.greaterThan(Duration.ZERO)) {
                            progress.set(newVal.toMillis() / total.toMillis());
                        }
                    })
            );

            // 6. 就绪回调 — 更新总时长、自动播放，并通知外部组件（如均衡器重新绑定）
            player.setOnReady(() -> Platform.runLater(() -> {
                totalTime.set(formatTime(player.getTotalDuration()));
                player.play();
                isPlaying.set(true);
                for (Consumer<MediaPlayer> cb : onPlayerReadyCallbacks) {
                    try {
                        cb.accept(player);
                    } catch (Exception ex) {
                        System.err.println("播放器就绪回调执行失败: " + ex.getMessage());
                        ex.printStackTrace();
                    }
                }
                // 自动启动所有已注册的 PCM 可视化器
                for (AudioVisualizer viz : pcmVisualizers) {
                    if (!viz.isRunning()) {
                        viz.start();
                    }
                }
            }));

            // 7. 播放结束自动回到起点（不释放资源，方便用户重播）
            player.setOnEndOfMedia(() -> Platform.runLater(() -> {
                isPlaying.set(false);
                player.stop();
                currentTime.set(formatTime(Duration.ZERO));
                progress.set(0.0);
            }));

            // 8. 错误处理
            player.setOnError(() -> Platform.runLater(() -> {
                String msg = player.getError() != null ? player.getError().getMessage() : "未知";
                System.err.println("播放错误: " + msg);
                isPlaying.set(false);
            }));

            // 9. 保存引用并更新 UI
            mediaPlayer = player;
            currentDecodedWavFile = decodedWavFile;
            currentSourceFile = audioFile;
            hasMediaWrapper.set(true);
            currentFileName.set(audioFile.getName());

            // 10. 启动 PCM 数据读取线程，驱动 AudioVisualizer 子类
            startPCMThread(decodedWavFile, 0.0);

        } catch (Exception e) {
            // 加载失败时确保清理本次解码产生的临时文件
            if (decodedWavFile != null) {
                cleanupFile(decodedWavFile);
            }
            Platform.runLater(() -> currentFileName.set("加载失败: " + e.getMessage()));
            e.printStackTrace();
        }
    }

    // ── 播放控制 ──────────────────────────────────────────────────

    /**
     * 使用解码器将音频文件统一转换为 WAV 格式。
     *
     * @param audioFile 原始音频文件
     * @return 解码后的标准 WAV 文件，如果不支持则返回 null
     */
    private File decodeAudioFile(File audioFile) {
        try {
            var decoderFactory = AudioDecoderFactory.getInstance();
            var decoder = decoderFactory.getDecoder(audioFile);

            if (decoder == null) {
                System.err.println("不支持的音频格式: " + audioFile.getName());
                return null;
            }

            System.out.println("使用解码器: " + decoder.getMetadata().name());
            File decodedWavFile = decoder.decode(audioFile, null);
            System.out.println("音频已解码为: " + decodedWavFile.getAbsolutePath());
            return decodedWavFile;

        } catch (Exception e) {
            System.err.println("音频解码失败: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    /**
     * 清理当前解码后的临时 WAV 文件。
     */
    private void cleanupDecodedWavFile() {
        cleanupFile(currentDecodedWavFile);
        currentDecodedWavFile = null;
    }

    /**
     * 安全删除指定文件，失败时标记为 deleteOnExit。
     */
    private void cleanupFile(File file) {
        if (file != null && file.exists()) {
            try {
                if (!file.delete()) {
                    file.deleteOnExit();
                } else {
                    System.out.println("已清理解码文件: " + file.getAbsolutePath());
                }
            } catch (Exception e) {
                System.err.println("清理解码文件失败: " + e.getMessage());
                file.deleteOnExit();
            }
        }
    }

    /**
     * 开始或恢复播放。
     */
    public void play() {
        MediaPlayer p = mediaPlayer;
        if (p != null) {
            p.play();
            isPlaying.set(true);
        }
        pcmPlaying = true;
        synchronized (pcmLock) {
            pcmLock.notifyAll();
        }
        // 若 PCM 线程已终止（如 stop 后），从当前偏移位置重新启动
        if ((pcmThread == null || !pcmThread.isAlive()) && currentDecodedWavFile != null && currentDecodedWavFile.exists()) {
            startPCMThread(currentDecodedWavFile, pcmTimeOffset);
        }
    }

    /**
     * 暂停播放。
     */
    public void pause() {
        MediaPlayer p = mediaPlayer;
        if (p != null) {
            p.pause();
            isPlaying.set(false);
        }
        pcmPlaying = false;
    }

    /**
     * 停止播放并将进度与时间重置为 0，同时清空所有可视化器频谱。
     * <p>
     * 注意：此方法不会释放 MediaPlayer 资源，也不会删除解码后的临时文件，
     * 用户可随后调用 {@link #play()} 重新播放当前音频。
     */
    public void stop() {
        MediaPlayer p = mediaPlayer;
        if (p != null) {
            p.stop();
        }
        isPlaying.set(false);
        currentTime.set("0:00");
        progress.set(0.0);
        for (AudioSpectrumVisualizer viz : visualizers) {
            viz.clear();
        }
        stopPCMThread();
        pcmTimeOffset = 0.0;
    }

    /**
     * 切换播放/暂停状态。
     */
    public void togglePlayPause() {
        if (Boolean.TRUE.equals(isPlaying.get())) {
            pause();
        } else {
            play();
        }
    }

    /**
     * 跳转到指定百分比位置（0.0 ~ 1.0）。
     *
     * @param percentage 进度百分比
     */
    public void seek(double percentage) {
        MediaPlayer p = mediaPlayer;
        if (p == null) {
            return;
        }
        double clamped = Math.clamp(percentage, 0.0, 1.0);
        Duration total = p.getTotalDuration();
        if (total != null && !total.isUnknown() && total.greaterThan(Duration.ZERO)) {
            p.seek(total.multiply(clamped));
        }

        // 显式更新进度属性，确保 STOPPED 状态下 currentTimeProperty 不触发时也能同步
        progress.set(clamped);
        currentTime.set(formatTime(total != null ? total.multiply(clamped) : Duration.ZERO));

        // 同步 PCM 线程到对应位置
        if (currentDecodedWavFile != null && currentDecodedWavFile.exists()) {
            pcmTimeOffset = clamped;
            stopPCMThread();
            startPCMThread(currentDecodedWavFile, pcmTimeOffset);
        }
    }

    /**
     * 释放 MediaPlayer 占用的资源，并清理当前解码后的临时文件。
     * 通常在切换歌曲或关闭插件时调用。
     */
    public void disposePlayer() {
        releaseInternal();
    }

    /**
     * 彻底关闭音频控制器，释放播放器、清理临时文件并清空所有监听器。
     * 适合在插件关闭（如 {@code onDestroy}）时调用。
     */
    public void shutdown() {
        releaseInternal();
        visualizers.clear();
        onPlayerReadyCallbacks.clear();
        pcmVisualizers.clear();
    }

    /**
     * 释放当前播放器及关联资源，重置所有 UI 状态。
     * 私有方法，供 {@link #loadAudioFile}、{@link #disposePlayer}、{@link #shutdown} 使用。
     */
    private void releaseInternal() {
        MediaPlayer p = mediaPlayer;
        mediaPlayer = null;
        currentSourceFile = null;
        hasMediaWrapper.set(false);

        if (p != null) {
            try {
                p.stop();
                p.dispose();
            } catch (Exception e) {
                System.err.println("释放播放器失败: " + e.getMessage());
            }
        }

        stopPCMThread();
        cleanupDecodedWavFile();
        isPlaying.set(false);
        currentTime.set("0:00");
        totalTime.set("0:00");
        progress.set(0.0);
        currentFileName.set("未选择文件");
        for (AudioSpectrumVisualizer viz : visualizers) {
            viz.clear();
        }
    }

    // ── 内部工具 ──────────────────────────────────────────────────

    /**
     * 将 Duration 格式化为时间字符串。
     * <ul>
     *   <li>不足 1 小时：{@code m:ss}</li>
     *   <li>1 小时及以上：{@code h:mm:ss}</li>
     * </ul>
     */
    private String formatTime(Duration d) {
        if (d == null || d.isUnknown()) {
            return "0:00";
        }
        int totalSeconds = (int) d.toSeconds();
        int hours = totalSeconds / 3600;
        int minutes = (totalSeconds % 3600) / 60;
        int seconds = totalSeconds % 60;
        if (hours > 0) {
            return String.format("%d:%02d:%02d", hours, minutes, seconds);
        }
        return String.format("%d:%02d", minutes, seconds);
    }

    private static final class Holder {
        static final AudioPlayerController INSTANCE = new AudioPlayerController();
    }
}
