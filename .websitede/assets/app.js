/* VIVI Music DE website — resolves releases from the GitHub API and fills the
 * download buttons / version badges / changelog. Two channels are offered,
 * like the original site: "Stable" and "Nightly builds". Static fallbacks keep
 * the page usable if the API is unreachable. */
(function () {
  "use strict";

  var REPO = "PiBOH/vivi-music";
  var API_LIST = "https://api.github.com/repos/" + REPO + "/releases?per_page=30";
  var RELEASES_PAGE = "https://github.com/" + REPO + "/releases";

  var CHANNEL_SUFFIX = /-(nightly|alpha|beta|rc|stable)$/i;
  var releases = [];
  var currentChannel = "stable";

  function isStable(r) {
    return !r.prerelease && !CHANNEL_SUFFIX.test(r.tag_name || "");
  }
  function isPre(r) {
    return r.prerelease || /-(nightly|alpha|beta|rc)$/i.test(r.tag_name || "");
  }

  function firstByExt(assets, ext) {
    var hit = null;
    (assets || []).forEach(function (a) {
      if (hit) return;
      var name = (a.name || "").toLowerCase();
      if (name.split(".").pop() === ext) hit = a.browser_download_url;
    });
    return hit;
  }

  function applyChannel() {
    var rel = currentChannel === "nightly"
      ? (releases.find(isPre) || releases[0])
      : (releases.find(isStable) || releases[0]);

    var tag = rel ? (rel.tag_name || "") : "";
    document.querySelectorAll("[data-version]").forEach(function (el) {
      el.textContent = tag || "GitHub Releases";
    });

    var links = {
      winExe: firstByExt(rel && rel.assets, "exe"),
      winMsi: firstByExt(rel && rel.assets, "msi"),
      deb: firstByExt(rel && rel.assets, "deb"),
      appimage: firstByExt(rel && rel.assets, "appimage"),
      dmg: firstByExt(rel && rel.assets, "dmg"),
      pkg: firstByExt(rel && rel.assets, "pkg"),
    };

    document.querySelectorAll("[data-download]").forEach(function (a) {
      var url = links[a.getAttribute("data-download")];
      if (url) {
        a.href = url;
        a.classList.remove("disabled");
        a.removeAttribute("title");
      } else {
        a.classList.add("disabled");
        a.title = "Not in this release";
      }
    });

    var body = document.getElementById("changelog-body");
    if (body && rel && rel.body) body.textContent = rel.body.trim();
    var tagEl = document.getElementById("changelog-version");
    if (tagEl && tag) tagEl.textContent = tag;
  }

  function selectChannel(ch, button) {
    currentChannel = ch;
    document.querySelectorAll(".tab").forEach(function (t) {
      t.classList.toggle("active", t === button);
    });
    applyChannel();
  }

  function init() {
    var toggle = document.querySelector(".nav-toggle");
    var links = document.querySelector(".nav-links");
    if (toggle && links) {
      toggle.addEventListener("click", function () {
        var open = links.classList.toggle("open");
        toggle.setAttribute("aria-expanded", open ? "true" : "false");
        toggle.textContent = open ? "\u2715" : "\u2630";
      });
    }

    document.querySelectorAll(".tab").forEach(function (t) {
      t.addEventListener("click", function () {
        selectChannel(t.getAttribute("data-channel"), t);
      });
    });

    fetch(API_LIST, { headers: { Accept: "application/vnd.github+json" } })
      .then(function (res) {
        if (!res.ok) throw new Error("api");
        return res.json();
      })
      .then(function (list) {
        releases = Array.isArray(list) ? list : [];
        // If only pre-releases exist, default to Nightly so the buttons always work.
        if (!releases.find(isStable) && releases.find(isPre)) {
          currentChannel = "nightly";
          document.querySelectorAll(".tab").forEach(function (t) {
            t.classList.toggle("active", t.getAttribute("data-channel") === "nightly");
          });
        }
        applyChannel();
      })
      .catch(function () {
        document.querySelectorAll("[data-download]").forEach(function (a) {
          a.href = RELEASES_PAGE;
          a.classList.remove("disabled");
        });
        document.querySelectorAll("[data-version]").forEach(function (el) {
          el.textContent = "GitHub Releases";
        });
      });
  }

  if (document.readyState === "loading") {
    document.addEventListener("DOMContentLoaded", init);
  } else {
    init();
  }
})();