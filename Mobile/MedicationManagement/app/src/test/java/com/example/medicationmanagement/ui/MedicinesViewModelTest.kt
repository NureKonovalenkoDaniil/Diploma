package com.example.medicationmanagement.ui

import android.content.Context
import com.example.medicationmanagement.api.MedicineApi
import com.example.medicationmanagement.api.MedicineActionsApi
import com.example.medicationmanagement.api.RetrofitClient
import com.example.medicationmanagement.api.QuantityRequest
import com.example.medicationmanagement.model.Medicine
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
class MedicinesViewModelTest {

    private lateinit var mockMedicineApi: MedicineApi
    private lateinit var mockMedicineActionsApi: MedicineActionsApi
    private lateinit var viewModel: MedicinesViewModel
    private val testDispatcher = UnconfinedTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        mockMedicineApi = mock()
        mockMedicineActionsApi = mock()

        RetrofitClient.registerMockApi(MedicineApi::class.java, mockMedicineApi)
        RetrofitClient.registerMockApi(MedicineActionsApi::class.java, mockMedicineActionsApi)

        val context = mock<Context>()
        viewModel = MedicinesViewModel(context)
    }

    @After
    fun tearDown() {
        RetrofitClient.clearMockApis()
        Dispatchers.resetMain()
    }

    @Test
    fun fetchMedicines_success() = runTest {
        val list = listOf(Medicine(medicineID = 1, name = "Aspirin", quantity = 10))
        whenever(mockMedicineApi.getMedicines()).thenReturn(Response.success(list))

        viewModel.fetchMedicines()

        val medicines = viewModel.medicines.value
        assertEquals(1, medicines.size)
        assertEquals("Aspirin", medicines[0].name)
        assertEquals(10, medicines[0].quantity)
        assertFalse(viewModel.isLoading.value)
        assertNull(viewModel.error.value)
    }

    @Test
    fun fetchMedicines_failure() = runTest {
        whenever(mockMedicineApi.getMedicines()).thenReturn(Response.error(500, "".toResponseBody(null)))

        viewModel.fetchMedicines()

        val medicines = viewModel.medicines.value
        assertTrue(medicines.isEmpty())
        assertFalse(viewModel.isLoading.value)
        assertNotNull(viewModel.error.value)
    }

    @Test
    fun issueMedicine_success() = runTest {
        val medicineId = 1
        val quantity = 5
        whenever(mockMedicineActionsApi.issue(eq(medicineId), any())).thenReturn(Response.success(mock()))
        whenever(mockMedicineApi.getMedicines()).thenReturn(Response.success(emptyList()))

        viewModel.issueMedicine(medicineId, quantity)

        verify(mockMedicineActionsApi).issue(eq(medicineId), any())
    }

    @Test
    fun deleteMedicine_success() = runTest {
        val medicineId = 1
        whenever(mockMedicineApi.deleteMedicine(eq(medicineId))).thenReturn(Response.success(Unit))
        whenever(mockMedicineApi.getMedicines()).thenReturn(Response.success(emptyList()))

        viewModel.deleteMedicine(medicineId)

        verify(mockMedicineApi).deleteMedicine(eq(medicineId))
    }
}
