package org.readium.r2.pdf2epub.utils

import android.content.Context
import android.os.Environment
import android.util.Log
import com.itextpdf.kernel.pdf.PdfDocument
import com.itextpdf.kernel.pdf.PdfReader
import com.itextpdf.kernel.pdf.canvas.parser.PdfTextExtractor
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.nio.charset.StandardCharsets
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import nl.siegmann.epublib.domain.Author
import nl.siegmann.epublib.domain.Book
import nl.siegmann.epublib.domain.Resource
import nl.siegmann.epublib.epub.EpubWriter


//object PDFToTextConverter {
//    fun extractTextFromPdf( pdfFile: File): List<String> {
//        val pagesText: MutableList<String> = ArrayList()
//
//        try {
//            // Step 1: Initialize the PDF Reader
//            val reader = PdfReader(pdfFile)
//
//            // Step 2: Create a PdfDocument instance
//            val pdfDocument = PdfDocument(reader)
//
//            // Step 3: Loop through all pages and extract text
//            for (i in 1..pdfDocument.numberOfPages) {
//                val text = PdfTextExtractor.getTextFromPage(pdfDocument.getPage(i))
//                pagesText.add(text)
//            }
//
//            // Step 4: Close the document
//            pdfDocument.close()
//
//        } catch (e: IOException) {
//            e.printStackTrace()
//        }
//
//        return pagesText
//    }
//
//
//    fun createAndSaveEpub(context: Context, pagesText: List<String?>, fileName: String): File? {
//        try {
//            val book = Book()
//
//            book.metadata.addTitle(fileName)
//            book.metadata.addAuthor(Author("Vinh Bui"))
//
//
//            val css = """
//            body { font-family: Arial, sans-serif; }
//            h1 { color: #333; }
//        """.trimIndent()
//            val cssResource = Resource(css.toByteArray(StandardCharsets.UTF_8), "styles.css")
//            book.resources.add(cssResource)
//
//            val contentBuilder = StringBuilder()
//            var chapterNumber = 1
//
//            for (pageText in pagesText) {
//                if (pageText.isNullOrEmpty()) continue
//
//                contentBuilder.append("<h1>Chapter $chapterNumber</h1>\n")
//
//                val lines = pageText.split("\n")
//                for (line in lines) {
//                    val sanitizedLine = sanitizeText(line.trim())
//                    if (sanitizedLine.isNotEmpty()) {
//                        contentBuilder.append("<p>").append(sanitizedLine).append("</p>\n")
//                    }
//                }
//
//                chapterNumber++
//            }
//
//            val fullContent = createFullHtml(contentBuilder.toString())
//            val contentResource = Resource(fullContent.toByteArray(StandardCharsets.UTF_8), "content.xhtml")
//            book.addSection("Content", contentResource)
//
//            val rootPath = context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS)?.absolutePath
//            if (rootPath == null) {
//                Log.e("SaveEpub", "External storage is not available")
//                return null
//            }
//
//            val epubDirectory = File("$rootPath/epubbooks")
//            if (!epubDirectory.exists()) {
//                epubDirectory.mkdirs()
//            }
//            val newName = UUID.randomUUID().toString()
//
//            val epubFile = File(epubDirectory, "$newName.epub")
//
//            val epubWriter = EpubWriter()
//            FileOutputStream(epubFile).use { outputStream ->
//                epubWriter.write(book, outputStream)
//            }
//            Log.d("SaveEpub", "EPUB saved to: ${epubFile.absolutePath}")
//            return epubFile
//
//        } catch (e: Exception) {
//            e.printStackTrace()
//            Log.e("SaveEpub", "Error saving EPUB: ${e.message}")
//
//        }
//        return null
//    }
//
//
//
//    fun createFullHtml(content: String): String {
//        return """<?xml version="1.0" encoding="UTF-8"?>
//<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.1//EN" "http://www.w3.org/TR/xhtml11/DTD/xhtml11.dtd">
//<html xmlns="http://www.w3.org/1999/xhtml">
//<head>
//    <title>My EPUB Book</title>
//    <link rel="stylesheet" type="text/css" href="styles.css" />
//</head>
//<body>
//$content
//</body>
//</html>"""
//    }
//
//    fun sanitizeText(text: String): String {
//        return text.replace("\u0000", "") // Remove null characters
//            .replace("&", "&amp;")
//            .replace("<", "&lt;")
//            .replace(">", "&gt;")
//            .replace("\"", "&quot;")
//            .replace("'", "&apos;")
//    }
//
//}

