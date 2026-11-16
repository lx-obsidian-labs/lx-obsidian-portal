import SwiftUI
import UIKit

struct SupportView: View {
    @Environment(\.openURL) private var openURL
    @State private var issue: SupportIssue = .other
    @State private var description = ""
    @State private var includeDiagnostics = false
    @State private var preview: ContactEmail?
    @State private var unavailable: ContactEmail?
    @State private var status: String?
    private var version: String { Bundle.main.object(forInfoDictionaryKey: "CFBundleShortVersionString") as? String ?? "Unknown" }
    private var build: String { Bundle.main.object(forInfoDictionaryKey: "CFBundleVersion") as? String ?? "" }

    var body: some View {
        Form {
            Section("Technical support") {
                Text(AppContacts.support).textSelection(.enabled)
                Button("Contact technical support") { compose(ContactEmail(recipient: AppContacts.support, subject: "Vista Watch | Support | \(version)", body: "How can we help?\n\n")) }
                Button("Copy support email") { copy(AppContacts.support) }
                Link("Browser-based support", destination: AppContacts.supportPage)
                Link("Visit official website", destination: AppContacts.website)
            }
            Section("Report a problem") {
                Picker("Issue category", selection: $issue) { ForEach(SupportIssue.allCases) { Text($0.rawValue).tag($0) } }
                TextField("Describe what happened", text: $description, axis: .vertical).lineLimit(3...8)
                    .onChange(of: description) { _, value in if value.count > 4000 { description = String(value.prefix(4000)) } }
                Toggle("Include non-sensitive diagnostic details", isOn: $includeDiagnostics)
                Text("Optional: app/OS version, manufacturer/model and device category. Passwords, tokens, payment details, location, account IDs, logs and viewing history are never attached automatically.").font(.footnote).foregroundStyle(.secondary)
                Button("Review complete report") {
                    preview = BugReportBuilder.build(issue: issue, description: description, version: version,
                        includeDiagnostics: includeDiagnostics, diagnostics: ContactDiagnostics(version: "\(version) (\(build))",
                            os: "\(UIDevice.current.systemName) \(UIDevice.current.systemVersion)", manufacturer: "Apple",
                            model: UIDevice.current.model, category: UIDevice.current.userInterfaceIdiom == .pad ? "Tablet" : "Phone"))
                }.disabled(description.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty)
            }
            Section("Assistance & frequently asked questions") {
                DisclosureGroup("Playback and streaming") { Text("Check your connection, then try another playback source. Embedded sources use the provider's controls. Catalog entries do not guarantee a working stream.") }
                DisclosureGroup("Installation and updates") { Text("The current iOS developer build needs Apple signing/provisioning or TestFlight before it can be installed on an ordinary iPhone. Android APKs cannot be installed on iOS.") }
                DisclosureGroup("Account assistance") { Text("The initial native iOS preview does not yet include Firebase account features. For an Android account problem, contact technical support without including your password.") }
                DisclosureGroup("Watchlist and downloads") { Text("The iOS watchlist is stored locally. Saving a title does not download it, and offline media downloads are not yet included in this iOS preview.") }
                Button("Suggest a feature") { compose(ContactEmail(recipient: AppContacts.support, subject: "Vista Watch | Feature Suggestion | \(version)", body: "What would you like Vista Watch to do better?\n\n")) }
            }
            Section("Privacy & Legal") {
                Text(AppContacts.privacy).textSelection(.enabled)
                Button("Privacy, deletion or rights enquiry") { compose(ContactEmail(recipient: AppContacts.privacy,
                    subject: "Vista Watch | Privacy / Legal enquiry", body: "Describe your request. Do not include passwords or payment details.\n\n")) }
                Button("Copy privacy email") { copy(AppContacts.privacy) }
                Link("Privacy policy", destination: AppContacts.privacyPolicy)
                Link("Terms of service", destination: AppContacts.terms)
                Text("Opening an email draft does not complete a deletion request, complaint or legal action.").font(.footnote).foregroundStyle(.secondary)
            }
            Section("LX Obsidian Labs") {
                Text("Developed and maintained by LX Obsidian Labs.")
                LabeledContent("Installed version", value: "\(version) (\(build))")
                Button("Business partnerships") { compose(ContactEmail(recipient: AppContacts.partnerships, subject: "Vista Watch | Partnership enquiry", body: "Describe your proposed collaboration.\n\n")) }
                Text("© 2026 LX Obsidian Labs. All rights reserved.").font(.footnote)
            }
            if let status { Section { Text(status).foregroundStyle(.secondary) } }
        }.navigationTitle("Help & Support")
            .sheet(item: $preview) { email in
                NavigationStack {
                    ScrollView { VStack(alignment: .leading, spacing: 20) {
                        Text(email.preview).textSelection(.enabled)
                        Text("Opening an email app does not send this report. Review, edit and send it there.").font(.footnote).foregroundStyle(.secondary)
                        Button("Open email app") { preview = nil; compose(email) }.buttonStyle(.borderedProminent)
                    }.padding() }.navigationTitle("Review your report")
                        .toolbar { ToolbarItem(placement: .cancellationAction) { Button("Edit") { preview = nil } } }
                }.preferredColorScheme(.dark).tint(.red)
            }
            .alert("No email app is available", isPresented: Binding(get: { unavailable != nil }, set: { if !$0 { unavailable = nil } })) {
                Button("Copy report") { if let email = unavailable { copy(email.preview) }; unavailable = nil }
                Button("Open support page") { openURL(AppContacts.supportPage); unavailable = nil }
                Button("Cancel", role: .cancel) { unavailable = nil }
            } message: { Text("Nothing has been sent. Copy the email/report or use the official support website.") }
    }
    private func copy(_ value: String) { UIPasteboard.general.string = value; status = "Copied. Nothing has been sent." }
    private func compose(_ email: ContactEmail) {
        guard let url = email.mailto, UIApplication.shared.canOpenURL(url) else { unavailable = email; return }
        UIApplication.shared.open(url, options: [:]) { opened in
            if opened { status = "Email app opened. Review and send your message there." }
            else { unavailable = email }
        }
    }
}
