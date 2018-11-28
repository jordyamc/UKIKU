package knf.kuma.achievements

class LevelCalculator {
    var level: Int = 0
    var toLvlUp: Int = 400
    var progress: Int = 0
    var max: Int = 400
    private lateinit var levels: List<Int>

    init {
        createLevels()
    }


    fun calculate(points: Int) {
        if (points < 400) {
            level = 0
            toLvlUp = 400 - points
            progress = points
            max = 400
        } else {
            var index = 0
            for (value in levels) {
                if (points < value) {
                    level = index
                    max = value - (400 * index)
                    progress = points - levels[index - 1]
                    toLvlUp = /*max - progress*/ value - points
                    break
                }
                index++
            }
        }
    }

    private fun createLevels() {
        val lvls = mutableListOf<Int>()
        var last = 0
        for (i in 1..50) {
            val xp = (last + (400 + (175 * (i - 1)))).also { last = it }
            lvls.add(xp)
        }
        levels = lvls
    }
}