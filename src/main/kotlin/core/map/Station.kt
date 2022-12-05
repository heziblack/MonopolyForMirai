package org.hezisudio.core.map

import org.hezisudio.core.DEFAULT_STATION_PRICE
import org.hezisudio.core.PlayerEntity

class Station(landArg:String,name:String,descr:String,val price:Int = DEFAULT_STATION_PRICE):Land(name,descr) {
    val stationData:ArrayList<Int> = arrayListOf()
    var owner: PlayerEntity? = null
    init {
        parseStationData(landArg)
    }
    private fun parseStationData(str:String){
        val dataArray = str.split(",")
        try{
            for (item in dataArray){
                stationData.add(item.toInt())
            }
        }catch (e:Exception){
            println("错误的构建数据，已使用默认数据代替(错误数据位于：$name [Station地块])")
            stationData.clear()
            stationData.addAll(listOf(300,500,800,1200))
        }
    }

    fun stationDataToString():String{
        val sb = StringBuilder()
        for (d in stationData){
            sb.append(d)
            if (d!=stationData.last()){
                sb.append(",")
            }
        }
        return sb.toString()
    }

    fun reset(){
        owner = null
    }

}