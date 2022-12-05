package org.hezisudio.listener

import net.mamoe.mirai.event.EventHandler
import net.mamoe.mirai.event.ListenerHost
import net.mamoe.mirai.event.events.GroupMessageEvent
import net.mamoe.mirai.message.data.*
import org.hezisudio.Monopoly
import org.hezisudio.Monopoly.save
import org.hezisudio.data.PluginConfig
import kotlin.reflect.KSuspendFunction1

/** 插件管理者使用命令监听，前置已过滤管理者id */
object OwnerCommandListener:ListenerHost {
    /** 后置过滤，返回[OwnerCmd] (来自[listOfCmd]) 未找到则返回null */
    private fun filter(msg: MessageChain):OwnerCmd?{
        for (cmd in listOfCmd){
            if (cmd.regex.matches(msg.content)){
                return cmd
            }
        }
        return null
    }

    @EventHandler
    suspend fun onEvent(e:GroupMessageEvent){
        val cmd = filter(e.message)?:return
        cmd.cmdAction(e)
    }
    /**指令列表*/
    private val listOfCmd:List<OwnerCmd> = listOf(
        object : OwnerCmd(Regex("#大富翁开关")){
            override suspend fun cmdAction(e: GroupMessageEvent) {
                val groupNum = e.group.id
                val groupList = ArrayList(PluginConfig.workGroup)
                if (groupNum !in groupList){
                    groupList.add(groupNum)
                    e.group.sendMessage("已在本群开启大富翁功能")
                }else{
                    groupList.remove(groupNum)
                    e.group.sendMessage("已关闭本群大富翁功能")
                }
                PluginConfig.workGroup = groupList
                PluginConfig.save()
            }
        },
        object : OwnerCmd(Regex("""#插件状态""")){
            override suspend fun cmdAction(e: GroupMessageEvent) {
                val workGroupList = PluginConfig.workGroup
                val fmb:ForwardMessageBuilder = ForwardMessageBuilder(e.group)
                for (gid in workGroupList){
                    val game = Monopoly.findGameOrCreate(gid)
                    fmb.add(e.bot,PlainText(game.toMessage()))
                }
                e.group.sendMessage(fmb.build())
            }
        }
    )
    /**cmd指令类，用于对指令的抽象、校验和执行*/
    abstract class OwnerCmd(val regex:Regex){
        abstract suspend fun cmdAction(e:GroupMessageEvent):Unit
    }
}