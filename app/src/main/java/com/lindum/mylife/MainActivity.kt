package com.lindum.mylife

import android.app.Activity
import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.util.Log
import android.view.KeyEvent
import android.view.View
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import cn.hutool.core.date.DateUtil
import com.lindum.mylife.model.OneDay
import com.lindum.mylife.ui.theme.MyLifeTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.util.Calendar
import java.util.Date
import java.util.Locale

class MainActivity : ComponentActivity() {
    private val viewModel by viewModels<OneDayViewModel>()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        hideStatusBar()
        enableEdgeToEdge()

        setContent {
            MyLifeTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    OneDayScreen(
                        modifier = Modifier.padding(innerPadding),
                        viewModel = viewModel
                    )
                }
            }
        }
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        return when (keyCode) {
            KeyEvent.KEYCODE_G -> { // 昨天 点餐按钮
                viewModel.navigateDay(-1)
                true
            }

            KeyEvent.KEYCODE_H -> { // 明天 买单按钮
                viewModel.navigateDay(1)
                true
            }

            KeyEvent.KEYCODE_F -> { // 今天 福利按钮
                viewModel.loadTodayImage()
                true
            }

            KeyEvent.KEYCODE_E -> { // 预留 服务按钮
                Log.i("press", "todo func.")
                true
            }

            else -> super.onKeyDown(keyCode, event)
        }
    }

    @Suppress("DEPRECATION")
    private fun hideStatusBar() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.setDecorFitsSystemWindows(false)
            WindowCompat.setDecorFitsSystemWindows(window, false)
            WindowInsetsControllerCompat(window, window.decorView).let { controller ->
                controller.hide(WindowInsetsCompat.Type.systemBars())
                controller.systemBarsBehavior =
                    WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
        } else {
            @Suppress("DEPRECATION")
            window.setFlags(
                WindowManager.LayoutParams.FLAG_FULLSCREEN or
                        WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
                        WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN or
                        WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
                        WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
            )

            @Suppress("DEPRECATION")
            window.decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                    or View.SYSTEM_UI_FLAG_FULLSCREEN
                    or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY)
        }
    }

}

open class OneDayViewModel(application: Application) : AndroidViewModel(application) {
    private val apiURL = "https://apiv3.shanbay.com/weapps/dailyquote/quote/?date="
    var enSentence by mutableStateOf("")
    var enSentenceTranslation by mutableStateOf("")
    var enSentenceAuthor by mutableStateOf("")
    var timeString by mutableStateOf("")
    var bitmap by mutableStateOf<Bitmap?>(null)
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    private val timeFormat = SimpleDateFormat("yyyy年MM月dd日 HH:mm\n", Locale.getDefault())

    // 适用于 Calendar（周日为1）
    private val calendarWeekDays = arrayOf("周日", "周一", "周二", "周三", "周四", "周五", "周六")
    private val calendarDay = Calendar.getInstance().get(Calendar.DAY_OF_WEEK)
    private var today: String by mutableStateOf(dateFormat.format(Date()))
    private var lastLoadedDate: String? = null

    fun loadTodayImageIfNewDay() {
        val currentDate = dateFormat.format(Date())
        if (currentDate != lastLoadedDate) {
            lastLoadedDate = currentDate
            loadImage(currentDate)
        }
    }

    fun refreshTime() {
        val chineseCalendarDay = calendarWeekDays[calendarDay - 1]
        timeString = timeFormat.format(Date()) + chineseCalendarDay
    }

    fun navigateDay(offset: Int) {
        try {
            val calendar = Calendar.getInstance().apply {
                time = dateFormat.parse(today) ?: Date()
                add(Calendar.DAY_OF_MONTH, offset)
            }
            today = dateFormat.format(calendar.time)
            loadImage(today)
        } catch (e: Exception) {
            Log.e("OneDayViewModel", "日期导航错误", e)
        }
    }

    fun loadTodayImage() {
        today = dateFormat.format(Date())
        loadImage(today)
    }

    private fun loadImage(date: String) {
        viewModelScope.launch {
            try {
                val oneDay = fetchQuote(date)
                bitmap = loadImageFromUrl(oneDay?.enImageAndSentence?.get(0).toString())
                enSentence = oneDay?.enSentence ?: ""
                enSentenceTranslation = oneDay?.enSentenceTranslation ?: ""
                enSentenceAuthor = oneDay?.enSentenceAuthor ?: ""
            } catch (e: Exception) {
                Log.e("OneDayViewModel", "加载图片失败", e)
            }
        }
    }

