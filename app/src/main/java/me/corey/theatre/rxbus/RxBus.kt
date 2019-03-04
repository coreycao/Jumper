package me.corey.theatre.rxbus

import android.support.v4.util.SimpleArrayMap
import com.jakewharton.rxrelay2.PublishRelay
import io.reactivex.Observable
import io.reactivex.annotations.CheckReturnValue
import io.reactivex.functions.Consumer

/**
 *
 * @author handsomeyang
 * @date 2018/6/25
 *
 * Doesn't support backPressure.
 *
 * TODO  test and verify sticky function.
 *
 */
fun Any?.postEvent() {
    if (this != null) RxBus.post(this)
}

object RxBus {

    private val mBus = PublishRelay.create<Any>().toSerialized()

    /**
     *  Save the sticky events.
     */
    private val mStickyEventsMap = SimpleArrayMap<Class<*>, Any>()

    /**
     *  Publish the event.
     */
    fun post(any: Any) {
        mBus.accept(any)
    }

    /**
     * Publish sticky event.
     */
    fun postSticky(any: Any) {
        synchronized(mStickyEventsMap) {
            mStickyEventsMap.put(any::class.java, any)
        }
        mBus.accept(any)
    }

    /**
     * Register the event receiver, normally, it's a [Consumer]
     *
     * @sample RxBus.toObservable(String::class.java).subscribe (Consumer{  str ->
     *  // str is the object you post before.
     * })
     */
    @CheckReturnValue
    fun <T> toObservable(clazz: Class<T>): Observable<T> {
        return mBus.ofType(clazz)
    }

    /**
     * Register sticky event.
     *
     * The synchronous mechanism is intend to prevent reading the [mStickyEventsMap]
     * and writing into [mStickyEventsMap] happen at the same time.
     */
    @CheckReturnValue
    fun <T> toStickyObservable(clazz: Class<T>): Observable<T> {
        val event = synchronized(mStickyEventsMap) {
            mStickyEventsMap[clazz]
        }
        val observable = mBus.ofType(clazz)
        return if (event != null) {
            observable.mergeWith(Observable.create { emitter ->
                emitter.onNext(clazz.cast(event)!!)
            })
        } else {
            observable
        }
    }

    /**
     * Returns true if the subject has any Observers.
     */
    fun hasObservers(): Boolean {
        return mBus.hasObservers()
    }

    /**
     * Optional operation.
     *
     *  The synchronous mechanism is intend to prevent reading the [mStickyEventsMap]
     *  and writing into [mStickyEventsMap] happen at the same time.
     *
     * @see [removeAllStickyEvents]
     */
    fun <T> removeSomeKindStickyEvent(clazz: Class<T>) {
        synchronized(mStickyEventsMap) {
            mStickyEventsMap.remove(clazz).takeIf { mStickyEventsMap.containsKey(clazz) }
        }
    }

    /**
     * Important : call this to release events when quit app.
     * Cause [RxBus] is a single instance.
     */
    fun removeAllStickyEvents() {
        synchronized(mStickyEventsMap) {
            mStickyEventsMap.clear()
        }
    }

}
