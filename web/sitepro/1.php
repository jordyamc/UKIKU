<!DOCTYPE html>
<html lang="en">
<head>
	<meta http-equiv="content-type" content="text/html; charset=utf-8" />
	<title>UKIKU - Página Oficial</title>
	<base href="{{base_url}}" />
			<meta name="viewport" content="width=device-width, initial-scale=1" />
		<meta name="description" content="App para ver y descargar anime" />
	<meta name="keywords" content="anime descargar gratis online app sin anuncios premium ovas pelicula dragon ball one piece" />
	<!-- Facebook Open Graph -->
	<meta name="og:title" content="UKIKU - Página Oficial" />
	<meta name="og:description" content="App para ver y descargar anime" />
	<meta name="og:image" content="{{base_url}}gallery_gen/500166f32a78d6a767eff2182d492e63.png" />
	<meta name="og:type" content="article" />
	<meta name="og:url" content="{{curr_url}}" />
	<!-- Facebook Open Graph end -->
		
	<link href="css/bootstrap.min.css" rel="stylesheet" type="text/css" />
	<script src="js/jquery-1.11.3.min.js" type="text/javascript"></script>
	<script src="js/bootstrap.min.js" type="text/javascript"></script>
	<script src="js/main.js?v=20171219130823" type="text/javascript"></script>

	<link href="css/font-awesome/font-awesome.min.css?v=4.7.0" rel="stylesheet" type="text/css" />
	<link href="css/site.css?v=20180117130634" rel="stylesheet" type="text/css" />
	<link href="css/common.css?ts=1516928414" rel="stylesheet" type="text/css" />
	<link href="css/1.css?ts=1516928414" rel="stylesheet" type="text/css" />
	{{ga_code}}<link rel="shortcut icon" href="/gallery/ic_launcher-web-ts1516911373.png" type="image/png" />
	<script type="text/javascript">var currLang = '';</script>	
	<!--[if lt IE 9]>
	<script src="js/html5shiv.min.js"></script>
	<![endif]-->
</head>


<body><div class="root"><div class="vbox wb_container" id="wb_header">
	
<div class="wb_cont_inner"><div id="wb_element_instance0" class="wb_element" style=" line-height: normal;"><h4 class="wb-stl-pagetitle"><span class="wb_tr_ok">UKIKU</span></h4>
</div><div id="wb_element_instance2" class="wb_element wb_element_picture"><img alt="gallery/ic_launcher-web" src="gallery_gen/426d03e067942f73910b0ad925811791_67x67.png"></div></div><div class="wb_cont_outer"></div><div class="wb_cont_bg"></div></div>
<div class="vbox wb_container" id="wb_main">
	
<div class="wb_cont_inner"><div id="wb_element_instance3" class="wb_element" style=" line-height: normal;"><h1 class="wb-stl-heading1"><span class="wb_tr_ok">DESCARGA Y STREAMING DE ANIME</span></h1>
</div><div id="wb_element_instance4" class="wb_element wb-elm-orient-horizontal"><div class="wb-elm-line"></div></div><div id="wb_element_instance5" class="wb_element wb_element_picture"><img alt="gallery/ukiku-app" src="gallery_gen/500166f32a78d6a767eff2182d492e63_400x690.png"></div><div id="wb_element_instance6" class="wb_element"><a class="wb_button" href="get.php"><span>Descargar</span></a></div><div id="wb_element_instance8" class="wb_element" style="width: 100%;">
			<?php
				global $show_comments;
				if (isset($show_comments) && $show_comments) {
					renderComments(1);
			?>
			<script type="text/javascript">
				$(function() {
					var block = $("#wb_element_instance8");
					var comments = block.children(".wb_comments").eq(0);
					var contentBlock = $("#wb_main");
					contentBlock.height(contentBlock.height() + comments.height());
				});
			</script>
			<?php
				} else {
			?>
			<script type="text/javascript">
				$(function() {
					$("#wb_element_instance8").hide();
				});
			</script>
			<?php
				}
			?>
			</div></div><div class="wb_cont_outer"></div><div class="wb_cont_bg"></div></div>
<div class="vbox wb_container" id="wb_footer">
	
<div class="wb_cont_inner" style="height: 134px;"><div id="wb_element_instance1" class="wb_element" style=" line-height: normal;"><p class="wb-stl-footer">© 2018 <a data-name="http://ukiku.ga" href="http://ukiku.ga">ukiku.ga</a></p>
</div><div id="wb_element_instance9" class="wb_element" style="text-align: center; width: 100%;"><div class="wb_footer"></div><script type="text/javascript">
			$(function() {
				var footer = $(".wb_footer");
				var html = (footer.html() + "").replace(/^\s+|\s+$/g, "");
				if (!html) {
					footer.parent().remove();
					footer = $("#wb_footer, #wb_footer .wb_cont_inner");
					footer.css({height: ""});
				}
			});
			</script></div></div><div class="wb_cont_outer"></div><div class="wb_cont_bg"></div></div><div class="wb_sbg"></div></div>{{hr_out}}</body>
</html>
