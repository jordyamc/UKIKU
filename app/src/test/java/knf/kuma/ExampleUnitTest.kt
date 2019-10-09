package knf.kuma

import knf.kuma.achievements.LevelCalculator
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * @see [Testing documentation](http://d.android.com/tools/testing)
 */
class ExampleUnitTest {
    @Test
    @Throws(Exception::class)
    fun addition_isCorrect() {
        assertEquals(4, (2 + 2).toLong())
    }

    @Test
    fun testPoints() {
        val lc = LevelCalculator()
        var last = 0
        lc.levels.forEachIndexed { index, i ->
            print("Level ${index + 1}: ${i - last}\n")
            last = i
        }
    }
}