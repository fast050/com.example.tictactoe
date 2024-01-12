package com.example.models

import ch.qos.logback.core.joran.spi.EventPlayer
import kotlinx.serialization.Serializable

/**
 *  this data class to show the current state of the game is sent from backend to the client side
 */
@Serializable
data class GameState(
    val playerAtTurn : Char? = 'X',
    val field : Array<Array<Char?>> = emptyField(),
    val winningPlayer :Char? = null ,
    val isBoardFull :Boolean = false,
    val connectedPlayer: List<Char> = emptyList()
) {

    companion object{
        fun emptyField() :Array<Array<Char?>> = arrayOf(
            arrayOf(null , null , null),
            arrayOf(null , null , null),
            arrayOf(null , null , null)
        )
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as GameState

        if (playerAtTurn != other.playerAtTurn) return false
        if (!field.contentDeepEquals(other.field)) return false
        if (winningPlayer != other.winningPlayer) return false
        if (isBoardFull != other.isBoardFull) return false
        if (connectedPlayer != other.connectedPlayer) return false

        return true
    }

    override fun hashCode(): Int {
        var result = playerAtTurn?.hashCode() ?: 0
        result = 31 * result + field.contentDeepHashCode()
        result = 31 * result + (winningPlayer?.hashCode() ?: 0)
        result = 31 * result + isBoardFull.hashCode()
        result = 31 * result + connectedPlayer.hashCode()
        return result
    }


}
