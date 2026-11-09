
package com.example.onewinvirtuellepredictor

class MatchAnalyzer(
    private val database: MatchDatabase
) {

    suspend fun analyze(): List<MatchEntity> {
        return database.matchDao().getAllMatches()
    }

    fun calculateProbabilities(
        homeWins: Int,
        draws: Int,
        awayWins: Int
    ): Triple<Double, Double, Double> {

        val total = homeWins + draws + awayWins

        if (total == 0) {
            return Triple(0.0, 0.0, 0.0)
        }

        val homeProbability =
            homeWins.toDouble() / total * 100

        val drawProbability =
            draws.toDouble() / total * 100

        val awayProbability =
            awayWins.toDouble() / total * 100

        return Triple(
            homeProbability,
            drawProbability,
            awayProbability
        )
    }
}
