package it.dellapp.routes

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.patch
import io.ktor.server.routing.post
import io.ktor.server.routing.routing
import it.dellapp.models.BookingRequest
import it.dellapp.models.BookingResponse
import it.dellapp.models.BookingUpdateRequest
import it.dellapp.models.Bookings
import it.dellapp.models.FieldDTO
import it.dellapp.models.Fields
import it.dellapp.models.FreeSlot
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.andWhere
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update
import java.time.Duration
import java.time.LocalDate
import java.time.LocalTime

fun Application.bookingRoutes() {
    routing {
        // Lista campi
        get("/fields") {
            val fields = transaction {
                Fields.selectAll().map {
                    FieldDTO(
                        id = it[Fields.id],
                        name = it[Fields.name],
                        location = it[Fields.location]
                    )
                }
            }
            call.respond(fields)
        }

        // Prenotazioni per campo
        get("/fields/{id}/bookings") {
            val fieldIdParam = call.parameters["id"]
            val dateParam = call.request.queryParameters["date"]

            val fieldId = fieldIdParam?.toIntOrNull()
            if (fieldId == null) {
                call.respond(HttpStatusCode.BadRequest, "Parametro 'id' non valido")
                return@get
            }

            val date: LocalDate? = dateParam?.let {
                try {
                    LocalDate.parse(it)
                } catch (e: Exception) {
                    call.respond(
                        HttpStatusCode.BadRequest,
                        "Formato data non valido, usare YYYY-MM-DD"
                    )
                    return@get
                }
            }

            val bookings = withContext(Dispatchers.IO) {
                transaction {
                    val query = Bookings.selectAll().where { Bookings.fieldId eq fieldId }
                    val filtered = if (date != null) {
                        query.andWhere { Bookings.date eq date }
                    } else query

                    filtered.orderBy(Bookings.startTime to SortOrder.ASC).map {
                        BookingResponse(
                            id = it[Bookings.id],
                            fieldId = it[Bookings.fieldId],
                            date = it[Bookings.date].toString(),
                            startTime = it[Bookings.startTime].toString(),
                            endTime = it[Bookings.endTime].toString(),
                            firstName = it[Bookings.firstName],
                            lastName = it[Bookings.lastName],
                            code = it[Bookings.code],
                            email = it[Bookings.email],
                        )
                    }
                }
            }

            call.respond(bookings)
        }

        // Nuova prenotazione
        post("/bookings") {
            val req = call.receive<BookingRequest>()

            // Validazione orari
            if (req.startTime >= req.endTime) {
                call.respond(
                    HttpStatusCode.BadRequest,
                    "L'orario di fine deve essere dopo l'inizio"
                )
                return@post
            }

            val conflict = withContext(Dispatchers.IO) {
                transaction {
                    Bookings.selectAll().where {
                        (Bookings.fieldId eq req.fieldId) and
                                (Bookings.date eq LocalDate.parse(req.date)) and
                                (Bookings.startTime less LocalTime.parse(req.endTime)) and
                                (Bookings.endTime greater LocalTime.parse(req.startTime))
                    }.any()
                }
            }

            if (conflict) {
                call.respond(
                    HttpStatusCode.Conflict,
                    "Slot sovrapposto a una prenotazione esistente"
                )
            } else {
                withContext(Dispatchers.IO) {
                    transaction {
                        Bookings.insert {
                            it[fieldId] = req.fieldId
                            it[date] = LocalDate.parse(req.date)
                            it[startTime] = LocalTime.parse(req.startTime)
                            it[endTime] = LocalTime.parse(req.endTime)
                            it[firstName] = req.firstName
                            it[lastName] = req.lastName
                            it[code] = req.code
                            it[email] = req.email
                        }
                    }
                }
                call.respond(HttpStatusCode.Created, "Prenotazione effettuata!")
            }
        }

        // Cancella prenotazione
        delete("/bookings/{id}") {
            val id = call.parameters["id"]?.toIntOrNull()
            if (id == null) {
                call.respond(HttpStatusCode.BadRequest, "ID non valido")
                return@delete
            }
            transaction {
                Bookings.deleteWhere { Bookings.id eq id }
            }
            call.respond(HttpStatusCode.OK, "Prenotazione cancellata")
        }

        get("/fields/{id}/free-slots") {
            val fieldIdParam = call.parameters["id"]
            val dateParam = call.request.queryParameters["date"]

            val fieldId = fieldIdParam?.toIntOrNull()
            if (fieldId == null) {
                call.respond(HttpStatusCode.BadRequest, "Parametro 'id' non valido")
                return@get
            }

            val date = dateParam?.let {
                try {
                    LocalDate.parse(it)
                } catch (e: Exception) {
                    call.respond(
                        HttpStatusCode.BadRequest,
                        "Formato data non valido, usare YYYY-MM-DD"
                    )
                    return@get
                }
            } ?: run {
                call.respond(
                    HttpStatusCode.BadRequest,
                    "Parametro 'date' obbligatorio (formato YYYY-MM-DD)"
                )
                return@get
            }

            val bookings = withContext(Dispatchers.IO) {
                transaction {
                    Bookings.selectAll().where {
                        (Bookings.fieldId eq fieldId) and (Bookings.date eq date)
                    }
                        .orderBy(Bookings.startTime to SortOrder.ASC)
                        .map {
                            it[Bookings.startTime] to it[Bookings.endTime]
                        }
                }
            }

            val apertura = LocalTime.of(9, 0)
            val chiusura = LocalTime.of(23, 0)
            val durataSlot = Duration.ofMinutes(60)
            val step = Duration.ofMinutes(30)

            val existingIntervals = bookings.map { it.first to it.second }

            // Filtro anti-slot-passato solo per oggi
            val nowInRome = java.time.ZonedDateTime.now(java.time.ZoneId.of("Europe/Rome"))
            val oggi = nowInRome.toLocalDate()
            val orarioAdesso = nowInRome.toLocalTime()

            val slotLiberi = mutableListOf<FreeSlot>()
            var currentStart = apertura
            while (currentStart.plus(durataSlot) <= chiusura) {
                val currentEnd = currentStart.plus(durataSlot)
                // Verifica assenza sovrapposizione con le prenotazioni esistenti
                val overlaps = existingIntervals.any { (bStart, bEnd) ->
                    !(currentEnd <= bStart || currentStart >= bEnd)
                }
                // Filtro slot passati se la data richiesta è oggi
                val isFutureOrTodaySlot =
                    date != oggi || !currentStart.isBefore(orarioAdesso)

                if (!overlaps && isFutureOrTodaySlot) {
                    slotLiberi.add(FreeSlot(currentStart.toString(), currentEnd.toString()))
                }
                currentStart = currentStart.plus(step)
            }
            call.respond(slotLiberi)
        }

        patch("/bookings/{id}") {
            val bookingId = call.parameters["id"]?.toIntOrNull()
            if (bookingId == null) {
                call.respond(HttpStatusCode.BadRequest, "Parametro 'id' non valido")
                return@patch
            }

            val req = call.receive<BookingUpdateRequest>()

            if (req.startTime >= req.endTime) {
                call.respond(
                    HttpStatusCode.BadRequest,
                    "L'orario di fine deve essere dopo l'inizio"
                )
                return@patch
            }

            val conflict = withContext(Dispatchers.IO) {
                transaction {
                    Bookings.selectAll().where {
                        (Bookings.fieldId eq req.fieldId) and
                                (Bookings.date eq LocalDate.parse(req.date)) and
                                (Bookings.id neq bookingId) and // ignora la prenotazione stessa
                                (Bookings.startTime less LocalTime.parse(req.endTime)) and
                                (Bookings.endTime greater LocalTime.parse(req.startTime))
                    }.any()
                }
            }

            if (conflict) {
                call.respond(
                    HttpStatusCode.Conflict,
                    "Slot sovrapposto a una prenotazione esistente"
                )
                return@patch
            }

            val updated = withContext(Dispatchers.IO) {
                transaction {
                    Bookings.update({ Bookings.id eq bookingId }) {
                        it[fieldId] = req.fieldId
                        it[date] = LocalDate.parse(req.date)
                        it[startTime] = LocalTime.parse(req.startTime)
                        it[endTime] = LocalTime.parse(req.endTime)
                        req.firstName?.let { v -> it[firstName] = v }
                        req.lastName?.let { v -> it[lastName] = v }
                        req.code?.let { v -> it[code] = v }
                        req.email?.let { v -> it[email] = v }
                    }
                }
            }

            if (updated > 0) {
                call.respond(HttpStatusCode.OK, "Prenotazione aggiornata")
            } else {
                call.respond(HttpStatusCode.NotFound, "Prenotazione non trovata")
            }
        }

        get("/users/{email}/bookings") {
            val email = call.parameters["email"]
            if (email.isNullOrBlank()) {
                call.respond(HttpStatusCode.BadRequest, "Parametro 'email' mancante o non valido")
                return@get
            }

            val bookings = withContext(Dispatchers.IO) {
                transaction {
                    Bookings.selectAll().where { Bookings.email eq email }
                        .orderBy(
                            Bookings.date to SortOrder.ASC,
                            Bookings.startTime to SortOrder.ASC
                        )
                        .map {
                            BookingResponse(
                                id = it[Bookings.id],
                                fieldId = it[Bookings.fieldId],
                                date = it[Bookings.date].toString(),
                                startTime = it[Bookings.startTime].toString(),
                                endTime = it[Bookings.endTime].toString(),
                                firstName = it[Bookings.firstName],
                                lastName = it[Bookings.lastName],
                                code = it[Bookings.code],
                                email = it[Bookings.email],
                            )
                        }
                }
            }

            call.respond(bookings)
        }
    }
}