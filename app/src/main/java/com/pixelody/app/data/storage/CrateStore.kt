package com.pixelody.app.data.storage

import android.content.Context
import android.content.SharedPreferences
import com.pixelody.app.data.model.CrateBook
import com.pixelody.app.data.model.CrateCodec

/**
 * Crates are the one thing in this app the person arranged themselves, so losing
 * them is not the same class of accident as losing a view preference. They persist
 * to their own preferences file, separate from settings, and a book that fails to
 * decode returns empty rather than throwing — a crash on launch because one
 * character went wrong in a string is a worse outcome than a Home screen that asks
 * to be set up again.
 */
class CrateStore(private val prefs: SharedPreferences) {

    constructor(context: Context) : this(
        context.getSharedPreferences("pixelody_crates", Context.MODE_PRIVATE)
    )

    fun load(): CrateBook = CrateCodec.decodeBook(prefs.getString(KEY_BOOK, null))

    fun save(book: CrateBook) {
        prefs.edit().putString(KEY_BOOK, CrateCodec.encodeBook(book)).apply()
    }

    fun hasBook(): Boolean = !prefs.getString(KEY_BOOK, null).isNullOrBlank()

    fun clear() {
        prefs.edit().remove(KEY_BOOK).apply()
    }

    companion object {
        const val KEY_BOOK = "crate_book"
    }
}
