package com.example.editappwidget;

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
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

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
    private ActivityResultLauncher<Intent> imagePickerLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_app_widget);

        // 初始化Activity结果回调
        imagePickerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        Uri uri = result.getData().getData();
                        try {
                            InputStream inputStream = getContentResolver().openInputStream(uri);
                            selectedIconBitmap = BitmapFactory.decodeStream(inputStream);
                            widgetIconImageView.setImageBitmap(selectedIconBitmap);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
        );

        // 初始化视图组件
        initViews();

        // 初始化应用列表
        initAppList();

        // 初始化尺寸选择器
        initSizeSpinner();

        // 设置图标选择按钮点击事件
        selectIconButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openImageSelector();
            }
        });

        // 设置保存按钮点击事件
        saveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveWidgetConfiguration();
            }
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
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        // 使用ActivityResultLauncher替代startActivityForResult
        imagePickerLauncher.launch(Intent.createChooser(intent, "选择图标"));
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // 保留原方法，但空实现，因为我们使用了ActivityResultLauncher
        super.onActivityResult(requestCode, resultCode, data);
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

        // 保存图标（这里保存为Base64编码，实际应用中可能需要更高效的方法）
        // 此处简化处理，实际应用中建议使用文件存储或ContentProvider
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
}