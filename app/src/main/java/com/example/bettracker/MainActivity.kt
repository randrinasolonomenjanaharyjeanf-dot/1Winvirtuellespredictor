
package com.example.onewinvirtuellepredictor

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.room.Room
import kotlinx.coroutines.launch
import kotlin.math.exp
import kotlin.math.pow

class MainActivity : ComponentActivity() {

    private lateinit var database: MatchDatabase
    private lateinit var analyzer: MatchAnalyzer
    private lateinit var networkMonitor: NetworkMonitor

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        database = Room.databaseBuilder(
            applicationContext,
            MatchDatabase::class.java,
            "onewin_database"
        ).build()

        analyzer = MatchAnalyzer(database)
        networkMonitor = NetworkMonitor(applicationContext)

        setContent {
            PredictorApp(analyzer, networkMonitor)
        }
    }
}

data class Match(
    val home: String,
    val away: String,
    val homeGoals: Int,
    val awayGoals: Int
)

data class Prediction(
    val home: Double,
    val draw: Double,
    val away: Double,
    val expectedHome: Double,
    val expectedAway: Double
)

object PredictionEngine {

    private fun poisson(lambda: Double, goals: Int): Double {
        var factorial = 1.0
        for (i in 1..goals) factorial *= i
        return exp(-lambda) * lambda.pow(goals) / factorial
    }

    fun analyze(
        matches: List<Match>,
        homeTeam: String,
        awayTeam: String
    ): Prediction? {

        val homeHistory = matches.filter {
            it.home.equals(homeTeam, true) ||
            it.away.equals(homeTeam, true)
        }

        val awayHistory = matches.filter {
            it.home.equals(awayTeam, true) ||
            it.away.equals(awayTeam, true)
        }

        if (homeHistory.size < 3 || awayHistory.size < 3) {
            return null
        }

        fun average(
            team: String,
            history: List<Match>,
            scored: Boolean
        ): Double {
            val total = history.sumOf {
                when {
                    it.home.equals(team, true) ->
                        if (scored) it.homeGoals else it.awayGoals
                    else ->
                        if (scored) it.awayGoals else it.homeGoals
                }
            }
            return total.toDouble() / history.size
        }

        val homeAttack = average(homeTeam, homeHistory, true)
        val homeDefense = average(homeTeam, homeHistory, false)

        val awayAttack = average(awayTeam, awayHistory, true)
        val awayDefense = average(awayTeam, awayHistory, false)

        val lambdaHome =
            ((homeAttack + awayDefense) / 2.0).coerceIn(0.15, 5.0)

        val lambdaAway =
            ((awayAttack + homeDefense) / 2.0).coerceIn(0.15, 5.0)

        var h = 0.0
        var d = 0.0
        var a = 0.0
        var total = 0.0

        for (i in 0..8) {
            for (j in 0..8) {
                val probability =
                    poisson(lambdaHome, i) *
                    poisson(lambdaAway, j)

                total += probability

                when {
                    i > j -> h += probability
                    i == j -> d += probability
                    else -> a += probability
                }
            }
        }

        return Prediction(
            home = h / total * 100,
            draw = d / total * 100,
            away = a / total * 100,
            expectedHome = lambdaHome,
            expectedAway = lambdaAway
        )
    }
}

