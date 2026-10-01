package com.pixelody.app.modules
import org.junit.Assert.*
import org.junit.Test
class ModulePackageTest {
    private val valid = """{"format":1,"id":"pixelody.listening-notes","version":"1.0.0","kind":"listening-notes","platforms":["desktop","android"],"name":"Listening Notes","description":"Local notes","prompt":"Remember this listen"}"""
    @Test fun validPackage() { assertEquals("Listening Notes", ModulePackage.parse(valid.toByteArray()).getString("name")) }
    @Test fun rejectsCodeAndWrongPlatform() { for (text in listOf(valid.replace("\"format\":1", "\"script\":\"run\",\"format\":1"), valid.replace("android", "ios"), valid.replace("1.0.0", "2.0.0"))) { assertThrows(Exception::class.java) { ModulePackage.parse(text.toByteArray()) } } }
    @Test fun rejectsOversizeAndInvalidUtf8() { assertThrows(Exception::class.java) { ModulePackage.parse(ByteArray(8193)) }; assertThrows(Exception::class.java) { ModulePackage.parse(byteArrayOf(0xc3.toByte(), 0x28)) } }
}
