package ice.caster.android

import android.app.Application
import ice.caster.android.pref.PreferenceWrapper

/**
 * Created by fatih on 19/10/15.
 */
class App : Application() {
    override fun onCreate() {
        super.onCreate()
        PreferenceWrapper.instance.init(this)
    }
}