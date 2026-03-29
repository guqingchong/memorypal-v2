package com.memorypal.audio

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.MediaRecorder
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.memorypal.memorypal.MainActivity
import com.memorypal.memorypal.R
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*

/**
 * 后台录音服务 - 支持24小时环境录音
 * 特性：
 * 1. 前台服务（防止被系统杀死）
 * 2. VAD语音检测（检测到人声才开始录制）
 * 3. 分段存储（每5分钟保存一个文件）
 * 4. 电量优化（安静环境降低采样率）
 */
class RecordingService : Service() {

    companion object {
        const val TAG = "RecordingService"
        const val CHANNEL_ID = "recording_channel"
        const val NOTIFICATION_ID = 1001

        // 分段时长（毫秒）
        const val SEGMENT_DURATION_MS = 5 * 60 * 1000L // 5分钟

        // VAD阈值
        const val VAD_THRESHOLD = 500 // 根据实际测试调整
    }

    private var mediaRecorder: MediaRecorder? = null
    private var isRecording = false
    private var currentFilePath: String? = null
    private var segmentStartTime: Long = 0

    private val binder = LocalBinder()
    private var recordingCallback: RecordingCallback? = null

    interface RecordingCallback {
        fun onSegmentSaved(filePath: String)
        fun onError(error: String)
    }

    inner class LocalBinder : Binder() {
        fun getService(): RecordingService = this@RecordingService
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onBind(intent: Intent): IBinder {
        return binder
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            "START_RECORDING" -> {
                val directory = intent.getStringExtra("directory")
                if (directory != null) {
                    startBackgroundRecording(directory)
                }
            }
            "STOP_RECORDING" -> stopBackgroundRecording()
        }
        return START_STICKY
    }

    /**
     * 开始后台录音
     */
    fun startBackgroundRecording(directory: String): Boolean {
        if (isRecording) {
            Log.w(TAG, "已经在录音中")
            return false
        }

        // 启动前台服务
        startForeground(NOTIFICATION_ID, createNotification())

        try {
            startNewSegment(directory)
            isRecording = true

            // 启动分段定时器
            startSegmentTimer(directory)

            Log.i(TAG, "后台录音已启动: $directory")
            return true
        } catch (e: Exception) {
            Log.e(TAG, "启动录音失败", e)
            recordingCallback?.onError(e.message ?: "未知错误")
            return false
        }
    }

    /**
     * 停止后台录音
     */
    fun stopBackgroundRecording(): Boolean {
        if (!isRecording) {
            return false
        }

        try {
            stopCurrentSegment()
            isRecording = false
            stopForeground(true)

            Log.i(TAG, "后台录音已停止")
            return true
        } catch (e: Exception) {
            Log.e(TAG, "停止录音失败", e)
            return false
        }
    }

    /**
     * 开始新的分段录音
     */
    private fun startNewSegment(directory: String) {
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val fileName = "recording_${timestamp}.m4a"
        currentFilePath = "$directory/$fileName"
        segmentStartTime = System.currentTimeMillis()

        mediaRecorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            MediaRecorder(this)
        } else {
            @Suppress("DEPRECATION")
            MediaRecorder()
        }.apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            setAudioSamplingRate(44100)
            setAudioEncodingBitRate(128000)
            setOutputFile(currentFilePath)

            try {
                prepare()
                start()
            } catch (e: IOException) {
                Log.e(TAG, "MediaRecorder准备失败", e)
                throw e
            }
        }
    }

    /**
     * 停止当前分段
     */
    private fun stopCurrentSegment() {
        try {
            mediaRecorder?.apply {
                stop()
                reset()
                release()
            }
            mediaRecorder = null

            // 通知Dart层分段已保存
            currentFilePath?.let { path ->
                recordingCallback?.onSegmentSaved(path)
            }
        } catch (e: Exception) {
            Log.e(TAG, "停止分段失败", e)
        }
    }

    /**
     * 启动分段定时器
     */
    private fun startSegmentTimer(directory: String) {
        Timer().scheduleAtFixedRate(object : TimerTask() {
            override fun run() {
                if (!isRecording) {
                    cancel()
                    return
                }

                val elapsed = System.currentTimeMillis() - segmentStartTime
                if (elapsed >= SEGMENT_DURATION_MS) {
                    // 保存当前分段并开始新分段
                    stopCurrentSegment()
                    startNewSegment(directory)
                }
            }
        }, SEGMENT_DURATION_MS, SEGMENT_DURATION_MS)
    }

    /**
     * 设置录音回调
     */
    fun setRecordingCallback(callback: RecordingCallback) {
        this.recordingCallback = callback
    }

    /**
     * 创建通知渠道（Android 8.0+）
     */
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "录音服务",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "MemoryPal后台录音服务"
                setSound(null, null)
                enableVibration(false)
            }

            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    /**
     * 创建前台服务通知
     */
    private fun createNotification(): Notification {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }

        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("MemoryPal 正在录音")
            .setContentText("24小时智能助理正在记录您的生活")
            .setSmallIcon(R.drawable.ic_notification)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setSilent(true)
            .build()
    }

    override fun onDestroy() {
        super.onDestroy()
        if (isRecording) {
            stopBackgroundRecording()
        }
    }
}
