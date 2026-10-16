#!/usr/bin/env python3
"""
Deterministic generator for ToMakeTheCut mock API fixtures.

Why a generator instead of hand-written JSON?
  * ~1,000 odds lines per tournament are impossible to keep consistent by hand.
  * Odds, cut outcomes and the "recent results" shown in player stats all come
    from ONE simulation, so the data tells a coherent story (a player whose
    odds collapse in R2 shows "MC" in their results list).
  * Fixed RNG seed => byte-identical output => clean diffs in code review.

The numbers are ILLUSTRATIVE. Player names, countries, tournament names,
venues and a handful of well-known 2025 results are real; per-round scores,
prices and statistics are simulated. Swap the mock API for a real provider
before using this for anything that involves money.

Usage:  python3 tools/mockdata/generate.py
Output: core/data/src/main/resources/mock/**.json
"""
import json
import math
import os
import random
from datetime import date, datetime, timedelta, timezone

ROOT = os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
OUT = os.path.join(ROOT, "core", "data", "src", "main", "resources", "mock")

rng = random.Random(20260928)

PGA, KFT = "PGA_TOUR", "KORN_FERRY_TOUR"

# id, name, country, {season: (tour, strokes-gained-total per round)}
PLAYERS = [
    ("scottie-scheffler", "Scottie Scheffler", "USA", {2025: (PGA, 3.0), 2026: (PGA, 2.8)}),
    ("rory-mcilroy", "Rory McIlroy", "NIR", {2025: (PGA, 1.9), 2026: (PGA, 1.7)}),
    ("xander-schauffele", "Xander Schauffele", "USA", {2025: (PGA, 1.1), 2026: (PGA, 1.4)}),
    ("collin-morikawa", "Collin Morikawa", "USA", {2025: (PGA, 1.3), 2026: (PGA, 1.2)}),
    ("ludvig-aberg", "Ludvig Åberg", "SWE", {2025: (PGA, 1.2), 2026: (PGA, 1.4)}),
    ("justin-thomas", "Justin Thomas", "USA", {2025: (PGA, 1.3), 2026: (PGA, 1.2)}),
    ("russell-henley", "Russell Henley", "USA", {2025: (PGA, 1.4), 2026: (PGA, 1.2)}),
    ("tommy-fleetwood", "Tommy Fleetwood", "ENG", {2025: (PGA, 1.5), 2026: (PGA, 1.4)}),
    ("sepp-straka", "Sepp Straka", "AUT", {2025: (PGA, 0.9), 2026: (PGA, 0.9)}),
    ("keegan-bradley", "Keegan Bradley", "USA", {2025: (PGA, 0.8), 2026: (PGA, 0.7)}),
    ("hideki-matsuyama", "Hideki Matsuyama", "JPN", {2025: (PGA, 0.9), 2026: (PGA, 0.9)}),
    ("viktor-hovland", "Viktor Hovland", "NOR", {2025: (PGA, 0.5), 2026: (PGA, 0.8)}),
    ("patrick-cantlay", "Patrick Cantlay", "USA", {2025: (PGA, 1.2), 2026: (PGA, 1.0)}),
    ("shane-lowry", "Shane Lowry", "IRL", {2025: (PGA, 1.0), 2026: (PGA, 0.9)}),
    ("jj-spaun", "J.J. Spaun", "USA", {2025: (PGA, 1.1), 2026: (PGA, 1.0)}),
    ("harris-english", "Harris English", "USA", {2025: (PGA, 0.9), 2026: (PGA, 0.8)}),
    ("ben-griffin", "Ben Griffin", "USA", {2025: (PGA, 1.1), 2026: (PGA, 1.1)}),
    ("maverick-mcnealy", "Maverick McNealy", "USA", {2025: (PGA, 0.8), 2026: (PGA, 0.7)}),
    ("robert-macintyre", "Robert MacIntyre", "SCO", {2025: (PGA, 1.0), 2026: (PGA, 1.0)}),
    ("corey-conners", "Corey Conners", "CAN", {2025: (PGA, 0.9), 2026: (PGA, 0.8)}),
    ("sam-burns", "Sam Burns", "USA", {2025: (PGA, 0.8), 2026: (PGA, 0.8)}),
    ("cameron-young", "Cameron Young", "USA", {2025: (PGA, 0.9), 2026: (PGA, 1.0)}),
    ("wyndham-clark", "Wyndham Clark", "USA", {2025: (PGA, 0.1), 2026: (PGA, 0.3)}),
    ("jordan-spieth", "Jordan Spieth", "USA", {2025: (PGA, 0.6), 2026: (PGA, 0.6)}),
    ("min-woo-lee", "Min Woo Lee", "AUS", {2025: (PGA, 0.4), 2026: (PGA, 0.5)}),
    ("akshay-bhatia", "Akshay Bhatia", "USA", {2025: (PGA, 0.6), 2026: (PGA, 0.6)}),
    ("sungjae-im", "Sungjae Im", "KOR", {2025: (PGA, 0.3), 2026: (PGA, 0.5)}),
    ("tony-finau", "Tony Finau", "USA", {2025: (PGA, 0.3), 2026: (PGA, 0.3)}),
    ("max-greyserman", "Max Greyserman", "USA", {2025: (PGA, 0.5), 2026: (PGA, 0.5)}),
    ("nick-taylor", "Nick Taylor", "CAN", {2025: (PGA, 0.4), 2026: (PGA, 0.4)}),
    # 2025 Korn Ferry Tour graduates -> 2026 PGA TOUR rookies
    ("johnny-keefer", "Johnny Keefer", "USA", {2025: (KFT, 1.2), 2026: (PGA, 0.2)}),
    ("chandler-blanchet", "Chandler Blanchet", "USA", {2025: (KFT, 0.9), 2026: (PGA, 0.0)}),
    ("pontus-nyholm", "Pontus Nyholm", "SWE", {2025: (KFT, 0.8), 2026: (PGA, -0.1)}),
    # Korn Ferry Tour both seasons; plays PGA TOUR events on exemptions
    ("neal-shipley", "Neal Shipley", "USA", {2025: (KFT, 0.5), 2026: (KFT, 0.9)}),
]
PLAYER_BY_ID = {p[0]: p for p in PLAYERS}

