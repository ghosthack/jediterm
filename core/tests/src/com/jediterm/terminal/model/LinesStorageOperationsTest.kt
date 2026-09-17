package com.jediterm.terminal.model

import com.jediterm.terminal.TextStyle
import junit.framework.TestCase
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference

class LinesStorageOperationsTest : TestCase() {
  private val lines = listOf(
    terminalLine("line1"),
    terminalLine("line2"),
    terminalLine("line3"),
    terminalLine("line4"),
  )

  //--------------- Get ---------------------------------------------------------------------------

  fun `test get line from the range of existing lines`() {
    val storage = createScreenLinesStorage(lines)

    val line = storage[1]
    assertEquals("line2", line.text)
  }

  fun `test get line greater than current storage size`() {
    val storage = createScreenLinesStorage(lines)

    val line = storage[5]
    assertEquals("", line.text)

    // Empty lines should be added
    assertEquals(
      """
      line1
      line2
      line3
      line4
      
      
    """.trimIndent(), storage.getLinesAsString()
    )
  }

  //--------------- Insert lines ------------------------------------------------------------------

  fun `test insert lines to start`() {
    val storage = createScreenLinesStorage(lines)

    storage.insertLines(0, 2, 3, createFillerEntry(0))
    val expected = """
      
      
      line1
      line2
    """.trimIndent()
    assertEquals(expected, storage.getLinesAsString())
  }

  fun `test insert lines in the middle`() {
    val storage = createScreenLinesStorage(lines)

    storage.insertLines(1, 2, 3, createFillerEntry(0))
    val expected = """
      line1
      
      
      line2
    """.trimIndent()
    assertEquals(expected, storage.getLinesAsString())
  }

  fun `test insert lines to the end`() {
    val storage = createScreenLinesStorage(lines)

    // Effectively does nothing
    storage.insertLines(4, 2, 3, createFillerEntry(0))
    val expected = """
      line1
      line2
      line3
      line4
    """.trimIndent()
    assertEquals(expected, storage.getLinesAsString())
  }

  fun `test insert lines preserving end lines`() {
    val storage = createScreenLinesStorage(lines)

    storage.insertLines(1, 2, 2, createFillerEntry(0))
    val expected = """
      line1
      
      
      line4
    """.trimIndent()
    assertEquals(expected, storage.getLinesAsString())
  }

  fun `test insert more lines than in the y to lastLine range`() {
    val storage = createScreenLinesStorage(lines)

    // Effectively clears all lines between [1, 2] indexes.
    storage.insertLines(1, 10, 2, createFillerEntry(0))
    val expected = """
      line1
      
      
      line4
    """.trimIndent()
    assertEquals(expected, storage.getLinesAsString())
  }

  fun `test insert lines with y after lastLine`() {
    val storage = createScreenLinesStorage(lines)

    // Effectively does nothing
    storage.insertLines(4, 2, 3, createFillerEntry(0))
    val expected = """
      line1
      line2
      line3
      line4
    """.trimIndent()
    assertEquals(expected, storage.getLinesAsString())
  }

  fun `test insert lines with y and lastLine out of lines range`() {
    val storage = createScreenLinesStorage(lines)

    // Effectively does nothing
    storage.insertLines(5, 2, 7, createFillerEntry(0))
    val expected = """
      line1
      line2
      line3
      line4
    """.trimIndent()
    assertEquals(expected, storage.getLinesAsString())
  }

  fun `test insert zero lines`() {
    val storage = createScreenLinesStorage(lines)

    // Effectively does nothing
    storage.insertLines(0, 0, 3, createFillerEntry(0))
    val expected = """
      line1
      line2
      line3
      line4
    """.trimIndent()
    assertEquals(expected, storage.getLinesAsString())
  }


  //--------------- Delete lines ------------------------------------------------------------------

  fun `test delete lines from start`() {
    val storage = createScreenLinesStorage(lines)

    storage.deleteLines(0, 2, 3, createFillerEntry(0))
    val expected = """
      line3
      line4
      
      
    """.trimIndent()
    assertEquals(expected, storage.getLinesAsString())
  }

