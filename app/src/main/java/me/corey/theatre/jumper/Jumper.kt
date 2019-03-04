package me.corey.theatre.jumper

import android.os.Bundle
import android.support.annotation.IdRes
import android.support.v4.app.Fragment
import android.support.v4.app.FragmentActivity
import android.support.v4.app.FragmentManager
import android.support.v4.app.FragmentTransaction
import android.support.v4.util.SimpleArrayMap
import android.util.Log
import android.view.View
import me.corey.theatre.base.BaseFragment
import me.corey.theatre.rxbus.postEvent
import java.io.Serializable

/**
 * @author handsomeyang
 * @date 2018/7/26
 *
 * Helper of fragment operation.
 */
object Jumper {

    private val TAG = "Jumper"

    private lateinit var _fragmentManager: FragmentManager

    private lateinit var _target: Fragment

    /**
     * 需要传递的参数
     */
    private val mArgument = Bundle()

    /**
     * Fragments 栈
     */
    private val mFragmentStack = mutableListOf<Fragment>()

    /**
     * 以 map 的形式记录前一个被隐藏页面的焦点
     */
    private val mFocusTargetMapping = SimpleArrayMap<Fragment, View>()

    /**
     * 以 map 的形式记录前一个被隐藏的 fragment
     */
    private val mPreviousHiddenFragmentMapping = SimpleArrayMap<Fragment, Array<out Fragment>>()

    /**
     * 记录根 Activity 被隐藏的 rootView
     */
    private val mPreviousHiddenActivityContentViewMapping = SimpleArrayMap<Fragment, View>(2)

    private val mFragmentManager: FragmentManager
        get() = requireNotNull(_fragmentManager) { "Have you called \"initContext()\" ?" }

    private val mTarget: Fragment
        get() = requireNotNull(_target) { "Have you called \"setTarget(fragment)\" ?" }

    fun init(activity: FragmentActivity) =
        apply { _fragmentManager = activity.supportFragmentManager }

    fun target(target: Fragment) = apply {
        _target = target
        reset()
    }

    /**
     * 设置要传递的参数
     */
    fun addBundleData(pair: Pair<String, Any?>) =
        apply {
            val (key, data) = pair
            if (data != null) {
                when (data) {
                    is String -> mArgument.putString(key, data)
                    is Int -> mArgument.putInt(key, data)
                    is Serializable -> mArgument.putSerializable(key, data)
                    else -> throw IllegalArgumentException("Unsupported data type.")
                }
                mTarget.arguments = mArgument
            } else {
                Log.e(TAG, "Data $key represented in argument bundle is null")
            }
        }

    fun add(@IdRes containerId: Int, backStackName: String? = "") {
        for (index in mFragmentStack.indices) {
            val curFragment = mFragmentStack.get(index)
            if (curFragment.javaClass == mTarget.javaClass) {
                Log.d(TAG, "add, has same Fragment, index = $index, fragment = $mTarget")
                val isLastFragment = index + 1 == mFragmentStack.size
                val nextFragment = if (isLastFragment) mTarget else mFragmentStack.get(index + 1)
                mPreviousHiddenFragmentMapping.removeMapping(
                    curFragment
                ) { previousFragments: Array<out Fragment> ->
                    mPreviousHiddenFragmentMapping.put(nextFragment, previousFragments)
                }
                mPreviousHiddenActivityContentViewMapping.removeMapping(curFragment) { contentView ->
                    mPreviousHiddenActivityContentViewMapping.put(nextFragment, contentView)
                }

                // focus view
                mFocusTargetMapping.removeMapping(curFragment) {
                    mFocusTargetMapping.put(nextFragment, it)
                }

                mFragmentManager.beginTransaction()
                    .remove(curFragment)
                    .commit()

                if (isLastFragment && mFragmentStack.isEmpty().not()) {
                    mFragmentStack -= mFragmentStack.last()
                    TopFragmentEntity(mFragmentStack.lastOrNull()).postEvent()
                }
            }
        }

        mFragmentManager.operate(backStackName) {
            it.add(containerId, mTarget)
        }
        // do recording.
        mFragmentStack += mTarget
        TopFragmentEntity(mFragmentStack.last()).postEvent()
    }

    /**
     * 新建页面，将其添加到 android.R.id.content 容器中
     */
    fun addToWindowRoot(backStackName: String?) {
        if (backStackName.isNullOrEmpty()) {
            add(android.R.id.content)
        } else {
            add(android.R.id.content, backStackName)
        }

        mPreviousHiddenFragmentMapping.getValue(mTarget) { previousFragments: Array<out Fragment> ->
            previousFragments.forEach { previousFragment ->
                mFragmentManager.beginTransaction().hide(previousFragment).commit()
            }
        }

        mPreviousHiddenActivityContentViewMapping.getValue(mTarget) { contentView ->
            contentView.visibility = View.INVISIBLE
        }

        printPath()
    }

    /**
     * 记录下前一个页面的焦点
     */
    fun giveBackFocus(focused: View) = apply { mFocusTargetMapping.put(mTarget, focused) }

    /**
     * 打开 fragment 页面的同时隐藏根 activity 的 rootView
     */
    fun hidePreviousActivity(activityContentView: View) = apply {
        mPreviousHiddenActivityContentViewMapping.put(mTarget, activityContentView)
    }

    /**
     * 打开 fragment 的同时隐藏前一个 fragment
     */
    fun hidePreviousFragment(vararg previousFragment: Fragment) =
        apply {
            if (previousFragment.isNotEmpty()) {
                mPreviousHiddenFragmentMapping.put(mTarget, previousFragment)
            }
        }

