/*
 * Copyright (c) 2024 Music Player Project
 * Start.kt is part of Music Player.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
 
package com.HrshD1eux.musicplayer.tasker

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.core.content.ContextCompat
import com.HrshD1eux.musicplayer.IntegerTable
import com.HrshD1eux.musicplayer.MusicPlayerService
import com.HrshD1eux.musicplayer.R
import com.joaomgcd.taskerpluginlibrary.action.TaskerPluginRunnerActionNoOutputOrInput
import com.joaomgcd.taskerpluginlibrary.config.TaskerPluginConfig
import com.joaomgcd.taskerpluginlibrary.config.TaskerPluginConfigHelperNoOutputOrInput
import com.joaomgcd.taskerpluginlibrary.config.TaskerPluginConfigNoInput
import com.joaomgcd.taskerpluginlibrary.input.TaskerInput
import com.joaomgcd.taskerpluginlibrary.runner.TaskerPluginResult
import com.joaomgcd.taskerpluginlibrary.runner.TaskerPluginResultSucess

class StartActionHelper(config: TaskerPluginConfig<Unit>) :
    TaskerPluginConfigHelperNoOutputOrInput<StartActionRunner>(config) {
    override val runnerClass: Class<StartActionRunner>
        get() = StartActionRunner::class.java

    override fun addToStringBlurb(input: TaskerInput<Unit>, blurbBuilder: StringBuilder) {
        blurbBuilder.append(context.getString(R.string.lng_tasker_start))
    }
}

class ActivityConfigStartAction : Activity(), TaskerPluginConfigNoInput {
    override val context: Context
        get() = applicationContext

    private val taskerHelper by lazy { StartActionHelper(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        taskerHelper.finishForTasker()
    }
}

class StartActionRunner : TaskerPluginRunnerActionNoOutputOrInput() {
    override fun run(context: Context, input: TaskerInput<Unit>): TaskerPluginResult<Unit> {
        ContextCompat.startForegroundService(
            context,
            Intent(context, MusicPlayerService::class.java)
                .setAction(MusicPlayerService.ACTION_START)
                .putExtra(MusicPlayerService.INTENT_KEY_START_ID, IntegerTable.START_ID_TASKER),
        )
        while (!MusicPlayerService.isForeground) {
            Thread.sleep(100)
        }
        // Actually need to sleep even longer since for some reason the notification still
        // won't accept media button events for an arbitrary period.
        Thread.sleep(100)
        return TaskerPluginResultSucess()
    }
}