  fun `test delete lines in the middle`() {
    val storage = createScreenLinesStorage(lines)

    storage.deleteLines(1, 2, 3, createFillerEntry(0))
    val expected = """
      line1
      line4
      
      
    """.trimIndent()
    assertEquals(expected, storage.getLinesAsString())
  }

  fun `test delete lines at the end`() {
    val storage = createScreenLinesStorage(lines)

    storage.deleteLines(2, 2, 3, createFillerEntry(0))
    val expected = """
      line1
      line2


    """.trimIndent()
    assertEquals(expected, storage.getLinesAsString())
  }

  fun `test delete lines preserving end lines`() {
    val storage = createScreenLinesStorage(lines)

    storage.deleteLines(1, 2, 2, createFillerEntry(0))
    val expected = """
      line1


      line4
    """.trimIndent()
    assertEquals(expected, storage.getLinesAsString())
  }

  fun `test delete more lines than in the y to lastLine range`() {
    val storage = createScreenLinesStorage(lines)

    storage.deleteLines(1, 4, 2, createFillerEntry(0))
    val expected = """
      line1


      line4
    """.trimIndent()
    assertEquals(expected, storage.getLinesAsString())
  }

  fun `test delete lines with y after lastLine`() {
    val storage = createScreenLinesStorage(lines)

    // Effectively does nothing
    storage.deleteLines(4, 2, 3, createFillerEntry(0))
    val expected = """
      line1
      line2
      line3
      line4
    """.trimIndent()
    assertEquals(expected, storage.getLinesAsString())
  }

  fun `test delete lines with y and lastLine out of lines range`() {
    val storage = createScreenLinesStorage(lines)

    // Effectively does nothing
    storage.deleteLines(5, 2, 7, createFillerEntry(0))
    val expected = """
      line1
      line2
      line3
      line4
    """.trimIndent()
    assertEquals(expected, storage.getLinesAsString())
  }

  fun `test delete zero lines`() {
    val storage = createScreenLinesStorage(lines)

    // Effectively does nothing
    storage.deleteLines(0, 0, 3, createFillerEntry(0))
    val expected = """
      line1
      line2
      line3
      line4
    """.trimIndent()
    assertEquals(expected, storage.getLinesAsString())
  }


  //--------------- Remove bottom empty lines -----------------------------------------------------

  fun `test remove not all bottom empty lines`() {
    val storage = createScreenLinesStorage(lines)
    storage.addToBottom(TerminalLine(createFillerEntry(10)))
    storage.addToBottom(TerminalLine(createFillerEntry(10)))
    storage.addToBottom(TerminalLine(createFillerEntry(10)))

    val removedCount = storage.removeBottomEmptyLines(2)
    assertEquals(2, removedCount)

    val expected = """
      line1
      line2
      line3
      line4
      
    """.trimIndent()
    assertEquals(expected, storage.getLinesAsString())
  }

  fun `test remove all bottom empty lines`() {
    val storage = createScreenLinesStorage(lines)
    storage.addToBottom(TerminalLine(createFillerEntry(10)))
    storage.addToBottom(TerminalLine(createFillerEntry(10)))
    storage.addToBottom(TerminalLine(createFillerEntry(10)))

    val removedCount = storage.removeBottomEmptyLines(3)
    assertEquals(3, removedCount)

    val expected = """
      line1
      line2
      line3
      line4
    """.trimIndent()
    assertEquals(expected, storage.getLinesAsString())
  }

  fun `test request to remove zero bottom empty lines`() {
    val storage = createScreenLinesStorage(lines)
    storage.addToBottom(TerminalLine(createFillerEntry(10)))

    val removedCount = storage.removeBottomEmptyLines(0)
    assertEquals(0, removedCount)

    val expected = """
      line1
      line2
      line3
      line4
      
    """.trimIndent()
    assertEquals(expected, storage.getLinesAsString())
  }

