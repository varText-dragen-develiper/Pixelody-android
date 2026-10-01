package com.pixelody.app.modules

import android.content.Context
import android.util.AtomicFile
import org.json.JSONObject
import java.io.File

data class NotebookState(val installed: JSONObject? = null, val text: String = "", val shop: JSONObject? = null)
class ModuleStore(context: Context) {
    private val file = AtomicFile(File(context.filesDir, "modules/listening-notes.json"))
    @Synchronized fun read(): NotebookState {
        val bytes = try { file.openRead().use { it.readBounded(150001) } } catch (e: java.io.FileNotFoundException) { if (!file.baseFile.exists() && !File(file.baseFile.path + ".bak").exists()) return NotebookState() else throw e }
        require(bytes.size <= 150000) { "Notebook storage is too large. Existing notes were preserved." }
        val value = JSONObject(bytes.toString(Charsets.UTF_8))
        val text = value.get("text")
        require(text is String && text.length <= 20000) { "Notebook storage is invalid. Existing notes were preserved." }
        val pkg = if (value.isNull("installed")) null else ModulePackage.parse(value.getJSONObject("installed").toString().toByteArray())
        require(pkg == null || pkg.getString("kind") == "listening-notes")
        val shop = if (!value.has("shop") || value.isNull("shop")) null else ModulePackage.parse(value.getJSONObject("shop").toString().toByteArray())
        require(shop == null || shop.getString("kind") == "web-shop")
        return NotebookState(pkg, text, shop)
    }
    private fun write(state: NotebookState): NotebookState {
        val bytes = JSONObject().put("installed", state.installed ?: JSONObject.NULL).put("text", state.text).put("shop", state.shop ?: JSONObject.NULL).toString().toByteArray()
        val output = file.startWrite()
        try { output.write(bytes); file.finishWrite(output) } catch (e: Exception) { file.failWrite(output); throw e }
        return state
    }
    @Synchronized fun install(bytes: ByteArray): NotebookState { val pkg = ModulePackage.parse(bytes); val old = read(); return write(if (pkg.getString("kind") == "web-shop") old.copy(shop = pkg) else old.copy(installed = pkg)) }
    @Synchronized fun removeShop(): NotebookState = write(read().copy(shop = null))
    @Synchronized fun remove(): NotebookState = write(read().copy(installed = null))
    @Synchronized fun save(text: String): NotebookState { require(text.length <= 20000); val old = read(); require(old.installed != null) { "Install Listening Notes first." }; return write(old.copy(text = text)) }
}
