package com.trios2025dej.superpodcast.repository

import com.trios2025dej.superpodcast.service.ItunesApi

class ItunesRepo(private val api: ItunesApi) {

    suspend fun search(term: String) = api.searchPodcasts(term)
}
