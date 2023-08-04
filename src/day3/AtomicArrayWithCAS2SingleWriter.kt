package day3

import day3.AtomicArrayWithCAS2SingleWriter.Status.*
import kotlinx.atomicfu.*
import kotlin.time.measureTime

// This implementation never stores `null` values.
class AtomicArrayWithCAS2SingleWriter<E : Any>(size: Int, initialValue: E) {
    private val array = atomicArrayOfNulls<Any?>(size)

    init {
        // Fill array with the initial value.
        for (i in 0 until size) {
            array[i].value = initialValue
        }
    }

    fun get(index: Int): E {
        val value = array[index].value
        return if (value is AtomicArrayWithCAS2SingleWriter<*>.CAS2Descriptor) {
            val descriptor = value as AtomicArrayWithCAS2SingleWriter<E>.CAS2Descriptor

            when (descriptor.status.value) {
                UNDECIDED, FAILED -> if (descriptor.index1 == index) descriptor.expected1 else descriptor.expected2
                SUCCESS -> if (descriptor.index1 == index) descriptor.update1 else descriptor.update2
            }
        } else {
            value as E
        }
    }

    fun cas2(
        index1: Int, expected1: E, update1: E,
        index2: Int, expected2: E, update2: E
    ): Boolean {
        require(index1 != index2) { "The indices should be different" }
        val descriptor = CAS2Descriptor(
            index1 = index1, expected1 = expected1, update1 = update1,
            index2 = index2, expected2 = expected2, update2 = update2
        )
        descriptor.apply()
        return descriptor.status.value === SUCCESS
    }

    inner class CAS2Descriptor(
        val index1: Int,
        val expected1: E,
        val update1: E,
        val index2: Int,
        val expected2: E,
        val update2: E
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
                array[index2].value = this
                SUCCESS
            } else FAILED

        private fun updateStatus(installStatus: Status) {
            status.value = installStatus
        }

        private fun updateCells() {
            if (status.value == SUCCESS) {
                array[index1].compareAndSet(this, update1)
                array[index2].compareAndSet(this, update2)
            }
        }
    }

    enum class Status {
        UNDECIDED, SUCCESS, FAILED
    }
}