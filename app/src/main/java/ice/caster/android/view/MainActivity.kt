package ice.caster.android.view

import android.os.Bundle
import android.support.v4.app.FragmentActivity
import ice.caster.android.R
import ice.caster.android.fragment.BroadcastFragment

class MainActivity : FragmentActivity() {
    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        if (savedInstanceState != null) {
            supportFragmentManager
                    .beginTransaction()
                    .replace(R.id.rlContainer, BroadcastFragment())
                    .commit()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
    }
}