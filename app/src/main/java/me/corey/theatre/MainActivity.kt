package me.corey.theatre

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.view.View
import android.widget.TextView
import me.corey.theatre.base.BaseFragment
import me.corey.theatre.fragments.FirstFragment
import me.corey.theatre.jumper.Jumper
import me.corey.theatre.widget.FocusedButton

/**
 * @author corey
 * @date 2019/3/3
 */
class MainActivity : AppCompatActivity() {

    lateinit var rootView: View

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        init()
    }

    private fun init() {
        rootView = findViewById(R.id.main_root)
        findViewById<FocusedButton>(R.id.btn_main_a).setOnClickListener {
            Jumper.init(this)
                .target(FirstFragment())
                .hidePreviousActivity(rootView)
                .giveBackFocus(it)
                .addToWindowRoot(null)
        }
        findViewById<FocusedButton>(R.id.btn_main_b).setOnClickListener {
            Jumper.init(this)
                .target(FirstFragment())
                .hidePreviousActivity(rootView)
                .giveBackFocus(it)
                .addBundleData("param" to "hello, this is a message from MainActivity")
                .addToWindowRoot(null)
        }
        findViewById<TextView>(R.id.tv_title).text = Jumper.getFullPath()
    }

    override fun onBackPressed() = when {
        handleFragmentBackKeyEvent() -> Unit
        else -> {
            super.onBackPressed()
        }
    }

    /**
     * 处理 fragment 页面的回退
     */
    private fun handleFragmentBackKeyEvent(): Boolean {

        val fullScreenFragment = supportFragmentManager?.findFragmentById(android.R.id.content)

        val fragmentExitAndNotRemoved = fullScreenFragment != null &&
                fullScreenFragment.isRemoving.not()

        if (fragmentExitAndNotRemoved) {
            if (fullScreenFragment is BaseFragment) {
                if (fullScreenFragment.onPreBackPress()) {
                    return true
                }
                Jumper.init(this).target(fullScreenFragment).remove()
            }
            return true
        }

        return false
    }

}