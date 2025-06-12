package it.dellapp

import it.dellapp.models.Bookings
import it.dellapp.models.Fields
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.transactions.transaction
import java.net.URI

object DatabaseFactory {
    fun init() {
        val envUrl = System.getenv("DATABASE_URL")
        if (envUrl != null && envUrl.startsWith("postgres")) {
            val uri = URI(envUrl)
            val userInfo = uri.userInfo.split(':')
            val username = userInfo[0]
            val password = userInfo[1]
            val host = uri.host
            val port = if (uri.port != -1) uri.port else 5432
            val db = uri.path.drop(1) // remove leading '/'
            val query = uri.query?.let { "?$it" } ?: ""
            val jdbcUrl = "jdbc:postgresql://$host:$port/$db$query"
            Database.connect(
                url = jdbcUrl,
                driver = "org.postgresql.Driver",
                user = username,
                password = password
            )
        } else {
            Database.connect("jdbc:h2:mem:test;DB_CLOSE_DELAY=-1;", driver = "org.h2.Driver")
        }
        transaction {
            SchemaUtils.create(Fields, Bookings)
            // Popola solo se usi H2 in memoria
            if (envUrl == null) {
                Fields.insert {
                    it[name] = "Campo Calcetto"
                    it[location] = "Via per Calcata, 5, Mazzano Romano"
                }
            }
        }
    }
}