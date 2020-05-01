package ice.caster.android.fragment

import android.os.Bundle
import android.os.Handler
import android.os.Message
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import ice.caster.android.R
import ice.caster.android.shout.Config
import ice.caster.android.shout.Encoder
import kotlinx.android.synthetic.main.fragment_broadcast.*

class BroadcastFragment : Fragment() {

    private var encoder: Encoder? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        encoder = Encoder(
                Config().host(ICE_HOST).port(ICE_PORT).mount(ICE_MOUNT)
                        .username(ICE_USER).password(ICE_PASS).sampleRate(8000))
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_broadcast, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        encoder!!.setHandle(EncoderHandler(this::setInfoText, this::showError))
        btnStart.setOnClickListener { encoder?.start() }
        btnStop.setOnClickListener { encoder?.stop() }
    }

    private fun setInfoText(txt: String) {
        tvStatus.text = txt
    }

    private fun showError(err: String) {
        Toast.makeText(requireContext(), err, Toast.LENGTH_SHORT).show()
    }

    private class EncoderHandler(private val statusText: (String) -> Unit, private val error: (String) -> Unit) : Handler() {
        override fun handleMessage(msg: Message) {
            when (msg.what) {
                Encoder.MSG_REC_STARTED -> statusText("Streaming")
                Encoder.MSG_REC_STOPPED -> statusText("")
                Encoder.MSG_ERROR_GET_MIN_BUFFERSIZE -> {
                    statusText("")
                    error("MSG_ERROR_GET_MIN_BUFFERSIZE")
                }
                Encoder.MSG_ERROR_REC_START -> {
                    statusText("")
                    error("Can not start recording")
                }
                Encoder.MSG_ERROR_AUDIO_RECORD -> {
                    statusText("")
                    error("Error audio record")
                }
                Encoder.MSG_ERROR_AUDIO_ENCODE -> {
                    statusText("")
                    error("Error audio encode")
                }
                Encoder.MSG_ERROR_STREAM_INIT -> {
                    statusText("")
                    error("Can not init stream")
                }
                else -> {
                }
            }
        }
    }

    companion object {
        /**
         * Icecast host
         */
        const val ICE_HOST = "aaa.bbb.ccc.ddd"

        /**
         * Broadcast port that server listens incoming streams
         */
        const val ICE_PORT = 8002

        /**
         * Mount point of incoming source
         */
        const val ICE_MOUNT = "/test"

        /**
         * Credentials
         */
        const val ICE_USER = "user"
        const val ICE_PASS = "pass"
    }
}

