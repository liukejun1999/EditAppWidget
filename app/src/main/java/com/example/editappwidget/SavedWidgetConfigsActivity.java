package com.example.editappwidget;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import java.util.ArrayList;
import java.util.List;

public class SavedWidgetConfigsActivity extends AppCompatActivity {

    private ListView savedWidgetConfigsListView;
    private List<String> savedWidgetConfigs;
    private List<String> widgetIds;
    private SharedPreferences sharedPreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_saved_widget_configs);

        // 初始化视图组件
        savedWidgetConfigsListView = findViewById(R.id.saved_widget_configs_list_view);

        // 获取 SharedPreferences 实例
        sharedPreferences = getSharedPreferences("WidgetConfigs", MODE_PRIVATE);

        // 加载已保存的小部件配置
        loadSavedWidgetConfigs();

        // 设置列表适配器
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                R.layout.saved_widget_config_item,
                R.id.config_text,
                savedWidgetConfigs
        ) {
            @Override
            public View getView(int position, View convertView, android.view.ViewGroup parent) {
                View view = super.getView(position, convertView, parent);
                Button deleteButton = view.findViewById(R.id.delete_button);
                deleteButton.setOnClickListener(v -> {
                    deleteWidgetConfig(widgetIds.get(position));
                    savedWidgetConfigs.remove(position);
                    widgetIds.remove(position);
                    notifyDataSetChanged();
                });
                return view;
            }
        };

        savedWidgetConfigsListView.setAdapter(adapter);
    }

    private void loadSavedWidgetConfigs() {
        savedWidgetConfigs = new ArrayList<>();
        widgetIds = new ArrayList<>();

        // 遍历所有保存的小部件配置
        for (String key : sharedPreferences.getAll().keySet()) {
            if (key.startsWith("widget_") && key.endsWith("_name")) {
                String widgetId = key.substring(7, key.length() - 5);
                String widgetName = sharedPreferences.getString(key, "未命名");
                String widgetSize = sharedPreferences.getString("widget_" + widgetId + "_size", "未知尺寸");
                String triggerAction = sharedPreferences.getString("widget_" + widgetId + "_trigger_action", "未知触发方式");
                String feedbackAction = sharedPreferences.getString("widget_" + widgetId + "_feedback_action", "未知反馈方式");
                String appPackageName = sharedPreferences.getString("widget_" + widgetId + "_app_package", "未选择应用");

                String config = "名称: " + widgetName +
                        "\n尺寸: " + widgetSize +
                        "\n触发方式: " + triggerAction +
                        "\n反馈方式: " + feedbackAction +
                        "\n应用包名: " + appPackageName;

                savedWidgetConfigs.add(config);
                widgetIds.add(widgetId);
            }
        }
    }

    private void deleteWidgetConfig(String widgetId) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.remove("widget_" + widgetId + "_name");
        editor.remove("widget_" + widgetId + "_size");
        editor.remove("widget_" + widgetId + "_trigger_action");
        editor.remove("widget_" + widgetId + "_feedback_action");
        editor.remove("widget_" + widgetId + "_app_package");
        editor.apply();

        // 删除关联的 appWidgetId 配置
        for (String key : sharedPreferences.getAll().keySet()) {
            if (key.startsWith("appwidget_") && key.endsWith("_widget_id")) {
                String storedWidgetId = sharedPreferences.getString(key, null);
                if (widgetId.equals(storedWidgetId)) {
                    editor.remove(key);
                    editor.apply();
                    break;
                }
            }
        }

        Toast.makeText(this, "配置已删除", Toast.LENGTH_SHORT).show();
        CustomAppWidgetProvider.updateAllWidgets(this);
    }
}