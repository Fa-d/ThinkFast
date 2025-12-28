package dev.sadakat.thinkfast.data.seed.generator

import dev.sadakat.thinkfast.data.local.database.ThinkFastDatabase
import dev.sadakat.thinkfast.data.seed.config.PersonaConfig

/**
 * Interface for all seed generators.
 * Each persona implements this to generate appropriate seed data.
 */
interface SeedGenerator {
    /**
     * Seeds the database with persona-specific data.
     * Called on database onCreate (first install only).
     */
    suspend fun seedDatabase(database: ThinkFastDatabase)

    /**
     * Returns the persona configuration used by this generator.
     */
    fun getPersonaConfig(): PersonaConfig
}
