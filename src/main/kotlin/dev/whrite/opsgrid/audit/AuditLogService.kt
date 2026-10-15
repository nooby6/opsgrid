package dev.whrite.opsgrid.audit

import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class AuditLogService(
    private val jdbcTemplate: JdbcTemplate,
    private val objectMapper: ObjectMapper
) {
    @Transactional(propagation = Propagation.MANDATORY)
    fun record(
        actorId: String,
        action: String,
        entityType: String,
        entityId: UUID,
        beforeState: Any?,
        afterState: Any?
    ) {
        val actor = actorId.trim()
        require(actor.isNotEmpty()) { "X-Actor-Id must not be blank" }
        require(actor.length <= MAX_ACTOR_ID_LENGTH) {
            "X-Actor-Id must be at most $MAX_ACTOR_ID_LENGTH characters"
        }

        jdbcTemplate.update(
            """
            INSERT INTO audit_events (
                id, actor_id, action, entity_type, entity_id,
                occurred_at, before_state, after_state
            ) VALUES (?, ?, ?, ?, ?, CURRENT_TIMESTAMP, ?::jsonb, ?::jsonb)
            """.trimIndent(),
            UUID.randomUUID(),
            actor,
            action,
            entityType,
            entityId,
            beforeState?.let(objectMapper::writeValueAsString),
            afterState?.let(objectMapper::writeValueAsString)
        )
    }

    private companion object {
        const val MAX_ACTOR_ID_LENGTH = 120
    }
}
