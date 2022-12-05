package org.hezisudio.core.map

import io.github.evanrupert.excelkt.Sheet
import io.github.evanrupert.excelkt.workbook
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import org.hezisudio.core.DEFAULT_STATION_ARG
import org.hezisudio.core.chance.ChanceCard
import java.io.File

class GameMap(private val sourceFile: File?=null): ArrayList<Land>() {
    /**机会池，保存本地图的附带机会卡堆*/
    val chancePool:ArrayList<ChanceCard> = arrayListOf()
    val name:String
    init {
        if (sourceFile != null){
            load()
            name = sourceFile.name.replace(".xlsx","")
        }else{
            generateDefaultMap()
            name = "默认地图"
        }
    }
    /**根据[sourceFile]加载地图以及地图所有的机会池*/
    private fun load(){
        sourceFile!!
        val workBook = XSSFWorkbook(sourceFile)
        val mapSheet = workBook.getSheet("地图")
        val chanceSheet = workBook.getSheet("机会卡")
        mapSheet.forEach {
            if(it.rowNum == 0) return@forEach
            val landType = it.getCell(1).stringCellValue
            val name = it.getCell(2).stringCellValue
            val description = it.getCell(3).stringCellValue
            when(landType){
                "Normal"->{
                    val buildArg = it.getCell(4).stringCellValue
                    this.add(Normal(buildArg,name, description))
                }
                "Station"->{
                    val arg = it.getCell(4).stringCellValue
                    val price = it.getCell(5).numericCellValue.toInt()
                    this.add(Station(arg,name,description,price))
                }
                "Chance"->{
                    this.add(Chance(name,description))
                }
                "Lottery"->{
                    this.add(Lottery(name,description))
                }
                "Prison"->{
                    this.add(Prison(name,description))
                }
                "StartPoint"->{
                    this.add(StartPoint(name,description))
                }
                else->{
                    throw Exception("未知地图类型：$landType")
                }
            }
        }
        chanceSheet.forEach {
            if (it.rowNum==0) return@forEach
            val title = it.getCell(0).stringCellValue
            val description = it.getCell(1).stringCellValue
            val action = it.getCell(2).stringCellValue
            chancePool.add(ChanceCard(title,description,action))
        }
        workBook.close()
    }
    /**创建默认地图*/
    private fun generateDefaultMap(){
        val defaultLandList:List<Land> = listOf(
            StartPoint("起点","梦想开始的地方，经过此处将获得奖励"),
            Normal("600,100|500,200|500,500|500,900","中山东路","中山东路(Zhongshan Donglu)位于松江镇东部。东起环城路，西迄通波塘接中山中路。长1.5公里，宽36米，车行道宽20米。民国初名城中大街、府前街。民国26年(1937年)八一三毁于日机轰炸。后重筑。民国28年改名中山路。1973年改名中山东路。沿路有上海照相机总厂和中学、新村、方塔园等。是松江镇东西向主干道之一。"),
            Chance("机会","一个明智的人总是抓住机遇,把它变成美好的未来。  ——托·富勒"),
            Normal("1000,100|500,300|500,900|500,2700","淮海西路","淮海西路于民国14年(1925年)法公董局越界辟筑，以原英国驻华公使名命名乔敦路(Jordan Road)。20世纪30年代改名陆家路。民国32年改名庐山路，民国34年以曾任国民政府主席林森命名林森西路。1950年为纪念淮海战役胜利改今名。沿路多工厂，有交通大学、胸科医院等。"),
            Lottery("彩票站","机会无限，欢乐无穷~欢迎购买大富翁彩票！"),
            Station(DEFAULT_STATION_ARG,"浦东机场","上海浦东国际机场于1999年建成，1999年9月16日一期工程建成通航，2005年3月17日第二跑道正式启用，2008年3月26日第二航站楼及第三跑道正式通航启用，2015年3月28日第四跑道正式启用。"),
            Normal("1600,200|1000,600|1000,1800|1000,5000","延安西路","延安西路是中国上海市的一条交通干道，位于长宁区，东西走向，东到华山路，西到虹桥国际机场，上面则建设了双向6车道的延安高架路，连接市内高速公路。延安路高架西段由内环线高架起，西行经过虹桥及古北开发区，连接虹桥国际机场，全长6.2公里，于1995年11月28日动工，1996年12月2日通车。共有8个出入口，在虹桥机场亦建有专用出入口。全线高架路两侧均种植植物以作绿化。和上海多数高架路不同，延安路地面道路不设自行车道，也没有设置自行车穿越设施，成为附近道路上自行车通行的障碍。"),
            Chance("机会","君子藏器于身,待时而动。  ——佚名"),
            Normal("2000,200|1000,800|1000,2200|1000,6000","衡山路","衡山路始建于1922年，曾与上海市著名的荣乐东路、南京东路齐名，由法公董局修筑，是法租界著名的贝当路，1943年10月更名为衡山路，整条街全长2.3公里。衡山路南接商业中心徐家汇，北邻时尚购物街淮海路――是两大繁华区域间的幽静而高雅的通道。衡山路紧临领馆区，是上海的交通主干道之一，地铁一号线在衡山路上设有站点，道路两旁繁茂的法国梧桐和林荫中颇具特色的各类高档欧陆建筑，为衡山路增添了浓郁的异国文化气息。"),
            Prison("监狱","任何邪恶，终将绳之以法！"),
            Normal("2200,180|1500,900|1500,2500|1500,7000","复兴路","复兴路位于青浦区，北邻朱家角中心镇区，东邻朱家角工业园区（北起G318公路，南至沈砖公路） ，复兴路（318国道-沈砖公路）新建工程位于青浦区朱家角镇，工程北起318国道（沪青平公路），南至沈砖公路，沿线上跨G50高速公路，与规划沈青公路和沈砖公路相交。起点桩号K0+000，终点桩号K2+673.96，全长2673.96m，红线宽度35m，含新建桥梁5座，改建桥梁2座。"),
            Station(DEFAULT_STATION_ARG,"电力公司","1832年制成第一座发电机，引发堪称第二次工业革命的技术革命。自第一座发电厂于公元1875年建成，电气工业即蓬勃发展。"),
            Normal("2400,200|1500,1000|1500,3000|1500,7500","浦东大道","浦东大道是位于上海市的一条道路，是浦东北部沿江地区的交通主干线，一头连着陆家嘴金融城，一头连着外高桥保税区。它不仅仅是一条地面通道同时，它和轨交14号线、包括东西通道同步建设，这也是上海近年来施工规模最大、难度最高的综合交通系统工程。"),
            Chance("命运","命里如有终须有，命里若无莫怨天。好事总得善人做，哪有凡人做神仙？"),
            Normal("2600,300|1500,1100|1500,3300|1500,8000","世纪大道","世纪大道，曾名中央大道，从东方明珠至浦东世纪公园全长约5.5公里，宽100米。西起东方明珠，陆家嘴环岛，东至上海浦东新区行政文化中心，被誉为“东方的香榭丽舍大街”。"),
            Prison("停车场","无论再忙，你都要给自己留一点时间去与该相遇的人慢慢相遇，去看看新的风景，多去享受世界的美好和乐趣。"),
            Normal("2200,200|1500,900|1500,2500|1500,7000","滨江大道","滨江大道位于上海市浦东新区，1997年建成，全长2500米，从泰东路沿黄浦江一直到东昌路，与浦西外滩隔江相望，是集观光、绿化、交通及服务设施为一体的沿江景观工程。它由亲水平台、坡地绿化、半地下厢体及景观道路等组成。凭栏临江，浦东两岸百舸争流，和外滩万国博览建筑群的动与静的结合，给人们无限的遐想，有一种移步拾景的意境，它犹如一条彩带飘落在黄浦江的东岸，被人们赞誉为浦东的新外滩。"),
            Normal("2400,200|1500,1000|1500,3500|1500,7000","外白渡桥","外白渡桥（Garden Bridge of Shanghai）是中国上海市境内连接黄浦区与虹口区的过河通道，位于苏州河汇入黄浦江口附近，是中国的第一座全钢结构铆接桥梁和仅存的不等高桁架结构桥梁，也是上海市优秀历史保护建筑。1856年，第一代外白渡桥建成，名为“威尔斯桥”。1876年，第二代外白渡桥建成，定名为“公园桥”。1907年，外白渡桥建成并沿用至今。外白渡桥南起于南苏州路，北止北苏州路，整桥长104.24米，桥面为三车道城市主干道，设计速度40千米/小时。"),
            Chance("命运","没有准备向命运抗争，命运便会显示其威力。 ——马基雅弗利"),
            Normal("3000,300|2000,1300|2000,3900|2000,9000","陆家嘴路","陆家嘴路(Lujiazui Lu)。位于上海市浦东新区西北部。西起陆家嘴轮渡站，东至浦东南路。长1567米。"),
            Station(DEFAULT_STATION_ARG,"自来水厂","自来水厂指具有一定生产设备，能完成自来水整个生产过程，水质符合一般生产用水和生活用水要求，并可作为公司（厂）内部一级核算的生产单位。"),
            Chance("机会","速则济，缓则不急，此圣贤所以贵机会也。 ——苏轼"),
            Station(DEFAULT_STATION_ARG,"虹桥机场","上海虹桥国际机场始建于1921年，于1950年重建；1971年由军民合用改为民航专用；2010年启用2号航站楼及第二跑道；2014年底启动1号航站楼改造及东交通中心工程。"),
            Normal("3600,400|2000,1800|2000,5000|2000,10000","徐家汇路","徐家汇曾经是上海中心城区内的四大城市副中心之一，现在是上海中央活动区之一，同时亦为上海十大商业中心之一，东起宛平路，西至宜山路，北起广元路，南至零陵路，占地面积4.04平方公里。"),
            Prison("拘留所","失足未必千古恨,今朝立志做新人"),
            Normal("3200,300|2000,1500|2000,4500|2000,10000","外滩","外滩全长1.5千米，南起延安东路，北至苏州河上的外白渡桥，东面即黄浦江，西面是旧上海金融、外贸机构的集中地。上海辟为商埠以后，外国的银行、商行、总会、报社开始在此云集，外滩成为全国乃至远东的金融中心。民国三十二年（1943年）8月，外滩随交还上海公共租界于汪伪国民政府，结束长达百年的租界时期，于民国三十四年（1945年）拥有正式路名中山东一路。"),
            Lottery("彩票站","机会无限，欢乐无穷~欢迎购买大富翁彩票！"),
            Normal("2200,200|1500,900|1500,2500|1500,7000","沪宁高速公路","宁沪高速公路是连接中国上海市与江苏省省会南京市之间的一条重要高速公路干线。其全线均为中国国家高速公路网G42国道的组成部分，无锡 枢纽以东亦为中国国家高速公路网G2国道的组成部分。其不仅是南京至上海区域内的重要陆路通道，而且从中国北部、中西部进入长江三角洲的流量均汇集于此。该路已成为中国大陆最繁忙的公路之一，自开通以来交通流量以每年平均 15% 左右的速度增长。"),
            Chance("机会","只有不断寻找机会的人才会及时把握机会"),
            Normal("1400,100|1000,500|1000,1500|1500,4500","南京路","南京路步行街（Nanjing Road Walkway），位于上海市黄浦区境内，西起西藏中路，东至中山东一路外滩，全长1033米，路幅宽18—28米，总用地约3万平方米，建成于1999年9月20日。南京路步行街采用不对称的布置形式，以4.2米宽的“金带”为主线，贯穿于整条步行街中，“金带”上集中布置城市公共设施，如坐椅、购物亭、问讯亭、广告牌、雕塑小品、路灯、废物箱、电话亭等，并设有34个造型各异的花坛。")
        )
        this.addAll(defaultLandList)
        val defaultChancePool:List<ChanceCard> = listOf(
            ChanceCard("上海话演讲比赛第一名","现今的上海话是多种吴语方言混杂和融合而成，继承了老上海话的基本特色和吴语的特征，与苏州话同为当代吴语的代表方言。获得600奖金","Self,MoneyIn,600"),
            ChanceCard("搭乘京沪高铁，从北京到直达上海虹桥机场","京沪高铁，是中国一条链接北京市与上海市的高速铁路。预计全程运行时间只需4小时。","Self,Move,22"),
            ChanceCard("跟路边摊贩购买来路不明的活鸡","上海工商局为避免疫情扩散，规定除市政府规定的市场外，不得从事任何活禽牲畜交易。每个房产等级罚款300元","Self,MoneyInByLandLevel,300"),
            ChanceCard("到徐家汇天主教堂望弥撒","徐家汇天主教堂位于上海市繁华热闹的徐汇区，是上海第一座天主教堂。悠久的历史以及哥特式的建筑风格是其最重要的特色。免费升级一处房产","Self,UpdateLand,1"),
            ChanceCard("到上海参观世界博览会场馆","给每人100元作为观光费","Self,MoneyOutToOthers,100"),
            ChanceCard("参观黄浦区的历史遗迹城隍庙","上海城隍庙建于明永乐年间，至今已有600余年历史，是上海著名道教宫观，休息一回合","Self,Freeze,1"),
            ChanceCard("到上海水族馆照顾沙虎鲨，打工赚钱","上海水族馆拥有155米海底隧道，来自世界各地的海洋生物和濒临灭绝的稀有海底生物，是上海新景点之一。获得800元","Self,MoneyIn,800"),
            ChanceCard("获得《人口贡献光荣证》","为了鼓励生育，上海市政府特地颁发此证明并奖励多生多育的家庭。奖金1000元","Self,MoneyIn,1000"),
            ChanceCard("到上海大剧院表演京剧","上海拥有中国最丰富的西区资源。在上海可以欣赏到几乎所有地方的戏曲演出，而各类戏曲的大师也多活跃于上海。获得700元","Self,MoneyIn,700"),
            ChanceCard("到黄浦江参观东方明珠塔","东方明珠广播电视塔，又名东方明珠塔，是一座位于上海的电视塔也是亚洲第一高塔，目前以旅游观光和广播电视为主。花费700元","Self,MoneyOut,700"),
            ChanceCard("到中国最高的大酒店吃午茶","金茂大厦于1997年开张营业，塔楼高420.5米，是中国传统建筑风格与世界高新技术的完美结合。花费400元","Self,MoneyOut,400"),
            ChanceCard("到上海美术馆参观国家级的艺术品","本馆为1930年代英国新古典主义风格的历史建筑，集典藏、研究、陈列于一体的艺术殿堂，更是一个艺术教育普及的大课堂。花费500","Self,MoneyOut,500"),
            ChanceCard("参与上海方舱医院的建设工作","为了应对日趋严峻的防疫形式，政府招标修建放舱医院。免费升级一处地块","Self,UpdateLand,1"),
            ChanceCard("在上海路开店买奢侈品，生意兴隆","南京路和淮海路是上海最繁华的街区，素有“中华商业第一街”之誉，两侧商厦鳞次栉比，云集着非常多的商店。","Self,MoneyIn,600"),
            ChanceCard("遇到上海少见的大风雪，困在旅馆取暖","上海属于亚热带季风气候，四季分明，日照充分，雨量充沛，气候温和湿润。夏天和初秋容易受台风吹袭，冬天偶尔会下雪。玩家下回合暂停一次。","Self,Freeze,1"),
            ChanceCard("在搭乘公交车时不小心推挤到其他乘客","现金最多的家伙罚款1000","RichMan,MoneyOut,1000"),
            ChanceCard("因为太好吃了，一口气吃太多点心而肚子痛","上海有1800多家点心店，光早餐点心就有300多样，除此之外还有许多精心制作的特色小品，时上海饮食的一大特色。付600元看医生","Self,MoneyOut,600"),
            ChanceCard("从龙阳路搭乘磁悬浮列车回到起点","上海磁悬浮示范运营线，是世界上第一条投入商业运营化的磁悬浮列车示范线，最高运行速度为每小时430公里。回到起点并领取1000元","Self,Move,0|Self,MoneyIn,1000"),
            ChanceCard("成绩优异，考上复旦大学","复旦大学创建于1905年，是国家教育部直属高校，与北京大学、清华大学同被公认为中国最出色的高等学府。领取奖学金1000元","Self,MoneyIn,1000"),
            ChanceCard("在电线杆上张贴小广告","在上海，乱倒渣土垃圾、违法户外广告、扰民景观灯光、乱张贴、乱涂写、乱刻乱画等都是违法的，违反最多会被罚10万元。立刻坐牢","Self,Freeze,1|Self,Move,9"),
        )
        chancePool.addAll(defaultChancePool)
    }
    /**将地图输出到file文件*/
    fun toExcel(file:File){
        val father = this
        workbook {
            sheet("地图") {
                mapHeader()
                for ((index,land) in father.withIndex()){
                    row {
                        cell(index)
                        cell(land::class.simpleName?:"Error")
                        cell(land.name)
                        cell(land.description)
                        when(land){
                            is Normal->{
                                cell(land.buildDataToString())
                            }
                            is Station->{
                                cell(land.stationDataToString())
                                cell(land.price)
                            }
                            else->{}
                        }
                    }
                }
            }
            sheet("机会卡") {
                chanceHeader()
                for (chance in chancePool){
                    row {
                        cell(chance.title)
                        cell(chance.description)
                        val sb = StringBuilder()
                        for (mata in chance.chanceMatas){
                            sb.append(mata)
                            if (mata!= chance.chanceMatas.last()){
                                sb.append('|')
                            }
                        }
                        cell(sb.toString())
                    }
                }
            }
        }.write(file.absolutePath)
    }
    /**获取一份本地图的副本，避免不同游戏同时读写同一份地图*/
    fun getCopy():GameMap{
        return GameMap(sourceFile)
    }
    /**地图标题行*/
    private fun Sheet.mapHeader(){
        val headerList = listOf<String>("序号","地块类型","地块名","地块介绍","生成参数1","生成参数2")
        row {
            for (header in headerList){
                cell(header)
            }
        }
    }
    /**机会标题行*/
    private fun Sheet.chanceHeader(){
        val headerList = listOf<String>("标题","说明","操作参数")
        row {
            for (header in headerList){
                cell(header)
            }
        }
    }
}