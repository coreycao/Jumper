package me.corey.theatre.base

import android.os.Bundle
import android.support.annotation.LayoutRes
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup

/**
 * @author corey
 * @date 2019/3/3
 */
abstract class BaseFragment : Fragment() {

    lateinit var mRootView: View

    @LayoutRes
    protected abstract fun rootResId(): Int

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        mRootView = inflater.inflate(rootResId(), container, false)
        return mRootView
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?
    ) {
        super.onViewCreated(view, savedInstanceState)
        initAfterSetContent(view)
    }

    protected abstract fun initAfterSetContent(rootView: View)

    open fun onPreBackPress() = false

}