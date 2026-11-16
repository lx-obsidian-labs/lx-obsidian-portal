import SwiftUI

@main
struct VistaApp: App {
    @StateObject private var library = LibraryStore()
    var body: some Scene {
        WindowGroup {
            RootView().environmentObject(library).tint(.red).preferredColorScheme(.dark)
        }
    }
}

struct RootView: View {
    var body: some View {
        TabView {
            NavigationStack { HomeView() }.tabItem { Label("Home", systemImage: "house") }
            NavigationStack { SearchView() }.tabItem { Label("Search", systemImage: "magnifyingglass") }
            NavigationStack { LibraryView() }.tabItem { Label("Watchlist", systemImage: "bookmark") }
            NavigationStack { SettingsView() }.tabItem { Label("Settings", systemImage: "gearshape") }
        }
    }
}

struct Poster: View {
    let media: Media
    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            AsyncImage(url: media.posterURL) { image in image.resizable().scaledToFill() }
                placeholder: { RoundedRectangle(cornerRadius: 12).fill(.white.opacity(0.07)).overlay(Image(systemName: "film").foregroundStyle(.secondary)) }
                .frame(height: 210).frame(maxWidth: .infinity).clipped().clipShape(RoundedRectangle(cornerRadius: 12))
            Text(media.title).font(.subheadline.weight(.semibold)).lineLimit(2).frame(height: 38, alignment: .topLeading)
            Text([media.kind.label, media.year].filter { !$0.isEmpty }.joined(separator: " · ")).font(.caption).foregroundStyle(.secondary)
        }
        .foregroundStyle(.primary).accessibilityElement(children: .combine)
    }
}

struct RecoveryView: View {
    let title: String
    let message: String
    let action: () -> Void
    var body: some View {
        ContentUnavailableView {
            Label(title, systemImage: "film")
        } description: { Text(message) } actions: {
            Button("Try again", action: action).buttonStyle(.borderedProminent)
        }
    }
}

struct HomeView: View {
    @StateObject private var model = HomeModel()
    var body: some View {
        Group {
            if model.loading && model.rows.isEmpty { ProgressView("Finding your next story…").frame(maxWidth: .infinity, maxHeight: .infinity) }
            else if model.rows.isEmpty {
                RecoveryView(title: "Let's get your movies back", message: model.error ?? "Please try again.") { Task { await model.load() } }
            } else {
                ScrollView {
                    LazyVStack(alignment: .leading, spacing: 28) {
                        if let featured = model.rows.first?.items.first {
                            NavigationLink { DetailsView(media: featured) } label: {
                                ZStack(alignment: .bottomLeading) {
                                    AsyncImage(url: featured.backdropURL ?? featured.posterURL) { $0.resizable().scaledToFill() }
                                        placeholder: { Rectangle().fill(.white.opacity(0.06)) }
                                        .frame(height: 280).clipped()
                                    LinearGradient(colors: [.clear, .black.opacity(0.9)], startPoint: .top, endPoint: .bottom)
                                    VStack(alignment: .leading, spacing: 8) {
                                        Text("FEATURED TODAY").font(.caption.weight(.bold)).foregroundStyle(.red)
                                        Text(featured.title).font(.largeTitle.bold()).foregroundStyle(.white)
                                        Label("Explore this story", systemImage: "play.circle.fill").foregroundStyle(.white)
                                    }.padding(24)
                                }.clipShape(RoundedRectangle(cornerRadius: 20))
                            }.buttonStyle(.plain)
                        }
                        if let error = model.error {
                            VStack(alignment: .leading, spacing: 8) {
                                Text("Some titles couldn't refresh").font(.headline)
                                Text(error).font(.subheadline).foregroundStyle(.secondary)
                                Button("Try again") { Task { await model.load() } }
                            }.padding().frame(maxWidth: .infinity, alignment: .leading).background(.white.opacity(0.05), in: RoundedRectangle(cornerRadius: 16))
                        }
                        ForEach(model.rows) { row in
                            VStack(alignment: .leading, spacing: 14) {
                                Text(row.title).font(.title2.bold())
                                ScrollView(.horizontal) {
                                    LazyHStack(alignment: .top, spacing: 16) {
                                        ForEach(row.items, id: \.identity) { media in
                                            NavigationLink { DetailsView(media: media) } label: { Poster(media: media).frame(width: 140) }.buttonStyle(.plain)
                                        }
                                    }
                                }.scrollIndicators(.hidden)
                            }
                        }
                    }.padding()
                }.refreshable { await model.load() }
            }
        }.navigationTitle("VISTA").task { if model.rows.isEmpty { await model.load() } }
    }
}

