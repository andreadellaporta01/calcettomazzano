package it.dellapp.models

import kotlinx.serialization.Serializable
import org.jetbrains.exposed.sql.Table

@Serializable
data class FieldDTO(val id: Int, val name: String, val location: String)

object Fields : Table() {
    val id = integer("id").autoIncrement()
    val name = varchar("name", 50)
    val location = varchar("location", 100)
    override val primaryKey = PrimaryKey(id)
}