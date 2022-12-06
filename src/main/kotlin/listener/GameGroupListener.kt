package org.hezisudio.listener

import kotlinx.coroutines.delay
import net.mamoe.mirai.contact.Group
import net.mamoe.mirai.contact.remarkOrNameCardOrNick
import net.mamoe.mirai.event.EventHandler
import net.mamoe.mirai.event.ListenerHost
import net.mamoe.mirai.event.events.GroupMessageEvent
import net.mamoe.mirai.message.data.*
import org.hezisudio.Monopoly
import org.hezisudio.core.GameCore
import org.hezisudio.core.GameStatue
import org.hezisudio.core.PlayerEntity
import org.hezisudio.utils.MessageStringRender


object GameGroupListener:ListenerHost {
    private fun filter(msg: MessageChain):GameCmd?{
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
    private val listOfCmd:List<GameCmd> = listOf(
        object :GameCmd(Regex("""[!！]开始大富翁""")){
            override suspend fun cmdAction(e: GroupMessageEvent) {
                val gameCore = Monopoly.findGameOrCreate(e.group.id)
                if (gameCore.statue !== GameStatue.Closed) return
                gameCore.hostOpenGame(e.sender)
                e.group.sendMessage("游戏已启动")
            }
        },
        object :GameCmd(Regex("""[!！]结束大富翁""")){
            override suspend fun cmdAction(e: GroupMessageEvent) {
                val gameCore = Monopoly.findGameOrCreate(e.group.id)
                if (gameCore.statue == GameStatue.Closed) return
                gameCore.reset()
                e.group.sendMessage("游戏已关闭")
            }
        },
        object :GameCmd(Regex("""[!！]游戏状态""")){
            override suspend fun cmdAction(e: GroupMessageEvent) {
                val gameCore = Monopoly.findGameOrCreate(e.group.id)
                if(!gameCore.isGameHost(e.sender.id)) return
                val msg = gameCore.toMessage()
                e.group.sendMessage(msg)
            }
        },
        object :GameCmd(Regex("""加入""")){
            override suspend fun cmdAction(e: GroupMessageEvent) {
                val gameCore = Monopoly.findGameOrCreate(e.group.id)
                if (gameCore.statue != GameStatue.Readying) return
                if (gameCore.hasInGame(e.sender.id)) return
                gameCore.playerJoin(e.sender.id)
                e.group.sendMessage("成功加入游戏！")
            }
        },
        object :GameCmd(Regex("""[!！]开始游戏""")){
            override suspend fun cmdAction(e: GroupMessageEvent) {
                val gameCore = Monopoly.findGameOrCreate(e.group.id)
                if (gameCore.statue != GameStatue.Readying) return
                if (!gameCore.isGameHost(e.sender.id)) return
                if(gameCore.startGame()){
                    val playerOnTermId = gameCore.playerOnTerm()
                    val playerOnTerm = e.group[playerOnTermId]
                    val msgBuilder = MessageChainBuilder()
                    if (playerOnTerm==null){
                        msgBuilder.add("接下来是${playerOnTermId}的回合")
                    }else{
                        msgBuilder.add("接下来是")
                        msgBuilder.add(At(playerOnTerm))
                        msgBuilder.add("的回合")
                    }
                    e.group.sendMessage(msgBuilder.build())
                }else{
                    e.group.sendMessage("不能开始游戏，因为人数少于2")
                }
            }
        },
        object :GameCmd(Regex("""掷""")){
            override suspend fun cmdAction(e: GroupMessageEvent) {
                // 获取游戏
                val gameCore = Monopoly.findGameOrCreate(e.group.id)
                if (gameCore.statue != GameStatue.Playing) {
                    Monopoly.logger.info("游戏不在进行状态")
                    return
                }// 判断游戏是否进行中
                // 判断正在行动的玩家是否是发送者
                if (gameCore.playerOnTerm() != e.sender.id) {
                    Monopoly.logger.info("发送者不在行动回合")
                    return
                }
                // 判断此玩家是否处于行动回合的子状态(已经掷过骰子)
                if (!gameCore.playerOnTermFree()) {
                    Monopoly.logger.info("发送者处于行动回合子状态")
                    return
                }
                val player = gameCore.getPlayerOnTermEntity()
                // 获得随机数
                val dice = Dice.random()
                val step:Int = dice.value
                e.group.sendMessage(dice)
                delay(300)
                // 玩家移动
                val moveResult = gameCore.playerOnTermMove(step).split("|")
                e.group.sendMessage("${e.sender.remarkOrNameCardOrNick}掷出${step}点数，行至${moveResult[0]}")
                // 若经过起点多发一条信息
                if (moveResult[1] == "1"){
                    delay(300)
                    e.group.sendMessage("到达/经过起点，奖励${gameCore.gameSetting.repassAwards}元")
                }
                delay(300)
                // 处理玩家地块事件，不修改玩家状态，只修改相关值
                val landExeResult = gameCore.playerOnLandExecute()
                if (landExeResult!=""){
                    e.group.sendMessage(landExeResult)
                    delay(300)
                }
                /* !!判断是否有玩家出局，传递行动权限流程比较复杂，需要着重注意!! */
                // 玩家是否处于子状态
                if (player.hasSubContent()){
                    // 处于子状态
                    return
                }
                // 检查玩家是否出局(资金为负)
                if(player.money<0){
                    gameCore.outPlayer(player)
                    e.group.sendMessage("很遗憾，您破产了")
                    delay(300)
                }
                // 检查别的玩家是否出局(不包含行动中玩家)
                val outGames = gameCore.outPlayerList()
                if(outGames.isNotEmpty()){
                    val sb = StringBuilder()
                    for (p in outGames){
                        val name = e.group[p.id]?.remarkOrNameCardOrNick?:"[玩家${p.id}]"
                        sb.append(name)
                        if (p != outGames.last()){
                            sb.append('、')
                        }
                        gameCore.outPlayer(p)
                    }
                    sb.append("出局了。")
                    e.group.sendMessage(sb.toString())
                }
                // 检查游戏是否可以继续
                if (gameCore.canGameGoOn()){
                    // 传递执行权
                    val nextID = gameCore.passTerm(player)
                    // 检查是否开奖
                    checkLottery(gameCore, e)
                    sendNextMessage(e.group, nextID)
                }else{
                    // 不能继续，重置游戏
                    val winnerID = gameCore.getWinnerID()
                    gameCore.reset()
                    // 获取获胜玩家
                    if (winnerID!=-1L){
                        val winnerName = e.group[winnerID]?.remarkOrNameCardOrNick?:"[玩家${winnerID}]"
                        e.group.sendMessage("游戏结束，${winnerName}获得了最后的胜利！")
                    }else{
                        e.group.sendMessage("游戏结束，没有胜利者")
                    }
                }
            }
        },
        object :GameCmd(Regex("""查看地图""")){
            override suspend fun cmdAction(e: GroupMessageEvent) {
                val gameCore = Monopoly.findGameOrCreate(e.group.id)
                if (!gameCore.hasInGame(e.sender.id)) {return}
                val landStrList = gameCore.showLand()
                val fmb:ForwardMessageBuilder = ForwardMessageBuilder(e.group)
                for (landInfo in landStrList){
                    fmb.add(e.bot,PlainText(landInfo))
                }
                e.group.sendMessage(fmb.build())
            }
        },
        object :GameCmd(Regex("""[是Yy]""")){
            override suspend fun cmdAction(e: GroupMessageEvent) {
                // 处理对子状态的肯定指令
                if (!checkSubStatue(e)){return}
                val gameCore = Monopoly.findGameOrCreate(e.group.id)
                val player = gameCore.getPlayerOnTermEntity()
                val r = gameCore.subContentDeal(e.sender.id,"yes")
                if (r!="") e.group.sendMessage(r)
                val numOfNext = gameCore.passTerm(player)
                checkLottery(gameCore, e)
                sendNextMessage(e.group,numOfNext)
            }
        },
        object :GameCmd(Regex("""[否Nn]""")){
            override suspend fun cmdAction(e: GroupMessageEvent) {
                // 处理对子状态的否定指令
                if (!checkSubStatue(e)){return}
                val gameCore = Monopoly.findGameOrCreate(e.group.id)
                val player = gameCore.getPlayerOnTermEntity()
                val r = gameCore.subContentDeal(e.sender.id,"no")
                if (r!="") e.group.sendMessage(r)
                val numOfNext = gameCore.passTerm(player)
                checkLottery(gameCore, e)
                sendNextMessage(e.group,numOfNext)
            }
        },
        object :GameCmd(Regex("""\d{1,2}""")){
            override suspend fun cmdAction(e: GroupMessageEvent) {
                // 处理彩票购买的指令
                if (!checkSubStatue(e)){return}
                val gameCore = Monopoly.findGameOrCreate(e.group.id)
                val player = gameCore.getPlayerOnTermEntity()
                val r = gameCore.subContentDeal(e.sender.id,e.message.content)
                if (r!="") e.group.sendMessage(r)
                val numOfNext = gameCore.passTerm(player)
                checkLottery(gameCore, e)
                sendNextMessage(e.group,numOfNext)
            }
        }
    )
    /**检查彩票开奖，置于传递行动权之后*/
    private suspend fun checkLottery(gameCore: GameCore, e: GroupMessageEvent) {
        if (gameCore.timeToLottery) {
            // 执行开奖
            val lotteryMsg = gameCore.lotteryTime()
            val rendered = MessageStringRender.render(lotteryMsg, e.group)
            e.group.sendMessage(rendered)
        }
    }
    /**cmd指令类，用于对指令的抽象、校验和执行*/
    abstract class GameCmd(val regex:Regex){
        abstract suspend fun cmdAction(e: GroupMessageEvent):Unit
    }
    /**检查玩家是否处于子状态*/
    private fun checkSubStatue(e:GroupMessageEvent):Boolean{
        val gameCore = Monopoly.findGameOrCreate(e.group.id)
        if (gameCore.statue != GameStatue.Playing) {
            Monopoly.logger.info("游戏不在运行状态")
            return false
        }
        if (!gameCore.hasInGame(e.sender.id)) {
            Monopoly.logger.info("玩家不在游戏中")
            return false
        }
        if (gameCore.playerOnTerm() != e.sender.id) {
            Monopoly.logger.info("玩家不处于行动回合")
            return false
        }
        if (gameCore.playerOnTermFree()) {
            Monopoly.logger.info("玩家非子状态")
            return false
        }
        return true
    }
    /**发送下一个玩家宣告*/
    private suspend fun sendNextMessage(group: Group,nextID:Long){
        val next = group[nextID]
        if (next!=null) {
            val msgChain = messageChainOf(
                PlainText("现在是 "),
                At(next),
                PlainText(" 的回合，请发送‘掷’进行下一步")
            )
            group.sendMessage(msgChain)
        }else{
            group.sendMessage("现在是[玩家${nextID}]的回合，请发送‘掷’进行下一步")
        }
    }
}