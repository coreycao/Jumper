package me.corey.theatre.fragments

import android.view.View
import android.widget.TextView
import me.corey.theatre.R
import me.corey.theatre.base.BaseFragment
import me.corey.theatre.jumper.Jumper
import me.corey.theatre.widget.FocusedButton

/**
 * @author corey
 * @date 2019/3/3
 */
class FirstFragment : BaseFragment() {

    lateinit var btnA: FocusedButton

    lateinit var btnB: FocusedButton

    override fun rootResId(): Int =
        R.layout.fragment_first


    override fun initAfterSetContent(rootView: View) {
        btnA = rootView.findViewById(R.id.btn_first_a)
        btnB = rootView.findViewById(R.id.btn_first_b)

        btnA.setOnClickListener {
            Jumper.init(activity!!)
                .target(DefaultFragment())
                .hidePreviousFragment(this)
                .giveBackFocus(it)
                .addToWindowRoot(null)
        }

        btnB.setOnClickListener {
            Jumper.init(activity!!)
                .target(DefaultFragment())
                .transfer(this)
                .addToWindowRoot(null)
        }

        rootView.findViewById<TextView>(R.id.tv_title).text = Jumper.getFullPath()
        rootView.findViewById<TextView>(R.id.tv_param).text = arguments?.getString("param")
        rootView.requestFocus()

    }
}