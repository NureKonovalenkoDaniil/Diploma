package com.example.medicationmanagement.ui

import android.content.Context
import com.example.medicationmanagement.api.RetrofitClient
import com.example.medicationmanagement.api.StorageIncidentApi
import com.example.medicationmanagement.api.StorageIncidentDto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.*
import retrofit2.Response

@OptIn(ExperimentalCoroutinesApi::class)
class StorageIncidentsViewModelTest {

    private lateinit var mockStorageIncidentApi: StorageIncidentApi
    private lateinit var viewModel: StorageIncidentsViewModel
    private val testDispatcher = UnconfinedTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        mockStorageIncidentApi = mock()

        RetrofitClient.registerMockApi(StorageIncidentApi::class.java, mockStorageIncidentApi)

        val context = mock<Context>()
        viewModel = StorageIncidentsViewModel(context)
    }

    @After
    fun tearDown() {
        RetrofitClient.clearMockApis()
        Dispatchers.resetMain()
    }

    @Test
    fun fetchIncidents_success() = runTest {
        val list = listOf(
            StorageIncidentDto(
                id = 1,
                storageLocationId = 2,
                incidentType = "TemperatureSpike",
                severity = "Critical",
                description = "High temp",
                detectedAt = "2026-06-16",
                resolvedAt = null,
                isResolved = false
            )
        )
        whenever(mockStorageIncidentApi.getAll()).thenReturn(Response.success(list))

        viewModel.fetchIncidents()

        val incidents = viewModel.incidents.value
        assertEquals(1, incidents.size)
        assertEquals("TemperatureSpike", incidents[0].incidentType)
        assertEquals("Critical", incidents[0].severity)
        assertFalse(incidents[0].isResolved)
        assertFalse(viewModel.isLoading.value)
        assertNull(viewModel.error.value)
    }

    @Test
    fun fetchIncidents_failure() = runTest {
        whenever(mockStorageIncidentApi.getAll()).thenReturn(Response.error(401, "".toResponseBody(null)))

        viewModel.fetchIncidents()

        val incidents = viewModel.incidents.value
        assertTrue(incidents.isEmpty())
        assertFalse(viewModel.isLoading.value)
        assertNotNull(viewModel.error.value)
    }

    @Test
    fun resolveIncident_success() = runTest {
        whenever(mockStorageIncidentApi.resolve(eq(1), any())).thenReturn(Response.success(mock()))
        whenever(mockStorageIncidentApi.getAll()).thenReturn(Response.success(emptyList()))

        viewModel.resolveIncident(1, "Resolved comment")

        verify(mockStorageIncidentApi).resolve(eq(1), any())
    }
}
