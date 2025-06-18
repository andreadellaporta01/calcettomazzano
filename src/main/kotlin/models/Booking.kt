package it.dellapp.models

import kotlinx.serialization.Serializable
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.date
import org.jetbrains.exposed.sql.javatime.time

@Serializable
data class BookingRequest(
    val fieldId: Int,
    val date: String,
    val startTime: String,
    val endTime: String,
    val firstName: String,
    val lastName: String,
    val code: String,
    val email: String? = null,
)

@Serializable
data class BookingResponse(
    val id: Int,
    val fieldId: Int,
    val date: String,
    val startTime: String,
    val endTime: String,
    val firstName: String,
    val lastName: String,
    val code: String,
    val email: String?,
)

@Serializable
data class FreeSlot(
    val startTime: String,
    val endTime: String
)

@Serializable
data class BookingUpdateRequest(
    val fieldId: Int,
    val date: String,
    val startTime: String,
    val endTime: String,
    val firstName: String? = null,
    val lastName: String? = null,
    val code: String? = null,
    val email: String? = null,
)

object Bookings : Table() {
    val id = integer("id").autoIncrement()
    val fieldId = integer("field_id")
    val date = date("date")
    val startTime = time("start_time") // ora di inizio slot (tipo LocalTime)
    val endTime = time("end_time")     // ora di fine slot (tipo LocalTime)
    val firstName = varchar("first_name", 40)
    val lastName = varchar("last_name", 40)
    val code = varchar("code", 4)
    val email = varchar("email", 100).nullable()
    override val primaryKey = PrimaryKey(id)
}