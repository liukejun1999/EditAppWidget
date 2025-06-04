package com.example.editappwidget;

import android.util.Base64;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.Toast;
import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.io.ByteArrayOutputStream;

public class EditAppWidgetActivity extends AppCompatActivity {

    private EditText widgetNameEditText;
    private Spinner widgetSizeSpinner;
    private ImageView widgetIconImageView;
    private Button selectIconButton;
    private RadioGroup triggerActionRadioGroup;
    private RadioGroup feedbackActionRadioGroup;
    private RecyclerView appListRecyclerView;
    private Button saveButton;

    private static final int PICK_IMAGE_REQUEST = 1;
    private Bitmap selectedIconBitmap;
    private String selectedAppPackageName;
    private AppListAdapter appListAdapter;
    private List<AppInfo> installedApps;
    private SharedPreferences sharedPreferences;

    // 注册Activity结果回调
    private ActivityResultLauncher<String> imagePickerLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_app_widget);
        
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        
        // 设置返回按钮点击事件（左上角箭头）
        toolbar.setNavigationOnClickListener(v -> {
            finish(); // 关闭当前页面，自动返回上一级（我的小部件页面）
        });
    
        // 新增：初始化关键组件
        initViews();          // 绑定控件
        initAppList();        // 加载应用列表
        initSizeSpinner();    // 初始化尺寸选择器
        initImagePickerLauncher(); // 注册图片选择器
    
        // 保存按钮点击事件
        Button saveButton = findViewById(R.id.save_button);
        // 修改为调用正确的保存方法
        saveButton.setOnClickListener(v -> {
            // 执行保存逻辑（如存储配置到数据库/SharedPreferences）
            saveWidgetConfiguration(); 
            // 假设保存方法中已经处理了错误情况，这里直接关闭页面
            finish(); // 关闭页面，返回我的小部件页面
        });
    }

    private void initViews() {
        widgetNameEditText = findViewById(R.id.widget_name_edit_text);
        widgetSizeSpinner = findViewById(R.id.widget_size_spinner);
        widgetIconImageView = findViewById(R.id.widget_icon_image_view);
        selectIconButton = findViewById(R.id.select_icon_button);
        triggerActionRadioGroup = findViewById(R.id.trigger_action_radio_group);
        feedbackActionRadioGroup = findViewById(R.id.feedback_action_radio_group);
        appListRecyclerView = findViewById(R.id.app_list_recycler_view);
        saveButton = findViewById(R.id.save_button);

        // 默认选中第一个触发方式和反馈方式
        triggerActionRadioGroup.check(R.id.click_radio_button);
        feedbackActionRadioGroup.check(R.id.launch_app_radio_button);

        // 使用Context.MODE_PRIVATE替代MODE_PRIVATE
        sharedPreferences = getSharedPreferences("WidgetConfigs", Context.MODE_PRIVATE);

        // 新增：绑定图标选择按钮点击事件
        selectIconButton.setOnClickListener(v -> openImageSelector());
    }

    private void initAppList() {
        installedApps = getInstalledApps();
        appListAdapter = new AppListAdapter(installedApps, new AppListAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(AppInfo appInfo) {
                selectedAppPackageName = appInfo.packageName;
                Toast.makeText(EditAppWidgetActivity.this, "已选择: " + appInfo.name, Toast.LENGTH_SHORT).show();
            }
        });

        appListRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        appListRecyclerView.setAdapter(appListAdapter);
    }

    private List<AppInfo> getInstalledApps() {
        List<AppInfo> apps = new ArrayList<>();
        PackageManager pm = getPackageManager();

        // 使用getInstalledPackages替代getInstalledApplications
        List<PackageInfo> installedPackages = pm.getInstalledPackages(0);

        for (PackageInfo packageInfo : installedPackages) {
            ApplicationInfo appInfo = packageInfo.applicationInfo;
            if ((appInfo.flags & ApplicationInfo.FLAG_SYSTEM) == 0) {
                // 只显示非系统应用
                String appName = pm.getApplicationLabel(appInfo).toString();
                String packageName = appInfo.packageName;
                apps.add(new AppInfo(appName, packageName, appInfo.loadIcon(pm)));
            }
        }

        return apps;
    }

    private void initSizeSpinner() {
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
                R.array.widget_sizes, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        widgetSizeSpinner.setAdapter(adapter);
    }

    private void openImageSelector() {
        // 修正：直接传入 MIME 类型 "image/*"，无需手动创建 Intent
        imagePickerLauncher.launch("image/*");
    }

    private void saveWidgetConfiguration() {
        String widgetName = widgetNameEditText.getText().toString().trim();
        String widgetSize = widgetSizeSpinner.getSelectedItem().toString();
        String triggerAction = getSelectedTriggerAction();
        String feedbackAction = getSelectedFeedbackAction();

        // 校验输入
        if (TextUtils.isEmpty(widgetName)) {
            widgetNameEditText.setError("小部件名称不能为空");
            return;
        }

        if (widgetName.length() > 20) {
            widgetNameEditText.setError("小部件名称不能超过20个字符");
            return;
        }

        if (selectedIconBitmap == null) {
            Toast.makeText(this, "请选择小部件图标", Toast.LENGTH_SHORT).show();
            return;
        }

        if (selectedAppPackageName == null) {
            Toast.makeText(this, "请选择要启动的应用", Toast.LENGTH_SHORT).show();
            return;
        }

        // 生成唯一ID
        String widgetId = UUID.randomUUID().toString();

        // 保存配置到SharedPreferences
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString("widget_" + widgetId + "_name", widgetName);
        editor.putString("widget_" + widgetId + "_size", widgetSize);
        editor.putString("widget_" + widgetId + "_trigger_action", triggerAction);
        editor.putString("widget_" + widgetId + "_feedback_action", feedbackAction);
        editor.putString("widget_" + widgetId + "_app_package", selectedAppPackageName);

        // 保存图标（Base64编码）
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        selectedIconBitmap.compress(Bitmap.CompressFormat.PNG, 100, baos);
        byte[] iconBytes = baos.toByteArray();
        String iconBase64 = Base64.encodeToString(iconBytes, Base64.DEFAULT);

        // 保存配置到SharedPreferences（新增图标字段）
        editor.putString("widget_" + widgetId + "_icon", iconBase64); // 新增图标保存
        editor.apply();

        Toast.makeText(this, "小部件配置已保存", Toast.LENGTH_SHORT).show();
        finish();
    }

    private String getSelectedTriggerAction() {
        int selectedId = triggerActionRadioGroup.getCheckedRadioButtonId();
        if (selectedId == R.id.click_radio_button) {
            return "click";
        }
        return "click"; // 默认返回点击
    }

    private String getSelectedFeedbackAction() {
        int selectedId = feedbackActionRadioGroup.getCheckedRadioButtonId();
        if (selectedId == R.id.launch_app_radio_button) {
            return "launch_app";
        }
        return "launch_app"; // 默认返回启动应用
    }

    // 新增：初始化图片选择器Launcher的方法
    private void initImagePickerLauncher() {
        imagePickerLauncher = registerForActivityResult(
                new ActivityResultContracts.GetContent(),
                result -> {
                    if (result != null) {
                        ImageView widgetIconImageView = findViewById(R.id.widget_icon_image_view);
                        widgetIconImageView.setImageURI(result);
                        // 新增：保存选中的 Bitmap（解决后续保存图标时的空值问题）
                        try (InputStream inputStream = getContentResolver().openInputStream(result)) {
                            if (inputStream != null) {
                                selectedIconBitmap = BitmapFactory.decodeStream(inputStream);
                            }
                        } catch (IOException e) {
                            e.printStackTrace();
                            // 添加用户提示
                            Toast.makeText(EditAppWidgetActivity.this, "图标选择失败，请重试", Toast.LENGTH_SHORT).show();
                        }
                    }
                }
        );
    }
}