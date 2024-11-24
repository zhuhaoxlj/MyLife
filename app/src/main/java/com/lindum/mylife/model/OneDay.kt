package com.lindum.mylife.model

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Environment
import android.util.Log
import com.alibaba.fastjson.JSONArray
import com.alibaba.fastjson.JSONObject
import java.io.*
import java.net.HttpURLConnection
import java.net.URL

class OneDay {
    val imgURL: String? = null
    val cnSentence: String? = null
    var enSentence: String? = null
        private set
    var enSentenceTranslation: String? = null
        private set
    var enSentenceAuthor: String? = null
        private set

    // Remove the explicit getter method and just use the property
    var enImageAndSentence: JSONArray? = null
        private set
    val word: String? = null

    @Throws(IOException::class)
    fun init(url: String) {
        val responseCode: Int
        val urlConnection: HttpURLConnection
        val reader: BufferedReader
        var content: String?
        try {
            val m_url = URL(url)
            urlConnection = m_url.openConnection() as HttpURLConnection
            responseCode = urlConnection.responseCode
            if (responseCode == 200) {
                reader = BufferedReader(InputStreamReader(urlConnection.inputStream, "GBK"))
                while ((reader.readLine().also { content = it }) != null) {
                    println(content)
                    val `object`: JSONObject = JSONObject.parseObject(content)
                    this.enSentence = `object`.getString("content")
                    this.enSentenceAuthor = `object`.getString("author")
                    this.enImageAndSentence = `object`.getJSONArray("origin_img_urls")
                    this.enSentenceTranslation = `object`.getString("translation")
                }
            } else {
                println("获取不到网页的源码，服务器响应代码为：$responseCode")
            }
        } catch (e: Exception) {
            println("获取不到网页的源码,出现异常：$e")
        }
    }

    fun showInfo() {
        println(enSentence)
        println(enSentenceAuthor)
        println(enSentenceTranslation)
        println(enImageAndSentence?.getOrNull(0) ?: "")
    }

    fun inputStream2Bitmap(inputStream: InputStream?): Bitmap {
        return BitmapFactory.decodeStream(inputStream)
    }

    @Throws(Exception::class)
    fun getImageStream(path: String): InputStream? {
        val url = URL(path)
        val conn = url.openConnection() as HttpURLConnection
        conn.connectTimeout = 5 * 1000
        conn.requestMethod = "GET"
        if (conn.responseCode == HttpURLConnection.HTTP_OK) {
            return conn.inputStream
        }
        return null
    }

    fun saveInputStreamImage(inputStream: InputStream?) {
        val mBitmap = BitmapFactory.decodeStream(inputStream)
        try {
            val savePath = "$sDCardPath/AAABBB/"
            val temp = File(savePath)
            val filepath = "$savePath/21.png"
            val file = File(filepath)
            if (!temp.exists()) {
                val b = temp.mkdirs()
                Log.e("Bitmap", "Write Successful")
            }
            if (!file.exists()) {
                val b = file.createNewFile()
                Log.e("Bitmap", "Success")
            }
            FileOutputStream(file).use { fos ->
                mBitmap.compress(Bitmap.CompressFormat.PNG, 100, fos)
                fos.flush()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private val sDCardPath: String
        get() {
            val sdcardDir: File? =
                if (Environment.getExternalStorageState() == Environment.MEDIA_MOUNTED) {
                    Environment.getExternalStorageDirectory()
                } else null
            checkNotNull(sdcardDir)
            return sdcardDir.toString()
        }

    companion object {
        fun getImgInputStream(imageUrl: String, headers: Map<String, String>?): InputStream? {
            var stream: InputStream? = null
            try {
                val url = URL(imageUrl)
                val conn = url.openConnection()
                headers?.forEach { (key, value) ->
                    conn.setRequestProperty(key, value)
                }
                conn.doInput = true
                stream = conn.getInputStream()
            } catch (e: IOException) {
                e.printStackTrace()
            }
            return stream
        }
    }
}