package net.kdt.pojavlaunch.mobileglues.ui
import net.kdt.pojavlaunch.R

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import net.kdt.pojavlaunch.mobileglues.ui.material.MaterialApp

/**
 * 根：目前只有一套皮肤（MD3），这一层只负责跑分期间的保活。
 */
@Composable
fun MobileGluesApp(controller: AppController) {

    // 跑分期间不许自动熄屏。一次跑分要到一分多钟，而全程没有任何触摸——正是系统认定
    // 「用户走开了」的样子。屏幕一灭，测的就不再是游戏里那块 GPU 的状态：合成停了、
    // 频率策略换了，结果既不可比也不可信，而用户回来只看到一份莫名其妙的排名。
    val benchState by controller.benchState.collectAsStateWithLifecycle()
    val view = LocalView.current
    val benchRunning = benchState is AppController.BenchState.Running
    DisposableEffect(view, benchRunning) {
        // View 自己的开关，不碰 Activity 的 window flag，也就不需要为「谁是 Activity」
        // 做上下文回溯；视图不可见时系统自动失效，兜住了跑分中途切走的情形。
        view.keepScreenOn = benchRunning
        onDispose { view.keepScreenOn = false }
    }

    MaterialApp(controller)
}
