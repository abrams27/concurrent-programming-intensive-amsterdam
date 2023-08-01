package day1

import kotlinx.atomicfu.*
import java.util.concurrent.*
import kotlin.reflect.jvm.internal.impl.util.ModuleVisibilityHelper.EMPTY

open class TreiberStackWithElimination<E> : Stack<E> {
    private val stack = TreiberStack<E>()

    private val eliminationArray = atomicArrayOfNulls<Any?>(ELIMINATION_ARRAY_SIZE)

    override fun push(element: E) {
        if (tryPushElimination(element)) return
        stack.push(element)
    }

    protected open fun tryPushElimination(element: E): Boolean {
        val randomCellIndex = randomCellIndex()
        val randomCell = eliminationArray[randomCellIndex]

        if (randomCell.compareAndSet(CELL_STATE_EMPTY, element)) {
            var counter = 0
            while (counter < ELIMINATION_WAIT_CYCLES) {
                if (randomCell.compareAndSet(CELL_STATE_RETRIEVED, CELL_STATE_EMPTY)) return true
                counter++
            }

            return if (randomCell.compareAndSet(element, CELL_STATE_EMPTY)) {
                false
            } else {
                randomCell.value = CELL_STATE_EMPTY
                true
            }

        } else {
            return false
        }
    }

    override fun pop(): E? = tryPopElimination() ?: stack.pop()

    protected open fun tryPopElimination(): E? {
        val randomCellIndex = randomCellIndex()
        val randomCell = eliminationArray[randomCellIndex]

        val value = randomCell.value
        return if (value == CELL_STATE_EMPTY || value == CELL_STATE_RETRIEVED) {
            null
        } else {
            if (randomCell.compareAndSet(value, CELL_STATE_RETRIEVED)) value as E else null
        }
    }

    private fun randomCellIndex(): Int =
        ThreadLocalRandom.current().nextInt(eliminationArray.size)

    companion object {
        private const val ELIMINATION_ARRAY_SIZE = 2 // Do not change!
        private const val ELIMINATION_WAIT_CYCLES = 1 // Do not change!

        // Initially, all cells are in EMPTY state.
        private val CELL_STATE_EMPTY = null

        // `tryPopElimination()` moves the cell state
        // to `RETRIEVED` if the cell contains element.
        private val CELL_STATE_RETRIEVED = Any()
    }
}