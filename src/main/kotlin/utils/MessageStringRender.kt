package org.hezisudio.utils

import net.mamoe.mirai.contact.Group
import net.mamoe.mirai.contact.remarkOrNameCardOrNick

object MessageStringRender {
    fun render(origin: String, group:Group):String{
        val replacedGroup = regexOfGroup.replace(origin){
            val match = it.value
            val groupID = match.substring(2,match.length-1).toLong()
            if(group.id == groupID){
                group.name
            }else{
                "【群聊$groupID】"
            }
        }

        regexOfMember.replace(replacedGroup){
            val match = it.value
            val memberID = match.substring(2,match.length-1).toLong()
            group.members[memberID]?.remarkOrNameCardOrNick?:"【群员$memberID】"
        }

        return origin.replace(regexOfMember,"").replace(regexOfGroup,"")
    }

    private val regexOfMember = Regex("""\[m\d+]""")
    private val regexOfGroup = Regex("""\[g\d+]""")

}