object PDFToTextConverter {

//    fun extractTextFromPdf(pdfFile: File): List<String> {
//        val pagesText: MutableList<String> = ArrayList()
//
//        try {
//            // Step 1: Initialize the PDF Reader
//            val reader = PdfReader(pdfFile)
//
//            // Step 2: Create a PdfDocument instance
//            val pdfDocument = PdfDocument(reader)
//
//            // Step 3: Loop through all pages and extract text
//            for (i in 1..pdfDocument.numberOfPages) {
//                var text = PdfTextExtractor.getTextFromPage(pdfDocument.getPage(i))
//
//                // Split text by lines to handle each one
//                val lines = text.split("\n")
//                val sentenceBuilder = StringBuilder()
//
//                for (line in lines) {
//                    val trimmedLine = line.trim()
//
//                    // Handle hyphenation (soft line breaks)
//                    if (trimmedLine.endsWith("-")) {
//                        sentenceBuilder.append(trimmedLine.removeSuffix("-"))
//                    }
//                    // If the line ends with punctuation, add the full sentence
//                    else if (trimmedLine.endsWith(".") || trimmedLine.endsWith("?") || trimmedLine.endsWith("!")) {
//                        sentenceBuilder.append(trimmedLine)
//                        sentenceBuilder.append("\n")  // Keep line breaks where they should exist
//                    } else {
//                        // Add line to the current sentence, preserving the space
//                        sentenceBuilder.append(trimmedLine).append(" ")
//                    }
//                }
//
//                // Add the processed page's text to pagesText
//                pagesText.add(sentenceBuilder.toString().trim())
//            }
//
//            // Step 4: Close the document
//            pdfDocument.close()
//
//        } catch (e: IOException) {
//            e.printStackTrace()
//        }
//
//        return pagesText
//    }

    suspend fun extractTextFromPdf(pdfFile: File): List<String> = withContext(Dispatchers.IO) {
        val pagesText: MutableList<String> = ArrayList()

        try {
            // Step 1: Initialize the PDF Reader
            val reader = PdfReader(pdfFile)

            // Step 2: Create a PdfDocument instance
            val pdfDocument = PdfDocument(reader)

            // Step 3: Loop through all pages and extract text
            for (i in 1..pdfDocument.numberOfPages) {
                val text = PdfTextExtractor.getTextFromPage(pdfDocument.getPage(i))

                // Split text by lines to handle each one
                val lines = text.split("\n")
                val sentenceBuilder = StringBuilder()

                for (line in lines) {
                    val trimmedLine = line.trim()

                    // Handle hyphenation (soft line breaks)
                    if (trimmedLine.endsWith("-")) {
                        sentenceBuilder.append(trimmedLine.removeSuffix("-"))
                    }
                    // If the line ends with punctuation, add the full sentence
                    else if (trimmedLine.endsWith(".") || trimmedLine.endsWith("?") || trimmedLine.endsWith("!")) {
                        sentenceBuilder.append(trimmedLine)
                        sentenceBuilder.append("\n")  // Keep line breaks where they should exist
                    } else {
                        // Add line to the current sentence, preserving the space
                        sentenceBuilder.append(trimmedLine).append(" ")
                    }
                }

                // Add the processed page's text to pagesText
                pagesText.add(sentenceBuilder.toString().trim())
            }

            // Step 4: Close the document
            pdfDocument.close()

        } catch (e: IOException) {
            e.printStackTrace()
        }

        // Return the list of text from the PDF
        pagesText
    }
//    suspend fun createAndSaveEpub(context: Context, pagesText: List<String?>, fileName: String): File? = withContext(Dispatchers.IO) {
//        try {
//            val book = Book()
//
//            book.metadata.addTitle(fileName)
//            book.metadata.addAuthor(Author("Pdf2Epub"))
//
//            // CSS for the ebook styling
//            val css = """
//            body {
//                font-family: Arial, sans-serif;
//                word-wrap: break-word;
//                overflow-wrap: break-word;
//                hyphens: auto;
//            }
//            h1 { color: #333; }
//            p { max-width: 100%; }
//        """.trimIndent()
//
//            val cssResource = Resource(css.toByteArray(StandardCharsets.UTF_8), "styles.css")
//            book.resources.add(cssResource)
//
//            val contentBuilder = StringBuilder()
//
//            // Building the content for the EPUB
//            for (pageText in pagesText) {
//                if (pageText.isNullOrEmpty()) continue
//
//                // Processing text and converting to paragraphs
//                val processedText = processText(pageText)
//                val lines = processedText.split("\n")
//                for (line in lines) {
//                    val sanitizedLine = sanitizeText(line.trim())
//                    if (sanitizedLine.isNotEmpty()) {
//                        contentBuilder.append("<p>").append(sanitizedLine).append("</p>\n")
//                    }
//                }
//            }
//
//            // Create the final XHTML content
//            val fullContent = createFullHtml(contentBuilder.toString())
//            val contentResource = Resource(fullContent.toByteArray(StandardCharsets.UTF_8), "content.xhtml")
//            book.addSection("Content", contentResource)
//
//            // Save the EPUB to external storage
//            val rootPath = context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS)?.absolutePath
//            if (rootPath == null) {
//                Log.e("SaveEpub", "External storage is not available")
//                return@withContext null
//            }
//
//            val epubDirectory = File("$rootPath/epubbooks")
//            if (!epubDirectory.exists()) {
//                epubDirectory.mkdirs()
//            }
//
//            val epubFile = File(epubDirectory, "$fileName.epub")
//
//            // Writing the EPUB to the file
//            val epubWriter = EpubWriter()
//            FileOutputStream(epubFile).use { outputStream ->
//                epubWriter.write(book, outputStream)
//            }
//
//            Log.d("SaveEpub", "EPUB saved to: ${epubFile.absolutePath}")
//            return@withContext epubFile
//
//        } catch (e: Exception) {
//            e.printStackTrace()
//            Log.e("SaveEpub", "Error saving EPUB: ${e.message}")
//            return@withContext null
//        }
//    }

