package com.pixelody.app.modules

import org.junit.Assert.*
import org.junit.Test

class WebShopModuleTest {
    private val manifest = """{"format":1,"id":"pixelody.revenuecat-shop","version":"1.0.0","kind":"web-shop","platforms":["desktop","android"],"name":"RevenueCat Shop","description":"Optional shop","entry":"https://pixelody-web.pixelody101.workers.dev/shop?embedded=1"}"""
    @Test fun acceptsShopWithoutNotebookPrompt() {
        assertEquals("web-shop", ModulePackage.parse(manifest.toByteArray()).getString("kind"))
    }
    @Test fun rejectsForeignEntryAndCode() {
        for (candidate in listOf(manifest.replace("pixelody-web.pixelody101.workers.dev", "evil.example"), manifest.replace("https:", "http:"), manifest.replace("\"format\":1", "\"script\":\"x\",\"format\":1"))) {
            assertThrows(Exception::class.java) { ModulePackage.parse(candidate.toByteArray()) }
        }
    }
    @Test fun navigationRestrictsOriginsSchemesAndCredentials() {
        assertTrue(ModulePackage.allowedShopUrl(ModulePackage.SHOP_ENTRY))
        assertTrue(ModulePackage.allowedShopUrl("https://pixelody-web.pixelody101.workers.dev/modules/notes.pixelody-module"))
        for (url in listOf("http://pixelody-web.pixelody101.workers.dev/shop", "https://pixelody-web.pixelody101.workers.dev.evil.example/shop", "https://evil.example", "javascript:alert(1)", "file:///data/data/", "intent://shop", "https://user@pixelody-web.pixelody101.workers.dev/shop", "https://pixelody-web.pixelody101.workers.dev:8443/shop")) assertFalse(url, ModulePackage.allowedShopUrl(url))
    }
}
