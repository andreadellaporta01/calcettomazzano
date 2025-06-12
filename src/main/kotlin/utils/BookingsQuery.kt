package it.dellapp.utils

import it.dellapp.models.Bookings
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction

fun getBookingsByFieldId(fieldIdValue: Int): List<ResultRow> = transaction {
    Bookings.selectAll().where { Bookings.fieldId eq fieldIdValue }.toList()
}