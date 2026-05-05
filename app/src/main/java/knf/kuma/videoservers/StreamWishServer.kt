package knf.kuma.videoservers

import android.content.Context
import androidx.core.net.toUri
import knf.kuma.commons.PatternUtil
import knf.kuma.commons.jsoupCookies
import knf.kuma.videoservers.VideoServer.Names.STREAMWISH
import kotlinx.coroutines.runBlocking

class StreamWishServer internal constructor(context: Context, baseLink: String) : Server(context, baseLink) {

    override val isValid: Boolean
        get() = baseLink.contains("streamwish.to")

    override val name: String
        get() = STREAMWISH

    override val canStream: Boolean
        get() = true

    override val canDownload: Boolean
        get() = false

    override val videoServer: VideoServer?
        get() {
            return try {
                val downLink = "https://sfastwish.com/e/${PatternUtil.extractLink(baseLink).substringAfterLast("/")}"
                for (i in 0..3) {
                    val unpack = if (i >= 2) {
                        runBlocking { Unpacker.unpackWeb(context, downLink) }
                    } else {
                        runBlocking { Unpacker.unpack(downLink) }
                    }
                    val host = unpack.url!!.toUri().let { it.scheme + "://" + it.host }
                    val options = "hls\\d\": ?\"([^\"]*)".toRegex().findAll(unpack.unpacked).toList().reversed().mapIndexed { index, it ->
                        val (link) = it.destructured
                        Option(name, "HLS${index + 1}", if (link.startsWith("http")) link else host + link)
                    }.toMutableList()
                    val selected = options.firstOrNull { option ->
                        jsoupCookies(option.url)
                            .ignoreContentType(true)
                            .ignoreHttpErrors(true)
                            .execute().let { it.statusCode() in (200..299) && it.body().startsWith("#") }
                    }
                    if (selected == null) continue
                    return VideoServer(name, selected)
                }
                null
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }
}

/**
 *var uas=[];var links={"hls4":"/stream/c9o0NWswavouc9hYw-PUmQ/kjhhiuahiuhgihdf/1760171834/61398616/master.m3u8","hls3":"https://18jij6rntf.gridsoftwareworks.space/6wyav2r9i6j/hls3/01/12279/01msn9tjtg1f_,l,n,.urlset/master.txt","hls2":"https://18jij6rntf.premilkyway.com/hls2/01/12279/01msn9tjtg1f_,l,n,.urlset/master.m3u8?t=xOtUwpn24RvVVXZO5nikf9bfUEoV-A2S0A296hj2LgY&s=1760128634&e=129600&f=61398616&srv=6wyav2r9i6j&i=0.4&sp=500&p1=6wyav2r9i6j&p2=6wyav2r9i6j&asn=17072"};jwplayer("vplayer").setup({debug:"1",sources:[{file:links.hls4||links.hls3||links.hls2,type:"hls"}],image:"https://pixoraa.cc/01msn9tjtg1f_xt.jpg",width:"100%",height:"100%",stretching:"uniform",duration:"1419.86",preload:'auto',skin:{controlbar:{text:"#1db0ef",icons:"#1db0ef"},timeslider:{progress:"#1db0ef"},menus:{text:"#1db0ef"}},androidhls:"true",tracks:[{file:"/dl?op=get_slides&length=1420&url=https://pixoraa.cc/01msn9tjtg1f0000.jpg",kind:"thumbnails"}],captions:{userFontScale:1,color:'#FFFFFF',backgroundColor:'#FFFFFF',fontFamily:"Tahoma",backgroundOpacity:0,fontOpacity:'100',},"advertising":{"client":"vast","vpaidmode":"insecure"},'qualityLabels':{"1122":"720p","532":"480p"},abouttext:"StreamHG",aboutlink:"https://streamhg.com",logo:{file:"/upload-data/logo_402.png",link:"https://www3.animeflv.net",position:"top-right",margin:"5",hide:true},cast:{},playbackRateControls:true,playbackRates:[0.25,0.5,0.75,1,1.25,1.5,2]});var vvplay,vvad;var vastdone1=0,vastdone2=0,pop3done=0;var player=jwplayer();var prevt=0,tott=0,v2done=0,lastt=0;$.ajaxSetup({headers:{'Content-Cache':'no-cache'}});player.on('time',function(x){if(5>0&&x.position>=5&&vvad!=1){vvad=1;$('div.video_ad_fadein').fadeIn('slow')}var itads=0;uas.forEach(item=>{if(item.time<=x.position&&item.loaded==0){if(item.xtype=='vast'){if(item.link.startsWith('https://')){player.playAd(item.link)}else{var doc=new DOMParser().parseFromString(item.link,"text/html");item.link="data:application/xml;base64,"+btoa(unescape(encodeURIComponent(doc.documentElement.textContent)));player.playAd(item.link)}}else if(item.xtype=='direct'){pickDirect(itads,item.link)}else{var code=item.link.trim();var script=document.createElement('script');if(code.startsWith('https://')){script.src=code;script.async=true}else{var doc=new DOMParser().parseFromString(code,"text/html");code=doc.documentElement.textContent;var match=code.match(/<script[^>]*>([\s\S]*?)<\/script>/i);if(match){script.textContent=match[1]}else{script.textContent=code}}document.body.appendChild(script)}item.loaded=1}itads++});if(x.position>=lastt+5||x.position<lastt){lastt=x.position;ls.set('tt01msn9tjtg1f',Math.round(lastt),{ttl:60*60*24*7})}if(x.viewable){dt=x.position-prevt;if(dt>5)dt=1;tott+=dt}prevt=x.position;console.log(tott);if(tott>=60){$.post('https://logs.vectorrab.com/dl',{op:'view4',hash:'61398616-189-203-1760128633-498649b34b00770777c2f7b5775f6534',ss:parseInt(tott),file_id:61398616,file_real:'01msn9tjtg1f'},function(){},"jsonp");tott=0}if(pop3done!=1&&x.position>=1){pop3done=1;var a=document.createElement('script');a.src='/assets/jquery/p3anime.js?v=1.1';document.body.appendChild(a)}});player.on('seek',function(x){prevt=x.position});player.on('play',function(x){doPlay(x)});player.on('complete',function(){$('div.video_ad').show();ls.remove('tt01msn9tjtg1f')});player.on('pause',function(x){});function createCookieSec(name,value,sec){var date=new Date();date.setTime(date.getTime()+(sec*1000));document.cookie=name+"="+value+"; expires="+date.toGMTString()+"; domain=.auvexiug.com; path=/; SameSite=None; Secure"}function doPlay(x){$('div.video_ad').hide();$('#over_player_msg').hide();if(vvplay)return;vvplay=1;adb=0;const ggima=document.createElement('script');ggima.src='https://imasdk.googleapis.com/js/sdkloader/ima3.js';ggima.onerror=()=>{$.get('/dl?op=view&file_code=01msn9tjtg1f&hash=61398616-189-203-1760128633-498649b34b00770777c2f7b5775f6534&embed=1&referer=tryzendm.com&adb=1&hls4=1',function(data){$('#fviews').html(data)})};ggima.onload=()=>{$.get('/dl?op=view&file_code=01msn9tjtg1f&hash=61398616-189-203-1760128633-49
 */
