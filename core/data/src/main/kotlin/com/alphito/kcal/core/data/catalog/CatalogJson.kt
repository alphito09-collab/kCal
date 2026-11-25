package com.alphito.kcal.core.data.catalog

import com.alphito.kcal.core.model.Food
import com.alphito.kcal.core.model.FoodIngredient
import com.alphito.kcal.core.model.FoodRole
import com.alphito.kcal.core.model.FoodTag
import com.alphito.kcal.core.model.Glycemic
import com.alphito.kcal.core.model.IngredientCategory
import com.alphito.kcal.core.model.MealType
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.ImmutableMap
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentMapOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.collections.immutable.toImmutableMap
import kotlinx.collections.immutable.toImmutableSet
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.longOrNull

/** The catalogue as produced by backend/firebase/seed/build_catalog.py. */
data class Catalog(val version: Long, val foods: List<Food>)

/**
 * One schema, three sources: the bundled `assets/catalog.json`, Firestore `foods/{id}` documents
 * and Room's JSON columns. Everything funnels through [foodFromMap] so they can never drift.
 * Pure Kotlin (kotlinx-serialization JSON tree) – unit-testable on the JVM.
 */
object CatalogJson {
    private val json = Json { ignoreUnknownKeys = true }
    private val versionRegex = Regex("\"version\"\\s*:\\s*(\\d+)")

    fun parse(text: String): Catalog {
        val root = json.parseToJsonElement(text).toPlain() as Map<*, *>
        val foods = (root["foods"] as? List<*>).orEmpty().mapNotNull { raw ->
            val map = raw as? Map<*, *> ?: return@mapNotNull null
            foodFromMap(map["id"] as? String ?: return@mapNotNull null, map)
        }
        return Catalog(version = (root["version"] as? Number)?.toLong() ?: 0L, foods = foods)
    }

    /** Reads only the version number (the field is written first) without parsing 300 KB. */
    fun peekVersion(head: String): Long = versionRegex.find(head)?.groupValues?.get(1)?.toLongOrNull() ?: 0L

    /** Firestore / JSON map → [Food]; returns null for documents missing required fields. */
    fun foodFromMap(id: String, map: Map<*, *>): Food? = runCatching {
        val names = stringMap(map["names"])
        Food(
            id = id,
            name = names["en"] ?: return null,
            kcal = (map["kcal"] as? Number)?.toInt() ?: return null,
            role = FoodRole.valueOf(map["role"] as? String ?: return null),
            mealTypes = strings(map["mealTypes"]).mapNotNull { runCatching { MealType.valueOf(it) }.getOrNull() }.toImmutableSet(),
            tags = strings(map["tags"]).mapNotNull { runCatching { FoodTag.valueOf(it) }.getOrNull() }.toImmutableSet(),
            imageKey = map["imageKey"] as? String ?: "",
            names = names,
            cost = ((map["cost"] as? Number)?.toInt() ?: 2).coerceIn(1, 3),
            emoji = map["emoji"] as? String ?: "",
            proteinG = (map["protein"] as? Number)?.toDouble() ?: 0.0,
            carbsG = (map["carbs"] as? Number)?.toDouble() ?: 0.0,
            fatG = (map["fat"] as? Number)?.toDouble() ?: 0.0,
            fiberG = (map["fiber"] as? Number)?.toDouble() ?: 0.0,
            glycemic = runCatching { Glycemic.valueOf(map["gi"] as String) }.getOrDefault(Glycemic.Medium),
            ingredients = ingredients(map["ingredients"]),
            imageUrl = (map["imageUrl"] as? String)?.takeIf { it.isNotBlank() },
            regions = strings(map["regions"]).toImmutableSet(),
            active = map["active"] as? Boolean ?: true,
        )
    }.getOrNull()

    fun ingredients(raw: Any?): ImmutableList<FoodIngredient> = (raw as? List<*>).orEmpty().mapNotNull { item ->
        val m = item as? Map<*, *> ?: return@mapNotNull null
        FoodIngredient(
            id = m["id"] as? String ?: return@mapNotNull null,
            grams = (m["grams"] as? Number)?.toInt() ?: 0,
            category = IngredientCategory.fromKey(m["category"] as? String ?: ""),
            names = stringMap(m["names"]),
        )
    }.toImmutableList()

    // ---------------------------------------------------------------- Room column helpers

    fun parseIngredients(text: String): ImmutableList<FoodIngredient> =
        if (text.isBlank() || text == "[]") persistentListOf()
        else runCatching { ingredients(json.parseToJsonElement(text).toPlain()) }.getOrDefault(persistentListOf())

    fun ingredientsToJson(list: List<FoodIngredient>): String = JsonArray(
        list.map { i ->
            JsonObject(
                mapOf(
                    "id" to JsonPrimitive(i.id),
                    "grams" to JsonPrimitive(i.grams),
                    "category" to JsonPrimitive(i.category.name),
                    "names" to JsonObject(i.names.mapValues { JsonPrimitive(it.value) }),
                ),
            )
        },
    ).toString()

    fun parseStringMap(text: String): ImmutableMap<String, String> =
        runCatching { stringMap(json.parseToJsonElement(text).toPlain()) }.getOrDefault(persistentMapOf())

    fun stringMapToJson(map: Map<String, String>): String = JsonObject(map.mapValues { JsonPrimitive(it.value) }).toString()

    // ---------------------------------------------------------------- plumbing

    private fun stringMap(raw: Any?): ImmutableMap<String, String> =
        (raw as? Map<*, *>).orEmpty().entries.mapNotNull { (k, v) -> (k as? String)?.let { key -> (v as? String)?.let { key to it } } }
            .toMap().toImmutableMap()

    private fun strings(raw: Any?): List<String> = (raw as? List<*>).orEmpty().filterIsInstance<String>()

    private fun JsonElement.toPlain(): Any? = when (this) {
        is JsonNull -> null
        is JsonObject -> mapValues { it.value.toPlain() }
        is JsonArray -> map { it.toPlain() }
        is JsonPrimitive -> if (isString) content else booleanOrNull ?: longOrNull ?: doubleOrNull
    }
}
