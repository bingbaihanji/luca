package com.bingbaihanji.luca.plugin.audio.spectrum.core.capture.view.spectrogram;

import com.bingbaihanji.luca.plugin.audio.spectrum.core.capture.view.base.WidgetViewModel;
import javafx.scene.image.WritableImage;

/**
 * Spectrogram Widget 的 ViewModel
 */
public class SpectrogramViewModel extends WidgetViewModel {

    private WritableImage image;

    public SpectrogramViewModel() {
        setTitle("频谱图");
    }

    public WritableImage getImage() {
        return image;
    }

    public void updateImage(WritableImage img) {
        this.image = img;
    }
}