@Composable
fun PredictorApp(
    analyzer: MatchAnalyzer,
    networkMonitor: NetworkMonitor
) {

    var homeTeam by remember { mutableStateOf("") }
    var awayTeam by remember { mutableStateOf("") }
    var history by remember { mutableStateOf("") }
    var result by remember { mutableStateOf<Prediction?>(null) }
    var error by remember { mutableStateOf("") }
    var savedCount by remember { mutableIntStateOf(0) }

    val scope = rememberCoroutineScope()
    val isOnline by networkMonitor.isOnline.collectAsState(
        initial = false
    )

    val green = Color(0xFF00E69A)
    val dark = Color(0xFF0B1220)
    val panel = Color(0xFF182438)

    MaterialTheme {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(dark)
                .verticalScroll(rememberScrollState())
                .padding(18.dp)
        ) {

            Text(
                "1WIN VIRTUELLE",
                color = green,
                fontSize = 27.sp,
                fontWeight = FontWeight.Black
            )

            Text(
                "FOOTBALL 3D PREDICTOR",
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(Modifier.height(12.dp))

            Card(
                colors = CardDefaults.cardColors(
                    containerColor = panel
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = if (isOnline) "● ONLINE" else "● OFFLINE",
                    color = if (isOnline) green else Color.Yellow,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(14.dp)
                )
            }

            Spacer(Modifier.height(24.dp))

            Text(
                "MATCH ANALYSIS",
                color = green,
                fontWeight = FontWeight.Bold
            )

            Spacer(Modifier.height(12.dp))

            OutlinedTextField(
                value = homeTeam,
                onValueChange = {
                    homeTeam = it
                    result = null
                },
                label = { Text("Home Team") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Spacer(Modifier.height(10.dp))

            OutlinedTextField(
                value = awayTeam,
                onValueChange = {
                    awayTeam = it
                    result = null
                },
                label = { Text("Away Team") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Spacer(Modifier.height(22.dp))

            Text(
                "HISTORIQUE DES MATCHS",
                color = green,
                fontWeight = FontWeight.Bold
            )

            Spacer(Modifier.height(8.dp))

            Text(
                "Ampidiro ny résultats taloha. Ligne iray = match iray.",
                color = Color.LightGray,
                fontSize = 13.sp
            )

            Spacer(Modifier.height(8.dp))

            OutlinedTextField(
                value = history,
                onValueChange = {
                    history = it
                    result = null
                },
                label = {
                    Text("Home,Away,HomeGoals,AwayGoals")
                },
                placeholder = {
                    Text(
                        "Team A,Team B,2,1\n" +
                        "Team B,Team A,1,1\n" +
                        "Team A,Team C,3,0"
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp),
                minLines = 5
            )

            Spacer(Modifier.height(8.dp))

            Text(
                "Format: Team A,Team B,2,1",
                color = Color.LightGray,
                fontSize = 12.sp
            )

            Spacer(Modifier.height(20.dp))

            Button(
                onClick = {

                    error = ""
                    result = null

                    val matches = history.lines()
                        .filter { it.isNotBlank() }
                        .mapNotNull { line ->

                            val parts = line.split(",")
                                .map { it.trim() }

                            if (parts.size != 4) {
                                null
                            } else {
                                val hg = parts[2].toIntOrNull()
                                val ag = parts[3].toIntOrNull()

                                if (
                                    parts[0].isBlank() ||
                                    parts[1].isBlank() ||
                                    hg == null || ag == null ||
                                    hg < 0 || ag < 0
                                ) {
                                    null
                                } else {
                                    Match(
                                        parts[0],
                                        parts[1],
                                        hg,
                                        ag
                                    )
                                }
                            }
                        }

                    if (matches.size < 6) {
                        error = "Ampidiro match 6 farafahakeliny."
                    } else if (
                        homeTeam.isBlank() ||
                        awayTeam.isBlank() ||
                        homeTeam.equals(awayTeam, true)
                    ) {
                        error = "Hamarino ny anaran'ny ekipa."
                    } else {

                        scope.launch {
                            try {

                                val oldMatches = analyzer.analyze()

                                matches.forEach { match ->

                                    val alreadySaved = oldMatches.any {
                                        it.homeTeam.equals(match.home, true) &&
                                        it.awayTeam.equals(match.away, true) &&
                                        it.homeGoals == match.homeGoals &&
                                        it.awayGoals == match.awayGoals
                                    }

                                    if (!alreadySaved) {
                                        analyzer.saveMatch(
                                            MatchEntity(
                                                homeTeam = match.home,
                                                awayTeam = match.away,
                                                homeGoals = match.homeGoals,
                                                awayGoals = match.awayGoals
                                            )
                                        )
                                    }
                                }

                                val savedMatches = analyzer.analyze()

                                val databaseMatches = savedMatches.map {
                                    Match(
                                        it.homeTeam,
                                        it.awayTeam,
                                        it.homeGoals,
                                        it.awayGoals
                                    )
                                }

                                val prediction = PredictionEngine.analyze(
                                    databaseMatches,
                                    homeTeam.trim(),
                                    awayTeam.trim()
                                )

                                if (prediction == null) {
                                    error = "Mbola tsy ampy ny historique ho an'ireo ekipa."
                                } else {

                                    analyzer.savePrediction(
                                        PredictionEntity(
                                            homeTeam = homeTeam.trim(),
                                            awayTeam = awayTeam.trim(),
                                            homeProbability = prediction.home,
                                            drawProbability = prediction.draw,
                                            awayProbability = prediction.away
                                        )
                                    )

                                    result = prediction
                                    savedCount = analyzer.getPredictionHistory().size
                                }

                            } catch (e: Exception) {
                                error = "Database error: ${e.message}"
                            }
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = green
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    "ANALYZE MATCH",
                    color = Color.Black,
                    fontWeight = FontWeight.Bold
                )
            }

            if (error.isNotEmpty()) {
                Spacer(Modifier.height(12.dp))
                Text(error, color = Color.Red)
            }

            result?.let { prediction ->

                Spacer(Modifier.height(24.dp))

                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = panel
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(Modifier.padding(18.dp)) {

                        Text(
                            "PREDICTION RESULTS",
                            color = green,
                            fontSize = 19.sp,
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(Modifier.height(18.dp))

                        Text(
                            "HOME WIN: %.1f%%".format(prediction.home),
                            color = Color.White
                        )

                        Text(
                            "DRAW: %.1f%%".format(prediction.draw),
                            color = Color.White
                        )

                        Text(
                            "AWAY WIN: %.1f%%".format(prediction.away),
                            color = Color.White
                        )

                        Spacer(Modifier.height(18.dp))

                        Text(
                            "EXPECTED GOALS",
                            color = green,
                            fontWeight = FontWeight.Bold
                        )

                        Text(
                            "$homeTeam: %.2f".format(prediction.expectedHome),
                            color = Color.White
                        )

                        Text(
                            "$awayTeam: %.2f".format(prediction.expectedAway),
                            color = Color.White
                        )

                        Spacer(Modifier.height(18.dp))

                        val best = maxOf(
                            prediction.home,
                            prediction.draw,
                            prediction.away
                        )

                        val signal = when {
                            best < 55 -> "NO STRONG SIGNAL"
                            best == prediction.home -> "HOME WIN"
                            best == prediction.draw -> "DRAW"
                            else -> "AWAY WIN"
                        }

                        Text(
                            "PREDICTION SIGNAL",
                            color = green,
                            fontWeight = FontWeight.Bold
                        )

                        Text(
                            signal,
                            color = if (best >= 55) green else Color.Yellow,
                            fontSize = 23.sp,
                            fontWeight = FontWeight.Black
                        )

                        Text(
                            "Statistical estimate only - not guaranteed.",
                            color = Color.LightGray,
                            fontSize = 11.sp
                        )
                    }
                }
            }

            Spacer(Modifier.height(25.dp))

            Text(
                "ROOM DATABASE: ACTIVE",
                color = green,
                fontWeight = FontWeight.Bold
            )

            Text(
                "Saved predictions: $savedCount",
                color = Color.White,
                fontSize = 13.sp
            )

            Spacer(Modifier.height(12.dp))

            Text(
                "DATA MODE: MANUAL",
                color = green,
                fontWeight = FontWeight.Bold
            )

            Text(
                "Automatic data connection: API integration pending",
                color = Color.LightGray,
                fontSize = 12.sp
            )
        }
    }
}
