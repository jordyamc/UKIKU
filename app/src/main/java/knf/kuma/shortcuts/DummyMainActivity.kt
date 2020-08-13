package knf.kuma.shortcuts

import knf.kuma.commons.DesignUtils

class DummyMainActivity : DummyActivity() {
    override val intentClass: Class<*> = DesignUtils.mainClass
}