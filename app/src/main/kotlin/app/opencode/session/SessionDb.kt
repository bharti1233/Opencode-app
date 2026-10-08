package app.opencode.session

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.RoomDatabase
import androidx.room.Transaction

// Port of storage/schema.ts tables (Session/Message/Part) to Room.
// Blocking DAO (no suspend): call off the main thread; Phase 14 moves callers
// to Dispatchers.IO.

@Entity(tableName = "sessions")
data class SessionEntity(
    @PrimaryKey val id: String,
    val projectId: String,
    val directory: String,
    val title: String,
    val agent: String,
    val model: String,
    val updatedAt: Long,
)

@Entity(tableName = "messages", primaryKeys = ["id"])
data class MessageEntity(
    val id: String,
    val sessionId: String,
    val role: String,
    val seq: Int,
)

@Entity(tableName = "parts", primaryKeys = ["id"])
data class PartEntity(
    val id: String,
    val messageId: String,
    val sessionId: String,
    val kind: String,
    val text: String = "",
    val callId: String = "",
    val tool: String = "",
    val state: String = "",
    val args: String = "",
    val output: String = "",
    val error: String? = null,
)

@Dao
interface SessionDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun upsertSession(s: SessionEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertMessage(m: MessageEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertParts(p: List<PartEntity>)

    @Query("SELECT * FROM sessions WHERE id = :id")
    fun session(id: String): SessionEntity?

    @Query("SELECT * FROM sessions ORDER BY updatedAt DESC")
    fun sessions(): List<SessionEntity>

    @Query("SELECT * FROM messages WHERE sessionId = :sid ORDER BY seq")
    fun messages(sid: String): List<MessageEntity>

    @Query("SELECT * FROM parts WHERE messageId = :mid")
    fun parts(mid: String): List<PartEntity>

    @Query("SELECT COUNT(*) FROM messages WHERE sessionId = :sid")
    fun messageCount(sid: String): Int

    @Query("DELETE FROM sessions WHERE id = :id")
    fun deleteSession(id: String)

    @Transaction
    fun appendMessage(m: MessageEntity, p: List<PartEntity>) {
        insertMessage(m)
        insertParts(p)
    }
}

@Database(entities = [SessionEntity::class, MessageEntity::class, PartEntity::class], version = 1)
abstract class SessionDatabase : RoomDatabase() {
    abstract fun dao(): SessionDao
}

/** Pure mapping (JVM-testable without a database). */
object SessionTables {
    fun toRows(m: Message, seq: Int): Pair<MessageEntity, List<PartEntity>> {
        val me = MessageEntity(m.id, m.sessionId, m.role, seq)
        val ps = m.parts.mapIndexed { i, p ->
            val base = PartEntity("$i:${m.id}", m.id, m.sessionId, kindOf(p))
            when (p) {
                is Part.Text -> base.copy(text = p.text)
                is Part.Reasoning -> base.copy(text = p.text)
                is Part.ToolCall -> base.copy(callId = p.callId, tool = p.tool, state = p.state, args = p.args)
                is Part.ToolResult -> base.copy(callId = p.callId, output = p.output, error = p.error)
            }
        }
        return me to ps
    }

    fun fromRows(me: MessageEntity, ps: List<PartEntity>): Message =
        Message(me.id, me.sessionId, me.role, ps.map {
            when (it.kind) {
                "tool_call" -> Part.ToolCall(it.callId, it.tool, it.state, it.args)
                "tool_result" -> Part.ToolResult(it.callId, it.output, it.error)
                "reasoning" -> Part.Reasoning(it.text)
                else -> Part.Text(it.text)
            }
        })

    private fun kindOf(p: Part): String = when (p) {
        is Part.Text -> "text"
        is Part.Reasoning -> "reasoning"
        is Part.ToolCall -> "tool_call"
        is Part.ToolResult -> "tool_result"
    }
}
