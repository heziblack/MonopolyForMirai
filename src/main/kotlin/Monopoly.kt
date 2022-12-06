package org.hezisudio

import net.mamoe.mirai.console.data.PluginDataStorage
import net.mamoe.mirai.console.plugin.PluginManager.INSTANCE.load
import net.mamoe.mirai.console.plugin.jvm.JvmPluginDescription
import net.mamoe.mirai.console.plugin.jvm.KotlinPlugin
import net.mamoe.mirai.event.events.GroupMessageEvent
import net.mamoe.mirai.event.globalEventChannel
import net.mamoe.mirai.utils.info
import org.hezisudio.Monopoly.save
import org.hezisudio.core.GameCore
import org.hezisudio.core.map.*
import org.hezisudio.data.PluginConfig
import org.hezisudio.listener.GameGroupListener
import org.hezisudio.listener.OwnerCommandListener
import java.io.File
import java.io.FilenameFilter

object Monopoly : KotlinPlugin(
    JvmPluginDescription(
        id = "org.hezisudio.monopoly",
        name = "大富翁",
        version = "0.1.1",
    ) {
        author("HeziBlack")
    }
) {
    /**游戏列表，保存游戏状态的地方，只添加不除移*/
    val gameList:ArrayList<GameCore> = arrayListOf()
    /**默认地图*/
    val gameMap = GameMap()
    /**地图列表*/
    val mapList:ArrayList<GameMap> = arrayListOf(gameMap)

    override fun onEnable() {
        loadPluginConfigAndPrint()
        loadMapList()
        printGameMapList()

        /* 注册bot主人监听 */
        globalEventChannel().filter {
            (it as GroupMessageEvent).sender.id == PluginConfig.owner
        }.registerListenerHost(OwnerCommandListener)

        /* 注册游戏流程监听 */
        globalEventChannel().filter {
            (it as GroupMessageEvent).group.id in PluginConfig.workGroup
        }.registerListenerHost(GameGroupListener)

        logger.info { "初始化完成" }
    }
    /**加载插件配置并打印*/
    private fun loadPluginConfigAndPrint() {
        logger.info("读取配置")
        PluginConfig.reload()
        logger.info("bot主人：${PluginConfig.owner}")
        logger.info("工作群聊：${PluginConfig.workGroup}")
    }
    /**打印地图列表*/
    private fun printGameMapList() {
        logger.info("地图列表：")
        var index = 1
        mapList.forEach {
            logger.info("$index:${it.name}")
            index++
        }
    }
    /**加载自制地图*/
    private fun loadMapList(){
        logger.info("加载地图ing")
        val mapFolder = File(dataFolder,"map")
        if (!mapFolder.exists()){
            mapFolder.mkdirs()
        }
        val exMapList = mapFolder.listFiles { _, name ->
            name.endsWith(".xlsx")
        }?:throw Exception("获取地图文件失败")
        exMapList.forEach {
            mapList.add(GameMap(it))
            logger.info(it.name)
        }
    }

    override fun onDisable() {
        super.onDisable()
        PluginConfig.save()
    }

    /**指定群号找出游戏列表中的游戏*/
    private fun findGame(gid:Long):GameCore?{
        for (g in gameList){
            if (gid == g.gid){
                return g
            }
        }
        return null
    }

    /**指定群号找出游戏列表中的游戏，若不存在则创建并添加到游戏列表*/
    fun findGameOrCreate(gid:Long):GameCore{
        val game = findGame(gid)?:GameCore(gid)
        gameList.add(game)
        return game
    }

}