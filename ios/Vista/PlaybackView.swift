import SwiftUI
import WebKit

struct PlaybackSource: Identifiable {
    let id: String
    let name: String
    let url: URL
    static let hosts = ["vidsrc.to", "vidsrc.pro", "www.2embed.cc", "multiembed.mov", "api.codespecters.com"]

    static func allowsTopLevel(_ url: URL) -> Bool {
        guard url.scheme?.lowercased() == "https", let host = url.host?.lowercased() else { return false }
        return hosts.contains { host == $0 || host.hasSuffix(".\($0)") }
    }
    static func sources(media: Media, season: Int, episode: Int, backupKey: String) -> [PlaybackSource] {
        guard media.id > 0, season >= 0, episode > 0 else { return [] }
        let movie = media.kind == .movie
        let path = movie ? "movie/\(media.id)" : "tv/\(media.id)/\(season)/\(episode)"
        var values: [(String, String, String)] = [
            ("vidsrc", "VidSrc", "https://vidsrc.to/embed/\(path)"),
            ("vidsrc-pro", "VidSrc PRO", "https://vidsrc.pro/embed/\(path)"),
            ("2embed", "2Embed", movie ? "https://www.2embed.cc/embed/\(media.id)" : "https://www.2embed.cc/embedtv/\(media.id)?s=\(season)&e=\(episode)")
        ]
        values.append(("multiembed", "MultiEmbed", "https://multiembed.mov/?video_id=\(media.id)&tmdb=1" + (movie ? "" : "&s=\(season)&e=\(episode)")))
        var result = values.compactMap { id, name, value in URL(string: value).map { PlaybackSource(id: id, name: name, url: $0) } }
        let key = backupKey.trimmingCharacters(in: .whitespacesAndNewlines)
        if movie, !key.isEmpty, !key.contains("$("), !key.contains("_key_here") {
            var components = URLComponents(string: "https://api.codespecters.com/embed/movie/\(media.id)")!
            components.queryItems = [URLQueryItem(name: "apikey", value: key)]
            if let url = components.url { result.append(PlaybackSource(id: "nexstream", name: "NexStream · Backup", url: url)) }
        }
        return result
    }
}

struct PlaybackView: View {
    let media: Media
    let season: Int
    let episode: Int
    @Environment(\.dismiss) private var dismiss
    @State private var sourceIndex = 0
    @State private var loading = true
    @State private var error: String?
    @State private var reload = UUID()
    private var sources: [PlaybackSource] {
        PlaybackSource.sources(media: media, season: season, episode: episode,
            backupKey: Bundle.main.object(forInfoDictionaryKey: "VistaBackupMoviesAPIKey") as? String ?? "")
    }
    var body: some View {
        NavigationStack {
            VStack(spacing: 12) {
                if sources.indices.contains(sourceIndex) {
                    Picker("Playback source", selection: $sourceIndex) {
                        ForEach(Array(sources.enumerated()), id: \.element.id) { index, source in Text(source.name).tag(index) }
                    }.pickerStyle(.menu)
                    Text("Embedded sources use the provider's player and controls.").font(.caption).foregroundStyle(.secondary).padding(.horizontal)
                    if let error {
                        RecoveryView(title: "Let's get playback working", message: error) { self.error = nil; loading = true; reload = UUID() }
                        if sources.count > 1 {
                            Button("Try another source") { sourceIndex = (sourceIndex + 1) % sources.count }.buttonStyle(.bordered).padding()
                        }
                    } else {
                        ZStack {
                            ProviderWebView(url: sources[sourceIndex].url, loading: $loading, error: $error).id("\(sources[sourceIndex].id)-\(reload)")
                            if loading { ProgressView("Opening the player…").padding().background(.ultraThinMaterial, in: RoundedRectangle(cornerRadius: 12)) }
                        }
                    }
                } else {
                    ContentUnavailableView("No playback source", systemImage: "play.slash", description: Text("This title doesn't have a compatible identifier."))
                }
            }.background(.black).navigationTitle(media.title).navigationBarTitleDisplayMode(.inline)
                .toolbar { ToolbarItem(placement: .confirmationAction) { Button("Done") { dismiss() } } }
                .onChange(of: sourceIndex) { _, _ in error = nil; loading = true }
        }.preferredColorScheme(.dark).tint(.red)
    }
}

struct ProviderWebView: UIViewRepresentable {
    let url: URL
    @Binding var loading: Bool
    @Binding var error: String?
    func makeCoordinator() -> Coordinator { Coordinator(self) }

