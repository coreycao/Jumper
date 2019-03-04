package me.corey.theatre.fragments

import android.view.View
import android.widget.TextView
import me.corey.theatre.R
import me.corey.theatre.base.BaseFragment
import me.corey.theatre.jumper.Jumper
import me.corey.theatre.widget.FocusedButton

/**
 * @author corey
 * @date 2019/3/4
 */
class DefaultFragment : BaseFragment() {

    lateinit var btnA: FocusedButton

    lateinit var btnB: FocusedButton

    override fun rootResId(): Int {
        return R.layout.fragment_default
    }

    override fun initAfterSetContent(rootView: View) {
        btnA = rootView.findViewById(R.id.btn_default_a)
        btnB = rootView.findViewById(R.id.btn_default_b)

        btnA.setOnClickListener {
            Jumper.init(activity!!)
                .target(SecondFragment())
                .hidePreviousFragment(this)
                .giveBackFocus(it)
                .addToWindowRoot(null)
        }

        btnB.setOnClickListener {
            Jumper.init(activity!!)
                .target(SecondFragment())
                .hidePreviousFragment(this)
                .giveBackFocus(it)
                .addToWindowRoot(null)
        }

        rootView.findViewById<TextView>(R.id.tv_title).text = Jumper.getFullPath()

        btnA.requestFocus()

    }
}