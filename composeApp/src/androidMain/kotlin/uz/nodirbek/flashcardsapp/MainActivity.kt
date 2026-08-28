package uz.nodirbek.flashcardsapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import uz.nodirbek.flashcardsapp.notification.NotificationScheduler
import uz.nodirbek.flashcardsapp.ui.navigation.Screen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val container = AppContainer(this)
        NotificationScheduler(this).scheduleStreakWarning() // daily 20:00 streak protection reminder

        val navigateToStudy = intent?.getBooleanExtra("navigateToStudy", false) ?: false

        setContent {
            App(
                container = container,
                startDestination = if (navigateToStudy) Screen.SrsReview.createRoute("default") else null
            )
        }
    }
}
