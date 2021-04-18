package ice.caster.android.shout

import java.io.Serializable


data class ConfigItem(
        val title: String,
        val host: String,
        val mount: String,
        val username: String,
        val password: String,
        val port: Int,
        val sampleRate: Int,
        var isRecording: Boolean = false
) : Serializable {

    fun uid() = "$host:$port$mount-$username-$password"
}


data class ConfigList(val list: ArrayList<ConfigItem>) : Serializable