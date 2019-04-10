<?php
$title = $_GET["title"];
$link = $_GET["url"];
$url = "https://web.facebook.com/plugins/comments.php?api_key=156149244424100&channel_url=https%3A%2F%2Fstaticxx.facebook.com%2Fconnect%2Fxd_arbiter%2Fr%2FlY4eZXm_YWu.js%3Fversion%3D42%23cb%3Df3448d0a8b0514c%26domain%3Danimeflv.net%26origin%3Dhttps%253A%252F%252Fanimeflv.net%252Ff304e603e6a096%26relation%3Dparent.parent&href=" . urlencode($_GET["url"]) . "&locale=es_LA&numposts=50&sdk=joey&version=v2.3";
echo "<!DOCTYPE html>
<html>
<head>
<title>$title</title>
<meta http-equiv='X-UA-Compatible' content='IE=edge'>
<meta name='viewport' content='width=device-width, initial-scale=1'>
</head>
<body>
<div id='fb-root'></div>
<script>(function(d, s, id) {
  var js, fjs = d.getElementsByTagName(s)[0];
  if (d.getElementById(id)) return;
  js = d.createElement(s); js.id = id;
  js.src = 'https://connect.facebook.net/es_LA/sdk.js#xfbml=1&version=v3.1&appId=1730508916998105&autoLogAppEvents=1';
  fjs.parentNode.insertBefore(js, fjs);
}(document, 'script', 'facebook-jssdk'));</script>

<div class='fb-comments' data-href='$link' style='width:100%;' width='100%' data-numposts='50'></div>
</body>
</html>";
