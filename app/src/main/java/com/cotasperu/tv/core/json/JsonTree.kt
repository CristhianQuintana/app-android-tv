package com.cotasperu.tv.core.json

import com.squareup.moshi.JsonReader
import okio.Buffer

internal class PayloadException : Exception()

internal sealed interface JsonValue {
    data class Obj(val map: Map<String, JsonValue>) : JsonValue
    data class Arr(val items: List<JsonValue>) : JsonValue
    data class Str(val value: String) : JsonValue
    data class Num(val value: Double) : JsonValue
    data class Bool(val value: Boolean) : JsonValue
    data object Nil : JsonValue
}

internal object JsonTree {
    private const val MAX_DEPTH = 16
    private const val MAX_ITEMS = 5_000
    private const val MAX_STRING = 100_000

    fun parse(json: String): JsonValue {
        val reader = JsonReader.of(Buffer().writeUtf8(json.trim().removePrefix("\uFEFF")))
        reader.isLenient = false
        val value = read(reader, 0)
        if (reader.peek() != JsonReader.Token.END_DOCUMENT) throw PayloadException()
        reader.close()
        return value
    }

    private fun read(reader: JsonReader, depth: Int): JsonValue {
        if (depth > MAX_DEPTH) throw PayloadException()
        return when (reader.peek()) {
            JsonReader.Token.BEGIN_OBJECT -> {
                val map = LinkedHashMap<String, JsonValue>()
                reader.beginObject()
                while (reader.hasNext()) {
                    if (map.size >= MAX_ITEMS) throw PayloadException()
                    val name = reader.nextName()
                    map[name] = read(reader, depth + 1)
                }
                reader.endObject()
                JsonValue.Obj(map)
            }
            JsonReader.Token.BEGIN_ARRAY -> {
                val items = ArrayList<JsonValue>()
                reader.beginArray()
                while (reader.hasNext()) {
                    if (items.size >= MAX_ITEMS) throw PayloadException()
                    items += read(reader, depth + 1)
                }
                reader.endArray()
                JsonValue.Arr(items)
            }
            JsonReader.Token.STRING -> {
                val value = reader.nextString()
                if (value.length > MAX_STRING) throw PayloadException()
                JsonValue.Str(value)
            }
            JsonReader.Token.NUMBER -> JsonValue.Num(reader.nextDouble())
            JsonReader.Token.BOOLEAN -> JsonValue.Bool(reader.nextBoolean())
            JsonReader.Token.NULL -> {
                reader.nextNull<Unit>()
                JsonValue.Nil
            }
            else -> throw PayloadException()
        }
    }
}

internal fun JsonValue.asObject(): Map<String, JsonValue>? = (this as? JsonValue.Obj)?.map

internal fun JsonValue.string(key: String): String? {
    val value = asObject()?.get(key) ?: return null
    return (value as? JsonValue.Str)?.value
}

internal fun JsonValue.child(key: String): JsonValue? = asObject()?.get(key)

internal fun JsonValue.objectChild(key: String): JsonValue? = child(key)?.takeIf { it is JsonValue.Obj }

internal fun JsonValue.arrayChild(key: String): List<JsonValue>? = (child(key) as? JsonValue.Arr)?.items

internal fun JsonValue.integral(): Long? {
    val number = this as? JsonValue.Num ?: return null
    if (number.value % 1.0 != 0.0) return null
    if (number.value < Long.MIN_VALUE.toDouble() || number.value > Long.MAX_VALUE.toDouble()) return null
    return number.value.toLong()
}
