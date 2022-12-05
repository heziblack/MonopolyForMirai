package org.hezisudio.data

import net.mamoe.mirai.console.data.AutoSavePluginConfig
import net.mamoe.mirai.console.data.value

object PluginConfig:AutoSavePluginConfig("PluginConfig") {
    var owner by value<Long>(-1)
    var workGroup by value<List<Long>>(listOf())
}