package blackorbs.dev.jetfiledownloader

import android.app.Application
import android.content.Context
import androidx.test.runner.AndroidJUnitRunner

@Suppress("unused")
class CustomTestRunner: AndroidJUnitRunner() {
    override fun newApplication(
        cl: ClassLoader?, className: String?, context: Context?
    ): Application {
        return super.newApplication(cl, FakeApp::class.java.name, context)
    }
}