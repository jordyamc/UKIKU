<?php

/**
 * Publish progress verifier
 */
class VerifyUtil {
	
	/**
	 * Get folder file tree
	 * @param string $dir folder path to get tree from
	 * @param string $rel relative path
	 * @param string $sdir published site root folder name
	 * @return array
	 */
	public static function getFileTree($dir, $rel = null, $sdir = null) {
		$list = array();
		clearstatcache();
		if (is_dir($dir)) {
			if (($dh = opendir($dir.($rel ? ('/'.$rel) : '')))) {
				while (($file = readdir($dh)) !== false) {
					if ($file == '.' || $file == '..') { continue; }
					$rel_path = ($rel ? ($rel.'/') : '').$file;
					$path = $dir.'/'.$rel_path;
					if ($sdir && is_dir($path) && $file != $sdir) continue;
					$inf = (object) array(
						'type' => '-',
						'perms' => null,
						'user' => null,
						'group' => null,
						'size' => 0,
						'time' => null,
						'name' => $file,
						'path' => $rel_path
					);
					if (is_dir($path)) {
						$inf->type = 'd';
						$inf->time = date('c', filemtime($path));
						$list[$inf->path] = $inf;
						// do not pass $sdir here
						$list = array_merge($list, self::getFileTree($dir, $rel_path));
					} else {
						$stat = stat($path);
						$inf->user = $stat['uid'];
						$inf->group = $stat['gid'];
						$inf->size = $stat['size'];
						$inf->time = date('c', $stat['mtime']);
						$list[$inf->path] = $inf;
					}
				}
				closedir($dh);
			} else if (is_null($rel)) {
				// indicate that getting file tree failed
				return null;
			}
		}
		return $list;
	}
	
	public static function main() {
		header('Content-Type: application/json; charset=utf-8');
		@set_time_limit(0);
		echo json_encode(self::getFileTree(dirname(__DIR__), null, ((isset($_GET['sdir']) && $_GET['sdir']) ? $_GET['sdir'] : null)));
		@unlink(__FILE__);
		exit();
	}
	
}

if (!function_exists('json_encode') && file_exists(dirname(__FILE__).'/class.json.php')) {
	require(dirname(__FILE__).'/class.json.php');
	function json_encode($value) {
		$srvc = new Services_JSON();
		return $srvc->encode($value);
	}
}

VerifyUtil::main();
