package org.hezisudio.core.map

import org.hezisudio.core.DEFAULT_LAND_BUILD_STRING
import org.hezisudio.core.PlayerEntity

class Normal(
    buildArg:String,
    name:String,
    description:String
):Land(name, description) {
    /**地块数据包含购买/升级价格与对应购买/升级之后的过路费，默认三个等级0(无主状态)，1，2，3*/
    val buildData:ArrayList<Pair<Int,Int>> = arrayListOf()
    /**普通地块的等级
     *
     * 无人购买时[level]=0*/
    var level:Int = 0
    var owner:PlayerEntity? = null
    init {
        buildLevel(buildArg)
    }

    /**读取buildArg解析为buildData*/
    private fun buildLevel(buildArg:String){

        val args = buildArg.split("|")
        try {
            for (singleArg in args){
                val pair = singleArg.split(",")
                buildData.add(Pair(pair[0].toInt(),pair[1].toInt()))
            }
        }catch (e:Exception){
            println("错误的构建数据，已使用默认数据代替(错误数据位于：$name [Normal地块])")
            buildData.clear() // 先清空buildData
            val newArgs = DEFAULT_LAND_BUILD_STRING.split("|")
            for (singleArg in newArgs){
                val pair = singleArg.split(",")
                buildData.add(Pair(pair[0].toInt(),pair[1].toInt()))
            }
        }
    }
    /**地图数据重置*/
    fun reset(){
        level = 0
        owner = null
    }
    /**返回地块数据字符串*/
    fun buildDataToString():String{
        val sb = StringBuilder()
        for (item in buildData){
            sb.append("${item.first},${item.second}")
            if (item != buildData.last()){
                sb.append('|')
            }
        }
        return sb.toString()
    }

    val topLevel:Boolean
        get() {
            return buildData.size-1 == level
        }



}