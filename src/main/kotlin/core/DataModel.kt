package org.hezisudio.core

import org.hezisudio.core.map.GameMap

/** 游戏状态，用于标注群聊游戏状态 */
enum class GameStatue(){
    /**游戏未启动，等待玩家发起游戏*/
    Closed,
    /**游戏准备阶段，群成员均可加入，发起人可以进行游戏参数设置*/
    Readying,
    /**游戏游玩阶段，群成员不可加入，不可修改游戏设置*/
    Playing,
}

/**玩家游戏状态*/
enum class PlayerStatue(){
    /** 等待，处于房间等待状态或游戏等待状态 */
    Waiting,
    /** 处于行动回合 */
    OnTerm,
    /**场外状态，在游戏中输掉了*/
    OutGame,
    /**冻结，暂停行动*/
    Freeze,
}

/** 地图类型 */
enum class LandType(){
    /**通常地块*/
    Normal,
    /**起点地块*/
    StartPoint,
    /**机遇地块*/
    Chance,
    /**监狱地块*/
    Prison,
    /**彩票站地块*/
    Lottery,
}

/**子状态*/
enum class SubStatue{
    /**无子状态*/
    Null,
    /**购买地块*/
    BuyLand,
    /**购买彩票*/
    BuyLottery,
    /**升级地块*/
    UpgradeLand
}

const val DEFAULT_LOTTERY_DURATION = 15
const val DEFAULT_LOTTERY_AWARDS = 5000
const val DEFAULT_LOTTERY_COST = 50
const val DEFAULT_SELL_COST:Double = 0.8
const val DEFAULT_REPASS_AWARDS = 2000
const val DEFAULT_LAND_BUILD_STRING = "1800,300|2000,500|3200,1500|4000,2200"
const val DEFAULT_STATION_ARG = "300,500,800,1200"
const val DEFAULT_STATION_PRICE = 2000
const val DEFAULT_START_MONEY = 30000

val POSITIVE_ANSWER = listOf<String>("是","Y","y","yes")
val NEGATIVE_ANSWER = listOf<String>("否","N","n","no")