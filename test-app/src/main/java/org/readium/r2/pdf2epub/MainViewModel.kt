/*
 * Copyright 2023 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.pdf2epub

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.receiveAsFlow
import org.readium.r2.pdf2epub.domain.Bookshelf
import org.readium.r2.pdf2epub.domain.ImportError
import org.readium.r2.pdf2epub.utils.EventChannel

class MainViewModel(
    application: Application
) : AndroidViewModel(application) {

    private val app =
        getApplication<org.readium.r2.pdf2epub.Application>()

    val channel: EventChannel<Event> =
        EventChannel(Channel(Channel.UNLIMITED), viewModelScope)

    init {
        app.bookshelf.channel.receiveAsFlow()
            .onEach { sendImportFeedback(it) }
            .launchIn(viewModelScope)
    }

    private fun sendImportFeedback(event: Bookshelf.Event) {
        when (event) {
            is Bookshelf.Event.ImportPublicationError -> {
                channel.send(Event.ImportPublicationError(event.error))
            }
            Bookshelf.Event.ImportPublicationSuccess -> {
                channel.send(Event.ImportPublicationSuccess)
            }
            Bookshelf.Event.ImportPublicationInit -> {
                channel.send(Event.ImportPublicationInit)
            }

            Bookshelf.Event.CreateEpubFromText -> {
                channel.send(Event.CreateEpubFromText)
            }
            Bookshelf.Event.ExtractTextFromPdf -> {
                channel.send(Event.ExtractTextFromPdf)
            }
        }
    }

    sealed class Event {
        object ExtractTextFromPdf :
            Event()
        object CreateEpubFromText :
            Event()
        object ImportPublicationInit :
            Event()

        object ImportPublicationSuccess :
            Event()

        class ImportPublicationError(
            val error: ImportError
        ) : Event()
    }
}
