package org.hezisudio.core.chance

/**机会卡片*/
class ChanceCard(
    /**卡面名*/
    val title:String,
    /**机会卡描述，形容效果细节*/
    val description:String,
    /**操作，机会卡的操作*/
    mataString: String
){

    val chanceMatas:List<ChanceMata> = mataStringParse(mataString)
    /**机会操作元
     * @property obj 操作主体
     * @property act 操作
     * @property param 操作参数，默认-1，代表无参数*/
    class ChanceMata(
        val obj:MataTarget,
        val act:MataAction,
        val param:Long = -1L,
    ){
        override fun toString(): String {
            return """
                ${this.obj},${this.act},${this.param}
            """.trimIndent()
        }
    }

    /**操作元行为，有修改可能*/
    enum class MataAction{
        /**移动位置*/
        Move,
        /**失去金钱*/
        MoneyOut,
        /**获得金钱*/
        MoneyIn,
        /**升级土地*/
        UpdateLand,
        /**失去金钱根据土地等级*/
        MoneyOutByLandLevel,
        /**获得金钱根据土地等级*/
        MoneyInByLandLevel,
        /**交钱给其他玩家，主体只能是Self*/
        MoneyOutToOthers,
        /**向其他玩家收钱，主体只能是Self*/
        MoneyInFromOthers,
        /**冻结行动*/
        Freeze,
    }

    /**操作元对象, 限定一个操作的操作对象，有增加或修改的可能*/
    enum class MataTarget(val description:String){
        Self("玩家本身"),
        Someone("除玩家外的某人"),
        Others("除玩家外的所有人"),
        All("所有存活玩家"),
        LandMan("土地块数最多的玩家, 若有多个则取多个"),
        LandlessMan("土地块数最少的玩家, 若有多个则取多个"),
        RichMan("现金最多的玩家"),
        PoorMan("现金最少的玩家")
    }

    /**将一个操作元描述字符串转换为对象
     * @throws ParseErrorException*/
    private fun mataParse(singleMataStr:String):ChanceMata{
        val mata = singleMataStr.split(",")
        val t = try{
            MataTarget.valueOf(mata[0])
        }catch (e:Exception){
            throw ParseErrorException("无法解析：‘${mata[0]}’为操作元对象")
        }
        val a = try{
            MataAction.valueOf(mata[1])
        }catch (e:Exception){
            throw ParseErrorException("无法解析：‘${mata[1]}’为操作行为")
        }
        val arg = try{ mata[2].toLong() }catch (e:Exception){
            throw ParseErrorException("无法解析：‘${mata[1]}’为操作行为参数(需要整数)")
        }
        return ChanceMata(t,a,arg)
    }

    /**转换描述文本为操作列表
     * @throws ParseErrorException*/
    private fun mataStringParse(str:String):List<ChanceMata>{
        val al = arrayListOf<ChanceMata>()
        val mataList = str.split("|")
        mataList.forEach {
            al.add(mataParse(it))
        }
        return al
    }

    /**用于转换出错时抛出异常*/
    class ParseErrorException(override val message: String?):Throwable(){

    }
}