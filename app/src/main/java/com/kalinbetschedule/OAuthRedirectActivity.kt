package com.kalinbetschedule
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import com.kalinbetschedule.data.backup.BackupManager
class OAuthRedirectActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        handle(intent)
    }
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handle(intent)
    }
    private fun handle(intent: Intent?) {
        intent?.data?.let { BackupManager.onRedirect(applicationContext, it) }
        finish()
    }
}
