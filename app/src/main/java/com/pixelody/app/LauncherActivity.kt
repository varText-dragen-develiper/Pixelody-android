package com.pixelody.app

import android.app.Activity
import android.app.ActivityManager
import android.content.Intent
import android.os.Bundle

/** Disposable alias target; the music task never belongs to a mutable icon alias. */
class LauncherActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // An update can leave a recent task whose base Intent still names an old
        // mutable alias. Retire that task record before Android can reuse it.
        val aliasPrefix = MainActivity::class.java.name.substringBeforeLast('.') + ".Launcher"
        getSystemService(ActivityManager::class.java).appTasks.forEach { task ->
            val info = task.taskInfo
            if (info.id != taskId && info.baseIntent.component?.className?.startsWith(aliasPrefix) == true) {
                task.finishAndRemoveTask()
            }
        }
        startActivity(Intent(intent).setClass(this, MainActivity::class.java)
            .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP))
        finish()
    }
}
