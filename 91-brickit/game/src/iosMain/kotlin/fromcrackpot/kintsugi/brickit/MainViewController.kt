package fromcrackpot.kintsugi.brickit

import androidx.compose.ui.AbsoluteAlignment
import androidx.compose.ui.window.ComposeUIViewController
import platform.UIKit.UIViewController

fun MainViewController(): UIViewController = ComposeUIViewController { App(AbsoluteAlignment.CenterLeft) }
