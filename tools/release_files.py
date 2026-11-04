"""Small, import-safe helpers shared by the packaging and export scripts (tested in test_release_packaging.py)."""
import os
import pathlib
import re


def gradle_home():
    """The user's Gradle home: GRADLE_USER_HOME when set, else ~/.gradle."""
    env = os.environ.get('GRADLE_USER_HOME')
    return pathlib.Path(env) if env else pathlib.Path.home() / '.gradle'


def resolve_local_path(root, local_path):
    r"""Where a platform-lock localPath lives on this machine.

    Relative paths (dependencies/mods/x.jar) resolve under root. An absolute path that points into a Gradle cache
    (the old format, e.g. C:\Users\someone\.gradle\caches\modules-2\files-2.1\...) is re-rooted under this
    machine's Gradle home, so the lock file stays valid on other machines and the old format keeps working.
    Any other absolute path is used as written."""
    text = str(local_path)
    parts = [p for p in re.split(r'[\\/]+', text) if p]
    if 'caches' in parts:
        i = parts.index('caches')
        if i + 1 < len(parts) and parts[i + 1] == 'modules-2' and (pathlib.PureWindowsPath(text).is_absolute() or pathlib.PurePosixPath(text).is_absolute()):
            return gradle_home().joinpath(*parts[i:])
    return pathlib.Path(root) / text


def require_release_notes(root, release_version):
    """The player-facing docs/RELEASE-<version>.txt, or a clear error when it has not been written."""
    path = pathlib.Path(root) / 'docs' / ('RELEASE-%s.txt' % release_version)
    if not path.is_file():
        raise SystemExit('Cannot package %s: docs/RELEASE-%s.txt is missing. Write the player-facing release notes '
                         '(see docs/RELEASE-0.5.0.txt for the style; a draft is in docs/releases/) and run again.'
                         % (release_version, release_version))
    return path


def mrpack_summary(release_version):
    """The .mrpack index summary, driven by the release version (a -suffix marks a pre-release)."""
    pre = ' Pre-release build.' if '-' in release_version else ''
    return 'Collect an army and command fantasy battles. Graves and Glory %s.%s' % (release_version, pre)
