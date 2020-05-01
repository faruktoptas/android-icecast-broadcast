package ice.caster.android.view

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Message
import android.support.annotation.StringRes
import android.support.v4.app.ActivityCompat
import android.support.v4.app.FragmentActivity
import android.support.v4.content.ContextCompat
import android.widget.Toast
import com.google.zxing.integration.android.IntentIntegrator
import ice.caster.android.R
import ice.caster.android.shout.Config
import ice.caster.android.shout.Encoder
import kotlinx.android.synthetic.main.activity_main.*


class MainActivity : FragmentActivity() {

    private var encoder: Encoder? = null

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val permAudio = Manifest.permission.RECORD_AUDIO
        val permCamera = Manifest.permission.CAMERA
        if (ContextCompat.checkSelfPermission(this, permAudio) != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(this, permCamera) != PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, permAudio)) {
                Toast.makeText(this, R.string.perm_error, Toast.LENGTH_SHORT).show()
            } else {
                ActivityCompat.requestPermissions(this, arrayOf(permAudio, permCamera), 10001)
            }
        }

        btnStart.setOnClickListener {
            encoder?.start() ?: showError(R.string.qr_not_set)
        }
        btnStop.setOnClickListener { encoder?.stop() }
        btnQr.setOnClickListener { scanQr() }
    }


    private fun setStatus(recording: Boolean) {
        val bgColor: Int
        if (recording) {
            bgColor = R.color.green
            tvStatus.setText(R.string.recording)
        } else {
            bgColor = R.color.white
            tvStatus.text = ""
        }
        rlContainer.background = ColorDrawable(ContextCompat.getColor(this, bgColor))
    }

    private fun showError(err: String) {
        Toast.makeText(this, err, Toast.LENGTH_SHORT).show()
    }

    private fun showError(@StringRes err: Int) {
        Toast.makeText(this, err, Toast.LENGTH_SHORT).show()
    }

    private fun scanQr() {
        val integrator = IntentIntegrator(this)
        integrator.title = "Please scan a fully qualified URI"
        integrator.initiateScan(IntentIntegrator.QR_CODE_TYPES)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, intent: Intent?) {
        val scanResult = IntentIntegrator.parseActivityResult(requestCode, resultCode, intent)
        if (scanResult != null) {
            val u: Uri = try {
                Uri.parse(scanResult.contents)
            } catch (e1: Exception) {
                showError(R.string.invalid_qr)
                return
            }
            setConfig(u)
        }
    }

    private fun setConfig(uri: Uri) {
        if (uri.userInfo != null && uri.userInfo.split(":").toTypedArray().size >= 2) {
            val authority = uri.userInfo.split(":").toTypedArray()
            val user = authority[0]
            val pass = authority[1]
            val mount = uri.path.replace("^/", "");

            encoder = Encoder(
                    Config().host(uri.host).port(uri.port).mount(mount)
                            .username(user).password(pass).sampleRate(8000))

            encoder?.setHandler(EncoderHandler(this::setStatus, this::showError))
        } else {
            showError(R.string.invalid_qr)
        }
    }

    private class EncoderHandler(private val recording: (Boolean) -> Unit, private val error: (String) -> Unit) : Handler() {
        override fun handleMessage(msg: Message) {
            when (msg.what) {
                Encoder.MSG_REC_STARTED -> recording(true)
                Encoder.MSG_REC_STOPPED -> recording(false)
                Encoder.MSG_ERROR_GET_MIN_BUFFERSIZE -> {
                    recording(false)
                    error("MSG_ERROR_GET_MIN_BUFFERSIZE")
                }
                Encoder.MSG_ERROR_REC_START -> {
                    recording(false)
                    error("Can not start recording")
                }
                Encoder.MSG_ERROR_AUDIO_RECORD -> {
                    recording(false)
                    error("Error audio record")
                }
                Encoder.MSG_ERROR_AUDIO_ENCODE -> {
                    recording(false)
                    error("Error audio encode")
                }
                Encoder.MSG_ERROR_STREAM_INIT -> {
                    recording(false)
                    error("Can not init stream")
                }
                else -> {
                }
            }
        }
    }
}