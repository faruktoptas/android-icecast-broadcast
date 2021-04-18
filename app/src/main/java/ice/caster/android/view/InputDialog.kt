package ice.caster.android.view

import android.app.Dialog
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import ice.caster.android.R

class InputDialog(private val listener: (String) -> Unit) : DialogFragment() {


    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val builder = AlertDialog.Builder(activity!!)

        val view = activity!!.layoutInflater.inflate(R.layout.dialog_input, null, false)
        val et = view.findViewById<EditText>(R.id.etName)
        view.findViewById<Button>(R.id.btnStartQr).setOnClickListener {
            val name = et.text.toString()
            if (name.isNotEmpty()) {
                listener(name)
            }
            dismiss()
        }
        builder.setView(view)

        return builder.create()
    }


}