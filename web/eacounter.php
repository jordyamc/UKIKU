<?php
/**
 * Created by PhpStorm.
 * User: jordy
 * Date: 13/04/2018
 * Time: 06:16 PM
 */
if (isset($_GET['count'])){
    $count=intval(file_get_contents('eacount.txt'));
    http_response_code(200);
    $count++;
    echo $count;
    file_put_contents('eacount.txt',$count);
}else {
    http_response_code(403);
    echo "error";
}