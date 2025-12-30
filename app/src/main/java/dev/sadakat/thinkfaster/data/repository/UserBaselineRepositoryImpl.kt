package dev.sadakat.thinkfaster.data.repository

import dev.sadakat.thinkfaster.data.local.database.dao.UserBaselineDao
import dev.sadakat.thinkfaster.data.mapper.toEntity
import dev.sadakat.thinkfaster.data.mapper.toDomain
import dev.sadakat.thinkfaster.domain.model.UserBaseline
import dev.sadakat.thinkfaster.domain.repository.UserBaselineRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * UserBaselineRepositoryImpl - Implementation of UserBaselineRepository
 * First-Week Retention Feature - Phase 1.5: Repository Implementation
 *
 * Provides access to user baseline data with entity↔domain mapping
 */
class UserBaselineRepositoryImpl(
    private val baselineDao: UserBaselineDao
) : UserBaselineRepository {

    override suspend fun saveBaseline(baseline: UserBaseline) {
        baselineDao.upsertBaseline(baseline.toEntity())
    }

    override suspend fun getBaseline(): UserBaseline? {
        return baselineDao.getBaseline()?.toDomain()
    }

    override fun observeBaseline(): Flow<UserBaseline?> {
        return baselineDao.observeBaseline().map { it?.toDomain() }
    }
}
