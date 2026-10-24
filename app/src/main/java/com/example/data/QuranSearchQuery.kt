package com.example.data

/** Removes search instructions, while leaving the actual question and its negation intact. */
internal object QuranSearchQuery {
    private val searchEnding = Regex(
        "\\s+(?:(?:konusu(?:yla)?\\s+)?(?:hakkında|hakkındaki|ile\\s+ilgili|konusunda|bahseden)\\s*)?(?:(?:ayet(?:ler|leri)?|âyet(?:ler|leri)?)(?:\\s+(?:arıyorum|bul|göster|nelerdir|istiyorum))?)\\s*[?.!]*$",
        RegexOption.IGNORE_CASE
    )
    private val explicitSwitch = Regex("\\s+(?:hakkında\\s+)?değil\\s*[,;:]?\\s+", RegexOption.IGNORE_CASE)

    fun prepare(text: String): String {
        val trimmed = text.trim().replace(Regex("\\s+"), " ")
        val switch = explicitSwitch.find(trimmed)
        val focused = if (switch != null) {
            val right = trimmed.substring(switch.range.last + 1).trim()
            val explicitRequest = switch.value.any { it == ',' || it == ';' || it == ':' } ||
                trimmed.contains("hakkında", ignoreCase = true) || searchEnding.containsMatchIn(trimmed)
            val negativeQuestion = Regex("^(?:mi|mı|mu|mü|midir|mıdır|mudur|müdür|miydi|mıydı|muydu|müydü)(?:\\s|[?!.,;:]|$)",
                RegexOption.IGNORE_CASE).containsMatchIn(right)
            if (explicitRequest && !negativeQuestion && right.any(Char::isLetter)) right else trimmed
        } else trimmed
        return focused.replace(searchEnding, "").replace(Regex("\\s+(?:hakkında|konusunda)\\s*[?.!]*$", RegexOption.IGNORE_CASE), "").trim().trimEnd('?', '.', '!').ifBlank { trimmed }
    }

    /** One conservative extra neural query for an otherwise empty short Turkish derivation. */
    fun derivativeFallback(text: String): String? {
        val words = text.split(' ')
        if (words.size > 3) return null
        val suffixes = listOf(
            "leşmek", "laşmak", "lenmek", "lanmak", "çilik", "çılık", "çuluk", "çülük",
            "cilik", "cılık", "culuk", "cülük", "lik", "lık", "luk", "lük", "çi", "çı", "çu", "çü",
            "ci", "cı", "cu", "cü", "li", "lı", "lu", "lü"
        ).sortedByDescending(String::length)
        val selected = words.indices.sortedByDescending { words[it].length }.firstNotNullOfOrNull { index ->
            val word = words[index]
            if (!word.all(Char::isLetter)) null else suffixes.firstNotNullOfOrNull { suffix ->
                if (word.endsWith(suffix, ignoreCase = true) && word.length - suffix.length >= 4) {
                    index to word.dropLast(suffix.length)
                } else null
            }
        } ?: return null
        return words.toMutableList().also { it[selected.first] = selected.second }.joinToString(" ")
    }

}
