package com.bingbaihanji.luca.plugin.audio.spectrum.ui.center;

import com.bingbaihanji.luca.api.filetree.FileSelectionApi;
import com.bingbaihanji.luca.api.menu.TabPaneAccessor;
import com.bingbaihanji.luca.plugin.audio.spectrum.controller.AudioPlayerController;
import com.bingbaihanji.luca.plugin.audio.spectrum.core.visualizer.view.*;
import com.bingbaihanji.luca.plugin.audio.spectrum.ui.view.node.AudioVisualizerViewNode;
import com.bingbaihanji.luca.plugin.audio.spectrum.ui.view.node.RadialSpectrumViewNode;
import com.bingbaihanji.luca.plugin.audio.spectrum.ui.view.node.TubeSpectrumViewNode;
import com.bingbaihanji.sunsen.api.annotation.Extension;
import javafx.application.Platform;

import java.io.File;
import java.nio.file.Path;

/**
 * @author bingbaihanji
 * @date 2026-05-19 14:39:15
 */
@Extension
public class SpectrumUI implements FileSelectionApi {

    private static final String RADIAL_TAB_ID = "RadialSpectrumView";
    private static final String TUBE_TAB_ID = "TubeSpectrumView";
    // 由 SpectrumMenuExtension.bindTabPane 注入
    public static volatile TabPaneAccessor tabPaneAccessor;
    // 节点静态持有 — Sunsen 可能为每个扩展点创建不同实例，故用 static 保证唯一
    private static volatile RadialSpectrumViewNode radialNode;
    private static volatile TubeSpectrumViewNode tubeNode;

    // PCM 可视化器节点
    private static volatile AudioVisualizerViewNode<CircularSpectrumVisualizer> circularNode;
    private static volatile AudioVisualizerViewNode<WaveformVisualizer> waveformNode;
    private static volatile AudioVisualizerViewNode<SpiralVisualizer> spiralNode;
    private static volatile AudioVisualizerViewNode<StereoMeterVisualizer> stereoMeterNode;
    private static volatile AudioVisualizerViewNode<SpectrumBarsVisualizer> spectrumBarsNode;

    /**
     * 返回（按需创建）圆形布局节点，须在 FX 线程调用。
     */
    public static RadialSpectrumViewNode radialNode() {
        if (radialNode == null) {
            radialNode = new RadialSpectrumViewNode();
        }
        return radialNode;
    }

    /**
     * 返回（按需创建）发光试管节点，须在 FX 线程调用。
     */
    public static TubeSpectrumViewNode tubeNode() {
        if (tubeNode == null) {
            tubeNode = new TubeSpectrumViewNode();
        }
        return tubeNode;
    }

    // ── PCM 可视化器节点工厂 ──────────────────────────────────────

    /**
     * 返回（按需创建）圆形频谱（PCM）节点，须在 FX 线程调用。
     */
    public static AudioVisualizerViewNode<CircularSpectrumVisualizer> circularNode() {
        if (circularNode == null) {
            circularNode = new AudioVisualizerViewNode<>(
                    "圆形频谱(PCM)", new CircularSpectrumVisualizer(), "CircularSpectrumPCM");
        }
        return circularNode;
    }

    /**
     * 返回（按需创建）波形示波器节点，须在 FX 线程调用。
     */
    public static AudioVisualizerViewNode<WaveformVisualizer> waveformNode() {
        if (waveformNode == null) {
            waveformNode = new AudioVisualizerViewNode<>(
                    "波形示波器", new WaveformVisualizer(), "WaveformVisualizer");
        }
        return waveformNode;
    }

    /**
     * 返回（按需创建）螺旋粒子节点，须在 FX 线程调用。
     */
    public static AudioVisualizerViewNode<SpiralVisualizer> spiralNode() {
        if (spiralNode == null) {
            spiralNode = new AudioVisualizerViewNode<>(
                    "螺旋粒子", new SpiralVisualizer(), "SpiralVisualizer");
        }
        return spiralNode;
    }

    /**
     * 返回（按需创建）立体声电平表节点，须在 FX 线程调用。
     */
    public static AudioVisualizerViewNode<StereoMeterVisualizer> stereoMeterNode() {
        if (stereoMeterNode == null) {
            stereoMeterNode = new AudioVisualizerViewNode<>(
                    "立体声电平", new StereoMeterVisualizer(), "StereoMeterVisualizer");
        }
        return stereoMeterNode;
    }

    /**
     * 返回（按需创建）频谱柱状条节点，须在 FX 线程调用。
     */
    public static AudioVisualizerViewNode<SpectrumBarsVisualizer> spectrumBarsNode() {
        if (spectrumBarsNode == null) {
            spectrumBarsNode = new AudioVisualizerViewNode<>(
                    "频谱柱状条", new SpectrumBarsVisualizer(), "SpectrumBarsVisualizer");
        }
        return spectrumBarsNode;
    }

    // ── FileSelectionApi ──────────────────────────────────────────

    @Override
    public void onFileSelected(Path path, boolean isDirectory) {
        if (isDirectory) {
            return;
        }
        if (!RadialSpectrumViewNode.isAudioFile(path)) {
            return;
        }

        File file = path.toFile();
        // 单例控制器统一处理，已注册的所有可视化器自动收到频谱广播
        Platform.runLater(() -> AudioPlayerController.getInstance().loadAudioFile(file));
    }
}
