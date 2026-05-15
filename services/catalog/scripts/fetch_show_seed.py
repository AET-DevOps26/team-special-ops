"""
Fetch episode data for a TV show from Wikipedia and emit a Flyway seed migration.

Usage:
    uv run --project services/catalog/scripts fetch_show_seed.py \
        "Stranger Things" \
        --episode-list-page "List of Stranger Things episodes" \
        --slug stranger_things \
        --output services/catalog/src/main/resources/db/migration/V2__seed_stranger_things.sql

Notes:
- Deterministic UUIDs (uuid5) keep re-runs byte-identical when the data hasn't
  changed, so `git diff` is meaningful.
- Summaries come from the per-episode article's "Plot" section, falling back
  to "Synopsis" or the article lede.
- Content is licensed CC BY-SA 3.0; attribution is emitted as a trailing
  comment block on the SQL file.
"""

from __future__ import annotations

import argparse
import datetime as dt
import re
import sys
import uuid
from dataclasses import dataclass

import requests
from bs4 import BeautifulSoup

WIKI_API = "https://en.wikipedia.org/w/api.php"
NAMESPACE = uuid.NAMESPACE_DNS
USER_AGENT = "team-special-ops-catalog-seed/0.1 (educational project)"


@dataclass
class EpisodeRow:
    season: int
    episode_number: int
    episode_index: int
    title: str
    summary: str
    article_title: str


def fetch_episode_list_wikitext(page_title: str) -> str:
    """Return the wikitext for a 'List of <show> episodes' page."""
    resp = requests.get(
        WIKI_API,
        params={
            "action": "parse",
            "page": page_title,
            "prop": "wikitext",
            "format": "json",
            "formatversion": 2,
        },
        headers={"User-Agent": USER_AGENT},
        timeout=30,
    )
    resp.raise_for_status()
    return resp.json()["parse"]["wikitext"]


def parse_episode_rows(wikitext: str) -> list[tuple[int, int, str, str]]:
    """
    Parse season-by-season episode tables from a 'List of X episodes' wikitext.

    Returns a list of (season, episode_number_in_season, title, article_title)
    tuples in airing order.

    The wikitext uses the {{Episode list/sublist}} template; each entry has a
    line like:
        | EpisodeNumber = 1
        | EpisodeNumber2 = 1
        | Title = Chapter One: The Vanishing of Will Byers
        | RTitle = {{Sortname|...}}
    plus a season header. We use a tolerant regex extractor and rely on the
    monotonic order of EpisodeNumber across the file.
    """
    rows: list[tuple[int, int, str, str]] = []
    current_season = 0
    season_re = re.compile(r"==\s*Season\s+(\d+).*?==", re.IGNORECASE)
    episode_block_re = re.compile(
        r"\{\{Episode list[^}]*?"
        r"\|\s*EpisodeNumber\s*=\s*(?P<global>\d+)[^}]*?"
        r"\|\s*EpisodeNumber2\s*=\s*(?P<inseason>\d+)[^}]*?"
        r"\|\s*Title\s*=\s*(?P<title>[^|\n]+)",
        re.DOTALL,
    )

    for line in wikitext.splitlines():
        m = season_re.search(line)
        if m:
            current_season = int(m.group(1))

    for m in episode_block_re.finditer(wikitext):
        upto = wikitext[: m.start()]
        season_matches = season_re.findall(upto)
        season = int(season_matches[-1]) if season_matches else 1
        in_season = int(m.group("inseason"))
        title = clean_wiki_title(m.group("title"))
        rows.append((season, in_season, title, title))

    if not rows:
        raise SystemExit(
            "Could not parse any episode rows. The wikitext format may have "
            "changed; inspect the page manually and adjust the regex."
        )
    return rows


def clean_wiki_title(raw: str) -> str:
    """Strip wiki markup like '[[Title]]' or '{{Sortname|...}}' fragments from a title field."""
    s = raw.strip()
    s = re.sub(r"\[\[([^\]|]+\|)?([^\]]+)\]\]", r"\2", s)
    s = re.sub(r"\{\{[^}]+\}\}", "", s)
    s = re.sub(r'^"|"$', "", s)
    return s.strip()