# Known real-world 2025 winners, used so the mock data doesn't contradict history.
KNOWN_2025_WINNERS = {
    "genesis-invitational-2025": "ludvig-aberg",
    "arnold-palmer-invitational-2025": "russell-henley",
    "the-players-championship-2025": "rory-mcilroy",
    "masters-tournament-2025": "rory-mcilroy",
    "pga-championship-2025": "scottie-scheffler",
    "the-memorial-tournament-2025": "scottie-scheffler",
    "us-open-2025": "jj-spaun",
    "the-open-championship-2025": "scottie-scheffler",
    "wyndham-championship-2025": "cameron-young",
}
KNOWN_2025_WIN_COUNTS = {
    "scottie-scheffler": 6, "rory-mcilroy": 3, "ben-griffin": 2, "russell-henley": 1,
    "ludvig-aberg": 1, "jj-spaun": 1, "cameron-young": 1, "tommy-fleetwood": 1,
    "justin-thomas": 1, "harris-english": 1, "sepp-straka": 2, "keegan-bradley": 1,
    "collin-morikawa": 0, "xander-schauffele": 0, "johnny-keefer": 2, "chandler-blanchet": 1,
}

MAJOR, SIGNATURE, FULL_FIELD = "MAJOR", "SIGNATURE", "FULL_FIELD"

