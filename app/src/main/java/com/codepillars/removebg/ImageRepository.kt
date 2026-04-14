package com.codepillars.removebg


import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import com.codepillars.removebg.RetrofitClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File

class ImageRepository {

    suspend fun removeBackground(context: Context, uri: Uri): Result<ByteArray> {
        return withContext(Dispatchers.IO) {
            try {
                val file = uriToFile(context, uri)
                val requestBody = file.asRequestBody("image/*".toMediaTypeOrNull())
                val part = MultipartBody.Part.createFormData("file", file.name, requestBody)

                val response = RetrofitClient.api.removeBackground(part)
                if (response.isSuccessful) {
                    val bytes = response.body()?.bytes()
                    if (bytes != null) {
                        Result.success(bytes)
                    } else {
                        Result.failure(Exception("Empty response body"))
                    }
                } else {
                    Result.failure(Exception("Upload failed: ${response.code()}"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    private fun uriToFile(context: Context, uri: Uri): File {
        val contentResolver: ContentResolver = context.contentResolver
        val fileName = getFileName(context, uri) ?: "upload_image.jpg"
        val tempFile = File.createTempFile("upload_", fileName, context.cacheDir)

        contentResolver.openInputStream(uri)?.use { input ->
            tempFile.outputStream().use { output ->
                input.copyTo(output)
            }
        } ?: throw IllegalArgumentException("Unable to open file")

        return tempFile
    }

    private fun getFileName(context: Context, uri: Uri): String? {
        var name: String? = null
        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (cursor.moveToFirst() && index >= 0) {
                name = cursor.getString(index)
            }
        }
        return name
    }
}