    private suspend fun fetchQuote(date: String): OneDay? {
        return withContext(Dispatchers.IO) {
            // 获取每日一句数据
            try {
                OneDay().apply { init("$apiURL$date") }
            } catch (e: Exception) {
                Log.e("OneDayViewModel", "获取数据失败", e)
                null
            }
        }
    }

    private suspend fun loadImageFromUrl(url: String): Bitmap? {
        return withContext(Dispatchers.IO) {
            try {
                val headers = mapOf("User-Agent" to "Mozilla/5.0")
                val inputStream = OneDay.getImgInputStream(url, headers)
                BitmapFactory.decodeStream(inputStream)
            } catch (e: Exception) {
                Log.e("OneDayViewModel", "图片加载错误", e)
                null
            }
        }
    }
}

@Composable
fun OneDayScreen(modifier: Modifier, viewModel: OneDayViewModel) {
    // 使用 LocalContext.current 但要安全地检查类型
    val context = LocalContext.current

    // 只在实际运行时（非预览模式）处理 WakeLock
    if (context is Activity) {
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager

        val wakeLock = remember {
            powerManager.newWakeLock(
                PowerManager.PROXIMITY_SCREEN_OFF_WAKE_LOCK,
                "${context.packageName}:proximity_lock"
            )
        }
        val screenWakeLock = remember {
            powerManager.newWakeLock(
                PowerManager.SCREEN_BRIGHT_WAKE_LOCK or PowerManager.ACQUIRE_CAUSES_WAKEUP,
                "${context.packageName}:screen_bright_lock"
            )
        }

        LaunchedEffect(Unit) {
            viewModel.loadTodayImage()

            while (true) {
                delay(1000)
                viewModel.refreshTime()
                val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
                val minute = Calendar.getInstance().get(Calendar.MINUTE)

                if (hour == 0 && minute == 1) {
                    viewModel.loadTodayImageIfNewDay()
                }

                if (hour in 6..22) {
                    if (!screenWakeLock.isHeld) {
                        screenWakeLock.acquire()
                    }
                    if (wakeLock.isHeld) {
                        wakeLock.release()
                    }
                } else {
                    if (!wakeLock.isHeld) {
                        wakeLock.acquire()
                    }
                    if (screenWakeLock.isHeld) {
                        screenWakeLock.release()
                    }
                }
            }
        }

        DisposableEffect(Unit) {
            onDispose {
                releaseWakeLock(wakeLock)
                releaseWakeLock(screenWakeLock)
            }
        }
    }

    // UI 部分保持不变
    Surface(modifier = modifier, color = Color.White) {
        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            // 背景图片
            viewModel.bitmap?.let {
                Image(
                    bitmap = it.asImageBitmap(),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop  // 保持长宽比并裁剪以填充
                )
            }

            // 半透明渐变遮罩，使文字更容易阅读
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.5f)
                            )
                        )
                    )
            )
            Text(
                text = viewModel.timeString,
                fontSize = 23.sp,
                color = Color.White,
                modifier = Modifier
                    .padding(32.dp, 50.dp, 0.dp, 0.dp)
            )
            // 文字内容
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Bottom
            ) {
                Text(
                    text = viewModel.enSentence,
                    color = Color.White,
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = viewModel.enSentenceTranslation,
                    color = Color.White.copy(alpha = 0.9f),
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = viewModel.enSentenceAuthor,
                    color = Color.White.copy(alpha = 0.7f),
                    style = MaterialTheme.typography.bodySmall,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun OneDayScreenPreview() {
    class PreviewViewModel : OneDayViewModel(Application()) {
        init {
            enSentence = "Life is like a box of chocolates."
            enSentenceTranslation = "生活就像一盒巧克力。"
            enSentenceAuthor = "Forrest Gump"
            timeString = "2024年11月4日 12:00\n星期一"
            bitmap = Bitmap.createBitmap(1920, 1080, Bitmap.Config.ARGB_8888).apply {
                android.graphics.Canvas(this).drawColor(Color.Gray.toArgb())
            }
        }
    }

    OneDayScreen(
        modifier = Modifier.fillMaxSize(),
        viewModel = PreviewViewModel()
    )
}

private fun releaseWakeLock(wakeLock: PowerManager.WakeLock) {
    if (wakeLock.isHeld) wakeLock.release()
}
