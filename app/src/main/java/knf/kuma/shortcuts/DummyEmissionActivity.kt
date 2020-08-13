package knf.kuma.shortcuts

import knf.kuma.commons.DesignUtils

class DummyEmissionActivity : DummyActivity() {
    override val intentClass: Class<*> = DesignUtils.emissionClass
}