    suspend fun createAndSaveEpub(context: Context, pagesText: List<String?>, fileName: String): File? = withContext(Dispatchers.IO) {
        try {
            val book = Book()

            book.metadata.addTitle(fileName)
            book.metadata.addAuthor(Author("Pdf2Epub"))

            // CSS for the ebook styling
            val css = """
        body { 
            font-family: Arial, sans-serif; 
            word-wrap: break-word;
            overflow-wrap: break-word;
            hyphens: auto;
        }
        h1 { color: #333; }
        p { max-width: 100%; }
    """.trimIndent()

            val cssResource = Resource(css.toByteArray(StandardCharsets.UTF_8), "styles.css")
            book.resources.add(cssResource)

            // Building the content for the EPUB with chapters
            val chaptersCount = (pagesText.size + 19) / 20 // Round up division
            for (chapterIndex in 0 until chaptersCount) {
                val chapterBuilder = StringBuilder()
                chapterBuilder.append("<h1>Chapter ${chapterIndex + 1}</h1>\n")

                val startPage = chapterIndex * 20
                val endPage = minOf((chapterIndex + 1) * 20, pagesText.size)

                for (pageIndex in startPage until endPage) {
                    val pageText = pagesText[pageIndex]
                    if (pageText.isNullOrEmpty()) continue

                    // Processing text and converting to paragraphs
                    val processedText = processText(pageText)
                    val lines = processedText.split("\n")
                    for (line in lines) {
                        val sanitizedLine = sanitizeText(line.trim())
                        if (sanitizedLine.isNotEmpty()) {
                            chapterBuilder.append("<p>").append(sanitizedLine).append("</p>\n")
                        }
                    }
                }

                // Create the final XHTML content for this chapter
                val chapterContent = createFullHtml(chapterBuilder.toString())
                val chapterResource = Resource(chapterContent.toByteArray(StandardCharsets.UTF_8), "chapter${chapterIndex + 1}.xhtml")
                book.addSection("Chapter ${chapterIndex + 1}", chapterResource)
            }

            // Save the EPUB to external storage
            val rootPath = context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS)?.absolutePath
            if (rootPath == null) {
                Log.e("SaveEpub", "External storage is not available")
                return@withContext null
            }

            val epubDirectory = File("$rootPath/epubbooks")
            if (!epubDirectory.exists()) {
                epubDirectory.mkdirs()
            }

            val epubFile = File(epubDirectory, "$fileName.epub")

            // Writing the EPUB to the file
            val epubWriter = EpubWriter()
            FileOutputStream(epubFile).use { outputStream ->
                epubWriter.write(book, outputStream)
            }

            Log.d("SaveEpub", "EPUB saved to: ${epubFile.absolutePath}")
            return@withContext epubFile

        } catch (e: Exception) {
            e.printStackTrace()
            Log.e("SaveEpub", "Error saving EPUB: ${e.message}")
            return@withContext null
        }
    }

    fun processText(text: String): String {
        // Split long words or sequences without spaces
        val maxLength = 20 // Maximum length of a word before splitting
        val processedText = text.replace(Regex("\\S{$maxLength,}")) { matchResult ->
            val word = matchResult.value
            word.chunked(maxLength).joinToString(" ")
        }

        // Ensure there's a space after punctuation marks
        return processedText.replace(Regex("([.!?,:;])(?=\\S)"), "$1 ")
    }

    fun createFullHtml(content: String): String {
        return """<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.1//EN" "http://www.w3.org/TR/xhtml11/DTD/xhtml11.dtd">
<html xmlns="http://www.w3.org/1999/xhtml">
<head>
    <title>My EPUB Book</title>
    <link rel="stylesheet" type="text/css" href="styles.css" />
    <meta name="viewport" content="width=device-width, initial-scale=1.0" />
</head>
<body>
$content
</body>
</html>"""
    }

    fun sanitizeText(text: String): String {
        return text.replace("\u0000", "") // Remove null characters
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&apos;")
    }
}

fun getFileNameWithoutExtension(fileName: String): String {
    return fileName.substringBeforeLast(".")
}