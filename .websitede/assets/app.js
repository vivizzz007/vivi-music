/* VIVI Music DE website — resolves the latest release from the GitHub API and
 * fills the download buttons / version badges / changelog. Static fallbacks
 * keep the page usable if the API is unreachable. */
(function () {
  "use strict";

  var REPO = "PiBOH/vivi-music";
  var API = "https://api.github.com/repos/" + REPO + "/releases/latest";
  var RELEASES_PAGE = "https://github.com/" + REPO + "/releases";

  function firstByExt(assets, ext) {
    var hit = null;
    (assets || []).forEach(function (a) {
      if (hit) return;
      var name = (a.name || "").toLowerCase();
      if (name.split(".").pop() === ext) hit = a.browser_download_url;
    });
    return hit;
  }

  function loadLatest() {
    fetch(API, { headers: { Accept: "application/vnd.github+json" } })
      .then(function (res) {
        if (!res.ok) throw new Error("api");
        return res.json();
      })
      .then(function (rel) {
        var tag = rel.tag_name || "";
        document.querySelectorAll("[data-version]").forEach(function (el) {
          el.textContent = tag;
        });

        var links = {
          winExe: firstByExt(rel.assets, "exe"),
          winMsi: firstByExt(rel.assets, "msi"),
          deb: firstByExt(rel.assets, "deb"),
          appimage: firstByExt(rel.assets, "appimage"),
          dmg: firstByExt(rel.assets, "dmg"),
          pkg: firstByExt(rel.assets, "pkg"),
          apk: firstByExt(rel.assets, "apk"),
        };

        document.querySelectorAll("[data-download]").forEach(function (a) {
          var url = links[a.getAttribute("data-download")];
          if (url) {
            a.href = url;
            a.classList.remove("disabled");
          }
        });

        // Changelog page: render the latest release notes.
        var body = document.getElementById("changelog-body");
        if (body && rel.body) {
          body.textContent = rel.body.trim();
        }
        var tagEl = document.getElementById("changelog-version");
        if (tagEl && tag) tagEl.textContent = tag;
      })
      .catch(function () {
        // Fallback: point every button at the releases page.
        document.querySelectorAll("[data-download]").forEach(function (a) {
          a.href = RELEASES_PAGE;
        });
        document.querySelectorAll("[data-version]").forEach(function (el) {
          el.textContent = "GitHub Releases";
        });
      });
  }

  if (document.readyState === "loading") {
    document.addEventListener("DOMContentLoaded", loadLatest);
  } else {
    loadLatest();
  }
})();