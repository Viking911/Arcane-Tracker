package net.mbonnin.arcanetracker.deckstrings

import android.util.Base64
import net.mbonnin.arcanetracker.CardUtil
import net.mbonnin.arcanetracker.Deck
import net.mbonnin.arcanetracker.getClassIndex
import java.nio.ByteBuffer
import java.util.*
import kotlin.collections.HashMap

object DeckStringParser {

    fun parse(deckstring: String): Deck? {
        try {
            return parseUnsafe(deckstring)
        } catch (e: Exception) {
            return null
        }
    }

    private fun parseUnsafe(deckstring: String): Deck? {
        val deck = Deck()

        val result = Deckstrings.decode(deckstring)

        deck.classIndex = result.heroes.map {
            val card = CardUtil.getCard(it);
            card?.playerClass?.let { getClassIndex(it) } ?: -1
        }.firstOrNull() ?: -1

        if (deck.classIndex < 0) {
            return null
        }

        val map = result.cards.map {
            val card = CardUtil.getCard(it.dbfId)

            if (card == null) {
                null
            } else {
                card.id to it.count
            }
        }.filterNotNull()
                .toMap()

        deck.cards = HashMap(map) 

        return deck
    }
}
