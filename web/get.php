<?php
/**
 * Created by PhpStorm.
 * User: Jordy
 * Date: 31/01/2018
 * Time: 05:39 PM
 */
if (is_old_android()) {
    echo "<h1>Lo sentimos, esta app solo esta disponible para Android 5 o superior :(</h1>";
} else {
    header("Content-disposition: attachment; filename=app-release.apk");
    header("Content-Type", "application/vnd.android.package-archive");
    readfile("updater/app-release.apk");
}

function is_old_android($version = '5.0')
{

    if (strstr($_SERVER['HTTP_USER_AGENT'], 'Android')) {

        preg_match('/Android (\d+(?:\.\d+)+)[;)]/', $_SERVER['HTTP_USER_AGENT'], $matches);

        return version_compare($matches[1], $version, '<=');

    }
    return false;
}
