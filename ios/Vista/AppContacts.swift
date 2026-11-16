import Foundation

/// Official Vista Watch correspondence. Maintained by LX Obsidian Labs.
enum AppContacts {
    static let support = "support@lxobsidianportal.co.za"
    static let general = "info@lxobsidianportal.co.za"
    static let privacy = general
    static let billing = "accounts@lxobsidianportal.co.za"
    static let partnerships = "partnerships@lxobsidianportal.co.za"
    static let executive = "nathan@lxobsidianportal.co.za"
    static let sales = "sales@lxobsidianportal.co.za"
    static let admin = "admin@lxobsidianportal.co.za"
    static let careers = "careers@lxobsidianportal.co.za"
    static let hello = "hello@lxobsidianportal.co.za"
    static let website = URL(string: "https://www.lxobsidianportal.co.za/")!
    static let product = URL(string: "https://www.lxobsidianportal.co.za/vista")!
    static let supportPage = URL(string: "https://www.lxobsidianportal.co.za/vista#vista-support")!
    static let privacyPolicy = URL(string: "https://www.lxobsidianportal.co.za/privacy-policy")!
    static let terms = URL(string: "https://www.lxobsidianportal.co.za/vista-terms")!
}

struct ContactEmail: Identifiable {
    let id = UUID()
    let recipient: String
    let subject: String
    let body: String
    var mailto: URL? {
        var components = URLComponents()
        components.scheme = "mailto"
        components.path = recipient
        components.queryItems = [URLQueryItem(name: "subject", value: subject), URLQueryItem(name: "body", value: body)]
        return components.url
    }
    var preview: String { "To: \(recipient)\nSubject: \(subject)\n\n\(body)" }
}

enum SupportIssue: String, CaseIterable, Identifiable {
    case playback = "Playback problem", unavailable = "Stream unavailable", crash = "Application crashing"
    case download = "Download failure", login = "Login problem", television = "Smart TV navigation problem"
    case performance = "Performance problem", recommendations = "Recommendation issue", other = "Other"
    var id: String { rawValue }
}

struct ContactDiagnostics {
    let version: String
    let os: String
    let manufacturer: String
    let model: String
    let category: String
}

enum BugReportBuilder {
    static func build(issue: SupportIssue, description: String, version: String,
                      includeDiagnostics: Bool, diagnostics: ContactDiagnostics?) -> ContactEmail {
        var body = "Vista Watch bug report\nCategory: \(issue.rawValue)\n\nWhat happened:\n\(description.trimmingCharacters(in: .whitespacesAndNewlines).prefix(4000))\n"
        if includeDiagnostics, let details = diagnostics {
            body += "\nDiagnostic details included with your permission:\nApp version: \(details.version)\nOS version: \(details.os)\nDevice: \(details.manufacturer) \(details.model)\nDevice category: \(details.category)\n"
        } else { body += "\nDiagnostic details not included.\n" }
        return ContactEmail(recipient: AppContacts.support,
            subject: "Vista Watch | Bug Report | \(issue.rawValue) | \(version)", body: body)
    }
}
