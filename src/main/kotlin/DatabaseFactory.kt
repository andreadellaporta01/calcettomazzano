package it.dellapp

import it.dellapp.models.Bookings
import it.dellapp.models.Fields
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.transactions.transaction

object DatabaseFactory {
    fun init() {
        val dbUrl = System.getenv("DATABASE_URL")
            ?: "jdbc:h2:mem:test;DB_CLOSE_DELAY=-1;" // fallback per sviluppo locale

        val driver = if (dbUrl.startsWith("jdbc:postgresql") || dbUrl.startsWith("postgresql")) {
            "org.postgresql.Driver"
        } else {
            "org.h2.Driver"
        }
        // Render/Neon forniscono una stringa tipo postgresql://user:pass@host:port/db?sslmode=require
        // Exposed vuole jdbc:postgresql://user:pass@host:port/db?sslmode=require
        val jdbcUrl = if (dbUrl.startsWith("postgresql://")) {
            "jdbc:" + dbUrl
                .replaceFirst("postgresql://", "postgresql://") // già ok
        } else dbUrl

        Database.connect(jdbcUrl, driver = driver)
        transaction {
            SchemaUtils.create(Fields, Bookings)
            // Popola solo se usi H2 in memoria (evita duplicati su postgres!)
            if (driver == "org.h2.Driver") {
                Fields.insert {
                    it[name] = "Campo Calcetto"
                    it[location] = "Via per Calcata, 5, Mazzano Romano"
                }
            }
        }
    }
}