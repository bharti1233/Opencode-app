package app.opencode.session

import java.util.UUID

// SessionStore backed by Room: sessions survive app close/reopen (Phase 9
// acceptance). Compaction/pruning of old tool outputs lands with Phase 9's
// follow-up once token accounting exists.

class RoomSessionStore(private val dao: SessionDao) : SessionStore {
    override fun create(directory: String): Session {
        val s = Session("ses_" + UUID.randomUUID().toString().take(8), directory, directory)
        dao.upsertSession(
            SessionEntity(s.id, s.projectId, s.directory, s.title, s.agent, s.model, System.currentTimeMillis()),
        )
        return s
    }

    override fun get(id: String): Session? {
        val e = dao.session(id) ?: return null
        return Session(e.id, e.projectId, e.directory, e.title, e.agent, e.model)
    }

    fun list(): List<Session> = dao.sessions().map {
        Session(it.id, it.projectId, it.directory, it.title, it.agent, it.model)
    }

    override fun messages(sessionId: String): List<Message> =
        dao.messages(sessionId).map { me -> SessionTables.fromRows(me, dao.parts(me.id)) }

    override fun append(message: Message) {
        val (me, ps) = SessionTables.toRows(message, dao.messageCount(message.sessionId))
        dao.appendMessage(me, ps)
        touch(message.sessionId)
    }

    fun rename(id: String, title: String) {
        dao.session(id)?.let { dao.upsertSession(it.copy(title = title, updatedAt = System.currentTimeMillis())) }
    }

    private fun touch(id: String) {
        dao.session(id)?.let { dao.upsertSession(it.copy(updatedAt = System.currentTimeMillis())) }
    }
}