# id suffix, name, course, location, start date, type, cut rule text
CALENDAR = {
    2025: [
        ("wm-phoenix-open", "WM Phoenix Open", "TPC Scottsdale (Stadium)", "Scottsdale, AZ", date(2025, 2, 6), FULL_FIELD, "Top 65 & ties"),
        ("genesis-invitational", "The Genesis Invitational", "Torrey Pines (South)", "San Diego, CA", date(2025, 2, 13), SIGNATURE, "Top 50 & ties"),
        ("arnold-palmer-invitational", "Arnold Palmer Invitational", "Bay Hill Club & Lodge", "Orlando, FL", date(2025, 3, 6), SIGNATURE, "Top 50 & ties"),
        ("the-players-championship", "THE PLAYERS Championship", "TPC Sawgrass (Stadium)", "Ponte Vedra Beach, FL", date(2025, 3, 13), MAJOR, "Top 65 & ties"),
        ("masters-tournament", "Masters Tournament", "Augusta National Golf Club", "Augusta, GA", date(2025, 4, 10), MAJOR, "Top 50 & ties"),
        ("pga-championship", "PGA Championship", "Quail Hollow Club", "Charlotte, NC", date(2025, 5, 15), MAJOR, "Top 70 & ties"),
        ("the-memorial-tournament", "the Memorial Tournament", "Muirfield Village Golf Club", "Dublin, OH", date(2025, 5, 29), SIGNATURE, "Top 50 & ties"),
        ("us-open", "U.S. Open", "Oakmont Country Club", "Oakmont, PA", date(2025, 6, 12), MAJOR, "Top 60 & ties"),
        ("the-open-championship", "The Open Championship", "Royal Portrush", "Portrush, NIR", date(2025, 7, 17), MAJOR, "Top 70 & ties"),
        ("wyndham-championship", "Wyndham Championship", "Sedgefield Country Club", "Greensboro, NC", date(2025, 8, 7), FULL_FIELD, "Top 65 & ties"),
    ],
    2026: [
        ("wm-phoenix-open", "WM Phoenix Open", "TPC Scottsdale (Stadium)", "Scottsdale, AZ", date(2026, 2, 5), FULL_FIELD, "Top 65 & ties"),
        ("genesis-invitational", "The Genesis Invitational", "Riviera Country Club", "Pacific Palisades, CA", date(2026, 2, 19), SIGNATURE, "Top 50 & ties"),
        ("arnold-palmer-invitational", "Arnold Palmer Invitational", "Bay Hill Club & Lodge", "Orlando, FL", date(2026, 3, 5), SIGNATURE, "Top 50 & ties"),
        ("the-players-championship", "THE PLAYERS Championship", "TPC Sawgrass (Stadium)", "Ponte Vedra Beach, FL", date(2026, 3, 12), MAJOR, "Top 65 & ties"),
        ("masters-tournament", "Masters Tournament", "Augusta National Golf Club", "Augusta, GA", date(2026, 4, 9), MAJOR, "Top 50 & ties"),
        ("pga-championship", "PGA Championship", "Aronimink Golf Club", "Newtown Square, PA", date(2026, 5, 14), MAJOR, "Top 70 & ties"),
        ("the-memorial-tournament", "the Memorial Tournament", "Muirfield Village Golf Club", "Dublin, OH", date(2026, 6, 4), SIGNATURE, "Top 50 & ties"),
        ("us-open", "U.S. Open", "Shinnecock Hills Golf Club", "Southampton, NY", date(2026, 6, 18), MAJOR, "Top 60 & ties"),
        ("the-open-championship", "The Open Championship", "Royal Birkdale", "Southport, ENG", date(2026, 7, 16), MAJOR, "Top 70 & ties"),
        ("wyndham-championship", "Wyndham Championship", "Sedgefield Country Club", "Greensboro, NC", date(2026, 8, 6), FULL_FIELD, "Top 65 & ties"),
        ("procore-championship", "Procore Championship", "Silverado Resort (North)", "Napa, CA", date(2026, 9, 24), FULL_FIELD, "Top 65 & ties"),
        ("sanderson-farms-championship", "Sanderson Farms Championship", "The Country Club of Jackson", "Jackson, MS", date(2026, 10, 1), FULL_FIELD, "Top 65 & ties"),
    ],
}

KFT_EVENTS = [
    "Astara Golf Championship", "Panama Championship", "Club Car Championship",
    "Veritex Bank Championship", "Visit Knoxville Open", "UNC Health Championship",
    "Utah Championship", "Albertsons Boise Open", "Nationwide Children's Hospital Championship",
    "Korn Ferry Tour Championship",
]

# "Now" for the mock universe. Everything after this is UPCOMING.
NOW = datetime(2026, 9, 28, 16, 0, tzinfo=timezone.utc)

BOOKS = ["draftkings", "fanduel", "betmgm", "caesars"]
BOOK_BIAS = {"draftkings": 0.0, "fanduel": 0.06, "betmgm": -0.08, "caesars": 0.04}
BOOK_VIG = {"draftkings": 0.045, "fanduel": 0.04, "betmgm": 0.055, "caesars": 0.05}

