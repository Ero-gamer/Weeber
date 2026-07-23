package org.koitharu.kotatsu.core.db.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Migration 32 → 33: query-optimisation indexes.
 *
 * Adds indexes on the columns most frequently used in WHERE / ORDER BY clauses
 * for library, history, tags and stats queries. All statements use
 * CREATE INDEX IF NOT EXISTS, so re-running on a database that already has an
 * index from a manual VACUUM or a prior test is safe.
 *
 * Also drops two legacy DESC composite indexes that may exist in databases
 * from an earlier optimisation attempt, if present.
 *
 * Index names and column lists match Room's generated v33 schema exactly, so
 * Room's schema hash will stay consistent after the migration.
 */
class Migration32To33 : Migration(32, 33) {

    override fun migrate(db: SupportSQLiteDatabase) {
        // tags: fast lookup/sort by title
        db.execSQL("CREATE INDEX IF NOT EXISTS index_tags_title ON tags(title)")

        // favourites: library list filtered/sorted by soft-delete and insertion time
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS index_favourites_deleted_at_created_at " +
                "ON favourites(deleted_at, created_at)"
        )

        // scrobblings: join from manga side
        db.execSQL("CREATE INDEX IF NOT EXISTS index_scrobblings_manga_id ON scrobblings(manga_id)")

        // local_index: join from manga side
        db.execSQL("CREATE INDEX IF NOT EXISTS index_local_index_manga_id ON local_index(manga_id)")

        // track_logs: feed sorted by insertion time
        db.execSQL("CREATE INDEX IF NOT EXISTS index_track_logs_created_at ON track_logs(created_at)")

        // stats: per-manga aggregation and time-range queries
        db.execSQL("CREATE INDEX IF NOT EXISTS index_stats_manga_id ON stats(manga_id)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_stats_started_at ON stats(started_at)")

        // source_health: sort sources by failure count
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS index_source_health_consecutive_failures " +
                "ON source_health(consecutive_failures)"
        )

        // Drop legacy indexes if they exist (from earlier optimisation attempt)
        db.execSQL("DROP INDEX IF EXISTS index_history_deleted_updated")
        db.execSQL("DROP INDEX IF EXISTS index_favourites_deleted_created")
    }
}