    /**
     * 将当前 fragment 记录的信息传递给指定的目标
     * 使用该方法可以实现页面的连续跳转，同时保证记录的信息不丢失
     */
    fun transfer(curFragment: BaseFragment) = apply {
        mPreviousHiddenFragmentMapping.removeMapping(
            curFragment
        ) { previousFragments: Array<out Fragment> ->
            mPreviousHiddenFragmentMapping.put(mTarget, previousFragments)
        }
        mPreviousHiddenActivityContentViewMapping.removeMapping(curFragment) { contentView ->
            mPreviousHiddenActivityContentViewMapping.put(mTarget, contentView)
        }

        // focus view
        mFocusTargetMapping.removeMapping(curFragment) {
            mFocusTargetMapping.put(mTarget, it)
        }

        mFragmentManager.beginTransaction()
            .remove(curFragment)
            .commit()

        if (mFragmentStack.isEmpty().not()) {
            mFragmentStack -= mFragmentStack.last()
            TopFragmentEntity(mFragmentStack.lastOrNull()).postEvent()
        }

        printPath()
    }

    /**
     * 移除目标 fragment 页面
     */
    fun remove() {

        val innerTransaction = mFragmentManager.beginTransaction()

        innerTransaction.remove(mTarget)

        mPreviousHiddenFragmentMapping.removeMapping(
            mTarget
        ) { previousFragments: Array<out Fragment> ->
            previousFragments.forEach { previousFragment ->
                innerTransaction.show(previousFragment)
            }
        }

        innerTransaction.commit()

        mPreviousHiddenActivityContentViewMapping.removeMapping(mTarget) { contentView ->
            contentView.visibility = View.VISIBLE
        }

        // focus view
        mFocusTargetMapping.removeMapping(mTarget) {
            it.requestFocus()
        }
        // remove the recording
        if (mFragmentStack.isEmpty().not()) {
            mFragmentStack -= mFragmentStack.last()
            TopFragmentEntity(mFragmentStack.lastOrNull()).postEvent()
        }

        printPath()
    }

    /**
     * 返回指定的层级，若超过总层级数量则直接返回主页面
     */
    fun backStack(level: Int) {
        if (mFragmentStack.isEmpty()) {
            Log.d(TAG, "fragment stack is empty")
            return
        }

        var targetLevel = if (level > mFragmentStack.size) 0 else mFragmentStack.size - level

        val start = System.currentTimeMillis()
        val innerTransaction = mFragmentManager.beginTransaction()

        for (i in mFragmentStack.size - 1 downTo targetLevel) {
            val item = mFragmentStack[i]
            Log.d(TAG, "remove $item")
            innerTransaction.remove(item)
            mPreviousHiddenFragmentMapping.removeMapping(
                item
            ) { previousFragments: Array<out Fragment> ->
                previousFragments.forEach { previousFragment ->
                    Log.d(TAG, "show $previousFragment")
                    innerTransaction.show(previousFragment)
                }
            }
            if (i == 0) {
                mPreviousHiddenActivityContentViewMapping.removeMapping(item) { contentView ->
                    contentView.visibility = View.VISIBLE
                }
            }
            mFocusTargetMapping.removeMapping(item) {
                it.requestFocus()
            }
            notifyTopFragment()
        }

        innerTransaction.commit()
        val end = System.currentTimeMillis()
        Log.d(TAG, "back stack time cost: ${end - start}")
        printPath()
    }

    /**
     * 清空栈，直接回到主页面
     */
    fun clearTop2Main() {
        backStack(mFragmentStack.size)
    }

    private fun notifyTopFragment() {
        if (mFragmentStack.isEmpty().not()) {
            mFragmentStack -= mFragmentStack.last()
            TopFragmentEntity(mFragmentStack.lastOrNull()).postEvent()
        }
    }

    /**
     * 为 SimpleArrayMap 添加拓展函数
     * 功能是获得目标值后，对其执行一定的操作
     */
    private fun <T> SimpleArrayMap<Fragment, T>.getValue(
        target: Fragment,
        operation: (T) -> Unit
    ) {
        val valueView = this[target]
        if (valueView != null) {
            operation.invoke(valueView)
        }
    }

    /**
     * 为 SimpleArrayMap 添加拓展函数
     * 功能是移除目标值后，对其执行一定的操作
     */
    private fun <T> SimpleArrayMap<Fragment, T>.removeMapping(
        target: Fragment,
        operation: ((T) -> Unit)? = null
    ) {
        getValue(target) {
            operation?.invoke(it)
            this.remove(target)
        }
    }

    /**
     * 为 FragmentManager 添加拓展函数，实质是一个简单的重载函数
     */
    private fun FragmentManager.operate(
        name: String? = "",
        operation: (FragmentTransaction) -> FragmentTransaction
    ) {
        val transaction = this
            .beginTransaction()
        operation(transaction)

        if (name == null || name != "") {
            transaction.addToBackStack(name).commit()
        } else {
            transaction.commit()
        }
    }

    fun getFullPath(): String {
        val path = StringBuilder("current fragments path: Main --> ")
        for (fm in mFragmentStack) {
            path.append(fm)
            path.append("-->")
        }
        path.append("END")
        return path.toString()
    }

    private fun printPath() {
        Log.d(TAG, getFullPath())
    }

    /**
     * 本类是单例的，因此每次初始化需要进行一些清空操作
     */
    private fun reset() {
        mArgument.clear()
    }

    data class TopFragmentEntity(val top: Fragment?)

}