package at.hannibal2.skyhanni.data.jsonobjects.local

import at.hannibal2.skyhanni.features.misc.visualwords.VisualWord
import com.google.gson.annotations.Expose

class VisualWordsJson {
    @Expose
    var modifiedWords: MutableList<VisualWord> = ArrayList()
}
