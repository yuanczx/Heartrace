package me.heartrace.feature.testing

import me.heartrace.data.ReminderSchedulerPort

class FakeReminderScheduler : ReminderSchedulerPort {
    val enabledValues = mutableListOf<Boolean>()

    override fun setEnabled(enabled: Boolean) {
        enabledValues += enabled
    }
}
