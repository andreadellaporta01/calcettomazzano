package it.dellapp

import it.dellapp.models.Bookings
import it.dellapp.models.Fields
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.transactions.transaction

object DatabaseFactory {
    fun init() {
        Database.connect("jdbc:h2:mem:test;DB_CLOSE_DELAY=-1;", driver = "org.h2.Driver")
        transaction {
            SchemaUtils.create(Fields, Bookings)
            Fields.insert {
                it[name] = "Campo Calcetto"
                it[location] = "Via per Calcata, 5, Mazzano Romano"
            }
        }
    }
}