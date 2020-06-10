/*
 * Copyright 2018 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.android.advancedcoroutines

import androidx.lifecycle.*
import com.example.android.advancedcoroutines.util.CacheOnSuccess
import com.example.android.advancedcoroutines.utils.ComparablePair
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

/**
 * Repository module for handling data operations.
 *
 * This PlantRepository exposes two UI-observable database queries [plants] and
 * [getPlantsWithGrowZone].
 *
 * To update the plants cache, call [tryUpdateRecentPlantsForGrowZoneCache] or
 * [tryUpdateRecentPlantsCache].
 */
class PlantRepository private constructor(
    private val plantDao: PlantDao,
    private val plantService: NetworkService,
    private val defaultDispatcher: CoroutineDispatcher = Dispatchers.Default
) {
    private var plantsListSortOrderCache =
        //  It will fallback to an empty list if there's a network error from customPlantSortOrder()
        CacheOnSuccess(onErrorFallback = { listOf<String>() }) {
            plantService.customPlantSortOrder()
        }

    private fun List<Plant>.applySort(customSortOrder: List<String>): List<Plant> {
        return sortedBy { plant ->
            val positionForItem = customSortOrder.indexOf(plant.plantId).let { order ->
                if (order > -1) order else Int.MAX_VALUE
            }
            ComparablePair(positionForItem, plant.name)
        }
    }

    private suspend fun List<Plant>.applyMainSafeSort(customSortOrder: List<String>) =
        withContext(defaultDispatcher) {
            this@applyMainSafeSort.applySort(customSortOrder)
        }

    private val customSortFlow = flow {
        emit(plantsListSortOrderCache.getOrAwait())
    }

    @UseExperimental(FlowPreview::class)
    private val customSortFlowSimplified =
        plantsListSortOrderCache::getOrAwait.asFlow()
        // The transform onStart will happen when an observer listens before other operators
        // and it can emit placeholder values. So here we're emitting an empty list,
        // delaying calling getOrAwait by 1500ms, then continuing the original flow.
            /*.onStart {
                emit(listOf())
                delay(3500)
            }*/

    /**
     * Fetch a list of [Plant]s from the database.
     */
//    val plants = plantDao.getPlants()

    /**
     * Replaced the code for list of [plant]s
     * Returns a LiveData-wrapped List of Plants.
     */
    val plants: LiveData<List<Plant>> = liveData {
        // Observe plants from the database (just like a normal LiveData + Room return)
        val plantsLiveData = plantDao.getPlants()
        // Fetch our custom sort from the network in a main-safe suspending call (cached)
        val customSortOrder = plantsListSortOrderCache.getOrAwait()
        // Map the LiveData, applying the sort criteria
        emitSource(plantsLiveData.map { plantList ->
            plantList.applySort(customSortOrder)
        })
    }

    @ExperimentalCoroutinesApi
    val plantsFlow: Flow<List<Plant>>
        get() = plantDao.getPlantsFlow()
            // When the result of customSortFlow is available,
            // this will combine it with the latest value from
            // the flow above.  Thus, as long as both `plants`
            // and `sortOrder` are have an initial value (their
            // flow has emitted at least one value), any change
            // to either `plants` or `sortOrder`  will call
            // `plants.applySort(sortOrder)`.

            // Both flows will run in their own coroutine
            // That means that while Room starts the network request, Retrofit can start the network query.
            // Then, as soon as a result is available for both flows,
            // it will call the combine lambda where we apply the loaded sort order to the loaded plants.
            .combine(customSortFlowSimplified) { plants, sortOrder ->
                plants.applySort(sortOrder)
            }
            // However, as our data set grows in size, the call to applySort may become slow enough to block the main thread.
            // Flow offers a declarative API called flowOn to control which thread the flow runs on.

            // The coroutine launched by flowOn is allowed to produce results faster than the caller consumes them,
            // and it will buffer a large number of them by default.
            .flowOn(defaultDispatcher)
            // make a buffer of flowOn :
            // a *buffer* to send results from the new coroutine to later calls. (sending the results to the UI)
            // it modifies the buffer of flowOn to store only the last result.
            // If another result comes in before the previous one is read, it gets overwritten.
            .conflate()


    /**
     * Fetch a list of [Plant]s from the database that matches a given [GrowZone].
     */
//    fun getPlantsWithGrowZone(growZone: GrowZone) =
//        plantDao.getPlantsWithGrowZoneNumber(growZone.number)

    /**
     * Returns a LiveData-wrapped List of Plants.
     */
    fun getPlantsWithGrowZone(growZone: GrowZone): LiveData<List<Plant>> =
        liveData {
            val plantsGrowZoneLiveData = plantDao.getPlantsWithGrowZoneNumber(growZone.number)
            val customSortOrder = plantsListSortOrderCache.getOrAwait()
            emitSource(plantsGrowZoneLiveData.map { plantList ->
                plantList.applySort(customSortOrder)
            })
        }

    fun getPlantsWithGrowZoneAdvanced(growZone: GrowZone) =
        plantDao.getPlantsWithGrowZoneNumber(growZone.number)
            .switchMap { plantList ->
                liveData {
                    val customSortOrder = plantsListSortOrderCache.getOrAwait()
                    emit(plantList.applyMainSafeSort(customSortOrder))
                }
            }

    fun getPlantsWithGrowZoneFlow(growZone: GrowZone): Flow<List<Plant>> {
        return plantDao.getPlantsWithGrowZoneNumberFlow(growZone.number)
    }

    /**
     * Returns true if we should make a network request.
     */
    private fun shouldUpdatePlantsCache(): Boolean {
        // suspending function, so you can e.g. check the status of the database here
        return true
    }

    /**
     * Update the plants cache.
     *
     * This function may decide to avoid making a network requests on every call based on a
     * cache-invalidation policy.
     */
    suspend fun tryUpdateRecentPlantsCache() {
        if (shouldUpdatePlantsCache()) fetchRecentPlants()
    }

    /**
     * Update the plants cache for a specific grow zone.
     *
     * This function may decide to avoid making a network requests on every call based on a
     * cache-invalidation policy.
     */
    private suspend fun tryUpdateRecentPlantsForGrowZoneCache(growZoneNumber: GrowZone) {
        if (shouldUpdatePlantsCache()) fetchPlantsForGrowZone(growZoneNumber)
    }

    /**
     * Fetch a new list of plants from the network, and append them to [plantDao]
     */
    private suspend fun fetchRecentPlants() {
        val plants = plantService.allPlants()
        plantDao.insertAll(plants)
    }

    /**
     * Fetch a list of plants for a grow zone from the network, and append them to [plantDao]
     */
    private suspend fun fetchPlantsForGrowZone(growZone: GrowZone) {
        val plants = plantService.plantsByGrowZone(growZone)
        plantDao.insertAll(plants)
    }

    companion object {

        // For Singleton instantiation
        @Volatile
        private var instance: PlantRepository? = null

        fun getInstance(plantDao: PlantDao, plantService: NetworkService) =
            instance ?: synchronized(this) {
                instance ?: PlantRepository(plantDao, plantService).also { instance = it }
            }
    }
}
