<?php
$tcode = intval(file_get_contents("act_code.num"));
?>
    <!DOCTYPE html>
    <html>
    <head>
        <title>UKIKU updater</title>
    </head>
    <body>
    <form enctype="multipart/form-data" action="upload_apk.php" method="POST">
        <p>Update UKIKU</p>
        Código: <input type="number" name="code" value="<?PHP echo $tcode; ?>" min="<?PHP echo $tcode; ?>" title="Código: " required><br/>
        APK: <input type="file" name="uploaded_file" required/><br/><br/>
        <input type="submit" value="Upload"/>
    </form>
    </body>
    </html>
<?PHP
if (!empty($_FILES['uploaded_file']) and !empty($_POST['code'])) {
    $code = file_get_contents('act_code.num');
    $ncode = $_POST['code'];
    if ($code === $ncode) {
        echo "Same code!!!!";
    } elseif (intval($code) >= intval($ncode)) {
        echo "Code error!!!";
    } elseif (pathinfo($_FILES['uploaded_file']['name'], PATHINFO_EXTENSION) !== 'apk') {
        echo "No APK!!!!";
    } else {
        $path = basename($_FILES['uploaded_file']['name']);
        if (move_uploaded_file($_FILES['uploaded_file']['tmp_name'], $path)) {
            file_put_contents('act_code.num', $ncode);
            echo "The file " . basename($_FILES['uploaded_file']['name']) .
                " has been uploaded";
        } else {
            echo "There was an error uploading the file, please try again!";
        }
    }
}
?>