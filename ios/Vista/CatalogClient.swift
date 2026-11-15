import Foundation
import Combine

struct CatalogClient {
    let token: String
    let session: URLSession
    init(token: String = Bundle.main.object(forInfoDictionaryKey: "VistaTMDBAccessToken") as? String ?? "",
         session: URLSession = .shared) {
        self.token = token
        self.session = session
    }

    func request<T: Decodable>(_ path: String, query: [URLQueryItem] = []) async throws -> T {
        guard !token.isEmpty, !token.contains("$(") else { throw CatalogError.configuration }
        var components = URLComponents(string: "https://api.themoviedb.org/3/\(path)")!
        components.queryItems = query.isEmpty ? nil : query
        var request = URLRequest(url: components.url!, cachePolicy: .useProtocolCachePolicy, timeoutInterval: 20)
        request.setValue("Bearer \(token)", forHTTPHeaderField: "Authorization")
        request.setValue("application/json", forHTTPHeaderField: "Accept")
        let (data, response) = try await session.data(for: request)
        guard let http = response as? HTTPURLResponse else { throw CatalogError.response }
        switch http.statusCode {
        case 200...299: break
        case 401, 403: throw CatalogError.access
        case 429: throw CatalogError.busy
        default: throw CatalogError.service
        }
        do { return try JSONDecoder().decode(T.self, from: data) }
        catch { throw CatalogError.response }
    }

    func list(_ path: String, kind: MediaKind, page: Int = 1, query: String? = nil) async throws -> (items: [Media], hasMore: Bool) {
        var parameters = [URLQueryItem(name: "page", value: String(page))]
        if let query { parameters.append(URLQueryItem(name: "query", value: query)) }
        let result: MediaPage = try await request(path, query: parameters)
        return (result.results.compactMap { $0.media(kind: kind) }, page < result.totalPages)
    }

    func details(_ media: Media) async throws -> MediaDTO {
        try await request("\(media.kind.rawValue)/\(media.id)")
    }
    func episodes(seriesID: Int, season: Int) async throws -> [Episode] {
        let result: SeasonEpisodes = try await request("tv/\(seriesID)/season/\(season)")
        return result.episodes
    }
}

@MainActor
final class HomeModel: ObservableObject {
    @Published var rows: [CatalogRow] = []
    @Published var loading = false
    @Published var error: String?
    private let client: CatalogClient
    init(client: CatalogClient = CatalogClient()) { self.client = client }

    func load() async {
        guard !loading else { return }
        loading = true; error = nil
        defer { loading = false }
        let sections: [(String, String, MediaKind)] = [
            ("trending/movie/week", "Trending movies", .movie),
            ("movie/popular", "Popular movies", .movie),
            ("tv/popular", "Popular series", .tv),
            ("movie/top_rated", "Top-rated movies", .movie)
        ]
        var loaded: [CatalogRow] = []
        var firstError: String?
        // Each row has its own failure boundary; a failed service call doesn't erase healthy rows.
        for (path, title, kind) in sections {
            if Task.isCancelled { return }
            do {
                let result = try await client.list(path, kind: kind)
                if !result.items.isEmpty { loaded.append(CatalogRow(id: path, title: title, items: result.items)) }
            } catch is CancellationError { return }
            catch { if firstError == nil { firstError = error.localizedDescription } }
        }
        if !loaded.isEmpty { rows = loaded }
        error = firstError ?? (loaded.isEmpty ? "No titles are available right now. Please try again." : nil)
    }
}

@MainActor
final class SearchModel: ObservableObject {
    @Published var items: [Media] = []
    @Published var loading = false
    @Published var error: String?
    @Published var hasMore = false
    private var activeQuery = ""
    private var page = 0
    private var generation = 0
    private let client: CatalogClient
    init(client: CatalogClient = CatalogClient()) { self.client = client }

    func search(_ query: String, more: Bool = false) async {
        let clean = query.trimmingCharacters(in: .whitespacesAndNewlines)
        if more && (loading || !hasMore) { return }
        generation += 1
        let ticket = generation
        if !more { items = []; page = 0; hasMore = false; activeQuery = clean }
        guard !clean.isEmpty else { loading = false; error = nil; return }
        loading = true; error = nil
        defer { if ticket == generation { loading = false } }
        do {
            let result = try await client.list("search/multi", kind: .movie, page: page + 1, query: activeQuery)
            try Task.checkCancellation()
            guard ticket == generation else { return }
            var seen = Set(items.map(\.identity))
            items += result.items.filter { seen.insert($0.identity).inserted }
            page += 1; hasMore = result.hasMore
        } catch is CancellationError { }
        catch { if ticket == generation && !Task.isCancelled { self.error = error.localizedDescription } }
    }
}
