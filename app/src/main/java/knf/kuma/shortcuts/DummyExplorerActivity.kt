package knf.kuma.shortcuts

import knf.kuma.commons.DesignUtils

class DummyExplorerActivity : DummyActivity() {
    override val intentClass: Class<*> = DesignUtils.explorerClass
}