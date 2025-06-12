package it.dellapp.models

import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.date
import org.jetbrains.exposed.sql.javatime.time
import java.time.LocalDate
import java.time.LocalTime

@Serializable
data class BookingRequest(
    val fieldId: Int,
    @Contextual val date: LocalDate,
    @Contextual val startTime: LocalTime,
    @Contextual val endTime: LocalTime,
    val firstName: String,
    val lastName: String,
    val phone: String,
    val email: String? = null,
    val notes: String? = null
)

@Serializable
data class BookingResponse(
    val id: Int,
    val fieldId: Int,
    @Contextual val date: LocalDate,
    @Contextual val startTime: LocalTime,
    @Contextual val endTime: LocalTime,
    val firstName: String,
    val lastName: String,
    val phone: String,
    val email: String?,
    val notes: String?
)

@Serializable
data class FreeSlot(
    @Contextual val startTime: LocalTime,
    @Contextual val endTime: LocalTime
)

@Serializable
data class BookingUpdateRequest(
    val fieldId: Int,
    @Contextual val date: LocalDate,
    @Contextual val startTime: LocalTime,
    @Contextual val endTime: LocalTime,
    val firstName: String? = null,
    val lastName: String? = null,
    val phone: String? = null,
    val email: String? = null,
    val notes: String? = null
)

object Bookings : Table() {
    val id = integer("id").autoIncrement()
    val fieldId = integer("field_id")
    val date = date("date")
    val startTime = time("start_time") // ora di inizio slot (tipo LocalTime)
    val endTime = time("end_time")     // ora di fine slot (tipo LocalTime)
    val firstName = varchar("first_name", 40)
    val lastName = varchar("last_name", 40)
    val phone = varchar("phone", 20)
    val email = varchar("email", 100).nullable()
    val notes = varchar("notes", 255).nullable()
    override val primaryKey = PrimaryKey(id)
}