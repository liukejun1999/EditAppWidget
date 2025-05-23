package com.example.editappwidget;

import android.appwidget.AppWidgetManager;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import java.util.ArrayList;
import java.util.List;

public class WidgetConfigureActivity extends AppCompatActivity {

    private int appWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID;
    private ListView widgetTemplatesListView;
    private List<String> widgetTemplateIds;
    private List<String> widgetTemplateNames;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_widget_configure);

        // 获取启动此Activity的Intent中的appWidgetId
        Intent intent = getIntent();
        Bundle extras = intent.getExtras();
        if (extras != null) {
            appWidgetId = extras.getInt(
                    AppWidgetManager.EXTRA_APPWIDGET_ID,
                    AppWidgetManager.INVALID_APPWIDGET_ID
            );
        }

        // 如果appWidgetId无效，结束Activity
        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish();
            return;
        }

        // 初始化视图组件
        widgetTemplatesListView = findViewById(R.id.widget_templates_list_view);

        // 加载已保存的小部件模板
        loadWidgetTemplates();

        // 设置列表项点击事件
        widgetTemplatesListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                String selectedWidgetId = widgetTemplateIds.get(position);
                saveWidgetConfiguration(selectedWidgetId);
            }
        });
    }

    private void loadWidgetTemplates() {
        widgetTemplateIds = new ArrayList<>();
        widgetTemplateNames = new ArrayList<>();

        SharedPreferences prefs = getSharedPreferences("WidgetConfigs", MODE_PRIVATE);

        // 遍历所有保存的小部件配置
        for (String key : prefs.getAll().keySet()) {
            if (key.startsWith("widget_") && key.endsWith("_name")) {
                String widgetId = key.substring(7, key.length() - 5);
                String widgetName = prefs.getString(key, "未命名");

                widgetTemplateIds.add(widgetId);
                widgetTemplateNames.add(widgetName);
            }
        }

        // 设置列表适配器
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_list_item_1,
                widgetTemplateNames
        );

        widgetTemplatesListView.setAdapter(adapter);
    }

    private void saveWidgetConfiguration(String widgetId) {
        // 保存appWidgetId与widgetId的关联
        SharedPreferences prefs = getSharedPreferences("WidgetConfigs", MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString("appwidget_" + appWidgetId + "_widget_id", widgetId);
        editor.apply();

        // 更新小部件
        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(this);
        CustomAppWidgetProvider.updateAppWidget(this, appWidgetManager, appWidgetId);

        // 设置结果并结束Activity
        Intent resultValue = new Intent();
        resultValue.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
        setResult(RESULT_OK, resultValue);
        finish();
    }
}