struct SearchView: View {
    @StateObject private var model = SearchModel()
    @State private var query = ""
    var body: some View {
        ScrollView {
            if query.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty {
                ContentUnavailableView("Find your next story", systemImage: "magnifyingglass", description: Text("Search movies and series by title."))
            } else {
                LazyVGrid(columns: [GridItem(.adaptive(minimum: 130), alignment: .top)], spacing: 20) {
                    ForEach(model.items, id: \.identity) { media in
                        NavigationLink { DetailsView(media: media) } label: { Poster(media: media) }.buttonStyle(.plain)
                    }
                }.padding()
                if model.loading { ProgressView("Searching…").padding() }
                else if let error = model.error {
                    RecoveryView(title: "Search couldn't finish", message: error) { Task { await model.search(query, more: !model.items.isEmpty) } }
                } else if model.items.isEmpty {
                    ContentUnavailableView.search(text: query)
                } else if model.hasMore {
                    Button("Load more titles") { Task { await model.search(query, more: true) } }.buttonStyle(.bordered).padding()
                }
            }
        }.navigationTitle("Search").searchable(text: $query, prompt: "Movies and series")
            .task(id: query) {
                do { if !query.isEmpty { try await Task.sleep(for: .milliseconds(350)) }; try Task.checkCancellation(); await model.search(query) }
                catch { }
            }
    }
}

struct LibraryView: View {
    @EnvironmentObject private var library: LibraryStore
    var body: some View {
        Group {
            if library.items.isEmpty {
                ContentUnavailableView("Your next watches live here", systemImage: "bookmark", description: Text("Save movies and series from their details page. Your watchlist stays on this device."))
            } else {
                ScrollView {
                    LazyVGrid(columns: [GridItem(.adaptive(minimum: 130), alignment: .top)], spacing: 20) {
                        ForEach(library.items, id: \.identity) { media in
                            NavigationLink { DetailsView(media: media) } label: { Poster(media: media) }.buttonStyle(.plain)
                        }
                    }.padding()
                }
            }
        }.navigationTitle("Watchlist")
    }
}

struct SettingsView: View {
    var body: some View {
        List {
            Section("Help & Support") {
                NavigationLink("Help & Support") { SupportView() }
                Text(AppContacts.support).textSelection(.enabled)
                Link("Official website", destination: AppContacts.website)
            }
            Section("Vista Watch") {
                LabeledContent("Version", value: "\(Bundle.main.object(forInfoDictionaryKey: "CFBundleShortVersionString") as? String ?? "") · iOS preview")
                LabeledContent("Watchlist", value: "Stored on this device")
                Text("Live TV and Sports are being planned. Availability and timing are not confirmed.").font(.subheadline).foregroundStyle(.secondary)
            }
            Section("About the catalog") {
                Text("This product uses the TMDB API but is not endorsed or certified by TMDB.").font(.footnote)
                Link("The Movie Database", destination: URL(string: "https://www.themoviedb.org")!)
                Link("Vista Watch privacy policy", destination: AppContacts.privacyPolicy)
                Link("Terms of service", destination: AppContacts.terms)
            }
        }.navigationTitle("Settings")
    }
}
