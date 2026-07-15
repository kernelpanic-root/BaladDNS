package com.kernelpanic.baladdns.data.nextdns.setup

/** Locale-backed guide text with explicit placeholder values. */
data class GuideRichText(
    val template: String,
    val values: Map<String, String> = emptyMap(),
)

data class GuideTextSegment(
    val value: String,
    val emphasized: Boolean,
)


fun GuideRichText.segments(): List<GuideTextSegment> {
    val result = mutableListOf<GuideTextSegment>()
    val activeTags = mutableListOf<Int>()

    fun append(value: String) {
        if (value.isEmpty()) return
        val emphasized = activeTags.isNotEmpty()
        val previous = result.lastOrNull()
        if (previous != null && previous.emphasized == emphasized) {
            result[result.lastIndex] = previous.copy(value = previous.value + value)
        } else {
            result += GuideTextSegment(value, emphasized)
        }
    }

    var index = 0
    while (index < template.length) {
        when {
            template[index] == '{' && index + 1 < template.length && template[index + 1] == '{' -> {
                val end = template.indexOf("}}", index + 2)
                if (end == -1) {
                    append(template[index].toString())
                    index++
                } else {
                    val key = template.substring(index + 2, end)
                    append(values[key] ?: "{{$key}}")
                    index = end + 2
                }
            }

            template[index] == '<' -> {
                var cursor = index + 1
                val closing = cursor < template.length && template[cursor] == '/'
                if (closing) cursor++
                val digitsStart = cursor
                while (cursor < template.length && template[cursor].isDigit()) cursor++
                val isTag = cursor > digitsStart && cursor < template.length && template[cursor] == '>'
                if (!isTag) {
                    append(template[index].toString())
                    index++
                } else {
                    val tag = template.substring(digitsStart, cursor).toInt()
                    if (closing) {
                        activeTags.lastIndexOf(tag)
                            .takeIf { it >= 0 }
                            ?.let(activeTags::removeAt)
                    } else {
                        activeTags += tag
                    }
                    index = cursor + 1
                }
            }

            else -> {
                append(template[index].toString())
                index++
            }
        }
    }

    return result
}
