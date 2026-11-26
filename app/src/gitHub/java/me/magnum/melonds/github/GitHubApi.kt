package me.magnum.melonds.github

import me.magnum.melonds.github.dtos.ReleaseDto
import retrofit2.http.GET

interface GitHubApi {
    @GET("/repos/crowdedmovie/WatermelonDS/releases?per_page=100")
    suspend fun getReleases(): List<ReleaseDto>
}
