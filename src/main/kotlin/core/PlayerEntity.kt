package org.hezisudio.core

/** 玩家实体类 */
class PlayerEntity(val id:Long) {
    var statue:PlayerStatue = PlayerStatue.Waiting
    var money = 0
    val lotteries:ArrayList<LotteryHold> = arrayListOf()
    val lands:ArrayList<LandHold> = arrayListOf()
    var position:Int = 0
    val session:Session = Session()
    class LotteryHold(
        val lotteryID:Int,
    )

    var freezeLeft = 0

    class LandHold(
        val landPos:Int,
    )

    data class Session(var content:String = ""){
        var statue: SubStatue = SubStatue.Null
    }

    /**玩家是否包含子操作*/
    fun hasSubContent():Boolean{
        return session.statue != SubStatue.Null
    }
    /**清除玩家购买的彩票*/
    fun clearLottery(){
        lotteries.clear()
    }

}