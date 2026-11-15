import Foundation
import Combine

@MainActor
final class LibraryStore: ObservableObject {
    @Published private(set) var items: [Media]
    @Published private(set) var error: String?
    private let url: URL

    init(url: URL? = nil) {
        let directory = FileManager.default.urls(for: .applicationSupportDirectory, in: .userDomainMask)[0]
        self.url = url ?? directory.appendingPathComponent("Vista/watchlist.json")
        if let data = try? Data(contentsOf: self.url), let decoded = try? JSONDecoder().decode([Media].self, from: data) {
            items = decoded
        } else { items = [] }
    }
    func contains(_ media: Media) -> Bool { items.contains { $0.identity == media.identity } }
    func toggle(_ media: Media) {
        var updated = items
        if contains(media) { updated.removeAll { $0.identity == media.identity } }
        else { updated.insert(media, at: 0) }
        do {
            try FileManager.default.createDirectory(at: url.deletingLastPathComponent(), withIntermediateDirectories: true)
            try JSONEncoder().encode(updated).write(to: url, options: .atomic)
            items = updated; error = nil
        } catch { self.error = "Your watchlist couldn't be saved. Please check your device storage and try again." }
    }
}
