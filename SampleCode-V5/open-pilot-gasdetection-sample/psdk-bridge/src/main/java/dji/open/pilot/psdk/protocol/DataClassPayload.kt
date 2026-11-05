package dji.open.pilot.psdk.protocol

import java.lang.reflect.Field
import java.lang.reflect.Modifier

/**
 * Reflects a data-class payload into the map accepted by `PsdkBridge.request(cmd, payload)`.
 *
 * Fields are emitted in declaration order (superclass fields first), using the field name as
 * the JSON key. Kotlin `val`s, `var`s and constructor properties are all included; static,
 * synthetic and compiler-generated (`$...`) fields are skipped. Values are normalized so that
 * [PayloadCodec] implementations do not need to handle JVM-only types:
 * - enums become their [Enum.ordinal],
 * - `Byte` and `Short` become `Int`,
 * - `ByteArray` is copied.
 *
 * This is a convenience for simple data classes; pass a [Map] directly for full control.
 */
fun Any.toPsdkPayload(): Map<String, Any?> {
    val result = linkedMapOf<String, Any?>()
    allFields(javaClass).forEach { field ->
        field.isAccessible = true
        result[field.name] = when (val value = field.get(this)) {
            is Enum<*> -> value.ordinal
            is ByteArray -> value.copyOf()
            is Byte -> value.toInt()
            is Short -> value.toInt()
            else -> value
        }
    }
    return result
}

private fun allFields(type: Class<*>): List<Field> =
    generateSequence(type) { it.superclass }
        .takeWhile { it != Any::class.java }
        .toList()
        .reversed()
        .flatMap { level ->
            level.declaredFields.filterNot {
                Modifier.isStatic(it.modifiers) || it.isSynthetic || it.name.startsWith("$")
            }
        }