# Cut threshold T in "strokes relative to field" for P(make) = Phi((T + 2s) / SIGMA36)
THRESHOLD = {MAJOR: -0.6, SIGNATURE: 0.9, FULL_FIELD: 0.5}
SIGMA_ROUND = 2.9
SIGMA36 = SIGMA_ROUND * math.sqrt(2)

# (label, days before R1 or None, holes completed)
SNAPSHOTS = [
    ("Opening line", -4, 0), ("Monday", -3, 0), ("Tuesday", -2, 0), ("Eve of R1", -1, 0),
    ("R1 thru 6", 0, 6), ("R1 thru 12", 0, 12), ("After R1", 0, 18),
    ("R2 thru 6", 1, 24), ("R2 thru 12", 1, 30), ("R2 thru 15", 1, 33),
]


def phi(x):
    return 0.5 * (1 + math.erf(x / math.sqrt(2)))


def logit(p):
    p = min(max(p, 1e-4), 1 - 1e-4)
    return math.log(p / (1 - p))


def sigmoid(x):
    return 1 / (1 + math.exp(-x))


def to_american(p):
    p = min(max(p, 0.001), 0.999)
    if p >= 0.5:
        v = -100 * p / (1 - p)
    else:
        v = 100 * (1 - p) / p
    step = 5 if abs(v) < 1000 else 50
    v = int(round(v / step) * step)
    if -100 < v < 100:
        v = -100 if v <= 0 else 100
    return v


def season_players(season, predicate=lambda s: True):
    return [p for p in PLAYERS if p[3][season][0] == PGA and predicate(p[3][season][1])]


def field_for(season, event_type, tid):
    if event_type == MAJOR:
        return season_players(season)
    if event_type == SIGNATURE:
        return season_players(season, lambda s: s >= 0.6)
    field = season_players(season, lambda s: s < 1.2)
    if tid.startswith("sanderson") or tid.startswith("procore"):
        field = season_players(season, lambda s: s < 1.0)
    field += [p for p in PLAYERS if p[3][season][0] == KFT and season == 2026]
    # a couple of stars always tee it up at the Phoenix Open
    if tid.startswith("wm-phoenix"):
        field += [PLAYER_BY_ID["scottie-scheffler"], PLAYER_BY_ID["justin-thomas"]]
    return field


def iso(dt):
    return dt.strftime("%Y-%m-%dT%H:%M:%SZ")


