package com.bingbaihanji.luca.plugin.audio.spectrum.core.capture.view.base;

import javafx.beans.property.SimpleStringProperty;

/**
 * Widget ViewModel 基类
 * <p>
 * 所有 Widget 的数据模型都应继承此类
 */
public abstract class WidgetViewModel {

    private final SimpleStringProperty title = new SimpleStringProperty("Widget");

    public SimpleStringProperty titleProperty() {
        return title;
    }

    public String getTitle() {
        return title.get();
    }

    public void setTitle(String title) {
        this.title.set(title);
    }
}
