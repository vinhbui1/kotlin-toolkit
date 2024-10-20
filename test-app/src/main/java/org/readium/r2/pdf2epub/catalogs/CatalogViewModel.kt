/*
 * Copyright 2021 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.pdf2epub.catalogs

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import org.readium.r2.opds.OPDS1Parser
import org.readium.r2.opds.OPDS2Parser
import org.readium.r2.shared.opds.ParseData
import org.readium.r2.shared.publication.Publication
import org.readium.r2.shared.util.AbsoluteUrl
import org.readium.r2.shared.util.Try
import org.readium.r2.shared.util.http.HttpRequest
import org.readium.r2.pdf2epub.data.model.Catalog
import org.readium.r2.pdf2epub.utils.EventChannel
import timber.log.Timber

class CatalogViewModel(application: Application) : AndroidViewModel(application) {

    val channel = EventChannel(Channel<Event>(Channel.BUFFERED), viewModelScope)

    lateinit var publication: Publication
    private val app = getApplication<org.readium.r2.pdf2epub.Application>()

    fun parseCatalog(catalog: Catalog) = viewModelScope.launch {
        var parseRequest: Try<ParseData, Exception>? = null
        catalog.href.let { href ->
            AbsoluteUrl(href)
                ?.let { HttpRequest(it) }
                ?.let { request ->
                    parseRequest = if (catalog.type == 1) {
                        OPDS1Parser.parseRequest(request, app.readium.httpClient)
                    } else {
                        OPDS2Parser.parseRequest(request, app.readium.httpClient)
                    }
                }
        }
        parseRequest?.onSuccess {
            channel.send(Event.CatalogParseSuccess(it))
        }
        parseRequest?.onFailure {
            Timber.e(it)
            channel.send(Event.CatalogParseFailed)
        }
    }

    fun downloadPublication(publication: Publication) = viewModelScope.launch {
        app.bookshelf.importPublicationFromOpds(publication)
    }

    sealed class Event {

        object CatalogParseFailed : Event()

        class CatalogParseSuccess(val result: ParseData) : Event()
    }
}