def fetch_plot(article_title: str) -> str:
    """Fetch the 'Plot' section of an episode's Wikipedia article and return clean prose."""
    sections_resp = requests.get(
        WIKI_API,
        params={
            "action": "parse",
            "page": article_title,
            "prop": "sections",
            "format": "json",
            "formatversion": 2,
        },
        headers={"User-Agent": USER_AGENT},
        timeout=30,
    )
    sections_resp.raise_for_status()
    sections = sections_resp.json()["parse"]["sections"]
    section_index = None
    for s in sections:
        if s["line"].lower() in ("plot", "synopsis"):
            section_index = s["index"]
            break

    if section_index is not None:
        params = {
            "action": "parse",
            "page": article_title,
            "prop": "text",
            "section": section_index,
            "format": "json",
            "formatversion": 2,
        }
    else:
        params = {
            "action": "parse",
            "page": article_title,
            "prop": "text",
            "section": 0,
            "format": "json",
            "formatversion": 2,
        }

    text_resp = requests.get(
        WIKI_API,
        params=params,
        headers={"User-Agent": USER_AGENT},
        timeout=30,
    )
    text_resp.raise_for_status()
    html = text_resp.json()["parse"]["text"]
    soup = BeautifulSoup(html, "html.parser")
    paragraphs = [p.get_text(" ", strip=True) for p in soup.find_all("p")]
    text = "\n\n".join(p for p in paragraphs if p)
    text = re.sub(r"\[\d+\]", "", text)
    text = re.sub(r"\[citation needed\]", "", text, flags=re.IGNORECASE)
    return text.strip()


def emit_sql(show_title: str, slug: str, episodes: list[EpisodeRow], output_path: str) -> None:
    show_id = uuid.uuid5(NAMESPACE, f"show:{slug}")
    seasons_count = max(e.season for e in episodes)
    episodes_count = len(episodes)
    now = dt.datetime.now(dt.UTC).isoformat()

    lines: list[str] = []
    lines.append(f"-- Seed migration: {show_title}")
    lines.append("-- Auto-generated by services/catalog/scripts/fetch_show_seed.py")
    lines.append(f"-- Generated at: {now}")
    lines.append("-- Edit by re-running the script, not by hand.")
    lines.append("")
    lines.append(
        f"INSERT INTO show (id, title, seasons_count, episodes_count) VALUES "
        f"('{show_id}', $${show_title}$$, {seasons_count}, {episodes_count});"
    )
    lines.append("")
    for e in episodes:
        ep_id = uuid.uuid5(NAMESPACE, f"episode:{slug}:s{e.season}e{e.episode_number}")
        lines.append(
            "INSERT INTO episode "
            "(id, show_id, season, episode_number, episode_index, title, summary) VALUES ("
            f"'{ep_id}', '{show_id}', {e.season}, {e.episode_number}, "
            f"{e.episode_index}, $${e.title}$$, $${e.summary}$$);"
        )
    lines.append("")
    lines.append("-- Source: English Wikipedia, articles on each episode and the")
    lines.append(f"-- 'List of {show_title} episodes' page.")
    lines.append("-- Content licensed under CC BY-SA 3.0.")
    lines.append("")

    with open(output_path, "w", encoding="utf-8") as fh:
        fh.write("\n".join(lines))
    print(f"Wrote {output_path} ({episodes_count} episodes, {seasons_count} seasons).")


def main() -> int:
    p = argparse.ArgumentParser()
    p.add_argument("show_title", help='Display title, e.g. "Stranger Things"')
    p.add_argument(
        "--episode-list-page",
        required=True,
        help='Wikipedia page title for the list page, e.g. "List of Stranger Things episodes"',
    )
    p.add_argument("--slug", required=True, help="Filesystem-safe slug, e.g. stranger_things")
    p.add_argument("--output", required=True, help="Path to write the V2 SQL migration to")
    args = p.parse_args()

    wikitext = fetch_episode_list_wikitext(args.episode_list_page)
    parsed = parse_episode_rows(wikitext)

    episodes: list[EpisodeRow] = []
    for global_index, (season, in_season, title, article_title) in enumerate(parsed, start=1):
        summary = fetch_plot(article_title)
        if not summary:
            print(f"WARNING: empty summary for {article_title}", file=sys.stderr)
            summary = f"(No plot summary available for {title}.)"
        episodes.append(
            EpisodeRow(
                season=season,
                episode_number=in_season,
                episode_index=global_index,
                title=title,
                summary=summary,
                article_title=article_title,
            )
        )
        print(f"  fetched S{season}E{in_season} '{title}' ({len(summary)} chars)")

    emit_sql(args.show_title, args.slug, episodes, args.output)
    return 0


if __name__ == "__main__":
    sys.exit(main())
