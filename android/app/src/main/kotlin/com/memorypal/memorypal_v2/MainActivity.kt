package com.memorypal.memorypal_v2

import android.content.Intent
import android.os.Bundle
import androidx.annotation.NonNull
import io.flutter.embedding.android.FlutterActivity
import io.flutter.embedding.engine.FlutterEngine
import io.flutter.plugin.common.MethodChannel

class MainActivity : FlutterActivity() {
    private val RECORDING_CHANNEL = "com.memorypal/recording"
    private val WHISPER_CHANNEL = "com.memorypal/whisper"
    private val MODEL_CHANNEL = "com.memorypal/model"

    override fun configureFlutterEngine(@NonNull flutterEngine: FlutterEngine) {
        super.configureFlutterEngine(flutterEngine)

        // 录音服务通道
        MethodChannel(flutterEngine.dartExecutor.binaryMessenger, RECORDING_CHANNEL)
            .setMethodCallHandler { call, result ->
                when (call.method) {
                    "startRecording" -> {
                        // TODO: 实现开始录音
                        result.success(true)
                    }
                    "stopRecording" -> {
                        // TODO: 实现停止录音
                        result.success(true)
                    }
                    "startBackgroundRecording" -> {
                        val directory = call.argument<String>("directory")
                        // TODO: 启动后台服务
                        result.success(true)
                    }
                    "stopBackgroundRecording" -> {
                        // TODO: 停止后台服务
                        result.success(true)
                    }
                    else -> result.notImplemented()
                }
            }

        // Whisper通道
        MethodChannel(flutterEngine.dartExecutor.binaryMessenger, WHISPER_CHANNEL)
            .setMethodCallHandler { call, result ->
                when (call.method) {
                    "initialize" -> {
                        result.success(true)
                    }
                    "loadModel" -> {
                        result.success(true)
                    }
                    "transcribe" -> {
                        result.success(mapOf("text" to "", "language" to "zh"))
                    }
                    else -> result.notImplemented()
                }
            }

        // 模型下载通道
        MethodChannel(flutterEngine.dartExecutor.binaryMessenger, MODEL_CHANNEL)
            .setMethodCallHandler { call, result ->
                when (call.method) {
                    "getStorageInfo" -> {
                        val stat = applicationContext.cacheDir?.let {
                            mapOf(
                                "totalBytes" to it.totalSpace,
                                "availableBytes" to it.usableSpace
                            )
                        } ?: mapOf()
                        result.success(stat)
                    }
                    else -> result.notImplemented()
                }
            }
    }
}
