package com.bingbaihanji.luca.plugin.audio.spectrum.decoder;

import java.util.ServiceLoader;

/**
 * 音频解码器插件加载器
 *
 * <p>使用 Java SPI (Service Provider Interface) 机制来加载解码器。
 * 该加载器会自动发现并初始化所有在 classpath 中注册的解码器实现。</p>
 *
 * <p>工作原理:
 * <ul>
 *   <li>通过 ServiceLoader 扫描 META-INF/services/ 目录</li>
 *   <li>查找 AudioDecoder 接口的所有实现</li>
 *   <li>自动注册到 AudioDecoderFactory 工厂中</li>
 * </ul>
 * </p>
 */
public class DecoderPluginLoader {

    private final static AudioDecoderFactory factory = AudioDecoderFactory.getInstance();

    private DecoderPluginLoader() {
        // 禁止实例化
    }

    /**
     * 初始化并加载所有内置解码器
     *
     * <p>此方法会使用 ServiceLoader 自动发现所有实现了 AudioDecoder 接口的类，
     * 然后将它们注册到 AudioDecoderFactory 单例中。</p>
     *
     * <p>这个过程完全自动化，无需手动编写注册代码，只需在 META-INF/services/
     * com.bingbaihanji.luca.plugin.audio.spectrum.ui.view.widget.waveform.decoder.AudioDecoder 文件中列出实现类即可。</p>
     */
    public static void loadBuiltInDecoders() {

        //   ServiceLoader<AudioDecoder> loader = ServiceLoader.load(AudioDecoder.class);
        // 使用插件自身的 ClassLoader，避免从上下文 ClassLoader 中查找（插件环境下 TCCL 无法访问插件 JAR 内的 META-INF/services）
        ServiceLoader<AudioDecoder> loader = ServiceLoader.load(AudioDecoder.class, AudioDecoder.class.getClassLoader());

        // 遍历发现的所有实现，逐个注册到工厂
        for (AudioDecoder decoder : loader) {
            factory.registerDecoder(decoder);
        }

        System.out.println("已自动加载解码器");
        System.out.println("支持的格式: " + factory.getSupportedFormats());
    }
}
