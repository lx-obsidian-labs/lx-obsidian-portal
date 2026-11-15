import SwiftUI

struct DetailsView: View {
    let media: Media
    @EnvironmentObject private var library: LibraryStore
    @State private var details: MediaDTO?
    @State private var episodes: [Episode] = []
    @State private var selectedSeason = 1
    @State private var detailsError: String?
    @State private var episodeError: String?
    @State private var loadingEpisodes = false
    @State private var playing: EpisodeSelection?
    private let client = CatalogClient()
    private var display: Media { details?.media(kind: media.kind) ?? media }
    private var seasons: [Season] { (details?.seasons ?? []).filter { $0.episodeCount > 0 } }

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 22) {
                AsyncImage(url: display.backdropURL ?? display.posterURL) { $0.resizable().scaledToFill() }
                    placeholder: { Rectangle().fill(.white.opacity(0.06)) }
                    .frame(height: 260).frame(maxWidth: .infinity).clipped()
                VStack(alignment: .leading, spacing: 20) {
                    Text(display.title).font(.largeTitle.bold())
                    HStack {
                        Text([display.kind.label, display.year].filter { !$0.isEmpty }.joined(separator: " · "))
                        if let rating = display.rating, rating > 0 { Label(String(format: "%.1f", rating), systemImage: "star.fill").foregroundStyle(.yellow) }
                    }.font(.subheadline).foregroundStyle(.secondary)
                    if media.kind == .movie {
                        Button { playing = EpisodeSelection(season: 1, episode: 1) } label: {
                            Label("Watch movie", systemImage: "play.fill").frame(maxWidth: .infinity).padding(8)
                        }.buttonStyle(.borderedProminent)
                    }
                    Button { library.toggle(display) } label: {
                        Label(library.contains(media) ? "Remove from watchlist" : "Add to watchlist", systemImage: library.contains(media) ? "bookmark.fill" : "bookmark")
                            .frame(maxWidth: .infinity).padding(8)
                    }.buttonStyle(.bordered)
                    if let error = library.error { Text(error).font(.footnote).foregroundStyle(.orange) }
                    Text("The story").font(.title2.bold())
                    Text(display.overview.isEmpty ? "A synopsis isn't available for this title yet." : display.overview).foregroundStyle(.secondary)
                    if let error = detailsError {
                        VStack(alignment: .leading, spacing: 8) {
                            Text(error).font(.footnote).foregroundStyle(.orange)
                            Button("Refresh details") { Task { await loadDetails() } }
                        }
                    }
                    if media.kind == .tv {
                        Text("Episodes").font(.title2.bold())
                        if !seasons.isEmpty {
                            Picker("Season", selection: $selectedSeason) {
                                ForEach(seasons) { season in Text(season.name).tag(season.seasonNumber) }
                            }.pickerStyle(.menu)
                        }
                        if loadingEpisodes { ProgressView("Loading episodes…") }
                        else if let error = episodeError {
                            Text(error).font(.footnote).foregroundStyle(.orange)
                            Button("Retry episodes") { Task { await loadEpisodes() } }
                        } else if episodes.isEmpty { Text("No episodes are available for this season.").foregroundStyle(.secondary) }
                        else {
                            ForEach(episodes) { episode in
                                Button { playing = EpisodeSelection(season: selectedSeason, episode: episode.episodeNumber) } label: {
                                    HStack(spacing: 16) {
                                        Image(systemName: "play.circle.fill").font(.title).foregroundStyle(.red)
                                        VStack(alignment: .leading, spacing: 6) {
                                            Text("\(episode.episodeNumber). \(episode.name)").font(.headline).foregroundStyle(.primary)
                                            if let overview = episode.overview, !overview.isEmpty {
                                                Text(overview).font(.caption).foregroundStyle(.secondary).lineLimit(3)
                                            }
                                        }
                                    }.padding().frame(maxWidth: .infinity, alignment: .leading)
                                        .background(.white.opacity(0.05), in: RoundedRectangle(cornerRadius: 12))
                                }.buttonStyle(.plain)
                            }
                        }
                    }
                }.padding([.horizontal, .bottom])
            }
        }.navigationTitle(media.kind.label).navigationBarTitleDisplayMode(.inline)
            .task { await loadDetails() }
            .task(id: selectedSeason) { if media.kind == .tv { await loadEpisodes() } }
            .sheet(item: $playing) { selection in
                PlaybackView(media: display, season: selection.season, episode: selection.episode)
            }
    }

    @MainActor private func loadDetails() async {
        detailsError = nil
        do {
            let loaded = try await client.details(media)
            try Task.checkCancellation()
            details = loaded
            if media.kind == .tv, let first = seasons.first(where: { $0.seasonNumber > 0 }) ?? seasons.first,
               !seasons.contains(where: { $0.seasonNumber == selectedSeason }) {
                selectedSeason = first.seasonNumber
            }
        } catch { if !Task.isCancelled { detailsError = error.localizedDescription } }
    }
    @MainActor private func loadEpisodes() async {
        let requestedSeason = selectedSeason
        loadingEpisodes = true; episodeError = nil
        defer { if requestedSeason == selectedSeason { loadingEpisodes = false } }
        do {
            let loaded = try await client.episodes(seriesID: media.id, season: requestedSeason)
            try Task.checkCancellation()
            if requestedSeason == selectedSeason { episodes = loaded }
        } catch { if !Task.isCancelled && requestedSeason == selectedSeason { episodeError = error.localizedDescription; episodes = [] } }
    }
}

struct EpisodeSelection: Identifiable {
    let season: Int
    let episode: Int
    var id: String { "\(season):\(episode)" }
}
