package sdk.chat.core.base

import android.graphics.Bitmap
import androidx.exifinterface.media.ExifInterface
import id.zelory.compressor.Compressor
import id.zelory.compressor.constraint.format
import id.zelory.compressor.constraint.quality
import id.zelory.compressor.constraint.resolution
import kotlinx.coroutines.runBlocking
import sdk.chat.core.session.ChatSDK
import java.io.File
import kotlin.math.max

@JvmOverloads
fun compressImage(file: File, maxImageSize: Int = 600): File {
    val context = ChatSDK.ctx() ?: return file
    val (width, height) = file.getImageExtras(maxImageSize)

    return runBlocking {
        Compressor.compress(context, file) {
            quality(80)
            format(Bitmap.CompressFormat.JPEG)
            if (width != 0 && height != 0)
                resolution(width, height)
        }
    }
}

data class ImageExtras(
    val width: Int,
    val height: Int
)

@JvmOverloads
fun File.getImageExtras(maxImageSize: Int = 600): ImageExtras {
    val exif = exif

    val width = exif.getAttributeInt(ExifInterface.TAG_IMAGE_WIDTH, 0)
    val height = exif.getAttributeInt(ExifInterface.TAG_IMAGE_LENGTH, 0)

    val dstWidth: Int
    val dstHeight: Int

    when {
        max(width, height) < maxImageSize -> {
            dstWidth = width
            dstHeight = height
        }
        height > width -> {
            dstWidth = maxImageSize
            dstHeight = maxImageSize / width * height
        }
        else -> {
            dstWidth = maxImageSize / height * width
            dstHeight = maxImageSize
        }
    }

    return ImageExtras(dstWidth, dstHeight)
}

private val File.exif
    get() = inputStream().use {
        ExifInterface(it)
    }