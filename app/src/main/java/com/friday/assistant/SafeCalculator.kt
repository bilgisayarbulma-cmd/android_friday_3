package com.friday.assistant

import kotlin.math.*

/**
 * Guvenli matematiksel ifade hesaplayici. Windows surumundeki calculator.py
 * ile ayni mantik: sadece sayisal/matematiksel islemlere izin verir.
 * Basit bir recursive-descent parser: sayilar, + - * / ^ () ve temel
 * fonksiyonlar (sqrt, sin, cos, tan, log) desteklenir.
 */
object SafeCalculator {

    fun calculate(expression: String): Pair<Boolean, String> {
        if (expression.isBlank()) return false to "Hesaplanacak bir ifade verilmedi."

        return try {
            val sanitized = expression
                .replace("pi", Math.PI.toString())
                .replace("e", Math.E.toString())

            val result = Parser(sanitized).parseExpression()
            val formatted = if (result == result.toLong().toDouble()) {
                result.toLong().toString()
            } else {
                "%.8f".format(result).trimEnd('0').trimEnd('.')
            }
            true to formatted
        } catch (e: Exception) {
            false to "İfade anlaşılamadı: ${e.message}"
        }
    }

    private class Parser(private val input: String) {
        private var pos = 0

        fun parseExpression(): Double {
            val result = parseAddSub()
            skipWhitespace()
            if (pos < input.length) throw IllegalArgumentException("beklenmeyen karakter: ${input[pos]}")
            return result
        }

        private fun parseAddSub(): Double {
            var result = parseMulDiv()
            while (true) {
                skipWhitespace()
                when (peek()) {
                    '+' -> { pos++; result += parseMulDiv() }
                    '-' -> { pos++; result -= parseMulDiv() }
                    else -> return result
                }
            }
        }

        private fun parseMulDiv(): Double {
            var result = parsePow()
            while (true) {
                skipWhitespace()
                when (peek()) {
                    '*' -> { pos++; result *= parsePow() }
                    '/' -> {
                        pos++
                        val divisor = parsePow()
                        if (divisor == 0.0) throw ArithmeticException("sıfıra bölme")
                        result /= divisor
                    }
                    else -> return result
                }
            }
        }

        private fun parsePow(): Double {
            val base = parseUnary()
            skipWhitespace()
            if (peek() == '^') {
                pos++
                val exponent = parsePow()
                return base.pow(exponent)
            }
            return base
        }

        private fun parseUnary(): Double {
            skipWhitespace()
            if (peek() == '-') { pos++; return -parseUnary() }
            if (peek() == '+') { pos++; return parseUnary() }
            return parseAtom()
        }

        private fun parseAtom(): Double {
            skipWhitespace()
            if (peek() == '(') {
                pos++
                val value = parseAddSub()
                skipWhitespace()
                if (peek() != ')') throw IllegalArgumentException("eksik parantez")
                pos++
                return value
            }

            val funcName = tryParseIdentifier()
            if (funcName != null) {
                skipWhitespace()
                if (peek() == '(') {
                    pos++
                    val arg = parseAddSub()
                    skipWhitespace()
                    if (peek() != ')') throw IllegalArgumentException("eksik parantez")
                    pos++
                    return applyFunction(funcName, arg)
                }
                throw IllegalArgumentException("tanımsız: $funcName")
            }

            return parseNumber()
        }

        private fun applyFunction(name: String, arg: Double): Double = when (name) {
            "sqrt" -> sqrt(arg)
            "sin" -> sin(arg)
            "cos" -> cos(arg)
            "tan" -> tan(arg)
            "log" -> log10(arg)
            "ln" -> ln(arg)
            "abs" -> abs(arg)
            "floor" -> floor(arg)
            "ceil" -> ceil(arg)
            else -> throw IllegalArgumentException("desteklenmeyen fonksiyon: $name")
        }

        private fun tryParseIdentifier(): String? {
            val start = pos
            while (pos < input.length && input[pos].isLetter()) pos++
            return if (pos > start) input.substring(start, pos) else null
        }

        private fun parseNumber(): Double {
            val start = pos
            while (pos < input.length && (input[pos].isDigit() || input[pos] == '.')) pos++
            if (pos == start) throw IllegalArgumentException("sayı bekleniyor")
            return input.substring(start, pos).toDouble()
        }

        private fun peek(): Char? = if (pos < input.length) input[pos] else null

        private fun skipWhitespace() {
            while (pos < input.length && input[pos] == ' ') pos++
        }
    }
}
