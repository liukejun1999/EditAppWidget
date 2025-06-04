package com.example.editappwidget;

public class WidgetConfig {
    private final String id;
    private final String name;
    private final String size;
    private final String triggerAction;
    private final String feedbackAction;
    private final String appPackage;
    private final String iconBase64;  // 图标 Base64 字符串

    public WidgetConfig(String id, String name, String size, String triggerAction, String feedbackAction, String appPackage, String iconBase64) {
        this.id = id;
        this.name = name;
        this.size = size;
        this.triggerAction = triggerAction;
        this.feedbackAction = feedbackAction;
        this.appPackage = appPackage;
        this.iconBase64 = iconBase64;
    }

    // getter 方法
    public String getIconBase64() { return iconBase64; }
    public String getId() { return id; }
    public String getName() { return name; }
    public String getSize() { return size; }
    public String getTriggerAction() { return triggerAction; }
    public String getFeedbackAction() { return feedbackAction; }
    public String getAppPackage() { return appPackage; }
}