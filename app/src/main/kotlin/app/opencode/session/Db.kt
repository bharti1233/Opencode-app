package app.opencode.session

import android.content.Context
import androidx.room.Room

// Single database handle. allowMainThreadQueries: v0.1 pragmatism — all DAO
// calls are small metadata reads/writes; move to Dispatchers.IO with suspend
// DAO before any heavy use.

fun openDb(context: Context): SessionDatabase =
    Room.databaseBuilder(context, SessionDatabase::class.java, "opencode.db")
        .allowMainThreadQueries()
        .build()
