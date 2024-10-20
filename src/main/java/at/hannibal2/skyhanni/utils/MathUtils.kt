package at.hannibal2.skyhanni.utils

import kotlin.math.roundToInt

object MathUtils {
    fun evaluateExpression(expression: String): Int {
        val splits = split(expression)
        val parsedExpression = parse(splits)
        return evaluate(parsedExpression).roundToInt()
    }

    fun split(expression: String): List<String> {
        val regex = Regex("""(\d+\.?\d*|\+|\-|\*|\/|\(|\))""")
        return regex.findAll(expression).map { it.value }.toList()
    }

    fun parse(splits: List<String>): List<String> {
        val output = mutableListOf<String>()
        val operators = mutableListOf<String>()
        val precedence = mapOf("+" to 1, "-" to 1, "*" to 2, "/" to 2)

        for (split in splits) {
            when {
                split.isDouble() -> output.add(split)
                split == "(" -> operators.add(split)
                split == ")" -> {
                    while (operators.isNotEmpty() && operators.last() != "(") {
                        output.add(operators.removeAt(operators.size - 1))
                    }
                    operators.removeAt(operators.size - 1)
                }
                split in precedence -> {
                    while (operators.isNotEmpty() && (precedence[operators.last()] ?: 0) >= (precedence[split] ?: 0)) {
                        output.add(operators.removeAt(operators.size - 1))
                    }
                    operators.add(split)
                }
            }
        }

        while (operators.isNotEmpty()) {
            output.add(operators.removeAt(operators.size - 1))
        }

        return output
    }

    private fun evaluate(splits: List<String>): Double {
        val nums = mutableListOf<Double>()

        for (split in splits) {
            when {
                split.isDouble() -> nums.add(split.toDouble())
                split in listOf("+", "-", "*", "/") -> {
                    val b = nums.removeAt(nums.size - 1)
                    val a = nums.removeAt(nums.size - 1)
                    nums.add(
                        when (split) {
                            "+" -> a + b
                            "-" -> a - b
                            "*" -> a * b
                            "/" -> a / b
                            else -> throw IllegalArgumentException("Unknown operator: $split")
                        }
                    )
                }
            }
        }

        return nums[0]
    }

    private fun String.isDouble(): Boolean {
        return this.toDoubleOrNull() != null
    }
}
