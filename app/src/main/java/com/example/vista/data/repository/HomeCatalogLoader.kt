package com.example.vista.data.repository

import com.example.vista.data.local.catalog.CatalogSnapshotCodec
import com.example.vista.data.local.catalog.CatalogSnapshotStore
import com.example.vista.data.local.catalog.SavedCatalogRow
import com.example.vista.domain.model.*
import com.example.vista.domain.repository.CatalogRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.supervisorScope
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withTimeout
import java.io.IOException
import java.net.SocketTimeoutException
import javax.inject.Inject
import javax.inject.Singleton

data class HomeCatalogLoad(
    val sections: List<HomeSection>,
    val refreshing: Boolean,
    val error: String? = null
)

/** Emits a usable snapshot first, then merges independent refresh results without erasing failed rows. */
@Singleton
class HomeCatalogLoader(
    private val repository: CatalogRepository,
    private val snapshots: CatalogSnapshotStore,
    private val now: () -> Long
) {
    @Inject constructor(repository: CatalogRepository, snapshots: CatalogSnapshotStore) :
        this(repository, snapshots, System::currentTimeMillis)

    private val refreshMutex = Mutex()

    fun load(previous: List<HomeSection> = emptyList()): Flow<HomeCatalogLoad> = flow {
        refreshMutex.withLock {
            val cached = try { snapshots.read() }
            catch (e: CancellationException) { throw e }
            catch (_: Exception) { emptyList() }
            val prior = cached.filter { CatalogSnapshotCodec.usable(it.savedAt, now()) }
                .associateBy { it.section }.toMutableMap()
            // A failed disk write must not prevent keeping an already useful in-memory row.
            previous.forEach { row ->
                val section = CatalogSection.fromId(row.id)
                val savedAt = row.catalogSavedAt
                if (section != null && savedAt != null && row.items.isNotEmpty() && CatalogSnapshotCodec.usable(savedAt, now())) {
                    val existing = prior[section]
                    if (existing == null || existing.savedAt < savedAt) prior[section] = SavedCatalogRow(section, savedAt, row.items.take(20))
                }
            }
            if (prior.isNotEmpty()) emit(HomeCatalogLoad(buildSections(prior.values.toList(), prior.keys), refreshing = true))

            val permits = Semaphore(4)
            val results = supervisorScope {
                CatalogSection.entries.map { section -> async {
                    val result = try {
                        Result.success(withTimeout(25_000) { permits.withPermit { repository.getSection(section, 1) } })
                    } catch (_: TimeoutCancellationException) {
                        Result.failure(SocketTimeoutException("Catalog row deadline exceeded"))
                    } catch (e: CancellationException) { throw e }
                    catch (e: Exception) { Result.failure<List<MediaItem>>(e) }
                    section to result
                } }.associate { it.await() }
            }
            val retained = mutableSetOf<CatalogSection>()
            val refreshed = CatalogSection.entries.mapNotNull { section ->
                val result = results.getValue(section)
                if (result.isSuccess) {
                    // A confirmed empty response removes old data; only failures retain it.
                    result.getOrThrow().takeIf { it.isNotEmpty() }?.let { SavedCatalogRow(section, now(), it.take(20)) }
                } else prior[section]?.also { retained += section }
            }
            val failures = results.values.mapNotNull { it.exceptionOrNull() }
            var message = when {
                failures.isEmpty() && refreshed.isNotEmpty() -> null
                failures.isEmpty() -> "No titles are available right now. Please try again."
                refreshed.isNotEmpty() -> "Some collections couldn't refresh. Saved titles are shown where available."
                else -> CatalogLoadException.from(failures.first() as? Exception ?: IOException()).message
            }
            try { if (results.values.any { it.isSuccess }) snapshots.write(refreshed) }
            catch (e: CancellationException) { throw e }
            catch (_: Exception) { if (message == null) message = "Titles loaded, but couldn't be saved for a later visit." }
            emit(HomeCatalogLoad(buildSections(refreshed, retained), refreshing = false, error = message))
        }
    }

    private fun buildSections(rows: List<SavedCatalogRow>, saved: Set<CatalogSection>): List<HomeSection> {
        val ordered = rows.sortedBy { it.section.ordinal }.map { it.toHomeSection(it.section in saved) }
        val trending = ordered.firstOrNull { it.id == CatalogSection.TRENDING_MOVIES.id }
        val hero = trending?.copy(id = CatalogSection.FEATURED_ID, title = "Featured", type = HomeSectionType.FEATURED_CAROUSEL, items = trending.items.take(5))
        return listOfNotNull(hero) + ordered
    }
}
