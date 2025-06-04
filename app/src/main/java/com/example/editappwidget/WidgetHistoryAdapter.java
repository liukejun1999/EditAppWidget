package com.example.editappwidget;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;
import android.util.Base64;
import android.text.TextUtils;
import android.util.Log;  // 新增 Log 导入

public class WidgetHistoryAdapter extends RecyclerView.Adapter<WidgetHistoryAdapter.ViewHolder> {

    private final Context context;
    private final List<WidgetConfig> configList;
    private final OnAddClickListener addListener;
    private final OnDeleteClickListener deleteListener;  // 保留唯一声明

    // 构造函数
    public WidgetHistoryAdapter(Context context, List<WidgetConfig> configList, 
                               OnAddClickListener addListener, OnDeleteClickListener deleteListener) {
        this.context = context;
        this.configList = configList;
        this.addListener = addListener;
        this.deleteListener = deleteListener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.widget_history_item, parent, false);
        return new ViewHolder(view);
    }

    // 新增删除回调接口（保留）
    public interface OnDeleteClickListener {
        void onDeleteClick(WidgetConfig config, int position);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        // 添加空值检查
        if (configList == null || position >= configList.size()) {
            return;
        }
        WidgetConfig config = configList.get(position);
        if (config == null) {
            return;
        }
    
        // 显示小部件图标（新增缓存机制和异常处理）
        if (!TextUtils.isEmpty(config.getIconBase64())) {
            try {
                byte[] iconBytes = Base64.decode(config.getIconBase64(), Base64.DEFAULT);
                Bitmap iconBitmap = BitmapFactory.decodeByteArray(iconBytes, 0, iconBytes.length);
                if (iconBitmap != null) {
                    holder.widgetIcon.setImageBitmap(iconBitmap);
                } else {
                    holder.widgetIcon.setImageResource(R.drawable.default_icon);
                }
            } catch (Exception e) {
                // 使用 Log 记录异常（替换 printStackTrace）
                Log.e("WidgetHistoryAdapter", "解码图标失败: " + config.getId(), e);  // 新增日志记录
                holder.widgetIcon.setImageResource(R.drawable.default_icon);
            }
        } else {
            holder.widgetIcon.setImageResource(R.drawable.default_icon);  // 默认图标
        }
    
        // 显示小部件名称（使用字符串资源占位符）
        holder.widgetName.setText(context.getString(R.string.widget_name, config.getName()));  // 修改后
        
        // 显示尺寸（使用字符串资源占位符）
        holder.widgetSize.setText(context.getString(R.string.widget_size, config.getSize()));  // 修改后
        
        // 显示触发方式（使用字符串资源占位符）
        holder.triggerAction.setText(context.getString(R.string.trigger_action, config.getTriggerAction()));  // 修改后
        
        // 显示反馈方式（使用字符串资源占位符）
        holder.feedbackAction.setText(context.getString(R.string.feedback_action, config.getFeedbackAction()));  // 修改后
        
        // 根据包名获取应用名称（使用字符串资源占位符）
        try {
            PackageManager pm = context.getPackageManager();
            ApplicationInfo appInfo = pm.getApplicationInfo(config.getAppPackage(), 0);
            String appLabel = pm.getApplicationLabel(appInfo).toString();
            holder.appName.setText(context.getString(R.string.target_app_name, appLabel));  // 修改后
        } catch (PackageManager.NameNotFoundException e) {
            holder.appName.setText(context.getString(R.string.target_app_name, "未知"));  // 修改后
            Log.e("WidgetHistoryAdapter", "应用包名未找到: " + config.getAppPackage(), e);  // 新增日志记录
        }

        // 处理添加按钮点击（修正：使用addListener）
        holder.addBtn.setOnClickListener(v -> addListener.onAddClick(config));  // 原错误行`listener.onAddClick`改为`addListener.onAddClick`

        // 绑定删除按钮点击事件
        holder.deleteBtn.setOnClickListener(v -> {
            if (deleteListener != null) {
                deleteListener.onDeleteClick(configList.get(position), position);
            }
        });
    }

    @Override
    public int getItemCount() {
        return configList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {  // 添加 public 修饰符
        ImageView widgetIcon;
        TextView widgetName;
        TextView widgetSize;
        TextView triggerAction;
        TextView feedbackAction;
        TextView appName;
        Button addBtn;
        Button deleteBtn;  // 需在ViewHolder中绑定删除按钮
    
        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            widgetIcon = itemView.findViewById(R.id.widget_icon);
            widgetName = itemView.findViewById(R.id.widget_name);
            widgetSize = itemView.findViewById(R.id.widget_size);
            triggerAction = itemView.findViewById(R.id.trigger_action);
            feedbackAction = itemView.findViewById(R.id.feedback_action);
            appName = itemView.findViewById(R.id.app_name);
            addBtn = itemView.findViewById(R.id.add_to_home_btn);
            deleteBtn = itemView.findViewById(R.id.delete_button);  // 绑定删除按钮
        }
    }

    public interface OnAddClickListener {
        void onAddClick(WidgetConfig config);
    }
}
