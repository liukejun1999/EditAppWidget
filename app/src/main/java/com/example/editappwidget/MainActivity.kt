package com.example.editappwidget // 应用包名声明

// 兼容的Activity基类
import androidx.appcompat.app.AppCompatActivity
// 意图类，用于组件间通信
import android.content.Intent
// 用于保存Activity状态的Bundle类
import android.os.Bundle
// 日志工具类
import android.util.Log

// 应用主Activity，作为启动入口
class MainActivity : AppCompatActivity() {
    // 伴生对象，用于声明静态常量
    companion object {
        // 日志标签，方便过滤日志
        private const val TAG = "MainActivity"
    }

    /**
     * Activity生命周期方法：创建时调用
     * @param savedInstanceState 保存的Activity状态（可能为null）
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState) // 调用父类初始化方法

        // 记录启动日志（调试用，可观察Activity是否正常创建）
        Log.d(TAG, "MainActivity onCreate")

        // 创建意图：跳转到小部件配置管理界面（SavedWidgetConfigsActivity）
        // 第一个参数：当前Activity的上下文（this）
        // 第二个参数：目标Activity的Class对象
        val intent = Intent(this, SavedWidgetConfigsActivity::class.java)
        startActivity(intent) // 启动目标Activity

        // 记录跳转日志（调试用，确认是否成功触发跳转）
        Log.d(TAG, "Navigating to SavedWidgetConfigsActivity")

        // 关闭当前MainActivity：防止用户按返回键回到此界面（因为它仅作为中转）
        finish()
    }
}
