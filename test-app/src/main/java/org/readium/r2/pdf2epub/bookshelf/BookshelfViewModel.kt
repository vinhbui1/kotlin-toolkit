/*
 * Copyright 2021 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.pdf2epub.bookshelf

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.readium.r2.shared.util.AbsoluteUrl
import org.readium.r2.shared.util.toUrl
import org.readium.r2.pdf2epub.data.model.Book
import org.readium.r2.pdf2epub.reader.OpeningError
import org.readium.r2.pdf2epub.reader.ReaderActivityContract
import org.readium.r2.pdf2epub.utils.EventChannel

class BookshelfViewModel(application: Application) : AndroidViewModel(application) {

    private val app get() =
        getApplication<org.readium.r2.pdf2epub.Application>()

    val channel = EventChannel(Channel<Event>(Channel.BUFFERED), viewModelScope)
    val books = app.bookRepository.books()

    fun deletePublication(book: Book) =
        viewModelScope.launch {
            app.bookshelf.deleteBook(book)
        }

 suspend fun importPublicationFromStorage(uri: Uri) {
     withContext(Dispatchers.IO) {
         app.bookshelf.importPublicationFromStorage(uri, app.applicationContext)
     }
    }

    fun addPublicationFromStorage(uri: Uri) {
        app.bookshelf.addPublicationFromStorage(uri.toUrl()!! as AbsoluteUrl)
    }

    fun addPublicationFromWeb(url: AbsoluteUrl) {
        app.bookshelf.addPublicationFromWeb(url)
    }

    fun openPublication(
        bookId: Long
    ) {
        viewModelScope.launch {
            app.readerRepository
                .open(bookId)
                .onFailure {
                    channel.send(Event.OpenPublicationError(it))
                }
                .onSuccess {
                    val arguments = ReaderActivityContract.Arguments(bookId)
                    channel.send(Event.LaunchReader(arguments))
                }
        }
    }

    sealed class Event {

        class OpenPublicationError(
            val error: OpeningError
        ) : Event()

        class LaunchReader(
            val arguments: ReaderActivityContract.Arguments
        ) : Event()
    }
}
