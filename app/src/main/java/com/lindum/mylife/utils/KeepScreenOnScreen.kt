package com.lindum.mylife.utils

/**
 * 保持屏幕常亮工具类
 *
 * @author zhuhao
 * @date  01:46
 **/
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalView
import android.view.WindowManager
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import android.app.Activity
import androidx.compose.ui.unit.dp

@Composable
fun KeepScreenOnScreen() {
    // 获取当前视图
    val view = LocalView.current
    val context = LocalContext.current

    // 保持屏幕常亮状态
    DisposableEffect(Unit) {
        val window = (context as Activity).window
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        onDispose {
            // 当组件销毁时移除屏幕常亮
            window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    // 示例UI内容
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "这个界面会保持屏幕常亮",
            style = MaterialTheme.typography.headlineSmall
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "适用于视频播放、阅读等场景",
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

// 如果只想在特定组件中使用保持屏幕常亮的功能，可以封装成以下可重用的组件
@Composable
fun KeepScreenOn() {
    val context = LocalContext.current

    DisposableEffect(Unit) {
        val window = (context as Activity).window
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        onDispose {
            window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }
}