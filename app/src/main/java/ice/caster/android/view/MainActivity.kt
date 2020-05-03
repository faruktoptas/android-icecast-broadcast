package ice.caster.android.view

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Message
import androidx.annotation.StringRes
import androidx.core.app.ActivityCompat
import androidx.fragment.app.FragmentActivity
import androidx.core.content.ContextCompat
import android.view.WindowManager
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import com.google.zxing.integration.android.IntentIntegrator
import ice.caster.android.R
import ice.caster.android.pref.PreferenceWrapper
import ice.caster.android.shout.Config
import ice.caster.android.shout.Encoder
import kotlinx.android.synthetic.main.activity_main.*


class MainActivity : FragmentActivity() {

    private var encoder: Encoder? = null
    private val pref = PreferenceWrapper.instance

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

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
            encoder?.start() ?: showToast(R.string.qr_not_set)
        }
        btnStop.setOnClickListener { encoder?.stop() }
        btnQr.setOnClickListener { scanQr() }

        tvVersion.text = "v${applicationContext.packageManager.getPackageInfo(applicationContext.packageName, 0).versionName}"

        readPref()
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

    private fun showToast(err: String) {
        Toast.makeText(this, err, Toast.LENGTH_SHORT).show()
    }

    private fun showToast(@StringRes err: Int) {
        Toast.makeText(this, err, Toast.LENGTH_SHORT).show()
    }

    private fun scanQr() {
        val integrator = IntentIntegrator(this)
        integrator.title = "Please scan a fully qualified URI"
        integrator.initiateScan(IntentIntegrator.QR_CODE_TYPES)
    }

    private fun readPref() {
        if (pref.host.isNotEmpty() && pref.port > 0 && pref.mount.isNotEmpty() && pref.user.isNotEmpty() && pref.pass.isNotEmpty()) {
            setConfig(pref.host, pref.port, pref.mount, pref.user, pref.pass)
            showToast(R.string.qr_read)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, intent: Intent?) {
        val scanResult = IntentIntegrator.parseActivityResult(requestCode, resultCode, intent)
        if (scanResult != null) {
            val u: Uri = try {
                Uri.parse(scanResult.contents)
            } catch (e1: Exception) {
                showToast(R.string.invalid_qr)
                return
            }
            parseUri(u)
        }
    }

    private fun parseUri(uri: Uri) {
        if (uri.userInfo != null && uri.userInfo.split(":").toTypedArray().size >= 2) {
            val authority = uri.userInfo.split(":").toTypedArray()
            val user = authority[0]
            val pass = authority[1]
            val mount = uri.path?.replace("^/", "").orEmpty();
            val host = uri.host.orEmpty()
            val port = uri.port

            pref.host = host
            pref.port = uri.port
            pref.mount = mount
            pref.user = user
            pref.pass = pass

            setConfig(host, port, mount, user, pass)
        } else {
            showToast(R.string.invalid_qr)
        }
    }

    private fun setConfig(host: String, port: Int, mount: String, user: String, pass: String) {
        encoder = Encoder(Config()
                .host(host)
                .port(port)
                .mount(mount)
                .username(user)
                .password(pass)
                .sampleRate(8000))

        encoder?.setHandler(EncoderHandler(this::setStatus, this::showToast))
    }

    override fun onDestroy() {
        super.onDestroy()
        encoder?.stop()
    }

    override fun onBackPressed() {
        if (encoder?.isRecording == true) {
            AlertDialog.Builder(this)
                    .setMessage(R.string.quit_question)
                    .setPositiveButton(R.string.txt_stop) { _, _ ->
                        encoder?.stop()
                        finish()
                    }
                    .setNegativeButton(R.string.cancel, null)
                    .show()
        } else {
            super.onBackPressed()
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