def simulate_event(season, ev):
    slug, name, course, location, start, etype, cut_rule = ev
    tid = f"{slug}-{season}"
    start_dt = datetime(start.year, start.month, start.day, tzinfo=timezone.utc)
    end = start + timedelta(days=3)
    status = "COMPLETED" if datetime(end.year, end.month, end.day, 23, tzinfo=timezone.utc) < NOW else "UPCOMING"
    field = field_for(season, etype, tid)
    T = THRESHOLD[etype]
    forced_winner = KNOWN_2025_WINNERS.get(tid)

    snapshots = []
    for i, (label, day, holes) in enumerate(SNAPSHOTS):
        hour = {0: 12 if day > -4 else 20, 6: 15, 12: 18, 18: 23, 24: 15, 30: 18, 33: 20}[holes]
        captured = start_dt + timedelta(days=day, hours=hour)
        if status == "UPCOMING" and captured > NOW:
            continue
        snapshots.append({"id": f"s{i}", "label": label, "round": 0 if holes == 0 else (1 if holes <= 18 else 2),
                          "holesCompleted": holes, "capturedAt": iso(captured)})

    players_out, results = [], {}
    for pid, pname, country, seasons in field:
        s = seasons[season][1]
        # Rejection-sample the known 2025 winner so history is respected.
        while True:
            holes = [rng.gauss(-s / 18, SIGMA_ROUND / math.sqrt(18)) for _ in range(36)]
            x36 = sum(holes)
            if forced_winner != pid or x36 <= T - 2.5:
                break
        made = x36 <= T
        # Pre-tournament market view drifts a little with "form news".
        pre_drift = rng.gauss(0, 0.12)
        lines = []
        for snap in snapshots:
            idx = int(snap["id"][1:])
            h = SNAPSHOTS[idx][2]
            if h == 0:
                drift = pre_drift * (idx + 1) / 4
                p = phi((T + 2 * (s + drift)) / SIGMA36)
            else:
                xh = sum(holes[:h])
                rem = 36 - h
                p = phi((T - xh + s * rem / 18) / (SIGMA_ROUND * math.sqrt(rem / 18)))
            for book in BOOKS:
                if book == "betmgm" and s < 0.4:
                    continue  # BetMGM doesn't hang a price on everyone
                if book == "caesars" and idx == 0:
                    continue  # Caesars posts late
                # Books disagree in log-odds space, which keeps the noise sane near 0% / 100%.
                pb = sigmoid(logit(p) + BOOK_BIAS[book] + rng.gauss(0, 0.1))
                if pb > 0.985 or pb < 0.015:
                    continue  # market pulled at the extremes
                vig = BOOK_VIG[book] / 2
                lines.append({"sportsbook": book, "snapshotId": snap["id"],
                              "yesPrice": to_american(pb * (1 + vig)),
                              "noPrice": to_american((1 - pb) * (1 + vig))})
        entry = {"playerId": pid, "playerName": pname, "country": country,
                 "tour": seasons[season][0], "lines": lines}
        if status == "COMPLETED":
            entry["result"] = "MADE_CUT" if made else "MISSED_CUT"
            x72 = x36 + sum(rng.gauss(-s / 18, SIGMA_ROUND / math.sqrt(18)) for _ in range(36))
            results[pid] = (made, x72)
        players_out.append(entry)

    finishes = {}
    if status == "COMPLETED":
        made_sorted = sorted([(x, pid) for pid, (m, x) in results.items() if m])
        cut_size = {MAJOR: 70, SIGNATURE: 50, FULL_FIELD: 70}[etype]
        for rank, (x, pid) in enumerate(made_sorted):
            pos = 1 + int(round(rank / max(1, len(made_sorted)) * cut_size))
            if rank == 0:
                if forced_winner:
                    pos = 1 if pid == forced_winner else 2
                elif season == 2026:
                    pos = 1 if rng.random() < 0.3 else rng.randint(2, 5)
            if forced_winner == pid:
                pos = 1
            finishes[pid] = pos
        for pid, (m, _) in results.items():
            if not m:
                finishes[pid] = "MC"
        # Ensure a forced winner that isn't lowest in our sample still shows "1"
        if forced_winner in finishes:
            finishes[forced_winner] = 1
        # Collapse duplicates into ties like "T12"
        counts = {}
        for v in finishes.values():
            counts[v] = counts.get(v, 0) + 1
        finishes = {pid: ("MC" if v == "MC" else (f"T{v}" if counts[v] > 1 and v != 1 else str(v)))
                    for pid, v in finishes.items()}

    tournament = {"id": tid, "name": name, "course": course, "location": location,
                  "season": season, "startDate": start.isoformat(), "endDate": end.isoformat(),
                  "status": status, "eventType": etype, "cutRule": cut_rule}
    market = {"tournamentId": tid, "market": "make_cut", "snapshots": snapshots, "players": players_out}
    return tournament, market, finishes, end


