/*
 *
 */
package com.bingbaihanji.luca.plugin.audio.spectrum.ui.view.widget.waveform;


import com.bingbaihanji.luca.plugin.audio.spectrum.controller.AudioPlayerController;
import javafx.animation.AnimationTimer;
import javafx.application.Platform;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.scene.Cursor;
import javafx.scene.control.Tooltip;
import javafx.scene.input.MouseButton;
import javafx.scene.media.MediaPlayer;
import javafx.util.Duration;

/**
 * 可视化器类 - 音频波形绘制和交互
 *
 * <p>继承自 WaveFormPane，实现波形绘制、播放进度同步、
 * 鼠标交互（拖动seek、点击定位）、进度提示等功能。</p>
 *
 * <p>主要功能:
 * <ul>
 *   <li>实时波形绘制和更新</li>
 *   <li>播放进度指示</li>
 *   <li>鼠标交互（拖动、点击）</li>
 *   <li>时间提示工具</li>
 *   <li>节流控制防止频繁seek</li>
 * </ul>
 * </p>
 *
 * @author GOXR3PLUS
 */
public class WaveVisualization extends WaveFormPane {

    //  性能和交互常量 

    /**
     * seek 节流间隔（毫秒），防止拖动时频繁seek导致卡顿
     */
    private static final long SEEK_THROTTLE_MS = 50;

    /**
     * 动画帧更新间隔（纳秒），约 50ms
     */
    private static final long ANIMATION_UPDATE_INTERVAL_NS = 50_000_000L;
    /**
     * 不断重绘波形的服务（动画）
     */
    private final PaintService animationService;
    /**
     * 负责为绘图生成波形数据的服务
     */
    private final WaveFormService waveService;
    /**
     * 时间提示工具
     */
    private final Tooltip timeTooltip;
    /**
     * 标记是否需要重新计算波形数据
     */
    private boolean recalculateWaveData;
    /**
     * MediaPlayer媒体播放器对象
     */
    private MediaPlayer mediaPlayer;
    /**
     * 节流控制：上次seek的时间戳（纳秒）
     */
    private long lastSeekTime = 0;

    /**
     * 构造方法
     *
     * @param width  初始宽度
     * @param height 初始高度
     */
    public WaveVisualization(int width, int height) {
        this(width, height, null);
    }

    /**
     * 构造方法（支持传入MediaPlayer）
     *
     * @param width       初始宽度
     * @param height      初始高度
     * @param mediaPlayer 媒体播放器对象
     */
    public WaveVisualization(int width, int height, MediaPlayer mediaPlayer) {
        super(width, height);
        super.setWaveVisualization(this); // 设置当前可视化器

        this.mediaPlayer = mediaPlayer;

        //  关键设置：让Canvas响应鼠标事件 
        // 让整个Canvas区域都能接收鼠标事件（而不仅仅是绘制内容的像素）
        setPickOnBounds(true);
        // 确保鼠标事件不会被透传
        setMouseTransparent(false);


        // 初始化时间提示工具
        timeTooltip = new Tooltip();
        Tooltip.install(this, timeTooltip);

        // 初始化服务
        waveService = new WaveFormService(this); // 数据生成服务
        animationService = new PaintService();   // 动画绘制服务

        // 设置鼠标光标样式为手型
        setCursor(Cursor.HAND);

        // 宽度监听器：当控件宽度改变时重新绘制波形
        widthProperty().addListener((observable, oldValue, newValue) -> {
            this.width = Math.round(newValue.floatValue());
            recalculateWaveData = true; // 需要重新计算
            clear(); // 清除旧的波形
        });

        // 高度监听器：当控件高度改变时重新绘制波形
        heightProperty().addListener((observable, oldValue, newValue) -> {
            this.height = Math.round(newValue.floatValue());
            recalculateWaveData = true;
            clear();
        });

        // 鼠标移动事件：更新鼠标位置并显示时间提示
        setOnMouseMoved(m -> {
            this.setMouseXPosition((int) m.getX());
            updateTimeTooltip(m.getX());
        });

        // 鼠标拖动事件：更新鼠标位置并调节播放进度（带节流）
        setOnMouseDragged(m -> {
            this.setMouseXPosition((int) m.getX());
            updateTimeTooltip(m.getX());
            // 如果有MediaPlayer且正在拖动主按钮，则调节播放进度（带节流）
            if (this.mediaPlayer != null && m.isPrimaryButtonDown()) {
                seekToPositionThrottled(m.getX());
            }
        });

        // 鼠标移出事件：隐藏鼠标指示线
        setOnMouseExited(m -> this.setMouseXPosition(-1));

        // 鼠标点击事件：快速定位播放进度（不受节流限制）
        setOnMouseClicked(m -> {
            if (this.mediaPlayer != null && m.getButton() == MouseButton.PRIMARY) {
                seekToPosition(m.getX());
            }
        });


    }

    //

