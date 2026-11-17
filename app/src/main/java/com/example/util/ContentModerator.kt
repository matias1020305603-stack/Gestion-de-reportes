package com.example.util

import java.text.Normalizer
import java.util.Locale

object ContentModerator {
    private val SENSUAL_TERMS = listOf(
        "sexy", "sensual", "hot", "caliente", "nude", "nudes", "pack",
        "erotico", "erotica", "sex", "sexo", "tetas", "senos", "pechos",
        "boobs", "culo", "ass", "butt", "nalgas", "playboy", "sugar",
        "daddy", "onlyfans", "fetish", "xxx", "porno", "porn", "placer",
        "orgasmo", "stripper", "tentacion", "pasion", "babydoll", "traviesa",
        "travieso", "ardiente", "desnudo", "desnuda", "intimidad", "gemidos",
        "cachondo", "cachonda", "bitch", "zorra", "sugarbaby", "milf",
        "dildo", "condon", "pene", "vagina", "clitoris", "ano", "penetrar",
        "coito", "erotismo", "sensualon", "sensualona", "teton", "tetona",
        "culon", "culona", "mamacita", "papacito", "hotboy", "hotgirl",
        "sweetheart", "pervert", "pervertido", "pervertida"
    )

    private val RUDE_TERMS = listOf(
        "mierda", "pendejo", "pendeja", "culero", "cabron", "cabrona",
        "idiota", "estupido", "estupida", "marica", "maricon", "gonorrea",
        "hijueputa", "carechimba", "malparido", "malparida", "verga",
        "pito", "chucha", "coño", "polla", "carajo", "perra", "maldito",
        "maldita", "chinga", "chingar", "chingada", "chupame", "chupalo",
        "mamamela", "mamada", "imbecil", "tarado", "tarada", "zorron",
        "bastardo", "sapo", "pirobo", "guiso", "nazi", "hitler", "suicida",
        "puta", "puto", "putazo", "careverga", "caremonda", "monda",
        "huevon", "guevon", "baboso", "estupidez", "inutil", "asno"
    )

    fun checkContent(text: String): Pair<Boolean, String?> {
        val trimmed = text.trim()
        if (trimmed.isBlank()) {
            return Pair(false, "La publicación o comentario no puede estar vacío.")
        }

        val normalized = normalizeText(trimmed)

        for (term in SENSUAL_TERMS) {
            if (normalized.contains(term)) {
                return Pair(false, "Publicación rechazada: Contiene términos sensuales o no aptos para el entorno escolar ('$term').")
            }
        }

        for (term in RUDE_TERMS) {
            if (normalized.contains(term)) {
                return Pair(false, "Publicación rechazada: Contiene expresiones groseras o vulgares ('$term'). Mantén un lenguaje respetuoso.")
            }
        }

        return Pair(true, null)
    }

    private fun normalizeText(input: String): String {
        var text = input.lowercase(Locale.ROOT)
        text = text
            .replace("0", "o")
            .replace("1", "i")
            .replace("3", "e")
            .replace("4", "a")
            .replace("5", "s")
            .replace("7", "t")
            .replace("@", "a")
            .replace("$", "s")
            .replace("!", "i")
            .replace("+", "t")
            .replace("v", "u")
            .replace(".", "")
            .replace("-", "")
            .replace("_", "")

        val normalized = Normalizer.normalize(text, Normalizer.Form.NFD)
        return normalized.replace(Regex("\\p{InCombiningDiacriticalMarks}+"), "")
    }
}
