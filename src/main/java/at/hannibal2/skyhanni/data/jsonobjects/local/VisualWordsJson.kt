package at.hannibal2.hanni.data.jsonobjects.local

import at.hannibal2.hanni.features.misc.visualwords.VisualWord
import com.google.gson.annotations.Expose

class VisualWordsJson {
    @Expose
    var modifiedWords: MutableList<VisualWord> = ArrayList()
}
