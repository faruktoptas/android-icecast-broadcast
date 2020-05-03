package ice.caster.android.pref

import android.content.Context
import android.content.SharedPreferences
import android.preference.PreferenceManager
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

class PreferenceWrapper {

    private lateinit var preferences: SharedPreferences

    fun init(context: Context) {
        preferences = PreferenceManager.getDefaultSharedPreferences(context)
    }

    var host: String by StringPref(HOST)

    var port: Int by IntPref(PORT)

    var mount: String by StringPref(MOUNT)

    var user: String by StringPref(USER)

    var pass: String by StringPref(PASS)


    fun getString(key: String) = preferences.getString(key, "")

    fun setString(key: String, value: String) = preferences.putString(key, value)

    fun getInt(key: String) = preferences.getInt(key, -1)

    fun setInt(key: String, value: Int) = preferences.putInt(key, value)

    companion object {
        val instance = PreferenceWrapper()

        private const val HOST = "host"
        private const val PORT = "port"
        private const val MOUNT = "mount"
        private const val USER = "user"
        private const val PASS = "pass"
    }
}

class StringPref(private val key: String) :
        ReadWriteProperty<PreferenceWrapper, String> {

    override fun getValue(thisRef: PreferenceWrapper, property: KProperty<*>) =
            thisRef.getString(key).orEmpty()

    override fun setValue(thisRef: PreferenceWrapper, property: KProperty<*>, value: String) {
        thisRef.setString(key, value)
    }
}

class IntPref(private val key: String) : ReadWriteProperty<PreferenceWrapper, Int> {

    override fun getValue(thisRef: PreferenceWrapper, property: KProperty<*>) = thisRef.getInt(key)

    override fun setValue(thisRef: PreferenceWrapper, property: KProperty<*>, value: Int) {
        thisRef.setInt(key, value)
    }
}

fun SharedPreferences.putString(key: String, value: String) {
    edit().putString(key, value).apply()
}

fun SharedPreferences.putInt(key: String, value: Int) {
    edit().putInt(key, value).apply()
}