def build_stats(pid, season, pga_results):
    _, name, country, seasons = PLAYER_BY_ID[pid]
    tour, s = seasons[season]
    r = random.Random(f"{pid}-{season}")
    style = random.Random(pid)  # stable per player across seasons
    ott_bias, app_bias, arg_bias = style.gauss(0, 0.3), style.gauss(0, 0.3), style.gauss(0, 0.15)
    sg_total = round(s + r.gauss(0, 0.05), 2)
    ott = round(sg_total * 0.28 + ott_bias, 2)
    app = round(sg_total * 0.42 + app_bias, 2)
    arg = round(sg_total * 0.12 + arg_bias, 2)
    putt = round(sg_total - ott - app - arg, 2)
    events = r.randint(18, 25) if tour == PGA else r.randint(19, 24)
    if pid == "scottie-scheffler":
        events = 20

    if tour == PGA:
        results = sorted(pga_results, key=lambda x: x["date"], reverse=True)
    else:
        results = []
        kft_start = date(season, 3, 1)
        for i, ev in enumerate(r.sample(KFT_EVENTS, 8)):
            d = kft_start + timedelta(days=21 * i)
            made = r.random() < phi((0.5 + 2 * s) / SIGMA36)
            fin = "MC" if not made else (f"T{r.randint(2, 40)}" if r.random() > 0.15 * s else str(r.randint(1, 3)))
            results.append({"tournamentName": ev, "date": d.isoformat(), "finish": fin, "tour": KFT})
        # Exemption starts in PGA events are part of the same story
        results += pga_results
        results.sort(key=lambda x: x["date"], reverse=True)

    played_made = sum(1 for x in results if x["finish"] != "MC")
    other = max(0, events - len(results))
    p_make = phi((0.5 + 2 * s) / SIGMA36)
    cuts = played_made + sum(1 for _ in range(other) if r.random() < p_make)
    if season == 2025 and pid in KNOWN_2025_WIN_COUNTS:
        wins = KNOWN_2025_WIN_COUNTS[pid]
    else:
        wins = sum(1 for x in results if x["finish"] == "1")
        wins += sum(1 for _ in range(other) if r.random() < max(0.0, (s - 0.8) * 0.06))
    top10 = max(wins, sum(1 for x in results if x["finish"] != "MC" and int(x["finish"].lstrip("T")) <= 10)
                + sum(1 for _ in range(other) if r.random() < max(0.02, (s - 0.2) * 0.2)))
    cuts = min(events, max(cuts, top10))

    rank = {"johnny-keefer": 1, "chandler-blanchet": 2, "pontus-nyholm": 11}.get(pid) if season == 2025 else None
    if rank is None:
        ordered = sorted([p for p in PLAYERS if p[3][season][0] == tour], key=lambda p: -p[3][season][1])
        base = [p[0] for p in ordered].index(pid) + 1
        rank = base if pid == "scottie-scheffler" else base + r.randint(0, 3) * base // 3 + r.randint(0, 4)
        if tour == KFT and season == 2026:
            rank = 18
        if tour == KFT and season == 2025 and pid == "neal-shipley":
            rank = 43

    return {
        "playerId": pid, "playerName": name, "country": country, "season": season, "tour": tour,
        "eventsPlayed": events, "cutsMade": cuts, "wins": wins, "top10s": top10,
        "scoringAverage": round(71.0 - 0.95 * sg_total + r.gauss(0, 0.08), 2),
        "strokesGained": {"total": sg_total, "offTheTee": ott, "approach": app,
                          "aroundTheGreen": arg, "putting": putt},
        "drivingDistance": round(style.uniform(288, 318) + r.gauss(0, 2), 1),
        "drivingAccuracyPct": round(min(75, max(50, style.uniform(54, 68) + r.gauss(0, 1.5))), 1),
        "greensInRegulationPct": round(min(74, max(62, 66 + sg_total * 1.6 + r.gauss(0, 0.8))), 1),
        "pointsRank": rank,
        "pointsLabel": "FedExCup" if tour == PGA else "Korn Ferry Tour Points",
        "recentResults": results[:10],
    }


def write(path, data, compact=False):
    full = os.path.join(OUT, path)
    os.makedirs(os.path.dirname(full), exist_ok=True)
    with open(full, "w", encoding="utf-8") as f:
        if compact:
            # Odds markets are large; one JSON object per line keeps diffs readable and the APK small.
            f.write(json.dumps({k: v for k, v in data.items() if k != "players"}, ensure_ascii=False)[:-1])
            f.write(',"players":[\n')
            f.write(",\n".join(json.dumps(p, ensure_ascii=False, separators=(",", ":")) for p in data["players"]))
            f.write("\n]}\n")
        else:
            json.dump(data, f, indent=2, ensure_ascii=False)
            f.write("\n")


def main():
    for season in (2025, 2026):
        tournaments = []
        player_results = {p[0]: [] for p in PLAYERS}
        for ev in CALENDAR[season]:
            t, market, finishes, end = simulate_event(season, ev)
            tournaments.append(t)
            write(f"odds/make_cut/{t['id']}.json", market, compact=True)
            for pid, fin in finishes.items():
                player_results[pid].append({"tournamentName": t["name"], "date": end.isoformat(),
                                            "finish": fin, "tour": PGA})
        write(f"odds/tournaments_{season}.json", {"season": season, "tournaments": tournaments})
        for pid, *_ in PLAYERS:
            write(f"stats/{pid}/{season}.json", build_stats(pid, season, player_results[pid]))
    print(f"Wrote fixtures to {OUT}")


if __name__ == "__main__":
    main()
