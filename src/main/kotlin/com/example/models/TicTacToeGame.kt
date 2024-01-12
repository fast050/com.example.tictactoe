package com.example.models

import io.ktor.websocket.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.concurrent.ConcurrentHashMap

class TicTacToeGame {

    private val state = MutableStateFlow(GameState())

    //to sent data to specific client at anytime , also the "concurrentHasMap" so will not be any race condition
    private val playerSockets = ConcurrentHashMap<Char , WebSocketSession>()

    private val gameScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private var delayGameJob : Job? = null


    init {
        state.onEach (::broadcast).launchIn(gameScope)
    }


    fun connectPlayer (session: WebSocketSession) : Char? {

        val isPlayerX = state.value.connectedPlayer.any { it == 'X' }
        val player = if (isPlayerX) 'O' else 'X'

        state.update {
            if (state.value.connectedPlayer.contains(player))
                return null

            if (!playerSockets.contains(player))
                playerSockets[player] = session

            it.copy(
                connectedPlayer = it.connectedPlayer + player
            )
        }

        return player
    }

    fun disconnectPlayer (player:Char) {
        playerSockets.remove(player)
        state.update {
            it.copy(connectedPlayer = it.connectedPlayer - player)
        }
    }

    private suspend fun broadcast(state: GameState){
        playerSockets.values.forEach {socket->
            socket.send(
                Json.encodeToString(state)
            )
        }
    }

    fun finishTurn(player: Char , row:Int , column:Int){

        if (state.value.field[row][column] != null ||
            state.value.winningPlayer != null ||
            state.value.playerAtTurn != player)
            return


        val currentPlayer = state.value.playerAtTurn
        state.update {
            val newField = it.field.also { field ->
                field[row][column] = currentPlayer
            }

            val isBoardFull = newField.all { it.all { it!= null } }

            if (isBoardFull)
                startNewRoundDelayed()

            it.copy(
                playerAtTurn =  if (currentPlayer == 'X') 'O' else 'X',
                field = newField,
                isBoardFull = isBoardFull,
                winningPlayer = getWinningPlayer()?.also{
                    startNewRoundDelayed()
                }
            )
        }

    }

    private fun getWinningPlayer(): Char? {
        for (i in 0..2){
           if (areFieldEquals(state.value.field[i][0] , state.value.field[i][1] ,state.value.field[i][2])) return state.value.field[i][0] // check all row
           if (areFieldEquals(state.value.field[0][i] , state.value.field[1][i] ,state.value.field[2][i])) return state.value.field[0][i] // check all colum
        }

        // Check diagonals
        if (areFieldEquals(state.value.field[0][0], state.value.field[1][1], state.value.field[2][2])) return state.value.field[0][0]
        if (areFieldEquals(state.value.field[0][2], state.value.field[1][1], state.value.field[2][0])) return state.value.field[0][2]

        return null
    }

    private fun areFieldEquals(a : Char? , b :Char? , c:Char?) = a != null && a == b && b == c

    private fun startNewRoundDelayed() {
        delayGameJob?.cancel()
        delayGameJob = gameScope.launch {
            delay(5000)
            state.update {
                it.copy(
                    playerAtTurn = 'X',
                    field = GameState.emptyField(),
                    winningPlayer = null,
                    isBoardFull = false,
                    connectedPlayer = emptyList()
                )
            }
        }
    }


}