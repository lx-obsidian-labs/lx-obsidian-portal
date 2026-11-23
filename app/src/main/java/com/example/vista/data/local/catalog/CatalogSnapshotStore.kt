package com.example.vista.data.local.catalog

import android.content.Context
import android.util.AtomicFile
import com.example.vista.domain.model.CatalogSection
import com.example.vista.domain.model.HomeSection
import com.example.vista.domain.model.MediaItem
import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

data class SavedCatalogRow(val section: CatalogSection, val savedAt: Long, val items: List<MediaItem>) {
    fun toHomeSection(saved: Boolean) = HomeSection(section.id, section.title, section.type,
        items, isSavedCatalog = saved, catalogSavedAt = savedAt)
}

interface CatalogSnapshotStore {
    suspend fun read(): List<SavedCatalogRow>
    suspend fun write(rows: List<SavedCatalogRow>)
}

/** Versioned, typed public metadata only. Never serializes history, accounts or playback URLs. */
object CatalogSnapshotCodec {
    const val MAX_BYTES = 1_048_576
    const val MAX_AGE_MS = 7L * 24 * 60 * 60 * 1000
    private val gson = Gson()

    fun usable(savedAt: Long, now: Long) = savedAt > 0 && savedAt <= now && now - savedAt <= MAX_AGE_MS

    fun encode(rows: List<SavedCatalogRow>): String {
        val array = JsonArray()
        rows.distinctBy { it.section }.take(CatalogSection.entries.size).forEach { row ->
            val items = JsonArray()
            row.items.take(20).forEach { item ->
                val value = when (item) {
                    is MediaItem.MovieItem -> gson.toJsonTree(item).asJsonObject.apply { addProperty("kind", "movie") }
                    is MediaItem.SeriesItem -> gson.toJsonTree(item).asJsonObject.apply { addProperty("kind", "series") }
                    else -> null
                }
                value?.let(items::add)
            }
            if (items.size() > 0) array.add(JsonObject().apply {
                addProperty("section", row.section.id)
                addProperty("source", "vista-public-catalog")
                addProperty("savedAt", row.savedAt)
                add("items", items)
            })
        }
        val json = JsonObject().apply { addProperty("schema", 1); add("rows", array) }.toString()
        require(json.toByteArray(Charsets.UTF_8).size <= MAX_BYTES) { "Catalog snapshot exceeds its size limit." }
        return json
    }

    fun decode(json: String, now: Long): List<SavedCatalogRow> = try {
        if (json.toByteArray(Charsets.UTF_8).size > MAX_BYTES) emptyList() else {
            val root = JsonParser.parseString(json).asJsonObject
            if (root.get("schema")?.asInt != 1) emptyList() else {
                root.getAsJsonArray("rows").take(CatalogSection.entries.size).mapNotNull { element ->
                    val row = element.asJsonObject
                    val section = CatalogSection.fromId(row.get("section").asString) ?: return@mapNotNull null
                    val savedAt = row.get("savedAt").asLong
                    if (row.get("source")?.asString != "vista-public-catalog" || !usable(savedAt, now)) return@mapNotNull null
                    val items = row.getAsJsonArray("items").take(20).mapNotNull { value ->
                        val item: MediaItem? = when (value.asJsonObject.get("kind")?.asString) {
                            "movie" -> gson.fromJson(value, MediaItem.MovieItem::class.java)
                            "series" -> gson.fromJson(value, MediaItem.SeriesItem::class.java)
                            else -> null
                        }
                        item?.takeIf { !it.id.isNullOrBlank() && !it.title.isNullOrBlank() }
                    }
                    items.takeIf { it.isNotEmpty() }?.let { SavedCatalogRow(section, savedAt, it) }
                }.distinctBy { it.section }
            }
        }
    } catch (_: Exception) { emptyList() }
}

/** Atomic, bounded snapshot isolated from user databases, cloud backup and player-cache repair. */
@Singleton
class DiskCatalogSnapshotStore @Inject constructor(@ApplicationContext context: Context) : CatalogSnapshotStore {
    private val directory = File(context.noBackupFilesDir, "public_catalog")
    private val file = AtomicFile(File(directory, "home-v1.json"))
    private val mutex = Mutex()

    override suspend fun read(): List<SavedCatalogRow> = withContext(Dispatchers.IO) {
        mutex.withLock {
            try {
                file.openRead().use { input ->
                    if (input.channel.size() > CatalogSnapshotCodec.MAX_BYTES) emptyList()
                    else CatalogSnapshotCodec.decode(input.bufferedReader(Charsets.UTF_8).readText(), System.currentTimeMillis())
                }
            } catch (_: IOException) { emptyList() }
        }
    }

    override suspend fun write(rows: List<SavedCatalogRow>) = withContext(Dispatchers.IO) {
        val encoded = CatalogSnapshotCodec.encode(rows).toByteArray(Charsets.UTF_8)
        mutex.withLock {
            if (!directory.isDirectory && !directory.mkdirs()) throw IOException("Catalog snapshot storage unavailable.")
            val output = file.startWrite()
            try {
                output.write(encoded)
                file.finishWrite(output)
            } catch (failure: Exception) {
                file.failWrite(output)
                throw failure
            }
        }
    }
}
