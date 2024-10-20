/*
 * Copyright 2023 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.pdf2epub.domain

import android.content.Context
import android.net.Uri
import android.webkit.MimeTypeMap
import java.io.File
import java.util.UUID
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import org.readium.r2.shared.publication.Publication
import org.readium.r2.shared.util.AbsoluteUrl
import org.readium.r2.shared.util.DebugError
import org.readium.r2.shared.util.Try
import org.readium.r2.shared.util.asset.AssetRetriever
import org.readium.r2.shared.util.file.FileSystemError
import org.readium.r2.shared.util.format.Format
import org.readium.r2.shared.util.getOrElse
import org.readium.r2.shared.util.toUrl
import org.readium.r2.streamer.PublicationOpener
import org.readium.r2.pdf2epub.data.BookRepository
import org.readium.r2.pdf2epub.data.model.Book
import org.readium.r2.pdf2epub.utils.PDFToTextConverter.createAndSaveEpub
import org.readium.r2.pdf2epub.utils.PDFToTextConverter.extractTextFromPdf
import org.readium.r2.pdf2epub.utils.getFileNameWithoutExtension
import org.readium.r2.pdf2epub.utils.tryOrLog
import timber.log.Timber

/**
 * The [Bookshelf] supports two different processes:
 * - directly _adding_ the url to a remote asset or an asset from shared storage to the database
 * - _importing_ an asset, that is downloading or copying the publication the asset points to to the app storage
 *   before adding it to the database
 */
class Bookshelf(
    private val context: Context,
    private val bookRepository: BookRepository,
    private val coverStorage: CoverStorage,
    private val publicationOpener: PublicationOpener,
    private val assetRetriever: AssetRetriever,
    private val publicationRetriever: PublicationRetriever
) {
    sealed class Event {
        data object ImportPublicationInit :
            Event()
        data object ExtractTextFromPdf :
            Event()
        data object CreateEpubFromText :
            Event()
        data object ImportPublicationSuccess :
            Event()

        class ImportPublicationError(
            val error: ImportError
        ) : Event()
    }

    val channel: Channel<Event> =
        Channel(Channel.UNLIMITED)

    private val coroutineScope: CoroutineScope =
        MainScope()

    fun importPublicationFromStorage(
        uri: Uri,
        context : Context
    ) {
        coroutineScope.launch {
            channel.send(Event.ImportPublicationInit)
        addBookFeedback(publicationRetriever.retrieveFromStorage(uri,context).onSuccess {
            })
        }
    }

    fun importPublicationFromOpds(
        publication: Publication
    ) {
        coroutineScope.launch {
            addBookFeedback(publicationRetriever.retrieveFromOpds(publication))
        }
    }

    fun addPublicationFromWeb(
        url: AbsoluteUrl
    ) {
        coroutineScope.launch {
            addBookFeedback(url)
        }
    }

    fun addPublicationFromStorage(
        url: AbsoluteUrl
    ) {
        coroutineScope.launch {
            channel.send(Event.ImportPublicationInit)
            addBookFeedback(url)
        }
    }

    private suspend fun addBookFeedback(
        retrieverResult: Try<PublicationRetriever.Result, ImportError>
    ) {
        retrieverResult
            .map { addBook(it.publication.toUrl(), it.format, it.coverUrl,true)
                println(it.publication.toUrl())
                println("it.publication.toUrl()")

            }
            .onSuccess {
                channel.send(Event.ImportPublicationSuccess) }
            .onFailure { channel.send(Event.ImportPublicationError(it)) }
    }

    private suspend fun addBookFeedback(
        url: AbsoluteUrl,
        format: Format? = null,
        coverUrl: AbsoluteUrl? = null
    ) {

        println("method nào AbsoluteUrl")

        addBook(url, format, coverUrl)
            .onSuccess { channel.send(Event.ImportPublicationSuccess) }
            .onFailure { channel.send(Event.ImportPublicationError(it)) }
    }

    private suspend fun addBook(
        url: AbsoluteUrl,
        format: Format? = null,
        coverUrl: AbsoluteUrl? = null,
        convertToEPub: Boolean = false
    ): Try<Unit, ImportError> {
        val asset =
            if (format == null) {
                assetRetriever.retrieve(url)
            } else {
                assetRetriever.retrieve(url, format)
            }.getOrElse {
                return Try.failure(
                    ImportError.Publication(PublicationError(it))
                )
            }

        publicationOpener.open(
            asset,
            allowUserInteraction = false
        ).onSuccess { publication ->
            val coverFile =
                coverStorage.storeCover(publication, coverUrl)
                    .getOrElse {
                        return Try.failure(
                            ImportError.FileSystem(
                                FileSystemError.IO(it)
                            )
                        )
                    }
            var createEpubFile : File? = null
            if("application/pdf" == asset.format.mediaType.toString() && convertToEPub){
                val name  = UUID.randomUUID().toString()
                channel.send(Event.ExtractTextFromPdf)
                val extractedText = extractTextFromPdf(File(url.path))
                channel.send(Event.CreateEpubFromText)
                print(extractedText)
                createEpubFile = createAndSaveEpub( context,extractedText,
                    getFileNameWithoutExtension(publication.metadata.title ?:name )
                )
                url.path?.let { File(it).delete() }
            }
            val id = bookRepository.insertBook(
                if (createEpubFile != null) File(createEpubFile.absolutePath).toUrl() else url,
                asset.format.mediaType,
                publication,
                coverFile
            )
            if (id == -1L) {
                coverFile.delete()
                return Try.failure(
                    ImportError.Database(
                        DebugError("Could not insert book into database.")
                    )
                )
            }
        }
            .onFailure {
                Timber.e("Cannot open publication: $it.")
                return Try.failure(
                    ImportError.Publication(PublicationError(it))
                )
            }

        return Try.success(Unit)
    }

    suspend fun deleteBook(book: Book) {
        val id = book.id!!
        bookRepository.deleteBook(id)
        tryOrLog { book.url.toFile()?.delete() }
        tryOrLog { File(book.cover).delete() }
    }
}
