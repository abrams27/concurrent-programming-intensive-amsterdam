@file:Suppress("FoldInitializerAndIfToElvis", "DuplicatedCode")

package day2

import kotlinx.atomicfu.*

class MSQueueWithLinearTimeRemove<E> : QueueWithRemove<E> {
    private val head: AtomicRef<Node>
    private val tail: AtomicRef<Node>

    init {
        val dummy = Node(null)
        head = atomic(dummy)
        tail = atomic(dummy)
    }

    override fun enqueue(element: E) {
        while (true) {
            val node = Node(element)
            val currentTail = tail.value
            val next = currentTail.next
            if (next.compareAndSet(null, node)) {
                tail.compareAndSet(currentTail, node)
                if (currentTail.extractedOrRemoved) {
                    currentTail.remove()
                }
                return
            } else {
                tail.compareAndSet(currentTail, next.value!!)
            }
        }
    }

    override fun dequeue(): E? {
        while (true) {
            val currentHead = head.value
            val currentHeadNext = currentHead.next.value ?: return null

            if (head.compareAndSet(currentHead, currentHeadNext)) {
                if (currentHeadNext.markExtractedOrRemoved()) {
                    return currentHeadNext.element
                }
            }
        }
    }

    override fun remove(element: E): Boolean {
        // Traverse the linked list, searching the specified
        // element. Try to remove the corresponding node if found.
        // DO NOT CHANGE THIS CODE.
        var node = head.value
        while (true) {
            val next = node.next.value
            if (next == null) return false
            node = next
            if (node.element == element && node.remove()) return true
        }
    }

    /**
     * This is an internal function for tests.
     * DO NOT CHANGE THIS CODE.
     */
    override fun validate() {
        check(tail.value.next.value == null) {
            "tail.next must be null"
        }
        var node = head.value
        // Traverse the linked list
        while (true) {
            if (node !== head.value && node !== tail.value) {
                check(!node.extractedOrRemoved) {
                    "Removed node with element ${node.element} found in the middle of this queue"
                }
            }
            node = node.next.value ?: break
        }
    }

    inner class Node(
        var element: E?
    ) {
        val next = atomic<Node?>(null)

        private val _extractedOrRemoved = atomic(false)
        val extractedOrRemoved get() = _extractedOrRemoved.value

        fun markExtractedOrRemoved(): Boolean = _extractedOrRemoved.compareAndSet(false, true)

        /**
         * Removes this node from the queue structure.
         * Returns `true` if this node was successfully
         * removed, or `false` if it has already been
         * removed by [remove] or extracted by [dequeue].
         */
        fun remove(): Boolean {
            val removed = markExtractedOrRemoved()
            val currNext = next.value
            if (currNext != null) {
                val prevNode = findPrev()

                if (prevNode != null) {
                    prevNode.next.value = this.next.value
                }

                if (currNext.extractedOrRemoved) {
                    currNext.remove()
                }
            }

            return removed
        }

        private fun findPrev(): Node? {
            var prevNode = head.value
            while (prevNode.next.value != this) {
                prevNode = prevNode.next.value ?: return null
            }

            return prevNode
        }
    }
}
