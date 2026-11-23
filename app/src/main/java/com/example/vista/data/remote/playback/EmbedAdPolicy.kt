package com.example.vista.data.remote.playback

import okhttp3.HttpUrl.Companion.toHttpUrlOrNull

/** Provider-page ads only; never installed on Vista's own advertising surfaces. */
object EmbedAdPolicy {
    private val hosts = setOf(
        "doubleclick.net", "googleadservices.com", "googlesyndication.com", "imasdk.googleapis.com",
        "moatads.com", "popads.net", "propellerads.com", "adsterra.com", "exoclick.com",
        "juicyads.com", "onclickads.net", "adnxs.com", "adnuntius.com", "taboola.com",
        "outbrain.com", "histats.com", "popcash.net", "revcontent.com", "mgid.com", "zedo.com",
        "adsco.re", "adexchangeclear.com", "onclckvd.com", "onclickalgo.com", "onclkds.com",
        "hilltopads.net", "trafficjunky.net", "trafficstars.com", "tsyndicate.com", "syndication.exoclick.com",
        // Observed in the VidSrc fake robot/notification overlay on a connected device.
        "createlouisville.org", "tagivi.com", "interlinecustomroofingllc.com"
    )

    fun blocks(url: String): Boolean {
        val parsed = url.toHttpUrlOrNull() ?: return false
        val host = parsed.host
        return hosts.any { host == it || host.endsWith(".$it") } ||
            (host == "soycdn.lol" && parsed.encodedPath.startsWith("/__ads/")) ||
            ((host == "cloudpiko.com" || host.endsWith(".cloudpiko.com")) &&
                parsed.encodedPath.startsWith("/sb/notifications/"))
    }

    /**
     * Same-document ad overlays do not always make a distinct network request. Remove only
     * explicitly labelled ad containers/known ad frames; never generic overlays, player controls,
     * video nodes, consent dialogs, or arbitrary third-party CDNs.
     */
    val cleanupScript: String = """
        (function() {
          if (window.__vistaAdCleanup) return;
          window.__vistaAdCleanup = true;
          window.open = function() { return null; };
          var blocked = ${hosts.joinToString(prefix = "[", postfix = "]") { "'$it'" }};
          function isAdResource(value) {
            try {
              var u = new URL(value, location.href), host = u.hostname.toLowerCase();
              return blocked.some(function(domain) { return host === domain || host.endsWith('.' + domain); }) ||
                (host === 'soycdn.lol' && u.pathname.indexOf('/__ads/') === 0) ||
                ((host === 'cloudpiko.com' || host.endsWith('.cloudpiko.com')) && u.pathname.indexOf('/sb/notifications/') === 0);
            } catch (_) { return false; }
          }
          var scheduled = false;
          var watchedDocuments = new WeakSet();
          function watch(doc) {
            if (!doc || watchedDocuments.has(doc)) return;
            watchedDocuments.add(doc);
            new MutationObserver(schedule).observe(doc, {
              childList:true, subtree:true, attributes:true, attributeFilter:['src', 'href', 'class', 'id']
            });
          }
          function schedule() {
            if (!scheduled) { scheduled = true; requestAnimationFrame(clean); }
          }
          function cleanDocument(doc) {
            watch(doc);
            doc.querySelectorAll('#ad-overlay, #ad-container, .ad-overlay, .ad-container, .ad-banner').forEach(function(node) {
              if (!node.querySelector('video, audio, iframe')) node.remove();
            });
            doc.querySelectorAll('iframe').forEach(function(frame) {
              try {
                if (!frame.__vistaAdWatched) {
                  frame.__vistaAdWatched = true;
                  frame.addEventListener('load', schedule);
                }
                if (frame.src && isAdResource(frame.src)) { frame.remove(); return; }
                var child = frame.contentDocument;
                if (child && Array.from(child.querySelectorAll('script[src]')).some(function(script) {
                  return isAdResource(script.src);
                })) { frame.remove(); return; }
                // Same-origin blank frames can acquire an overlay after their load event.
                // Cross-origin documents receive their own document-start script where supported.
                if (child) cleanDocument(child);
              } catch (_) {}
            });
            doc.querySelectorAll('a[href]').forEach(function(link) {
              if (!isAdResource(link.href)) return;
              var parent = link.parentElement;
              if (parent && parent !== doc.body && getComputedStyle(parent).position === 'fixed' &&
                  parent.children.length === 1 && !parent.querySelector('video, audio, iframe')) parent.remove();
              else link.remove();
            });
          }
          function clean() {
            scheduled = false;
            cleanDocument(document);
          }
          // documentElement may not exist at document-start; Document is already observable.
          watch(document);
          clean();
        })();
    """.trimIndent()
}
