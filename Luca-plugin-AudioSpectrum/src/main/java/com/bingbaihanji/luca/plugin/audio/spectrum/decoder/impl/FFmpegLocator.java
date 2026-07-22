package com.bingbaihanji.luca.plugin.audio.spectrum.decoder.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ws.schild.jave.process.ProcessLocator;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

/**
 * 自定义 FFmpeg 可执行文件定位器。
 *
 * <p>解决宿主项目打包为 fat-jar 后，jave 默认的 {@code DefaultFFMPEGLocator}
 * 无法从嵌套 jar 中正确提取 ffmpeg.exe 的问题。</p>
 *
 * <p>实现原理：在首次使用时，通过当前类的 {@link ClassLoader} 从 classpath
 * 中读取 {@code ws/schild/jave/nativebin/ffmpeg-amd64.exe} 资源，并将其复制到
 * 系统临时目录，后续直接返回该绝对路径。</p>
 */
public class FFmpegLocator implements ProcessLocator {

    private static final Logger LOG = LoggerFactory.getLogger(FFmpegLocator.class);

    /**
     * ffmpeg 资源在 classpath 中的路径
     */
    private static final String FFMPEG_RESOURCE = "ws/schild/jave/nativebin/ffmpeg-amd64.exe";

    /**
     * 临时目录：{@code java.io.tmpdir/jave}
     */
    private static final File TEMP_DIR = new File(
            System.getProperty("java.io.tmpdir"), "jave"
    );

    /**
     * 提取后的可执行文件名称，与 jave 默认命名保持一致
     */
    private static final String EXE_NAME;
    private static final Object LOCK = new Object();
    private static volatile String executablePath;

    static {
        String arch = System.getProperty("os.arch");
        String version = ws.schild.jave.Version.getVersion();
        String suffix = System.getProperty("os.name", "").toLowerCase().contains("windows") ? ".exe" : "";
        EXE_NAME = "ffmpeg-" + arch + "-" + version + suffix;
    }

    @Override
    public String getExecutablePath() {
        if (executablePath != null) {
            return executablePath;
        }
        synchronized (LOCK) {
            if (executablePath != null) {
                return executablePath;
            }

            File target = new File(TEMP_DIR, EXE_NAME);
            if (!target.exists()) {
                extract(target);
            }

            if (!target.exists()) {
                throw new IllegalStateException(
                        "无法提取 FFmpeg 可执行文件，目标文件不存在: " + target.getAbsolutePath()
                );
            }

            executablePath = target.getAbsolutePath();
            LOG.debug("FFmpeg 可执行文件路径: {}", executablePath);
            return executablePath;
        }
    }

    /**
     * 从 classpath 中提取 ffmpeg 到指定文件。
     *
     * @param target 目标文件
     */
    private void extract(File target) {
        if (!TEMP_DIR.exists() && !TEMP_DIR.mkdirs()) {
            LOG.warn("创建临时目录失败: {}", TEMP_DIR.getAbsolutePath());
        }

        InputStream in = locateResource();
        if (in == null) {
            throw new IllegalStateException(
                    "在 classpath 中找不到 FFmpeg 资源: " + FFMPEG_RESOURCE +
                            "，请确保 jave-nativebin-win64 依赖已正确打包。"
            );
        }

        try (in) {
            Files.copy(in, target.toPath(), StandardCopyOption.REPLACE_EXISTING);
            LOG.debug("已将 FFmpeg 从 classpath 复制到: {}", target.getAbsolutePath());
        } catch (IOException e) {
            throw new IllegalStateException("复制 FFmpeg 可执行文件失败: " + target.getAbsolutePath(), e);
        }
    }

    /**
     * 尝试多种 ClassLoader 策略定位资源。
     *
     * @return 资源输入流，若找不到则返回 {@code null}
     */
    private InputStream locateResource() {
        // 1. 使用当前类的 ClassLoader（在插件/模块化环境中通常最可靠）
        ClassLoader cl = FFmpegLocator.class.getClassLoader();
        if (cl != null) {
            InputStream in = cl.getResourceAsStream(FFMPEG_RESOURCE);
            if (in != null) {
                LOG.debug("通过 FFmpegLocator.class.getClassLoader() 找到资源");
                return in;
            }
        }

        // 2. 线程上下文 ClassLoader（Spring Boot / 自定义容器常用）
        ClassLoader ctxCl = Thread.currentThread().getContextClassLoader();
        if (ctxCl != null) {
            InputStream in = ctxCl.getResourceAsStream(FFMPEG_RESOURCE);
            if (in != null) {
                LOG.debug("通过 Thread.currentThread().getContextClassLoader() 找到资源");
                return in;
            }
        }

        // 3. 系统 ClassLoader
        InputStream in = ClassLoader.getSystemResourceAsStream(FFMPEG_RESOURCE);
        if (in != null) {
            LOG.debug("通过 ClassLoader.getSystemResourceAsStream() 找到资源");
        }
        return in;
    }
}