    /**
     * 获取动画绘制服务
     */
    public PaintService getAnimationService() {
        return animationService;
    }

    /**
     * 获取波形数据生成服务
     */
    public WaveFormService getWaveService() {
        return waveService;
    }

    /**
     * 获取MediaPlayer
     */
    public MediaPlayer getMediaPlayer() {
        return mediaPlayer;
    }

    /**
     * 设置MediaPlayer
     */
    public void setMediaPlayer(MediaPlayer mediaPlayer) {
        this.mediaPlayer = mediaPlayer;
        System.out.println("[setMediaPlayer] MediaPlayer 已设置: " + mediaPlayer);
        if (mediaPlayer != null) {
            System.out.println("[setMediaPlayer] MediaPlayer 状态: " + mediaPlayer.getStatus());
        }
    }

    /**
     * 更新时间提示工具文本
     *
     * @param mouseX 鼠标X坐标
     */
    private void updateTimeTooltip(double mouseX) {
        try {
            if (mediaPlayer == null || mediaPlayer.getMedia() == null) {
                return;
            }

            Duration totalDuration = mediaPlayer.getTotalDuration();
            if (totalDuration == null || totalDuration.isUnknown() || totalDuration.lessThanOrEqualTo(Duration.ZERO)) {
                return;
            }

            // 计算鼠标对应的时间
            double progress = Math.max(0, Math.min(1, mouseX / getWidth()));
            Duration seekTime = totalDuration.multiply(progress);

            // 格式化时间为 MM:SS 格式
            String timeStr = formatDuration(seekTime);
            timeTooltip.setText(timeStr);
        } catch (Exception e) {
            // 忽略异常，工具提示不影响主功能
        }
    }

    /**
     * 将Duration格式化为可读的时间字符串 (MM:SS)
     */
    private String formatDuration(Duration duration) {
        double totalSeconds = duration.toSeconds();
        int minutes = (int) (totalSeconds / 60);
        int seconds = (int) (totalSeconds % 60);
        return String.format("%02d:%02d", minutes, seconds);
    }

    /**
     * 节流版本的seek操作（用于鼠标拖动）
     * 避免拖动时频繁seek导致的卡顿
     *
     * @param mouseX 鼠标X坐标
     */
    private void seekToPositionThrottled(double mouseX) {
        long currentTimeNanos = System.nanoTime();
        long elapsedMs = (currentTimeNanos - lastSeekTime) / 1_000_000;

        // 只有当距离上次seek超过节流间隔时，才执行seek
        if (elapsedMs >= SEEK_THROTTLE_MS) {
            System.out.println("[SeekThrottled] 执行 seek，距离上次 " + elapsedMs + "ms");
            seekToPosition(mouseX);
            lastSeekTime = currentTimeNanos;
        }
    }

    /**
     * 根据鼠标位置调节播放进度
     *
     * @param mouseX 鼠标X坐标
     */
    private void seekToPosition(double mouseX) {
        if (mediaPlayer == null) {
            System.err.println("[Seek] MediaPlayer 为 null");
            return;
        }

        try {
            // 检查媒体是否加载
            if (mediaPlayer.getMedia() == null) {
                System.err.println("[Seek] 媒体未加载");
                return;
            }

            // 检查画布宽度是否有效
            double canvasWidth = getWidth();
            if (canvasWidth <= 0) {
                System.err.println("[Seek] 画布宽度无效: " + canvasWidth);
                return;
            }

            // 计算播放进度百分比
            double progress = mouseX / canvasWidth;
            progress = Math.clamp(progress, 0, 1);

            Duration totalDuration = mediaPlayer.getTotalDuration();

            // 检查总时长是否有效
            if (totalDuration == null || totalDuration.isUnknown() || totalDuration.lessThanOrEqualTo(Duration.ZERO)) {
                System.err.println("[Seek] 总时长无效: " + totalDuration);
                return;
            }

            Duration seekTime = totalDuration.multiply(progress);

            // 验证计算出的seekTime是否有效
            if (seekTime.lessThan(Duration.ZERO) || seekTime.greaterThan(totalDuration)) {
                System.err.println("[Seek] 计算出的seek时间无效: " + seekTime + ", 总时长: " + totalDuration);
                return;
            }

            MediaPlayer.Status status = mediaPlayer.getStatus();

            // 根据不同状态处理seek
            switch (status) {
                case PLAYING:
                    // 正在播放，直接seek
                    try {
                        mediaPlayer.seek(seekTime);
                        System.out.println("[Seek] 已跳转到: " + formatDuration(seekTime));
                    } catch (Exception seekEx) {
                        System.err.println("[Seek] 播放状态跳转失败: " + seekEx.getMessage());
                    }
                    break;
                case PAUSED:
                    // 已暂停，直接seek
                    try {
                        mediaPlayer.seek(seekTime);
                        System.out.println("[Seek] 已跳转到: " + formatDuration(seekTime));
                    } catch (Exception seekEx) {
                        System.err.println("[Seek] 暂停状态跳转失败: " + seekEx.getMessage());
                    }
                    break;
                case STOPPED:
                case UNKNOWN:
                case READY:
                    // 这些状态下需要先暂停，再seek
                    try {
                        mediaPlayer.pause();
                        // 异步执行seek，给MediaPlayer时间响应
                        Platform.runLater(() -> {
                            try {
                                mediaPlayer.seek(seekTime);
                                System.out.println("[Seek Async] 已跳转到: " + formatDuration(seekTime));
                            } catch (Exception asyncSeekEx) {
                                System.err.println("[Seek Async] 跳转失败: " + asyncSeekEx.getMessage());
                            }
                        });
                    } catch (Exception pauseEx) {
                        System.err.println("[Seek] 暂停操作失败: " + pauseEx.getMessage());
                    }
                    break;
                default:
                    // 其他未知状态，尝试直接seek
                    try {
                        mediaPlayer.seek(seekTime);
                    } catch (Exception defaultSeekEx) {
                        System.err.println("[Seek] 默认状态跳转失败: " + defaultSeekEx.getMessage());
                    }
                    break;
            }

        } catch (IllegalArgumentException e) {
            System.err.println("[Seek] 参数错误: " + e.getMessage());
        } catch (Exception e) {
            System.err.println("[Seek] 未知错误: " + e.getMessage());
            e.printStackTrace();
        }
    }

