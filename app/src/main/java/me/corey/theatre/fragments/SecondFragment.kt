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
class SecondFragment : BaseFragment() {

    lateinit var btnA: FocusedButton

    lateinit var btnB: FocusedButton

    override fun rootResId(): Int = R.layout.fragment_second

    override fun initAfterSetContent(rootView: View) {
        btnA = rootView.findViewById(R.id.btn_second_a)
        btnB = rootView.findViewById(R.id.btn_second_b)

        rootView.findViewById<TextView>(R.id.tv_title).text = Jumper.getFullPath()

        btnA.setOnClickListener {
            Jumper.clearTop2Main()
        }

        btnB.setOnClickListener {
            Jumper.backStack(2)
        }

        rootView.requestFocus()
    }
}