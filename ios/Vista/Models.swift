import Foundation

enum MediaKind: String, Codable, CaseIterable {
    case movie, tv
    var label: String { self == .movie ? "Movie" : "Series" }
}

struct Media: Codable, Identifiable, Hashable {
    let id: Int
    let kind: MediaKind
    let title: String
    let overview: String
    let posterPath: String?
    let backdropPath: String?
    let date: String?
    let rating: Double?
    var identity: String { "\(kind.rawValue):\(id)" }
    var year: String { String((date ?? "").prefix(4)) }
    var posterURL: URL? { Self.imageURL(posterPath) }
    var backdropURL: URL? { Self.imageURL(backdropPath, size: "w1280") }
    static func imageURL(_ path: String?, size: String = "w500") -> URL? {
        guard let path, path.hasPrefix("/"), !path.contains("://") else { return nil }
        return URL(string: "https://image.tmdb.org/t/p/\(size)\(path)")
    }
}

struct MediaDTO: Decodable {
    let id: Int
    let title: String?
    let name: String?
    let overview: String?
    let posterPath: String?
    let backdropPath: String?
    let releaseDate: String?
    let firstAirDate: String?
    let voteAverage: Double?
    let mediaType: String?
    let seasons: [Season]?
    enum CodingKeys: String, CodingKey {
        case id, title, name, overview, seasons
        case posterPath = "poster_path", backdropPath = "backdrop_path"
        case releaseDate = "release_date", firstAirDate = "first_air_date"
        case voteAverage = "vote_average", mediaType = "media_type"
    }
    func media(kind fallback: MediaKind) -> Media? {
        if let mediaType, !["movie", "tv"].contains(mediaType) { return nil }
        let kind = mediaType.flatMap(MediaKind.init(rawValue:)) ?? fallback
        guard id > 0, let title = kind == .movie ? title : name, !title.isEmpty else { return nil }
        return Media(id: id, kind: kind, title: title, overview: overview ?? "", posterPath: posterPath,
                     backdropPath: backdropPath, date: kind == .movie ? releaseDate : firstAirDate, rating: voteAverage)
    }
}

struct MediaPage: Decodable {
    let results: [MediaDTO]
    let totalPages: Int
    enum CodingKeys: String, CodingKey { case results; case totalPages = "total_pages" }
}
struct Season: Decodable, Identifiable, Hashable {
    let id: Int
    let name: String
    let seasonNumber: Int
    let episodeCount: Int
    enum CodingKeys: String, CodingKey { case id, name; case seasonNumber = "season_number", episodeCount = "episode_count" }
}
struct Episode: Decodable, Identifiable {
    let id: Int
    let name: String
    let episodeNumber: Int
    let overview: String?
    enum CodingKeys: String, CodingKey { case id, name, overview; case episodeNumber = "episode_number" }
}
struct SeasonEpisodes: Decodable { let episodes: [Episode] }

struct CatalogRow: Identifiable {
    let id: String
    let title: String
    let items: [Media]
}

enum CatalogError: LocalizedError {
    case configuration, access, busy, service, response
    var errorDescription: String? {
        switch self {
        case .configuration: "The catalog key is missing from this build. Configure TMDB_ACCESS_TOKEN and rebuild."
        case .access: "The catalog service rejected this build's access. Please check for an update."
        case .busy: "The catalog service is busy. Please wait a moment and try again."
        case .service: "The catalog service is temporarily unavailable. Please try again."
        case .response: "The catalog response couldn't be read. Please check for an update."
        }
    }
}
