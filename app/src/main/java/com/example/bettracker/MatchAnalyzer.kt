
package com.example.winminespredictor

class MatchAnalyzer(
    private val database: MatchDatabase
) {

    fun analyze(matchId: String): String {
        val match = database.getMatch(matchId)

        return if (match != null) {
            "Match found: $match"
        } else {
            "No matching data found"
        }
    }
}
