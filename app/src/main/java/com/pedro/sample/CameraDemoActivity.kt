package com.pedro.sample

import android.os.Build
import android.os.Bundle
import android.view.SurfaceHolder
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.pedro.encoder.input.video.CameraOpenException
import com.pedro.rtplibrary.view.OpenGlView
import com.pedro.rtsp.rtsp.VideoCodec
import com.pedro.rtsp.utils.ConnectCheckerRtsp
import com.pedro.rtspserver.RtspServerCamera2
import kotlinx.android.synthetic.main.activity_camera_demo.*
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*

class CameraDemoActivity : AppCompatActivity(), ConnectCheckerRtsp, View.OnClickListener,
    SurfaceHolder.Callback {

  private lateinit var rtspServerCamera2: RtspServerCamera2

  private lateinit var button: Button
  private lateinit var bRecord: Button

  private var currentDateAndTime = ""
  private lateinit var folder: File

  private var openGlView: OpenGlView? = null
  private var videoCodecH = VideoCodec.H265

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    setContentView(R.layout.activity_camera_demo)
    folder = File(getExternalFilesDir(null)!!.absolutePath + "/rtmp-rtsp-stream-client-java")
    button = findViewById(R.id.b_start_stop)
    button.setOnClickListener(this)
    bRecord = findViewById(R.id.b_record)
    bRecord.setOnClickListener(this)
    switch_camera.setOnClickListener(this)

    openGlView = findViewById(R.id.surfaceView)

    rtspServerCamera2 = RtspServerCamera2(openGlView!!, this, 1935)
    surfaceView.holder.addCallback(this)

    rtspServerCamera2.disableAudio()
    rtspServerCamera2.setVideoCodec(videoCodecH)
    rtspServerCamera2.prepareVideo(1920, 1080, 30,4000*1024,0)


  }

  override fun onNewBitrateRtsp(bitrate: Long) {

  }

  override fun onConnectionSuccessRtsp() {
    runOnUiThread {
      Toast.makeText(this@CameraDemoActivity, "Connection success", Toast.LENGTH_SHORT).show()
    }
  }

  override fun onConnectionFailedRtsp(reason: String) {
    runOnUiThread {
      Toast.makeText(this@CameraDemoActivity, "Connection failed. $reason", Toast.LENGTH_SHORT)
          .show()
      rtspServerCamera2.stopStream()
      button.setText(R.string.start_button)
    }
  }

  override fun onConnectionStartedRtsp(rtspUrl: String) {
  }

  override fun onDisconnectRtsp() {
    runOnUiThread {
      Toast.makeText(this@CameraDemoActivity, "Disconnected", Toast.LENGTH_SHORT).show()
    }
  }

  override fun onAuthErrorRtsp() {
    runOnUiThread {
      Toast.makeText(this@CameraDemoActivity, "Auth error", Toast.LENGTH_SHORT).show()
      rtspServerCamera2.stopStream()
      button.setText(R.string.start_button)
      tv_url.text = ""
    }
  }

  override fun onAuthSuccessRtsp() {
    runOnUiThread {
      Toast.makeText(this@CameraDemoActivity, "Auth success", Toast.LENGTH_SHORT).show()
    }
  }

  override fun onClick(view: View) {
    when (view.id) {
      R.id.b_start_stop -> if (!rtspServerCamera2.isStreaming) {
        if (rtspServerCamera2.isRecording || rtspServerCamera2.prepareAudio() && rtspServerCamera2.prepareVideo()) {
          button.setText(R.string.stop_button)
          rtspServerCamera2.startStream()
          tv_url.text = rtspServerCamera2.getEndPointConnection()
        } else {
          Toast.makeText(this, "Error preparing stream, This device cant do it", Toast.LENGTH_SHORT)
              .show()
        }
      } else {
        button.setText(R.string.start_button)
        rtspServerCamera2.stopStream()
        tv_url.text = ""
      }
      R.id.switch_camera -> try {
        rtspServerCamera2.switchCamera()
      } catch (e: CameraOpenException) {
        Toast.makeText(this, e.message, Toast.LENGTH_SHORT).show()
      }

      R.id.b_record -> {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
          if (!rtspServerCamera2.isRecording) {
            try {
              if (!folder.exists()) {
                folder.mkdir()
              }
              val sdf = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
              currentDateAndTime = sdf.format(Date())
              if (!rtspServerCamera2.isStreaming) {
                if (rtspServerCamera2.prepareAudio() && rtspServerCamera2.prepareVideo()) {
                  rtspServerCamera2.startRecord(folder.absolutePath + "/" + currentDateAndTime + ".mp4")
                  bRecord.setText(R.string.stop_record)
                  Toast.makeText(this, "Recording... ", Toast.LENGTH_SHORT).show()
                } else {
                  Toast.makeText(
                    this, "Error preparing stream, This device cant do it",
                    Toast.LENGTH_SHORT
                  ).show()
                }
              } else {
                rtspServerCamera2.startRecord(folder.absolutePath + "/" + currentDateAndTime + ".mp4")
                bRecord.setText(R.string.stop_record)
                Toast.makeText(this, "Recording... ", Toast.LENGTH_SHORT).show()
              }
            } catch (e: IOException) {
              rtspServerCamera2.stopRecord()
              bRecord.setText(R.string.start_record)
              Toast.makeText(this, e.message, Toast.LENGTH_SHORT).show()
            }
          } else {
            rtspServerCamera2.stopRecord()
            bRecord.setText(R.string.start_record)
            Toast.makeText(
              this, "file " + currentDateAndTime + ".mp4 saved in " + folder.absolutePath,
              Toast.LENGTH_SHORT
            ).show()
          }
        } else {
          Toast.makeText(this, "You need min JELLY_BEAN_MR2(API 18) for do it...", Toast.LENGTH_SHORT).show()
        }
      }
      else -> {
      }
    }
  }

  override fun surfaceCreated(surfaceHolder: SurfaceHolder) {
  }

  override fun surfaceChanged(surfaceHolder: SurfaceHolder, i: Int, i1: Int, i2: Int) {
    rtspServerCamera2.startPreview()
  }

  override fun surfaceDestroyed(surfaceHolder: SurfaceHolder) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
      if (rtspServerCamera2.isRecording) {
        rtspServerCamera2.stopRecord()
        bRecord.setText(R.string.start_record)
        Toast.makeText(this, "file " + currentDateAndTime + ".mp4 saved in " + folder.absolutePath, Toast.LENGTH_SHORT).show()
        currentDateAndTime = ""
      }
    }
    if (rtspServerCamera2.isStreaming) {
      rtspServerCamera2.stopStream()
      button.text = resources.getString(R.string.start_button)
      tv_url.text = ""
    }
    rtspServerCamera2.stopPreview()
  }
}
