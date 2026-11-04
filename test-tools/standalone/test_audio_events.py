"""Every event in sounds.json is registered by AudioCues.java and (unless it is a music, UI or ambient bed event) has a subtitle in sounds.json and en_us.json."""
import json, re, unittest
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
ASSETS = ROOT / "src/main/resources/assets/gravesandglory"
CUES = ROOT / "src/main/java/gg/gravesandglory/AudioCues.java"
SOURCES = ROOT / "src/main/java/gg/gravesandglory"
# Cues Kort kept off (no game moment): the only registered events allowed to have no play site.
OFF = {"troop_voices", "march_leather", "siege_ram_swing", "gate_chains", "banner_flap"}

# Events that show no subtitle by design: UI clicks, the countdown tick, streamed music, and the long ambient beds.
NO_SUBTITLE = {"countdown_tick", "camp_room_tone", "survival_wind"}
NO_SUBTITLE_PREFIXES = ("ui_", "music.", "ambience_")
NO_SUBTITLE_SUFFIXES = ("_music",)


def registered_ids():
    src = CUES.read_text(encoding="utf-8")
    ids = re.findall(r'^\s+[A-Z_]+\("([a-z_.]+)",\s*null,', src, re.M)
    horns = re.search(r'HORNS\s*=\s*List\.of\(([^)]*)\)', src).group(1)
    return ids + re.findall(r'"([a-z_]+)"', horns)


def needs_subtitle(event):
    return not (event in NO_SUBTITLE or event.startswith(NO_SUBTITLE_PREFIXES) or event.endswith(NO_SUBTITLE_SUFFIXES))


class AudioEventsTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.sounds = json.loads((ASSETS / "sounds.json").read_text(encoding="utf-8"))
        cls.lang = json.loads((ASSETS / "lang/en_us.json").read_text(encoding="utf-8"))
        cls.ids = registered_ids()

    def test_every_sounds_json_event_is_registered(self):
        self.assertEqual(sorted(set(self.sounds) - set(self.ids)), [], "events in sounds.json that AudioCues does not register")

    def test_every_registered_event_is_in_sounds_json(self):
        self.assertEqual(sorted(set(self.ids) - set(self.sounds)), [], "registered events with no sounds.json entry")
        self.assertEqual(len(self.ids), len(set(self.ids)), "an event id is registered twice")

    def test_every_event_has_a_subtitle(self):
        for event, entry in self.sounds.items():
            key = f"subtitles.gravesandglory.{event}"
            if needs_subtitle(event):
                self.assertEqual(entry.get("subtitle"), key, f"{event}: sounds.json subtitle")
                self.assertTrue(self.lang.get(key), f"{event}: en_us.json has {key}")
            else:
                self.assertNotIn("subtitle", entry, f"{event}: music, UI and ambient beds show no subtitle")

    def test_every_variant_file_exists(self):
        for event, entry in self.sounds.items():
            for s in entry["sounds"]:
                name = s["name"].split(":", 1)[1]
                self.assertTrue((ASSETS / "sounds" / f"{name}.ogg").exists(), f"{event}: {name}.ogg is in the repo")

    def test_every_event_has_a_play_site(self):
        """Each cue's enum name appears as Cue.NAME in some Java source (the ambience beds in AudioCues.bed, which MusicDirector calls), except the five kept off."""
        src = CUES.read_text(encoding="utf-8")
        names = re.findall(r'^\s+([A-Z_]+)\("([a-z_.]+)",\s*null,', src, re.M)
        code = "\n".join(p.read_text(encoding="utf-8") for p in SOURCES.rglob("*.java"))
        director = (SOURCES / "MusicDirector.java").read_text(encoding="utf-8")
        for name, event in names:
            played = f"Cue.{name}" in code
            if event in OFF:
                self.assertFalse(played, f"{event} is kept off, so nothing plays it")
            else:
                self.assertTrue(played, f"{event}: no play site (Cue.{name})")
        events = [e for _, e in names]
        for bed in ("ambience_wind", "ambience_birds", "ambience_water", "ambience_crows", "ambience_snow", "camp_room_tone", "survival_wind"):
            self.assertIn(bed, events)
        self.assertIn("AudioCues.bed(", director, "MusicDirector plays the beds AudioCues.bed picks")
        self.assertIn("CAMPAIGN_BRIEFING_MUSIC", director)
        self.assertIn("CampaignClient.overtime()", director)

    def test_overtime_flag_is_in_the_snapshot(self):
        mode = (SOURCES / "BattleMode.java").read_text(encoding="utf-8")
        client = (SOURCES / "CampaignClient.java").read_text(encoding="utf-8")
        self.assertIn('addProperty("overtime"', mode)
        self.assertIn('"overtime"', client)


if __name__ == "__main__":
    unittest.main()
