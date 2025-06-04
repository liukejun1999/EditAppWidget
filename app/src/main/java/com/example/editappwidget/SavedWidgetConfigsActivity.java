package com.example.editappwidget;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

import android.annotation.SuppressLint;
import android.os.Bundle;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import android.content.Intent;
import android.content.ComponentName;
import android.content.SharedPreferences;
import android.widget.Toast;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.annotation.NonNull;
import android.widget.TextView;
import android.appwidget.AppWidgetManager;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;

public class SavedWidgetConfigsActivity extends AppCompatActivity 
        implements WidgetHistoryAdapter.OnAddClickListener, WidgetHistoryAdapter.OnDeleteClickListener {

    // 用于启动添加小部件的Activity结果回调（需初始化）
    private ActivityResultLauncher<Intent> addWidgetLauncher; 
    // 共享偏好存储，用于保存小部件配置
    private SharedPreferences sharedPreferences;
    // 当前加载的小部件配置列表
    private List<WidgetConfig> configList; 
    // RecyclerView适配器，用于展示配置列表
    private WidgetHistoryAdapter adapter; 

    @SuppressLint("NotifyDataSetChanged")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_saved_widget_configs);

        // 初始化Toolbar（顶部导航栏）
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // 初始化共享偏好存储（模式为私有，仅当前应用可访问）
        sharedPreferences = getSharedPreferences("WidgetConfigs", MODE_PRIVATE);
        // 加载已保存的小部件配置（从SharedPreferences解析）
        configList = loadSavedConfigs();

        // 处理空状态与RecyclerView的显示逻辑
        TextView emptyState = findViewById(R.id.empty_state);
        RecyclerView recyclerView = findViewById(R.id.recycler_view);
        if (configList.isEmpty()) {
            // 无配置时显示空状态提示
            emptyState.setVisibility(View.VISIBLE);
            recyclerView.setVisibility(View.GONE);
        } else {
            // 有配置时显示RecyclerView列表
            emptyState.setVisibility(View.GONE);
            recyclerView.setVisibility(View.VISIBLE);
            // 初始化适配器（传入当前Activity作为回调）
            adapter = new WidgetHistoryAdapter(this, configList, this, this);
            recyclerView.setLayoutManager(new LinearLayoutManager(this));
            recyclerView.setAdapter(adapter);
        }

        // 新增：初始化ActivityResultLauncher（关键！避免空指针）
        addWidgetLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    // 可选：处理添加小部件后的回调逻辑（如刷新列表）
                    if (result.getResultCode() == RESULT_OK) {
                        configList = loadSavedConfigs();
                        adapter.notifyDataSetChanged();
                    }
                }
        );
    }

    // 创建顶部菜单（加号图标用于跳转添加配置页面）
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_saved_configs, menu);
        return true;
    }

    // 处理菜单点击事件（加号图标跳转）
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.action_add) {
            // 跳转到添加小部件配置的页面（EditAppWidgetActivity）
            startActivity(new Intent(this, EditAppWidgetActivity.class));
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    // 配置项删除回调（用户点击删除按钮触发）
    @Override
    public void onDeleteClick(WidgetConfig config, int position) {
        // 显示删除确认对话框
        new AlertDialog.Builder(this)
                .setTitle("删除确认")
                .setMessage("确定要删除「" + config.getName() + "」吗？")
                .setPositiveButton("确认", (dialog, which) -> {
                    // 1. 从SharedPreferences中删除对应配置的所有键
                    SharedPreferences.Editor editor = sharedPreferences.edit();
                    editor.remove("widget_" + config.getId() + "_name");
                    editor.remove("widget_" + config.getId() + "_size");
                    editor.remove("widget_" + config.getId() + "_trigger_action");
                    editor.remove("widget_" + config.getId() + "_feedback_action");
                    editor.remove("widget_" + config.getId() + "_app_package");
                    editor.apply();

                    // 2. 从列表中移除数据并刷新RecyclerView（动画效果）
                    configList.remove(position);
                    adapter.notifyItemRemoved(position);
                })
                .setNegativeButton("取消", null)
                .show();
    }

    // 加载已保存的小部件配置（从SharedPreferences解析）
    private List<WidgetConfig> loadSavedConfigs() {
        List<WidgetConfig> list = new ArrayList<>();
        // 使用正则表达式匹配存储的键（格式：widget_<id>_name）
        Pattern pattern = Pattern.compile("widget_(.+)_name"); 
        for (String key : sharedPreferences.getAll().keySet()) {
            Matcher matcher = pattern.matcher(key);
            if (matcher.find()) {
                String widgetId = matcher.group(1);
                // 读取各配置字段（添加空值默认值）
                String name = sharedPreferences.getString("widget_" + widgetId + "_name", "未知名称");
                String size = sharedPreferences.getString("widget_" + widgetId + "_size", "未知尺寸");
                String trigger = sharedPreferences.getString("widget_" + widgetId + "_trigger_action", "未知触发");
                String feedback = sharedPreferences.getString("widget_" + widgetId + "_feedback_action", "未知反馈");
                String pkg = sharedPreferences.getString("widget_" + widgetId + "_app_package", "");
                String iconBase64 = sharedPreferences.getString("widget_" + widgetId + "_icon", "");  // 图标Base64字符串
                // 创建WidgetConfig对象并添加到列表
                list.add(new WidgetConfig(widgetId, name, size, trigger, feedback, pkg, iconBase64));
            }
        }
        return list;
    }

    // 用户点击添加按钮时触发（将配置应用到主屏幕小部件）
    @Override
    public void onAddClick(WidgetConfig config) {
        addWidgetToHomeScreen(config);
    }

    // 将小部件配置绑定到主屏幕（需系统权限支持）
    private void addWidgetToHomeScreen(WidgetConfig config) {
        // 创建绑定小部件的Intent（系统标准Action）
        Intent addIntent = new Intent(AppWidgetManager.ACTION_APPWIDGET_BIND);
        addIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID);
        // 指定小部件提供者（需与AndroidManifest.xml中注册的Provider一致）
        addIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_PROVIDER, new ComponentName(this, CustomAppWidgetProvider.class));

        // 关联当前配置ID（用于后续小部件更新时查找配置）
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString("appwidget_" + AppWidgetManager.INVALID_APPWIDGET_ID + "_widget_id", config.getId());
        editor.apply();

        // 检查是否有应用可以处理该Intent（避免崩溃）
        if (addIntent.resolveActivity(getPackageManager()) != null) {
            // 启动系统添加小部件界面（通过已初始化的Launcher）
            addWidgetLauncher.launch(addIntent);
        } else {
            Toast.makeText(this, "不支持添加小部件", Toast.LENGTH_SHORT).show();
        }
    }
}