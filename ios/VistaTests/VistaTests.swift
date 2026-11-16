import XCTest
@testable import Vista

final class VistaTests: XCTestCase {
    func testSupportReportDoesNotIncludeDiagnosticsWithoutConsent() {
        let details = ContactDiagnostics(version: "1.0.12", os: "Test OS", manufacturer: "Apple", model: "Private QA model", category: "Phone")
        let report = BugReportBuilder.build(issue: .playback, description: "Example", version: "1.0.12", includeDiagnostics: false, diagnostics: details)
        XCTAssertEqual(report.recipient, AppContacts.support)
        XCTAssertFalse(report.body.contains(details.model))
        XCTAssertEqual(URLComponents(url: report.mailto!, resolvingAgainstBaseURL: false)?.queryItems?.first { $0.name == "subject" }?.value, report.subject)
    }
    private func title(kind: MediaKind = .movie) -> Media {
        Media(id: 597, kind: kind, title: "Example", overview: "", posterPath: nil, backdropPath: nil, date: nil, rating: nil)
    }
    func testNumericNamespacesStaySeparate() {
        XCTAssertNotEqual(title().identity, title(kind: .tv).identity)
        let sources = PlaybackSource.sources(media: title(kind: .tv), season: 2, episode: 3, backupKey: "test-key")
        XCTAssertTrue(sources[0].url.path.contains("/tv/597/2/3"))
        XCTAssertFalse(sources.contains { $0.id == "nexstream" })
    }
    func testBackupMovieKeyIsEncodedAndLast() throws {
        let sources = PlaybackSource.sources(media: title(), season: 1, episode: 1, backupKey: "test+key&value")
        let backup = try XCTUnwrap(sources.last)
        XCTAssertEqual(backup.id, "nexstream")
        XCTAssertEqual(URLComponents(url: backup.url, resolvingAgainstBaseURL: false)?.queryItems?.first?.value, "test+key&value")
    }
    func testProviderNavigationDoesNotAllowLookalikeOrCleartextHosts() {
        XCTAssertTrue(PlaybackSource.allowsTopLevel(URL(string: "https://vidsrc.to/embed/movie/597")!))
        XCTAssertFalse(PlaybackSource.allowsTopLevel(URL(string: "https://vidsrc.to.evil.example/")!))
        XCTAssertFalse(PlaybackSource.allowsTopLevel(URL(string: "http://vidsrc.to/")!))
    }
    func testSearchExcludesPeopleAndKeepsTVNamespace() throws {
        let data = Data(#"{"results":[{"id":1,"name":"A person","media_type":"person"},{"id":597,"name":"A series","media_type":"tv"}],"total_pages":1}"#.utf8)
        let page = try JSONDecoder().decode(MediaPage.self, from: data)
        let values = page.results.compactMap { $0.media(kind: .movie) }
        XCTAssertEqual(values.count, 1)
        XCTAssertEqual(values[0].kind, .tv)
    }
    @MainActor func testWatchlistPersistsTypedIDs() throws {
        let folder = FileManager.default.temporaryDirectory.appendingPathComponent(UUID().uuidString)
        defer { try? FileManager.default.removeItem(at: folder) }
        let url = folder.appendingPathComponent("watchlist.json")
        let library = LibraryStore(url: url)
        library.toggle(title()); library.toggle(title(kind: .tv))
        XCTAssertEqual(LibraryStore(url: url).items.count, 2)
        library.toggle(title())
        XCTAssertEqual(LibraryStore(url: url).items.map(\.kind), [.tv])
    }
}
