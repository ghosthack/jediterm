package com.jediterm.terminal.model

/**
 * @param maxCapacity maximum number of stored lines; -1 means no restriction
 *
 * Every member is `@Synchronized`: [TerminalTextBuffer] guards its own mutations of this
 * storage with its own lock, but [get] is itself a mutating operation (it silently
 * extends the deque with empty lines up to `index`) and is called from places that don't
 * take that lock — e.g. the UI thread reads a line directly (triple-click-to-select-line
 * in `TerminalPanel`) while the emulator thread is concurrently clearing or scrolling the
 * same buffer. Racing two threads on an unsynchronized `ArrayDeque` doesn't throw; it
 * corrupts the backing array so a slot reads back null instead of a line — which then
 * surfaces much later, confusingly, as a `NullPointerException` in unrelated code that
 * trusted [get]'s non-null return type (e.g. `TerminalTextBuffer.clearLines`).
 */
internal class CyclicBufferLinesStorage(private val maxCapacity: Int) : LinesStorage {

  private val lines: ArrayDeque<TerminalLine> = ArrayDeque()

  private val isCapacityLimited: Boolean = maxCapacity >= 0

  override val size: Int
    @Synchronized get() = lines.size

  /** O(1) */
  @Synchronized
  override fun get(index: Int): TerminalLine {
    if (index < 0) {
      throw IndexOutOfBoundsException("Negative index: $index")
    }

    if (index >= size) {
      repeat(index - size + 1) {
        addToBottom(TerminalLine.createEmpty())
      }
    }

    return lines[index]
  }

  /** O(size) */
  @Synchronized
  override fun indexOf(line: TerminalLine): Int = lines.indexOf(line)

  /**
   * Amortized 0(1).
   * The worst case is when we need to extend the internal storage of the array deque.
   */
  @Synchronized
  override fun addToTop(line: TerminalLine) {
    if (isCapacityLimited && lines.size == maxCapacity) {
      return
    }
    lines.addFirst(line)
  }

  /**
   * Amortized 0(1).
   * The worst case is when we need to extend the internal storage of the array deque.
   */
  @Synchronized
  override fun addToBottom(line: TerminalLine) {
    lines.addLast(line)
    if (isCapacityLimited && lines.size > maxCapacity) {
      lines.removeFirst()
    }
  }

  /** O(1) */
  @Synchronized
  override fun removeFromTop(): TerminalLine {
    return lines.removeFirst()
  }

  /** O(1) */
  @Synchronized
  override fun removeFromBottom(): TerminalLine {
    return lines.removeLast()
  }

  /** O(size) */
  @Synchronized
  override fun clear() = lines.clear()

  /** A snapshot, not a live view: an `ArrayDeque`'s own iterator is fail-fast and would
   * throw `ConcurrentModificationException` (or worse, given the same unsynchronized
   * backing array, something less well-defined) if another thread mutates the deque
   * mid-iteration. */
  @Synchronized
  override fun iterator(): Iterator<TerminalLine> = lines.toList().iterator()
}
