package com.example.safedistance.utils

import android.app.NotificationManager
import android.content.Context
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class NotificationHelperTest() {
    @Mock
    private lateinit var context: Context
    @Mock
    private lateinit var notificationManager: NotificationManager

    @Before
    fun setUp() {
        this.context = mock()
        this.notificationManager = mock()
    }

    @Test
    fun `should create instance of class via static factory pattern`() {
        `when`(context.getSystemService(Context.NOTIFICATION_SERVICE)).thenReturn(notificationManager)

        val notificationHelper: NotificationHelper = NotificationHelper
            .create(context, "nameOfChannel", "descriptionOfChannel")

        assertNotNull(notificationHelper)
    }
}
