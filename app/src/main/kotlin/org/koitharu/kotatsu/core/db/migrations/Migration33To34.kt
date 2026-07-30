package org.koitharu.kotatsu.core.db.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Migration 33 → 34: creates manga_notes table.
 * Note: source_health table removed — tracking was too resource-intensive.
 */
class Migration33To34 : Migration(33, 34) {
	override fun migrate(db: SupportSQLiteDatabase) {
		db.execSQL(
			"""CREATE TABLE IF NOT EXISTS `manga_notes` (
				`manga_id` INTEGER NOT NULL,
				`note` TEXT NOT NULL DEFAULT '',
				`rating` REAL NOT NULL DEFAULT 0.0,
				`updated_at` INTEGER NOT NULL DEFAULT 0,
				PRIMARY KEY(`manga_id`)
			)"""
		)
	}
}
