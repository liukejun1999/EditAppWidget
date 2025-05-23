package com.example.editappwidget;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.widget.RemoteViews;

public class CustomAppWidgetProvider extends AppWidgetProvider {

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        // 对每个小部件实例进行更新
        for (int appWidgetId : appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId);
        }
    }

    static void updateAppWidget(Context context, AppWidgetManager appWidgetManager, int appWidgetId) {
        // 获取小部件配置
        SharedPreferences prefs = context.getSharedPreferences("WidgetConfigs", Context.MODE_PRIVATE);
        String widgetId = prefs.getString("appwidget_" + appWidgetId + "_widget_id", null);

        if (widgetId == null) {
            // 如果找不到对应的配置，使用默认值或返回
            return;
        }

        String widgetName = prefs.getString("widget_" + widgetId + "_name", "未命名小部件");
        String appPackageName = prefs.getString("widget_" + widgetId + "_app_package", null);

        // 创建RemoteViews对象，设置小部件布局
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.custom_app_widget);
        views.setTextViewText(R.id.appwidget_text, widgetName);

        // 设置点击事件
        if (appPackageName != null) {
            // 创建启动目标应用的PendingIntent
            Intent launchIntent = context.getPackageManager().getLaunchIntentForPackage(appPackageName);
            if (launchIntent != null) {
                PendingIntent pendingIntent = PendingIntent.getActivity(
                        context,
                        0,
                        launchIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
                );

                // 设置点击事件
                views.setOnClickPendingIntent(R.id.appwidget_layout, pendingIntent);
            }
        }

        // 更新小部件
        appWidgetManager.updateAppWidget(appWidgetId, views);
    }

    @Override
    public void onDeleted(Context context, int[] appWidgetIds) {
        // 小部件被删除时调用
        SharedPreferences prefs = context.getSharedPreferences("WidgetConfigs", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();

        for (int appWidgetId : appWidgetIds) {
            editor.remove("appwidget_" + appWidgetId + "_widget_id");
        }

        editor.apply();
    }

    @Override
    public void onEnabled(Context context) {
        // 第一个小部件实例被创建时调用
    }

    @Override
    public void onDisabled(Context context) {
        // 最后一个小部件实例被删除时调用
    }

    public static void updateAllWidgets(Context context) {
        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
        ComponentName thisWidget = new ComponentName(context, CustomAppWidgetProvider.class);
        int[] appWidgetIds = appWidgetManager.getAppWidgetIds(thisWidget);

        if (appWidgetIds.length > 0) {
            CustomAppWidgetProvider provider = new CustomAppWidgetProvider();
            provider.onUpdate(context, appWidgetManager, appWidgetIds);
        }
    }
}