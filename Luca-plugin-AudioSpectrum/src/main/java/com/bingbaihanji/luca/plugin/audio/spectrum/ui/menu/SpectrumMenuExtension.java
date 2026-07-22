package com.bingbaihanji.luca.plugin.audio.spectrum.ui.menu;

import com.bingbaihanji.luca.api.menu.MenuApi;
import com.bingbaihanji.luca.api.menu.TabPaneAccessor;
import com.bingbaihanji.luca.plugin.audio.spectrum.controller.AudioPlayerController;
import com.bingbaihanji.luca.plugin.audio.spectrum.equalizer.FxequalizerView;
import com.bingbaihanji.luca.plugin.audio.spectrum.ui.center.SpectrumUI;
import com.bingbaihanji.sunsen.api.annotation.Extension;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.control.Slider;
import javafx.scene.media.AudioEqualizer;
import javafx.scene.media.EqualizerBand;
import javafx.scene.media.MediaPlayer;

import java.util.List;

/**
 * @author bingbaihanji
 * @date 2026-05-19 14:28:59
 */
@Extension
public class SpectrumMenuExtension implements MenuApi {

    private static final String RADIAL_TAB_ID = "RadialSpectrumView";
    private static final String TUBE_TAB_ID = "TubeSpectrumView";
    private static final String CIRCULAR_TAB_ID = "CircularSpectrumPCM";
    private static final String WAVEFORM_TAB_ID = "WaveformVisualizer";
    private static final String SPIRAL_TAB_ID = "SpiralVisualizer";
    private static final String STEREO_METER_TAB_ID = "StereoMeterVisualizer";
    private static final String SPECTRUM_BARS_TAB_ID = "SpectrumBarsVisualizer";

    private TabPaneAccessor tabPane;

    @Override
    public void bindTabPane(TabPaneAccessor accessor) {
        this.tabPane = accessor;
    }

    @Override
    public void extend(MenuBar parentMenu) {
        for (Menu menu : parentMenu.getMenus()) {
            if (menu.getText().startsWith("视图")) {
                Menu spectrumMenu = new Menu("频谱");

                MenuItem radialItem = new MenuItem("圆形频谱");
                radialItem.setOnAction(e -> openRadial());

                MenuItem tubeItem = new MenuItem("发光试管");
                tubeItem.setOnAction(e -> openTube());

                MenuItem circularItem = new MenuItem("圆形频谱(PCM)");
                circularItem.setOnAction(e -> openCircular());

                MenuItem waveformItem = new MenuItem("波形示波器");
                waveformItem.setOnAction(e -> openWaveform());

                MenuItem spiralItem = new MenuItem("螺旋粒子");
                spiralItem.setOnAction(e -> openSpiral());

                MenuItem stereoMeterItem = new MenuItem("立体声电平");
                stereoMeterItem.setOnAction(e -> openStereoMeter());

                MenuItem spectrumBarsItem = new MenuItem("频谱柱状条");
                spectrumBarsItem.setOnAction(e -> openSpectrumBars());

                spectrumMenu.getItems().addAll(
                        radialItem, tubeItem,
                        new javafx.scene.control.SeparatorMenuItem(),
                        circularItem, waveformItem, spiralItem, stereoMeterItem, spectrumBarsItem
                );
                menu.getItems().add(spectrumMenu);

            } else if (menu.getText().startsWith("工具")) {
                MenuItem equalizerItem = new MenuItem("均衡器");
                equalizerItem.setOnAction(e -> {
                    FxequalizerView eqView = FxequalizerView.getInstance();
                    if (eqView.isShowing()) {
                        eqView.hide();
                    } else {
                        eqView.show();
                    }
                });
                menu.getItems().add(equalizerItem);

                // 注册一次回调：每次换歌（新 MediaPlayer 就绪）时自动重绑均衡器
                AudioPlayerController.getInstance().addOnPlayerReady(this::bindEqualizer);
            }
        }
    }

    private void openRadial() {
        if (tabPane == null) {
            return;
        }
        tabPane.addOrSelect(RADIAL_TAB_ID, "圆形频谱", SpectrumUI.radialNode());
    }

    private void openTube() {
        if (tabPane == null) {
            return;
        }
        tabPane.addOrSelect(TUBE_TAB_ID, "发光试管", SpectrumUI.tubeNode());
    }

    private void openCircular() {
        if (tabPane == null) {
            return;
        }
        tabPane.addOrSelect(CIRCULAR_TAB_ID, "圆形频谱(PCM)", SpectrumUI.circularNode());
    }

    private void openWaveform() {
        if (tabPane == null) {
            return;
        }
        tabPane.addOrSelect(WAVEFORM_TAB_ID, "波形示波器", SpectrumUI.waveformNode());
    }

    private void openSpiral() {
        if (tabPane == null) {
            return;
        }
        tabPane.addOrSelect(SPIRAL_TAB_ID, "螺旋粒子", SpectrumUI.spiralNode());
    }

    private void openStereoMeter() {
        if (tabPane == null) {
            return;
        }
        tabPane.addOrSelect(STEREO_METER_TAB_ID, "立体声电平", SpectrumUI.stereoMeterNode());
    }

    private void openSpectrumBars() {
        if (tabPane == null) {
            return;
        }
        tabPane.addOrSelect(SPECTRUM_BARS_TAB_ID, "频谱柱状条", SpectrumUI.spectrumBarsNode());
    }

    /**
     * 将均衡器视图滑块与新 MediaPlayer 的音频均衡器绑定。
     * 每次 loadAudioFile 后由 AudioPlayerController 自动调用一次。
     */
    private void bindEqualizer(MediaPlayer player) {
        FxequalizerView eqView = FxequalizerView.getInstance();
        AudioEqualizer equalizer = player.getAudioEqualizer();
        equalizer.setEnabled(true);

        List<EqualizerBand> bands = equalizer.getBands();
        List<Slider> sliders = eqView.getSliders();

        int count = Math.min(bands.size(), sliders.size());
        for (int i = 0; i < count; i++) {
            EqualizerBand band = bands.get(i);
            Slider slider = sliders.get(i);

            // 先把均衡器增益同步成当前滑块值（不丢失用户已调的预设）
            band.setGain(slider.getValue());

            // 后续滑块变动实时推到新 player
            slider.valueProperty().addListener((obs, oldVal, newVal) ->
                    band.setGain(newVal.doubleValue()));
        }
    }
}