    //

    /**
     * 启动波形绘制服务（动画计时器）
     */
    public void startPainterService() {
        animationService.start();
    }

    /**
     * 停止波形绘制服务
     */
    public void stopPainterService() {
        animationService.stop();
        clear();
    }

    /**
     * 判断绘图服务是否在运行
     *
     * @return 如果动画计时器正在运行，返回 true
     */
    public boolean isPainterServiceRunning() {
        return animationService.isRunning();
    }


    /**
     * 绘图服务类，继承自 AnimationTimer，
     * 负责定时刷新并绘制波形。
     *
     * @author GOXR3PLUS
     */
    public class PaintService extends AnimationTimer {

        /**
         * 记录动画是否正在运行的属性
         */
        private volatile SimpleBooleanProperty running = new SimpleBooleanProperty(false);

        /**
         * 上一次绘制的纳秒时间戳
         */
        private long previousNanos = 0;

        @Override
        public void start() {
            // 确保宽高大于 0
            if (width <= 0 || height <= 0) {
                width = height = 1;
            }

            super.start();
            running.set(true);
        }

        /**
         * 获取外部类 WaveVisualization 的引用
         */
        public WaveVisualization getWaveVisualization() {
            return WaveVisualization.this;
        }

        @Override
        public void handle(long nanos) {
            boolean shouldUpdateProgress = false;

            // 每隔 50ms 更新一次播放进度（大约），提高响应性
            if (nanos >= previousNanos + ANIMATION_UPDATE_INTERVAL_NS) {  // 50ms
                previousNanos = nanos;
                shouldUpdateProgress = true;
            }

            // 同步播放进度到波形显示
            if (shouldUpdateProgress) {
                double progress = 0.0;
                // 优先从 MediaPlayer 获取进度
                if (mediaPlayer != null && mediaPlayer.getMedia() != null) {
                    Duration currentTime = mediaPlayer.getCurrentTime();
                    Duration totalDuration = mediaPlayer.getTotalDuration();
                    if (currentTime != null && totalDuration != null && !totalDuration.isUnknown() && totalDuration.greaterThan(Duration.ZERO)) {
                        progress = currentTime.toMillis() / totalDuration.toMillis();
                    }
                }
                // MediaPlayer 进度为 0 时（如 STOPPED 后 seek），从控制器进度属性兜底同步
                if (progress == 0.0) {
                    Double ctrlProgress = AudioPlayerController.getInstance().progress.get();
                    if (ctrlProgress != null && ctrlProgress > 0) {
                        progress = ctrlProgress;
                    }
                }
                int xPosition = Math.max(0, Math.min(width, (int) (progress * width)));
                setTimerXPosition(xPosition);
            }

            // 如果尚未生成波形数据，或需要重新计算
            if (recalculateWaveData) {
                // 如果有媒体文件，启动波形数据生成服务
                if (getWaveService().getFileAbsolutePath() != null) {
                    getWaveService().startService(getWaveService().getFileAbsolutePath(), WaveFormService.WaveFormJob.WAVEFORM);
                    recalculateWaveData = false;
                }
            }

            // 执行波形绘制
            paintWaveForm();
        }

        @Override
        public void stop() {
            super.stop();
            running.set(false);
        }

        /**
         * 判断动画是否正在运行
         */
        public boolean isRunning() {
            return running.get();
        }

        /**
         * 获取动画运行状态的属性对象
         */
        public SimpleBooleanProperty runningProperty() {
            return running;
        }
    }
}
