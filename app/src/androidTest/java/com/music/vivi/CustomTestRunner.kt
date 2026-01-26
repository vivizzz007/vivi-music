package com.music.vivi

import android.app.Application
import android.content.Context
import androidx.test.runner.AndroidJUnitRunner
import dagger.hilt.android.testing.HiltTestApplication

/**
 * A custom [AndroidJUnitRunner] used for Hilt dependency injection in tests.
 *
 * Hilt requires a custom Application class ([HiltTestApplication]) to be used during tests
 * instead of the real app class, to support replacing modules with test doubles.
 */
class CustomTestRunner : AndroidJUnitRunner() {
    override fun newApplication(cl: ClassLoader?, className: String?, context: Context?): Application {
        // Use HiltTestApplication for tests
        return super.newApplication(cl, HiltTestApplication::class.java.name, context)
    }
}
