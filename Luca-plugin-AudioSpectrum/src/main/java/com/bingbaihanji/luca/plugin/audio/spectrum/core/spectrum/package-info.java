// javafx 简单的音频组件 通过 javafx.scene.media.AudioSpectrumListener 接口获取频谱数据:
///
/// ```java
///    MediaPlayer mediaPlayer = new MediaPlayer(media);
///    mediaPlayer.setAudioSpectrumListener((timestamp, duration, magnitudes, phases) -> {
///       updateSpectrum(magnitudes);
///    }
///
///
/// ```
///
package com.bingbaihanji.luca.plugin.audio.spectrum.core.spectrum;