  fun `test request to remove more bottom empty lines than present`() {
    val storage = createScreenLinesStorage(lines)
    storage.addToBottom(TerminalLine(createFillerEntry(10)))
    storage.addToBottom(TerminalLine(createFillerEntry(10)))
    storage.addToBottom(TerminalLine(createFillerEntry(10)))

    val removedCount = storage.removeBottomEmptyLines(5)
    assertEquals(3, removedCount)

    val expected = """
      line1
      line2
      line3
      line4
    """.trimIndent()
    assertEquals(expected, storage.getLinesAsString())
  }

  fun `test remove no bottom empty lines`() {
    val storage = createScreenLinesStorage(lines)

    val removedCount = storage.removeBottomEmptyLines(2)
    assertEquals(0, removedCount)

    val expected = """
      line1
      line2
      line3
      line4
    """.trimIndent()
    assertEquals(expected, storage.getLinesAsString())
  }

  fun `test writing and parsing`() {
    val storage = createScreenLinesStorage(emptyList())

    storage[2].writeString(3, CharBuffer("Hi!"), TextStyle.EMPTY)
    assertEquals(
      """
       |
       |
       |   Hi!
      """.trimMargin(), storage.getLinesAsString()
    )

    storage[1].writeString(1, CharBuffer("*****"), TextStyle.EMPTY)
    assertEquals(
      """
       |
       | *****
       |   Hi!
      """.trimMargin(), storage.getLinesAsString()
    )

    storage[1].writeString(3, CharBuffer("+"), TextStyle.EMPTY)
    assertEquals(
      """
       |
       | **+**
       |   Hi!
      """.trimMargin(), storage.getLinesAsString()
    )

    storage[1].writeString(4, CharBuffer("***"), TextStyle.EMPTY)
    assertEquals(
      """
       |
       | **+***
       |   Hi!
      """.trimMargin(), storage.getLinesAsString()
    )

    storage[1].writeString(8, CharBuffer("="), TextStyle.EMPTY)
    assertEquals(
      """
       |
       | **+*** =
       |   Hi!
      """.trimMargin(), storage.getLinesAsString()
    )
  }

  //--------------- Concurrency ---------------------------------------------------------

  /** Regression test for a real crash: the emulator thread clearing/erasing the buffer
   * (repeatedly calling `get`, which self-extends the deque with empty lines) racing
   * against another thread reading a line the same way — e.g. the UI thread's
   * triple-click-to-select-line, which reads via [LinesStorage.get] without taking
   * [TerminalTextBuffer]'s own lock. Before [CyclicBufferLinesStorage] synchronized its
   * own methods, this could corrupt the backing `ArrayDeque` so [get] returned a `null`
   * line despite its non-null return type, surfacing as a `NullPointerException` far
   * downstream in `TerminalTextBuffer.clearLines`. */
  fun `test concurrent get and addToBottom never return a null line`() {
    val storage = CyclicBufferLinesStorage(-1)
    val iterations = 20_000
    val failure = AtomicReference<Throwable>()
    val start = CountDownLatch(1)
    val pool = Executors.newFixedThreadPool(2)
    try {
      val writer = pool.submit {
        try {
          start.await()
          for (i in 0 until iterations) {
            storage.addToBottom(terminalLine("w$i"))
          }
        } catch (t: Throwable) {
          failure.compareAndSet(null, t)
        }
      }
      val reader = pool.submit {
        try {
          start.await()
          for (i in 0 until iterations) {
            val line = storage[i % 50]
            if (line == null) {
              failure.compareAndSet(null, AssertionError("storage[${i % 50}] returned null"))
            }
          }
        } catch (t: Throwable) {
          failure.compareAndSet(null, t)
        }
      }
      start.countDown()
      writer.get(30, TimeUnit.SECONDS)
      reader.get(30, TimeUnit.SECONDS)
    } finally {
      pool.shutdownNow()
    }
    failure.get()?.let { throw it }
  }

  private fun createScreenLinesStorage(lines: List<TerminalLine>): LinesStorage {
    val storage = CyclicBufferLinesStorage(-1)
    storage.addAllToBottom(lines)
    return storage
  }
}
