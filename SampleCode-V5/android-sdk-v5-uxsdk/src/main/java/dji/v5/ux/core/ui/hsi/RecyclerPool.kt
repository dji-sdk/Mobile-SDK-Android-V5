package dji.v5.ux.core.ui.hsi
import java.util.*

abstract class RecyclerPool<T>(poolSize: Int) {
    private val mPoolSize: Int
    private val mList: MutableList<T>

    init {
        mList = LinkedList()
        mPoolSize = poolSize
    }

    @Synchronized
    fun acquire(): T {
        val n = mList.size
        return if (n == 0) {
            create()
        } else {
            mList.removeAt(0)
        }
    }

    protected abstract fun create(): T
    @Synchronized
    fun recycle(t: T): Boolean {
        if (mList.size < mPoolSize) {
            mList.add(t)
            return true
        }
        return false
    }

    @Synchronized
    fun clear() {
        mList.clear()
    }
}