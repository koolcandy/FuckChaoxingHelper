package com.example.fuckchaoxinghelper

import android.util.Log
import android.webkit.WebView
import com.google.gson.Gson
import de.robv.android.xposed.IXposedHookLoadPackage
import de.robv.android.xposed.callbacks.XC_LoadPackage
import de.robv.android.xposed.XposedHelpers
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class MainHook : IXposedHookLoadPackage {
    @OptIn(DelicateCoroutinesApi::class)
    override fun handleLoadPackage(lpparam: XC_LoadPackage.LoadPackageParam) {

        XposedHelpers.findAndHookMethod(
            "android.webkit.WebView",
            lpparam.classLoader,
            "loadUrl",
            String::class.java,
            object : de.robv.android.xposed.XC_MethodHook() {
                override fun afterHookedMethod(param: MethodHookParam) {
                    val webView = param.thisObject as WebView

                    webView.post {
                        webView.evaluateJavascript(
                            """
        (function() {
            if (window.__alreadyInjected) {
                return "Already Injected";
            }
            window.__alreadyInjected = true;
    
            // 获取页面内容
            return document.body.innerText;
        })();""".trimIndent()
                        ) { text ->
                            if (text.contains("答题卡") && text != "Already Injected") {
                                GlobalScope.launch {
                                    val answer = MainService.getAnswerAsync(text)
                                    Log.d("fucker", "Answer: $answer")

                                    val targetAnswers = answer.choice
                                    val answersJson = Gson().toJson(targetAnswers) // 将答案数组转换为 JSON 格式

                                    webView.post {
                                        val jsCode = """
        (function() {
            const selectedOptions = $answersJson;
            
            // 遍历传入的选项数组，找到对应的 class="No" 并在后面加英文句号
            selectedOptions.forEach(option => {
                // 查找对应的选项
                const targetOption = Array.from(document.querySelectorAll('.No'))
                    .find(el => el.textContent.trim() === option);
                
                if (targetOption) {
                    // 在选项文本后添加句号
                    targetOption.textContent += '.';
                }
            });
        })();
    """.trimIndent()

                                        // 在 WebView 中执行 JavaScript
                                        webView.evaluateJavascript(jsCode) {
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        )
    }
}
