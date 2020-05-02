/*
 * Copyright (C) 2019 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.android.kotlincoroutines.main

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.example.android.kotlincoroutines.fakes.MainNetworkCompletableFake
import com.example.android.kotlincoroutines.fakes.MainNetworkFake
import com.example.android.kotlincoroutines.fakes.TitleDaoFake
import com.google.common.truth.Truth
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runBlockingTest
import org.junit.Rule
import org.junit.Test

class TitleRepositoryTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    @ExperimentalCoroutinesApi
    @Test
    fun whenRefreshTitleSuccess_insertsRows() = runBlockingTest {
        val titleDao = TitleDaoFake("title")
        val subject = TitleRepository(
                MainNetworkFake("OK"),
                titleDao
        )
        subject.refreshTitle()
        // use the fakes provided to check that "OK" is inserted to the database by refreshTitle.
        Truth.assertThat(titleDao.nextInsertedOrNull()).isEqualTo("OK")

        /**
          * launch starts a coroutine then immediately returns
          * since this is asynchronous code
          * this may be called *after* the test completes
          *
          * This test will sometimes fail
          * The call to launch will return immediately and
          * execute at the same time as the rest of the test case
          */
        /*GlobalScope.launch {

            subject.refreshTitle()
        }*/

        /**
         * test function returns immediately, and
         * doesn't see the results of refreshTitle
         */
    }

    @ExperimentalCoroutinesApi
    @Test(expected = TitleRefreshError::class)
    fun whenRefreshTitleTimeout_throws() = runBlockingTest {
        // TODO: Write this test
        val network = MainNetworkCompletableFake()
        val subject = TitleRepository(
                network,
                TitleDaoFake("title")
        )

        launch {
            subject.refreshTitle()
        }

        /**
         * One of the features of runBlocking test is that it won't let you leak coroutines after the test completes.
         * If there are any unfinished coroutines, like our launch coroutine,
         * at the end of the test, it will fail the test.
         */
        advanceTimeBy(5_000)
    }
}