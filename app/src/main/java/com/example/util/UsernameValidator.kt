package com.example.util

import java.text.Normalizer
import java.util.Locale

data class UsernameValidationResult(
    val isValid: Boolean,
    val message: String? = null
)

object UsernameValidator {

    // Lista de términos sensuales, eróticos, provocativos o no adecuados para un colegio
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

    // Lista de términos maleducados, groserías, vulgaridades, insultos o lenguaje despectivo
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

    fun validate(username: String): UsernameValidationResult {
        val trimmed = username.trim()

        if (trimmed.isBlank()) {
            return UsernameValidationResult(false, "El nombre de usuario no puede estar vacío.")
        }

        if (trimmed.length < 3) {
            return UsernameValidationResult(false, "El nombre de usuario debe tener mínimo 3 caracteres.")
        }

        if (trimmed.length > 20) {
            return UsernameValidationResult(false, "El nombre de usuario no puede superar los 20 caracteres.")
        }

        // Permitir solo letras, números, puntos, guiones y guiones bajos
        if (!trimmed.matches(Regex("^[a-zA-Z0-9._-]+$"))) {
            return UsernameValidationResult(
                false,
                "Solo se permiten letras, números, puntos (.), guiones (-) o guiones bajos (_)."
            )
        }

        val normalized = normalizeText(trimmed)

        // Verificar términos sensuales
        for (term in SENSUAL_TERMS) {
            if (normalized.contains(term)) {
                return UsernameValidationResult(
                    false,
                    "Nombre no permitido: Contiene términos sensuales o provocativos ('$term') no admitidos en el entorno escolar. Elige un nombre respetuoso e institucional."
                )
            }
        }

        // Verificar términos maleducados / groserías
        for (term in RUDE_TERMS) {
            if (normalized.contains(term)) {
                return UsernameValidationResult(
                    false,
                    "Nombre no permitido: Contiene expresiones maleducadas o vulgares ('$term'). Por favor usa un nombre apropiado para el colegio."
                )
            }
        }

        return UsernameValidationResult(true, null)
    }

    /**
     * Normaliza el texto removiendo acentos, signos de puntuación
     * y transformando leetspeak común (0->o, 1->i, 3->e, 4->a, 5->s, 7->t, @->a, $->s).
     */
    private fun normalizeText(input: String): String {
        var text = input.lowercase(Locale.ROOT)

        // Reemplazos de leetspeak
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
            .replace("v", "u") // para trampas como 'pvt4'
            .replace(".", "")
            .replace("-", "")
            .replace("_", "")

        // Quitar acentos diacríticos
        val normalized = Normalizer.normalize(text, Normalizer.Form.NFD)
        return normalized.replace(Regex("\\p{InCombiningDiacriticalMarks}+"), "")
    }
}
