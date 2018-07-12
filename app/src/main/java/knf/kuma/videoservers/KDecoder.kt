package knf.kuma.videoservers

class KDecoder {
    companion object {
        fun decodeMango(url: String, mask: Int): String? {
            val key = "=/+9876543210zyxwvutsrqponmlkjihgfedcbaZYXWVUTSRQPONMLKJIHGFEDCBA"
            val result = StringBuffer()
            val u = url.replace("[^A-Za-z0-9\\+\\/\\=]".toRegex(), "")
            var idx = 0
            while (idx < u.length) {
                val a = key.indexOf(u.substring(idx, idx + 1))
                idx++
                val b = key.indexOf(u.substring(idx, idx + 1))
                idx++
                val c = key.indexOf(u.substring(idx, idx + 1))
                idx++
                val d = key.indexOf(u.substring(idx, idx + 1))
                idx++
                val s1 = a shl 0x2 or (b shr 0x4) xor mask
                result.append(Character.valueOf(s1.toChar()))
                val s2 = b and 0xf shl 0x4 or (c shr 0x2)
                if (c != 0x40) {
                    result.append(Character.valueOf(s2.toChar()))
                }
                val s3 = c and 0x3 shl 0x6 or d
                if (d != 0x40) {
                    result.append(Character.valueOf(s3.toChar()))
                }
            }
            return result.toString()
        }
    }
}