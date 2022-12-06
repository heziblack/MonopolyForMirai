package org.hezisudio.core

import net.mamoe.mirai.contact.Member
import org.hezisudio.Monopoly
import org.hezisudio.core.chance.ChanceCard
import org.hezisudio.core.map.*

class GameCore(
    /**群号，作为标识符*/
    val gid:Long
) {
    /**游戏状态*/
    var statue:GameStatue = GameStatue.Closed
    /**发起者id*/
    var gameHolder:Long = -1L
    /**玩家id列表*/
    val playerIds:ArrayList<Long> = arrayListOf()
    /** 回合数 */
    var round:Int = 0
    /** 玩家实体表 */
    val players:ArrayList<PlayerEntity> = arrayListOf()
    /**游戏设置*/
    val gameSetting = GameSetting()
    /** 重置游戏 */
    fun reset(){
        statue = GameStatue.Closed
        gameHolder = -1L
        round = 0
        playerIds.clear()
        players.clear()
        gameSetting.reset()
        timeToLottery = false
    }
    /**标志位，是否开奖*/
    var timeToLottery:Boolean = false
    /**游戏设置类*/
    class GameSetting(
        /**彩票开奖间隔*/
        var lotteryDuration:Int = DEFAULT_LOTTERY_DURATION,
        /**彩票奖金*/
        var lotteryAwards:Int = DEFAULT_LOTTERY_AWARDS,
        /**彩票单价*/
        var lotteryCost:Int = DEFAULT_LOTTERY_COST,
        /**土地出售损失率*/
        var sellCost:Double = DEFAULT_SELL_COST,
        /**游戏地图*/
        var gameMap: GameMap = Monopoly.gameMap.getCopy(),
        /**经过起点奖励*/
        var repassAwards:Int = DEFAULT_REPASS_AWARDS,
        /**启动资金*/
        var startMoney:Int = DEFAULT_START_MONEY,
    ){
        /**重置*/
        fun reset(){
            lotteryAwards = DEFAULT_LOTTERY_AWARDS
            lotteryCost = DEFAULT_LOTTERY_COST
            lotteryDuration = DEFAULT_LOTTERY_DURATION
            sellCost = DEFAULT_SELL_COST
            gameMap = Monopoly.gameMap.getCopy()
            repassAwards = DEFAULT_REPASS_AWARDS
            startMoney = DEFAULT_START_MONEY
        }
    }
    /**有人开启游戏
     *
     * [statue] 修改为[GameStatue.Readying]
     *
     * [gameHolder]修改为发起人QQ
     *
     * [playerIds]添加此QQ
     *
     * [players]添加该QQ对应的[PlayerEntity]*/
    fun hostOpenGame(host:Member){
        statue = GameStatue.Readying
        gameHolder = host.id
        playerIds.add(host.id)
        players.add(PlayerEntity(host.id))
    }
    /**显示玩家拥有的土地情况*/
    fun landList(playerEntity: PlayerEntity):String{
        TODO("获取玩家拥有的土地数据，并形成排版好的列表")
    }
    /**设置游戏地图*/
    fun setGameMap(map:GameMap){
        gameSetting.gameMap = map.getCopy()
    }
    /**判断[uid]是否为游戏创建者(房主)*/
    fun isGameHost(uid:Long):Boolean{
        return uid == gameHolder
    }
    /**生成游戏状态文本*/
    fun toMessage():String{
        val sb = StringBuilder()
        sb.append("""
            群号：${gid}
            发起人：${gameHolder}
            游戏状态：$statue
            玩家列表：
            """.trimIndent())
        sb.append('\n')
        if (players.isEmpty()){
            sb.append("-空")
        }
        for (p in players){
            sb.append("""
                -${p.id}
                --${p.statue}
                --${p.money}
            """.trimIndent())
            if (p != players.last()) sb.append('\n')
        }
        return sb.toString()
    }
    /**判断[uid]是否已经加入了游戏*/
    fun hasInGame(uid: Long):Boolean{
        return uid in playerIds
    }
    /**玩家[uid]加入游戏*/
    fun playerJoin(uid:Long){
        playerIds.add(uid)
        players.add(PlayerEntity(uid))
    }
    /**启动游戏，返回启动结果
     * @return 启动成功为true，失败为false*/
    fun startGame():Boolean{
        if (playerIds.size < 2) return false
        for (p in players){
            p.money = gameSetting.startMoney
            if (p == players.first()){
                p.statue = PlayerStatue.OnTerm
            }
        }
        statue = GameStatue.Playing
        return true
    }
    /**返回处于行动回合的玩家的id*/
    fun playerOnTerm():Long{
        for (p in players){
            if (p.statue == PlayerStatue.OnTerm){
                return p.id
            }
        }
        throw Exception("未找到处于行动回合的玩家")
    }
    /**根据[uid]获取此游戏中的玩家实体，未找到则抛出异常*/
    private fun getPlayerEntity(uid: Long):PlayerEntity{
        players.forEach {
            if (it.id == uid){
                return it
            }
        }
        throw Exception("不存在的玩家id")
    }
    /**回合中玩家没有处于子状态*/
    fun playerOnTermFree():Boolean{
        val playerEntity = getPlayerEntity(playerOnTerm())
        return !playerEntity.hasSubContent()
    }
    /**于游戏中获得指定下标的[Land]*/
    private fun getLand(pos:Int): Land {
        return gameSetting.gameMap[pos]
    }
    /**回合中玩家移动，返回移动后的地块名[Land.name]与是否经过起点，若经过起点获取奖励*/
    fun playerOnTermMove(step: Int):String{
        // 行动回合的玩家实体
        val player = getPlayerEntity(playerOnTerm())
        val beforePos = player.position // 移动前位置
        val mapLength = gameSetting.gameMap.size
        player.position = (beforePos+step) % mapLength
        val landName = getLandName(player.position) // 需要返回的地块名
        return if (player.position < beforePos){
            player.money += gameSetting.repassAwards
            "${landName}|1"
        }else{
            "${landName}|0"
        }
    }
    /**清除所有出局玩家*/
    private fun cleanPlayerOut(){
        // 现金变成复数的玩家
        val playersOut = checkPlayerOut()
        playersOut.forEach{ p ->
            // 将玩家状态置为[OutGame]
            p.statue = PlayerStatue.OutGame
            // 检查玩家所有土地列表并重置
            p.lands.forEach{ l->
                when(val land = gameSetting.gameMap[l.landPos]){
                    is Normal->land.reset()
                    is Station->land.reset()
                }
            }
        }
    }
    /**获取当前现金变成负数的玩家的列表*/
    private fun checkPlayerOut():List<PlayerEntity>{
        val outGame = arrayListOf<PlayerEntity>()
        for (p in players){
            if (p.money<0){
                outGame.add(p)
            }
        }
        return outGame.toList()
    }
    /**处理玩家回合中子状态*/
    private fun subContentDeal(playerEntity: PlayerEntity,playerAction:String):String{
        val locatedLand = getLand(playerEntity.position)
        val sb = StringBuilder()
        when(playerEntity.session.statue){
            SubStatue.BuyLottery->{
                val answer = playerAction.toInt()
                val cost = gameSetting.lotteryCost
                if (playerEntity.money<cost){
                    // 买不起
                    sb.append("对不起，您的现金似乎不够，请下次再来吧")
                }else{
                    // 买得起
                    var canBuy = true
                    val lh = playerEntity.lotteries
                    for (l in lh){
                        if (answer==l.lotteryID){
                            canBuy = false
                        }
                    }
                    if (canBuy){
                        playerEntity.money -= cost
                        playerEntity.lotteries.add(PlayerEntity.LotteryHold(answer))
                        sb.append("购买彩票：${answer}号，请耐心等待开奖")
                    }else{
                        sb.append("购买失败：您已经购买过此号码，请下次光临")
                    }

                }
            }
            SubStatue.BuyLand->{
                val answer = when(playerAction){
                    in POSITIVE_ANSWER -> true
                    in NEGATIVE_ANSWER-> false
                    else->false
                }
                if (answer){
                    // 要购买地块
                    when(locatedLand){
                        is Normal->{
                            val price = locatedLand.buildData[0].first
                            if (playerEntity.money<price){
                                // 买不起
                                sb.append("对不起，您的现金似乎不够，请下次再来吧")
                            }else{
                                // 买得起
                                playerEntity.money -= price
                                locatedLand.owner = playerEntity
                                sb.append("恭喜您，购买成功\n${locatedLand.name}:过路费${locatedLand.buildData[0].second}")
                            }
                        }
                        is Station->{
                            val price = locatedLand.price
                            if (playerEntity.money<price){
                                // 买不起
                                sb.append("对不起，您的现金似乎不够，请下次再来吧")
                            }else{
                                // 买得起
                                playerEntity.money -= price
                                locatedLand.owner = playerEntity
                                val stationCount = playerHoldStation(playerEntity)
                                sb.append("恭喜您，购买成功\n${locatedLand.name}:过路费${locatedLand.stationData[stationCount-1]}")
                            }
                        }
                    }
                }
            }
            SubStatue.UpgradeLand->{
                val answer = when(playerAction){
                    in POSITIVE_ANSWER -> true
                    in NEGATIVE_ANSWER-> false
                    else->false
                }
                if (answer){
                    // 升级地块
                    if (locatedLand is Normal){
                        val cost = locatedLand.buildData[locatedLand.level+1].first
                        if (playerEntity.money<cost){
                            // 买不起
                            sb.append("对不起，您的现金似乎不够，请下次再来吧")
                        }else{
                            // 买得起
                            playerEntity.money -= cost
                            locatedLand.level++
                            sb.append("恭喜您，成功升级\n${locatedLand.name}:过路费${locatedLand.buildData[locatedLand.level].second}")
                        }
                    }
                }

            }
            else->{}
        }
        // 清除子状态
        playerEntity.session.content=""
        playerEntity.session.statue=SubStatue.Null
        return sb.toString()
    }
    /**处理玩家回合中子状态*/
    fun subContentDeal(playerID:Long,playerAction: String):String{
        val playerEntity = getPlayerEntity(playerID)
        return subContentDeal(playerEntity,playerAction)
    }
    /**获取正在行动的玩家实体*/
    fun getPlayerOnTermEntity():PlayerEntity{
        return getPlayerEntity(playerOnTerm())
    }
    /**根据下标获取[Land.name]*/
    private fun getLandName(pos: Int):String{
        val mapSize = gameSetting.gameMap.size
        return gameSetting.gameMap[(pos%mapSize)].name
    }
    /**根据玩家id获取[Land.name]*/
    fun getPlayerLocationName(uid: Long):String{
        return getLandName(getPlayerEntity(uid).position)
    }
    /**输出[Normal]的信息*/
    private fun normalLandInfo(land:Normal):String{
        val sb =StringBuilder()
        val ownerID = land.owner?.id
        val owner = if(ownerID!=null){
            "[m${ownerID}]"
        }else{
            ""
        }
        val landLevel = when(land.level){
            0-> "空地"
            else->{"lv.${land.level}"}
        }
        sb.append(land.name)
        sb.append("($landLevel)")
        if (owner!=""){
            sb.append("-$owner")
        }
        sb.append("\n")
        sb.append("""
            价格：${land.buildData[0].first}
            路费：${land.buildData[0].second}
            升级：（花费/路费）
        """.trimIndent())
        sb.append("\n")
        for (l in land.buildData){
            if (l == land.buildData.first()){
                continue
            }
            sb.append("${l.first}/${l.second}")
            if (l!=land.buildData.last()){
                sb.append("\n")
            }
        }
        return sb.toString()
    }
    /**将每个地图的信息输出*/
    fun showLand():List<String>{
        val l :ArrayList<String> = arrayListOf()
        for (land in gameSetting.gameMap){
            val pos = gameSetting.gameMap.indexOf(land)
            val sb = StringBuilder("${pos}:\n")
            when(land){
                is Normal->{
                    sb.append(normalLandInfo(land))
                }
                is Station->{
                    sb.append(stationLandInfo(land))
                }
                is Lottery-> {
                    sb.append(land.name)
                }
                is StartPoint-> {
                    sb.append("""
                        ${land.name}
                        经过奖励：${gameSetting.repassAwards}
                    """.trimIndent())
                }
                is Chance -> {
                    sb.append("""
                        ${land.name}
                        ${land.description}
                    """.trimIndent())
                }
                is Prison -> {
                    sb.append("""
                        ${land.name}
                        ${land.description}
                        来到此处暂停一回合
                    """.trimIndent())
                }
            }
            l.add(sb.toString())
        }
        return l.toList()
    }
    /**回合中玩家执行落子事件，返回用于输出的字符串文本*/
    fun playerOnLandExecute():String{
        // 获取行动中玩家实体
        val playerEntity = getPlayerEntity(playerOnTerm())
        // sb用于构建返回字符串文本
        val sb = StringBuilder()
        // 对玩家位置做判断并进入对应流程
        when(val land = getLand(playerEntity.position)){
            is Normal ->{
                if(land.owner!=null){
                    // 地块有主
                    if (land.owner == playerEntity){
                        // 主人是自己
                        if(land.topLevel){
                            // 地块已经升到顶级
                            sb.append("此处属于你，已达到最大升级：\n-过路费：${land.buildData.last().second}")
                        }else{
                            // 地块未升到顶级
                            val level = land.level
                            sb.append("""
                                此处属于你，等级${level}
                                -过路费：${land.buildData[level].second}
                                -升级(花费/升级后路费):
                                --${land.buildData[level+1].first}/${land.buildData[level+1].second}
                                是否升级?（是/否）
                            """.trimIndent())
                            // 写入子状态
                            playerEntity.session.content = ""
                            playerEntity.session.statue = SubStatue.UpgradeLand
                        }
                    }else{
                        // 主人是别的玩家
                        val landOwner = land.owner!! // 地主
                        val shouldPay = land.buildData[land.level].second // 过路费
                        playerEntity.money -= shouldPay
                        landOwner.money += shouldPay
                        sb.append("此处属于[m${landOwner.id}]，需支付${shouldPay}通过")
                    }
                }else{
                    // 地块无主
                    sb.append("此处还没有人购买\n")
                    sb.append(normalLandInfo(land))
                    sb.append("\n需要购买么？（是/否）")
                    // 写入子状态
                    playerEntity.session.content = ""
                    playerEntity.session.statue = SubStatue.BuyLand
                }
            }
            is Station ->{
                if(land.owner!=null){
                    // 地块有主
                    if (land.owner == playerEntity){
                        // 主人是自己
                        sb.append("此处属于你，无需支付过路费")
                    }else{
                        // 主人是别的玩家
                        val landOwner = land.owner!!
                        val stationHold = playerHoldStation(landOwner)
                        val shouldPay = land.stationData[stationHold-1]
                        playerEntity.money -= shouldPay
                        landOwner.money += shouldPay
                        sb.append("此处属于[m${landOwner.id}]，需支付${shouldPay}通过")
                    }
                }else{
                    // 地块无主
                    sb.append("此处还没有人购买\n")
                    sb.append(stationLandInfo(land))
                    sb.append("\n需要购买么？（是/否）")
                    playerEntity.session.content = ""
                    playerEntity.session.statue = SubStatue.BuyLand
                }
            }
            is Chance ->{
                // 机会: 抽取机会卡,并执行
                val card = gameSetting.gameMap.chancePool.random()
                sb.append(land.description+"\n")
                sb.append("${card.title}:\n${card.description}")
                chanceCardExecute(card, playerEntity)
            }
            is Lottery ->{
                // 彩票站
                sb.append(land.description+"\n")
                sb.append("请选择号码:(0-99)")
                // 写入子状态
                playerEntity.session.statue = SubStatue.BuyLottery
                playerEntity.session.content = ""
            }
            is Prison->{
                sb.append(land.description+"\n")
                sb.append("冻结1回合")
                playerEntity.statue = PlayerStatue.Freeze
                playerEntity.freezeLeft = 1
            }
            else ->{
                // 什么也没有发生（起点给钱已经执行不需在此处再次执行）
            }
        }
        return sb.toString()
    }
    /**查询玩家持有[Station]的数量*/
    private fun playerHoldStation(player: PlayerEntity):Int{
        var count = 0
        gameSetting.gameMap.forEach{
            if (it is Station){
                if(it.owner == player){
                    count++
                }
            }
        }
        return count
    }
    /**输出[Station]的信息*/
    private fun stationLandInfo(land:Station):String{
        val sb = StringBuilder()
        sb.append(land.name)
        val owner = land.owner
        val ownerID = owner?.id
        if (ownerID!=null){
            sb.append("-[m${ownerID}]")
        }
        sb.append('\n')
        if (owner!=null){
            val stationCount = playerHoldStation(owner)
            val passCost = land.stationData[stationCount-1]
            sb.append("路费：${passCost}")
        }else{
            sb.append("""
                价格：${land.price}
                路费：
            """.trimIndent())
            sb.append("\n")
            for ((idx,cost) in land.stationData.withIndex()){
                sb.append("-持有${idx+1}处:${cost}")
                if (cost != land.stationData.last()){
                    sb.append("\n")
                }
            }
        }
        return sb.toString()
    }
    /**执行机会卡[ChanceCard]的内容，待实现*/
    private fun chanceCardExecute(card: ChanceCard,player: PlayerEntity){
        val actionList = card.chanceMatas
        for (action in actionList){
            when(action.act){
                ChanceCard.MataAction.Move -> {
                    val host = action.obj
                    val targetEntity:ArrayList<PlayerEntity> = mataTargetToEntity(host,player)
                    for (t in targetEntity){
                        t.position = action.param.toInt()
                    }
                }
                ChanceCard.MataAction.MoneyOut -> {
                    val host = action.obj
                    val targetEntity:ArrayList<PlayerEntity> = mataTargetToEntity(host,player)
                    val param = action.param.toInt()
                    for (t in targetEntity){
                        t.money -= param
                    }
                }
                ChanceCard.MataAction.MoneyIn -> {
                    val host = action.obj
                    val targetEntity:ArrayList<PlayerEntity> = mataTargetToEntity(host,player)
                    val param = action.param.toInt()
                    for (t in targetEntity){
                        t.money += param
                    }
                }
                ChanceCard.MataAction.UpdateLand -> {
                    val host = action.obj
                    val targetEntity:ArrayList<PlayerEntity> = mataTargetToEntity(host,player)
                    for (target in targetEntity){
                        if (target.lands.size == 0) continue
                        for (landHold in target.lands){
                            val land = gameSetting.gameMap[landHold.landPos]
                            if(land is Normal){
                                if (!land.topLevel){
                                    land.level++
                                    break
                                }
                            }
                        }
                    }
                }
                ChanceCard.MataAction.MoneyOutByLandLevel -> {
                    // 根据玩家地块等级付出相应金钱（空地除外）
                    val host = action.obj
                    val targetEntity:ArrayList<PlayerEntity> = mataTargetToEntity(host,player)
                    val singlePrice = action.param.toInt()
                    for (target in targetEntity){
                        if (target.lands.size == 0) continue
                        val sum = playerLandLevel(target)
                        target.money -= sum*singlePrice
                    }
                }
                ChanceCard.MataAction.MoneyInByLandLevel -> {
                    val host = action.obj
                    val targetEntity:ArrayList<PlayerEntity> = mataTargetToEntity(host,player)
                    val singlePrice = action.param.toInt()
                    for (target in targetEntity){
                        if (target.lands.size == 0) continue
                        val sum = playerLandLevel(target)
                        target.money += sum*singlePrice
                    }
                }
                ChanceCard.MataAction.MoneyOutToOthers -> {
                    val sm = action.param.toInt()
                    for (pl in getPlayablePlayerList()){
                        if (pl == player) continue
                        pl.money += sm
                        player.money -= sm
                    }
                }
                ChanceCard.MataAction.MoneyInFromOthers -> {
                    val sm = action.param.toInt()
                    for (pl in getPlayablePlayerList()){
                        if (pl == player) continue
                        pl.money -= sm
                        player.money += sm
                    }
                }
                ChanceCard.MataAction.Freeze -> {
                    val host = action.obj
                    val targetEntity:ArrayList<PlayerEntity> = mataTargetToEntity(host,player)
                    val time = action.param.toInt()
                    for (t in targetEntity){
                        t.statue = PlayerStatue.Freeze
                        t.freezeLeft = time
                    }
                }
            }
        }
    }
    /**获取新出局玩家列表*/
    fun outPlayerList():List<PlayerEntity>{
        val pl = ArrayList<PlayerEntity>()
        for (p in players){
            if (p.statue != PlayerStatue.OutGame && p.money<0){
                pl.add(p)
            }
        }
        return pl
    }
    /**游戏是否可以继续*/
    fun canGameGoOn():Boolean{
        var countOfPlay = 0
        for (p in players){
            if (p.statue != PlayerStatue.OutGame){
                countOfPlay ++
            }
        }
        return countOfPlay>1
    }
    /**获取胜利玩家ID，仅在游戏分出胜负时使用*/
    fun getWinnerID():Long{
        for (p in players){
            if (p.statue != PlayerStatue.OutGame){
                return p.id
            }
        }
        return -1L
    }
    /**传递回合行动权，并返回下一个行动玩家的ID*/
    fun passTerm(lastPlayer:PlayerEntity = getPlayerEntity(playerOnTerm())):Long{
        var idxOfLastPlayer = players.indexOf(lastPlayer) // 获取上一个玩家的序号
        // 修改玩家状态
        if(lastPlayer.statue == PlayerStatue.OnTerm){
            lastPlayer.statue = PlayerStatue.Waiting
        }
        // 轮询玩家是否适合获得行动权
        while (true){
            idxOfLastPlayer ++
            if (idxOfLastPlayer==players.size){
                // 循环一回合，round+1, 序号置零
                round++
                if ( round%gameSetting.lotteryDuration==0 && round>0 ){
                    timeToLottery = true
                }
                idxOfLastPlayer=0
            }
            when(players[idxOfLastPlayer].statue){
                PlayerStatue.Waiting -> {
                    players[idxOfLastPlayer].statue = PlayerStatue.OnTerm
                    break
                }
                PlayerStatue.Freeze -> {
                    // 冻结残余时间-1，减到0后状态置为等待
                    players[idxOfLastPlayer].freezeLeft--
                    if (players[idxOfLastPlayer].freezeLeft<=0){
                        players[idxOfLastPlayer].statue = PlayerStatue.Waiting
                    }
                }
                else->{}
            }
        }
        return players[idxOfLastPlayer].id
    }
    /**获取从机会卡对象所对应的玩家实体*/
    private fun mataTargetToEntity(actionTarget: ChanceCard.MataTarget,host:PlayerEntity):ArrayList<PlayerEntity>{
        return when(actionTarget){
            ChanceCard.MataTarget.Self -> {
                arrayListOf(host)
            }
            ChanceCard.MataTarget.Someone -> {
                arrayListOf(randomOfSomeone(host))
            }
            ChanceCard.MataTarget.Others -> {
                playerOthers(host)
            }
            ChanceCard.MataTarget.All -> {
                allPlayer()
            }
            ChanceCard.MataTarget.LandMan -> {
                landMan()
            }
            ChanceCard.MataTarget.LandlessMan -> {
                landlessMan()
            }
            ChanceCard.MataTarget.RichMan -> {
                richMan()
            }
            ChanceCard.MataTarget.PoorMan -> {
                poorMan()
            }
        }
    }
    /**获取随机除当前玩家外的存活玩家*/
    private fun randomOfSomeone(host: PlayerEntity):PlayerEntity{
        val pl = arrayListOf<PlayerEntity>()
        for (p in players){
            if (p.statue != PlayerStatue.OutGame && p != host){
                pl.add(p)
            }
        }
        return pl.random()
    }
    /**获取除当前玩家外的存货玩家*/
    private fun playerOthers(host: PlayerEntity):ArrayList<PlayerEntity>{
        val pl = arrayListOf<PlayerEntity>()
        for (p in players){
            if (p.statue != PlayerStatue.OutGame && p != host){
                pl.add(p)
            }
        }
        return pl
    }
    /**获取依然在场上的玩家*/
    private fun allPlayer():ArrayList<PlayerEntity>{
        val pl = arrayListOf<PlayerEntity>()
        for (p in players){
            if (p.statue != PlayerStatue.OutGame){
                pl.add(p)
            }
        }
        return pl
    }
    /**获取场上土地数量最多的玩家*/
    private fun landMan():ArrayList<PlayerEntity>{
        val pl = arrayListOf<PlayerEntity>()
        var maxLandNum = 0
        for (p in players){
            if (p.statue != PlayerStatue.OutGame){
                if(p.lands.size > maxLandNum){
                    maxLandNum = p.lands.size
                }
            }
        }
        for (p in players){
            if (p.statue != PlayerStatue.OutGame){
                if (p.lands.size == maxLandNum){
                    pl.add(p)
                }
            }
        }
        return pl
    }
    /**获取场上土地数量最少的玩家*/
    private fun landlessMan():ArrayList<PlayerEntity>{
        val pl = arrayListOf<PlayerEntity>()
        val playerInGame = arrayListOf<PlayerEntity>()
        for (p in players){
            if (p.statue != PlayerStatue.OutGame){
                playerInGame.add(p)
            }
        }
        var minLandNum = playerInGame[0].lands.size
        for (p in playerInGame){
            if (p.lands.size <= minLandNum){
                minLandNum = p.lands.size
            }
        }
        for (p in playerInGame){
            if (p.lands.size == minLandNum){
                pl.add(p)
            }
        }
        return pl
    }
    /**获取场上现金最多的玩家*/
    private fun richMan():ArrayList<PlayerEntity>{
        val pl = arrayListOf<PlayerEntity>()
        var maxMoneyNum = 0
        for (p in players){
            if (p.statue != PlayerStatue.OutGame){
                if(p.money > maxMoneyNum){
                    maxMoneyNum = p.money
                }
            }
        }
        for (p in players){
            if (p.statue != PlayerStatue.OutGame){
                if (p.money == maxMoneyNum){
                    pl.add(p)
                }
            }
        }
        return pl
    }
    /**获取场上现金最少的玩家*/
    private fun poorMan():ArrayList<PlayerEntity>{
        val pl = arrayListOf<PlayerEntity>()
        val playerInGame = arrayListOf<PlayerEntity>()

        for (p in players){
            if (p.statue != PlayerStatue.OutGame){
                playerInGame.add(p)
            }
        }
        // 最少金钱
        var minMoneyNum = playerInGame[0].money
        for (p in playerInGame){
            if (p.money <= minMoneyNum){
                minMoneyNum = p.money
            }
        }
        for (p in playerInGame){
            if (p.money == minMoneyNum){
                pl.add(p)
            }
        }
        return pl
    }
    /**获取玩家土地等级之和，1-4*/
    private fun playerLandLevel(player: PlayerEntity):Int{
        var sum = 0
        for (landPos in player.lands){
            val land = gameSetting.gameMap[landPos.landPos]
            if(land is Normal){
                sum += (land.level)
            }
        }
        return sum
    }
    /**检查游戏中有且只有一个玩家处于行动状态，参数[player]是引起此次检查的玩家对象
     *
     * 从引起检查的玩家开始向下循环查询下一个有资格获得行动权限的玩家，将其置为行动状态并返回
     *
     * @return [PlayerEntity]：如果为空，表示不能找到下一个可以获得行动权的玩家，基本可以说明游戏结束*/
    fun termChecker(player:PlayerEntity):PlayerEntity?{
        val allPlayerNum = players.size
        val playableList = getPlayablePlayerList()
        val currentPlayerIndex = players.indexOf(player)
        if (playableList.size <=1){
            return null
        }

        while (true){

        }

        TODO("检查游戏中有且只有一个玩家处于行动状态，参数[player]是引起此次检查的玩家对象")
    }
    /**获取未出局玩家列表**/
    private fun getPlayablePlayerList():ArrayList<PlayerEntity>{
        val outList = ArrayList<PlayerEntity>()
        for (p in players){
            if (p.statue != PlayerStatue.OutGame){
                outList.add(p)
            }
        }
        return outList
    }
    /**检查游戏中有且只有一个玩家处于行动状态，参数[playerID]是引起此次检查的玩家对象
     *
     * 从引起检查的玩家开始向下循环查询下一个有资格获得行动权限的玩家，将其置为行动状态并返回
     *
     * @return [PlayerEntity]：如果为空，表示不能找到下一个可以获得行动权的玩家，基本可以说明游戏结束*/
    fun termChecker(playerID: Long):PlayerEntity?{
        return termChecker(getPlayerEntity(playerID))
    }
    /**将玩家的状态置为出局，并清空玩家与地图的关联*/
    fun outPlayer(player:PlayerEntity){
        player.statue = PlayerStatue.OutGame
        val lands = player.lands
        for (l in lands){
            when(val land = gameSetting.gameMap[l.landPos]){
                is Normal->{
                    land.owner = null
                    land.level = 0
                }
                is Station->{
                    land.owner = null
                }
            }
        }
        player.clearLottery()
        player.lands.clear()
    }
    /**将一个列表中的玩家置为出局*/
    fun outPlayer(playerList:List<PlayerEntity>){
        for (player in playerList){
            outPlayer(player)
        }
    }
    /**执行彩票的开奖
     * 并生成结果返回*/
    fun lotteryTime():String{
        val sb = StringBuilder("开奖啦！\n")
        val num = (0..99).random()
        sb.append("本期的幸运号码是：${num}\n")
        val listOfLotteryWinner = ArrayList<PlayerEntity>()
        for (p in players){
            if (p.statue == PlayerStatue.OutGame) continue
            val lh = p.lotteries
            for (l in lh){
                if (l.lotteryID == num){
                    listOfLotteryWinner.add(p)
                    break
                }
            }
        }
        if (listOfLotteryWinner.isEmpty()){
            sb.append("好可惜，没有人中奖呢~\n本期彩票将保留到下一次开奖哦~~")
        }else{
            val playerInGame = getPlayablePlayerList()
            sb.append("恭喜")
            for (p in listOfLotteryWinner){
                sb.append("[m${p.id}]")
                if (p != listOfLotteryWinner.last()){
                    sb.append("、")
                }
                p.money+=gameSetting.lotteryAwards
            }
            for (p in playerInGame){
                p.clearLottery()
            }
            sb.append("获得${gameSetting.lotteryAwards}奖金！\n彩票已重置")
        }

        timeToLottery = false
        return sb.toString()
    }
}