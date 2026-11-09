import os
import subprocess
import sys
import tempfile
import unittest

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
import booth_git as bg  # noqa: E402
from booth_config import BoothError  # noqa: E402


def git(repo, *args):
    return subprocess.run(["git", "-C", repo, *args], check=True, capture_output=True, text=True).stdout.strip()


def put(repo, rel, data):
    p = os.path.join(repo, *rel.split("/"))
    os.makedirs(os.path.dirname(p), exist_ok=True)
    with open(p, "wb") as f:
        f.write(data if isinstance(data, bytes) else data.encode("utf-8"))


def make_repo(tmp):
    repo = os.path.join(tmp, "repo")
    os.makedirs(repo)
    git(repo, "init", "-q", "-b", "target")
    git(repo, "config", "user.name", "t")
    git(repo, "config", "user.email", "t@example.invalid")
    git(repo, "config", "core.autocrlf", "false")
    git(repo, "config", "commit.gpgsign", "false")
    return repo


def commit(repo, msg):
    git(repo, "add", "-A")
    git(repo, "commit", "-q", "-m", msg)
    return git(repo, "rev-parse", "HEAD")


class GitTest(unittest.TestCase):
    def setUp(self):
        self.tmp = tempfile.TemporaryDirectory()
        self.addCleanup(self.tmp.cleanup)
        self.repo = make_repo(self.tmp.name)
        put(self.repo, "assets/x.geo.json", "old geo")
        put(self.repo, "assets/blob.bin", bytes(range(256)))
        put(self.repo, "art/anim/spear/build.py", "v1")
        put(self.repo, "art/anim/common/feel.py", "v1")
        put(self.repo, "src/A.java", "v1")
        self.target_sha = commit(self.repo, "base")
        git(self.repo, "checkout", "-q", "-b", "lane")
        put(self.repo, "assets/x.geo.json", "new geo")
        put(self.repo, "art/anim/spear/build.py", "v2")
        put(self.repo, "art/anim/common/feel.py", "v2")
        put(self.repo, "src/A.java", "v2")
        put(self.repo, "assets/added.json", "added")
        self.lane_sha = commit(self.repo, "lane work")

    def test_resolve_and_missing_ref(self):
        self.assertEqual(bg.resolve(self.repo, "target"), self.target_sha)
        self.assertEqual(bg.resolve(self.repo, "HEAD"), self.lane_sha)
        with self.assertRaises(BoothError):
            bg.resolve(self.repo, "no-such-branch")

    def test_extract_reads_old_version_without_touching_worktree(self):
        dest = os.path.join(self.tmp.name, "out")
        before = (git(self.repo, "status", "--porcelain"), git(self.repo, "rev-parse", "HEAD"))
        got = bg.extract(self.repo, self.target_sha, ["assets/x.geo.json"], dest)
        with open(got["assets/x.geo.json"], "rb") as f:
            self.assertEqual(f.read(), b"old geo")
        with open(os.path.join(self.repo, "assets", "x.geo.json"), "rb") as f:
            self.assertEqual(f.read(), b"new geo")
        self.assertEqual((git(self.repo, "status", "--porcelain"), git(self.repo, "rev-parse", "HEAD")), before)

    def test_extract_missing_file_is_none(self):
        got = bg.extract(self.repo, self.target_sha, ["assets/added.json", "assets/x.geo.json"],
                         os.path.join(self.tmp.name, "out"))
        self.assertIsNone(got["assets/added.json"])
        self.assertTrue(os.path.isfile(got["assets/x.geo.json"]))

    def test_extract_binary_roundtrip(self):
        got = bg.extract(self.repo, self.target_sha, ["assets/blob.bin"], os.path.join(self.tmp.name, "out"))
        with open(got["assets/blob.bin"], "rb") as f:
            self.assertEqual(f.read(), bytes(range(256)))

    def test_other_files_filters_shown(self):
        got = bg.other_files(self.repo, self.target_sha, self.lane_sha,
                             ["art/anim/spear", "assets/x.geo.json", "assets/added.json"])
        self.assertEqual(got, ["art/anim/common/feel.py", "src/A.java"])

    def test_other_files_prefix_is_whole_path_segment(self):
        put(self.repo, "art/anim/spearman/x.py", "v1")
        lane = commit(self.repo, "sibling family")
        got = bg.other_files(self.repo, self.target_sha, lane, ["art/anim/spear", "assets", "src", "art/anim/common"])
        self.assertEqual(got, ["art/anim/spearman/x.py"])

    def test_changed_between_sees_only_listed_paths(self):
        git(self.repo, "checkout", "-q", "target")
        put(self.repo, "src/Later.java", "target moved on")
        later = commit(self.repo, "unrelated")
        self.assertEqual(bg.changed_between(self.repo, self.target_sha, later, ["assets/x.geo.json", "art/anim/spear"]), [])
        put(self.repo, "assets/x.geo.json", "main changed the geo")
        moved = commit(self.repo, "geo on target")
        self.assertEqual(bg.changed_between(self.repo, self.target_sha, moved, ["assets/x.geo.json", "art/anim/spear"]),
                         ["assets/x.geo.json"])

    def test_other_files_uses_merge_base(self):
        git(self.repo, "checkout", "-q", "target")
        put(self.repo, "src/Later.java", "target moved on")
        new_target = commit(self.repo, "target later")
        git(self.repo, "checkout", "-q", "lane")
        got = bg.other_files(self.repo, new_target, self.lane_sha, ["art/anim/spear", "assets"])
        self.assertEqual(got, ["art/anim/common/feel.py", "src/A.java"])

    def test_dirty_inputs(self):
        put(self.repo, "assets/x.geo.json", "edited, not committed")
        put(self.repo, "assets/untracked.json", "new")
        got = bg.dirty_inputs(self.repo, ["assets/x.geo.json", "assets/untracked.json", "src/A.java"])
        self.assertEqual(got, ["assets/x.geo.json", "assets/untracked.json"])

    def test_dirty_inputs_sees_staged_and_missing(self):
        put(self.repo, "src/A.java", "staged")
        git(self.repo, "add", "src/A.java")
        got = bg.dirty_inputs(self.repo, ["src/A.java", "assets/never-existed.json", "assets/blob.bin"])
        self.assertEqual(got, ["src/A.java", "assets/never-existed.json"])


if __name__ == "__main__":
    unittest.main()
