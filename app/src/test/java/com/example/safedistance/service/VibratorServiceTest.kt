package com.example.safedistance.service

import android.content.Context
import android.content.Intent
import junit.framework.TestCase.assertEquals
import org.junit.Before
import org.junit.Test


import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.mock
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class VibratorServiceTest {
    private lateinit var vibratorService: VibratorService
    @Mock
    private lateinit var context: Context

    @Before
    fun setUp() {
        vibratorService = Robolectric.buildService(VibratorService::class.java).get()
        context = mock()
    }

    @Test
    fun `should set isScreenOn to false when ACTION_SCREEN_OFF is received`() {
        // given
        vibratorService.onCreate()
        val intent = Intent(Intent.ACTION_SCREEN_OFF)

        // when
        vibratorService.screenReceiver.onReceive(context, intent)

        // then
        assertEquals(false, vibratorService.isScreenOn)
    }

    @Test
    fun `should set isScreenOn to true when ACTION_USER_PRESENT is received`() {
        // given
        vibratorService.onCreate()
        val intent = Intent(Intent.ACTION_USER_PRESENT)

        // when
        vibratorService.screenReceiver.onReceive(context, intent)

        // then
        assertEquals(true, vibratorService.isScreenOn)
    }
}