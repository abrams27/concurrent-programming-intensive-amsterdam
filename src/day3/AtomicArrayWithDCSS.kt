package day3

import day3.AtomicArrayWithDCSS.Status.*
import kotlinx.atomicfu.*

// This implementation never stores `null` values.
class AtomicArrayWithDCSS<E : Any>(size: Int, initialValue: E) {
    private val array = atomicArrayOfNulls<Any?>(size)

    init {
        // Fill array with the initial value.
        for (i in 0 until size) {
            array[i].value = initialValue
        }
    }

    fun get(index: Int): E? {
        // TODO: the cell can store a descriptor
        val value = array[index].value

        return if (value is AtomicArrayWithDCSS<*>.DCSSDescriptor) {
            val descriptor = value as AtomicArrayWithDCSS<E>.DCSSDescriptor

            if (descriptor.index1 == index) {
                when (descriptor.status.value) {
                    UNDECIDED, FAILED -> descriptor.expected1
                    SUCCESS -> descriptor.update1
                }
            } else {
                value as? E
            }
        } else {
            value as? E
        }
    }

    fun cas(index: Int, expected: E?, update: E?): Boolean {
        // TODO: the cell can store a descriptor
        return array[index].compareAndSet(expected, update)
    }

    fun dcss(
        index1: Int, expected1: E?, update1: E?,
        index2: Int, expected2: E?
    ): Boolean {
        require(index1 != index2) { "The indices should be different" }
        // TODO This implementation is not linearizable!
        // TODO Store a DCSS descriptor in array[index1].
        if (array[index1].value != expected1 || array[index2].value != expected2) return false
        array[index1].value = update1
        return true
    }

    inner class DCSSDescriptor(
        val index1: Int,
        val expected1: E?,
        val update1: E?,
        val index2: Int,
        val expected2: E?,
    ) {
        val status = atomic(UNDECIDED)

        fun apply() {
            val installStatus= installDescriptor()
            updateStatus(installStatus)
            updateCells()
        }

        private fun installDescriptor(): Status =
            if (array[index1].value == expected1 && array[index2].value == expected2) {
                array[index1].value = this
                SUCCESS
            } else FAILED

        private fun updateStatus(installStatus: Status) {
            status.value = installStatus
        }

        private fun updateCells() {
            if (status.value == SUCCESS) {
                array[index1].compareAndSet(this, update1)
            }
        }
    }

    enum class Status {
        UNDECIDED, SUCCESS, FAILED
    }
}