    func makeUIView(context: Context) -> WKWebView {
        let configuration = WKWebViewConfiguration()
        configuration.allowsInlineMediaPlayback = true
        configuration.mediaTypesRequiringUserActionForPlayback = .all
        configuration.preferences.javaScriptCanOpenWindowsAutomatically = false
        configuration.userContentController.addUserScript(WKUserScript(source: Self.cleanup, injectionTime: .atDocumentStart, forMainFrameOnly: false))
        let view = WKWebView(frame: .zero, configuration: configuration)
        view.isOpaque = false; view.backgroundColor = .black
        view.navigationDelegate = context.coordinator; view.uiDelegate = context.coordinator
        WKContentRuleListStore.default().compileContentRuleList(forIdentifier: "vista-provider-ads-v1", encodedContentRuleList: Self.rules) { [weak view] rules, _ in
            DispatchQueue.main.async {
                guard let view, view.navigationDelegate != nil else { return }
                if let rules { view.configuration.userContentController.add(rules) }
                view.load(URLRequest(url: url, timeoutInterval: 25))
            }
        }
        return view
    }
    func updateUIView(_ view: WKWebView, context: Context) { context.coordinator.parent = self }
    static func dismantleUIView(_ view: WKWebView, coordinator: Coordinator) {
        view.stopLoading(); view.loadHTMLString("", baseURL: nil)
        view.navigationDelegate = nil; view.uiDelegate = nil
    }

    final class Coordinator: NSObject, WKNavigationDelegate, WKUIDelegate {
        var parent: ProviderWebView
        init(_ parent: ProviderWebView) { self.parent = parent }
        func webView(_ webView: WKWebView, decidePolicyFor action: WKNavigationAction, decisionHandler: @escaping (WKNavigationActionPolicy) -> Void) {
            guard let url = action.request.url, action.targetFrame != nil else { decisionHandler(.cancel); return }
            if action.targetFrame?.isMainFrame == true {
                decisionHandler(PlaybackSource.allowsTopLevel(url) ? .allow : .cancel)
            } else {
                decisionHandler(["https", "about", "blob"].contains(url.scheme?.lowercased() ?? "") ? .allow : .cancel)
            }
        }
        func webView(_ webView: WKWebView, didFinish navigation: WKNavigation!) { parent.loading = false }
        func webView(_ webView: WKWebView, didFailProvisionalNavigation navigation: WKNavigation!, withError error: Error) { fail(error) }
        func webView(_ webView: WKWebView, didFail navigation: WKNavigation!, withError error: Error) { fail(error) }
        private func fail(_ error: Error) {
            if (error as NSError).code == NSURLErrorCancelled { return }
            parent.loading = false; parent.error = "This source couldn't load. Check your connection or try another source."
        }
        func webView(_ webView: WKWebView, decidePolicyFor response: WKNavigationResponse, decisionHandler: @escaping (WKNavigationResponsePolicy) -> Void) {
            if response.isForMainFrame, let http = response.response as? HTTPURLResponse, http.statusCode >= 400 {
                parent.loading = false; parent.error = "This provider couldn't open the title. Try another source."
                decisionHandler(.cancel)
            } else { decisionHandler(.allow) }
        }
        func webViewWebContentProcessDidTerminate(_ webView: WKWebView) {
            parent.loading = false; parent.error = "The player stopped responding. Try reopening this source."
        }
        func webView(_ webView: WKWebView, createWebViewWith configuration: WKWebViewConfiguration, for action: WKNavigationAction, windowFeatures: WKWindowFeatures) -> WKWebView? { nil }
    }

    static let rules = #"""
    [{"trigger":{"url-filter":"^https?://([^/]+\\.)?(doubleclick\\.net|googleadservices\\.com|googlesyndication\\.com|popads\\.net|adsterra\\.com|exoclick\\.com|juicyads\\.com|popcash\\.net|adsco\\.re|createlouisville\\.org|tagivi\\.com|interlinecustomroofingllc\\.com)/"},"action":{"type":"block"}},
     {"trigger":{"url-filter":"^https?://soycdn\\.lol/__ads/"},"action":{"type":"block"}},
     {"trigger":{"url-filter":"^https?://([^/]+\\.)?cloudpiko\\.com/sb/notifications/"},"action":{"type":"block"}}]
    """#
    static let cleanup = #"""
    (() => {
      window.open = () => null;
      let queued = false;
      const clean = () => {
        queued = false;
        document.querySelectorAll('#ad-overlay,.ad-overlay,#ad-container,.ad-container,.ad-banner').forEach(node => {
          if (!node.querySelector('video,audio,iframe')) node.remove();
        });
      };
      new MutationObserver(() => { if (!queued) { queued = true; requestAnimationFrame(clean); } })
        .observe(document, {subtree:true,childList:true,attributes:true,attributeFilter:['class','id']});
      clean();
    })();
    """#
}
