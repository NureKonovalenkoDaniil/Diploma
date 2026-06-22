package com.example.medicationmanagement.ui

import android.content.Context
import com.example.medicationmanagement.api.IoTDeviceApi
import com.example.medicationmanagement.api.RetrofitClient
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
class AddDeviceViewModelTest {

    private lateinit var mockIoTDeviceApi: IoTDeviceApi
    private lateinit var viewModel: AddDeviceViewModel
    private val testDispatcher = UnconfinedTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        mockIoTDeviceApi = mock()

        RetrofitClient.registerMockApi(IoTDeviceApi::class.java, mockIoTDeviceApi)

        val context = mock<Context>()
        viewModel = AddDeviceViewModel(context)
    }

    @After
    fun tearDown() {
        RetrofitClient.clearMockApis()
        Dispatchers.resetMain()
    }

    @Test
    fun addDevice_success() = runTest {
        whenever(mockIoTDeviceApi.createDevice(any())).thenReturn(Response.success(mock()))

        viewModel.addDevice("DEV-100", "Warehouse A", "Thermometer")

        assertTrue(viewModel.success.value)
        assertFalse(viewModel.isLoading.value)
        assertNull(viewModel.error.value)
    }

    @Test
    fun addDevice_failure() = runTest {
        whenever(mockIoTDeviceApi.createDevice(any())).thenReturn(Response.error(400, "".toResponseBody(null)))

        viewModel.addDevice("DEV-100", "Warehouse A", "Thermometer")

        assertFalse(viewModel.success.value)
        assertFalse(viewModel.isLoading.value)
        assertNotNull(viewModel.error.value)
        assertEquals("Пристрій вже прив'язаний або некоректні дані", viewModel.error.value)
    }

    @Test
    fun addDevice_networkError() = runTest {
        whenever(mockIoTDeviceApi.createDevice(any())).thenThrow(RuntimeException("Network Error"))

        viewModel.addDevice("DEV-100", "Warehouse A", "Thermometer")

        assertFalse(viewModel.success.value)
        assertFalse(viewModel.isLoading.value)
        assertNotNull(viewModel.error.value)
        assertEquals("Network Error", viewModel.error.value)
    }
}
