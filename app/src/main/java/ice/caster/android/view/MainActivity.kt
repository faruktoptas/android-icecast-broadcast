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
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.gson.Gson
import com.google.zxing.integration.android.IntentIntegrator
import ice.caster.android.R
import ice.caster.android.pref.PreferenceWrapper
import ice.caster.android.shout.ConfigItem
import ice.caster.android.shout.ConfigList
import ice.caster.android.shout.Encoder
import kotlinx.android.synthetic.main.activity_main.*


class MainActivity : FragmentActivity() {

    private var encoder: Encoder? = null
    private val pref = PreferenceWrapper.instance
    private var configList = ConfigList(arrayListOf())
    private lateinit var adapter: ListAdapter
    private var currentPlayIndex = -1
    private var currentConfigName = ""

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

        tvVersion.text = "v${applicationContext.packageManager.getPackageInfo(applicationContext.packageName, 0).versionName}"

        adapter = ListAdapter(::startStream, ::deleteItem, ::scanQr)
        refreshList()
        rv.adapter = adapter

        initEncoder()
        readList()
        refreshList()
    }

    private fun startStream(position: Int) {
        if (configList.list.size > position) {
            encoder?.let {
                if (it.isRecording) {
                    it.stop()
                } else {
                    it.start(configList.list[position])
                    currentPlayIndex = position
                }
            }
        }
    }

    private fun deleteItem(position: Int) {
        MaterialAlertDialogBuilder(this)
                .setMessage(R.string.want_to_remove)
                .setPositiveButton(R.string.remove) { _, _ ->
                    configList.list.removeAt(position)
                    adapter.notifyItemRemoved(position)
                    saveList()
                    rv.postDelayed({ refreshList() }, 50)
                }
                .setNegativeButton(R.string.cancel, null)
                .show()

    }

    private fun refreshList() {
        adapter.list = configList.list
        adapter.notifyDataSetChanged()
    }

    private fun addNew(item: ConfigItem) {
        if (configList.list.firstOrNull { it.uid() == item.uid() } != null) {
            showToast(R.string.already_exists)
        } else {
            configList.list.add(item)
            saveList()
            refreshList()
        }

    }

    private fun saveList() {
        PreferenceWrapper.instance.list = Gson().toJson(configList)
    }

    private fun readList() {
        try {
            configList = Gson().fromJson(pref.list, ConfigList::class.java)
        } catch (e: Exception) {
        }
    }

    private fun setStatus(recording: Boolean) {
        if (currentPlayIndex > -1 && configList.list.size > currentPlayIndex) {
            configList.list.forEach { it.isRecording = false }
            configList.list[currentPlayIndex].isRecording = recording
            adapter.notifyDataSetChanged()
        }
    }

    private fun showToast(err: String) {
        Toast.makeText(this, err, Toast.LENGTH_SHORT).show()
    }

    private fun showToast(@StringRes err: Int) {
        Toast.makeText(this, err, Toast.LENGTH_SHORT).show()
    }

    private fun scanQr() {
        InputDialog {
            currentConfigName = it
            val integrator = IntentIntegrator(this)
            integrator.title = "Please scan a fully qualified URI"
            integrator.initiateScan(IntentIntegrator.QR_CODE_TYPES)
        }.show(supportFragmentManager, "input")
    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, intent: Intent?) {
        super.onActivityResult(requestCode, resultCode, intent)
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
        if (uri.userInfo != null && uri.userInfo!!.split(":").toTypedArray().size >= 2) {
            val authority = uri.userInfo!!.split(":").toTypedArray()
            val user = authority[0]
            val pass = authority[1]
            val mount = uri.path?.replace("^/", "").orEmpty()
            val host = uri.host.orEmpty()
            val port = uri.port

            addNew(ConfigItem(currentConfigName, host, mount, user, pass, port, SAMPLE_RATE))
        } else {
            showToast(R.string.invalid_qr)
        }
    }

    private fun initEncoder() {
        encoder = Encoder()
        encoder?.setHandler(EncoderHandler(this::setStatus, this::showToast))
    }

    override fun onDestroy() {
        super.onDestroy()
        encoder?.stop()
    }

    override fun onBackPressed() {
        if (encoder?.isRecording == true) {
            MaterialAlertDialogBuilder(this)
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

    companion object {
        const val SAMPLE_RATE = 8000
    }
}