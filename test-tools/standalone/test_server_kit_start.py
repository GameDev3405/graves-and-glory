"""Checks for the server kit's Java version read and extra JVM flags (tools/server-kit/lib.ps1, start.ps1, start.sh).

Runs the real PowerShell helpers in a child process; skipped when PowerShell is not installed. Nothing is started,
and no eula.txt is written."""
import pathlib
import re
import shutil
import subprocess
import tempfile
import unittest

KIT = pathlib.Path(__file__).resolve().parents[2] / 'tools' / 'server-kit'
PS = shutil.which('powershell') or shutil.which('pwsh')

BANNER = 'Picked up JAVA_TOOL_OPTIONS: -Dgg.build=camp -Dfoo.version="9"'


def ps(script):
    """Run the script after dot-sourcing lib.ps1. It goes through a temp file: quotes in -Command text are mangled."""
    with tempfile.TemporaryDirectory() as d:
        f = pathlib.Path(d) / 'case.ps1'
        lib = KIT / 'lib.ps1'
        f.write_text(". '" + str(lib) + "'\n" + script + "\n", encoding='utf-8-sig')
        done = subprocess.run([PS, '-NoProfile', '-NonInteractive', '-ExecutionPolicy', 'Bypass', '-File', str(f)],
                              capture_output=True, text=True, timeout=120)
    return done.returncode, done.stdout.strip(), done.stderr.strip()


@unittest.skipIf(PS is None, 'PowerShell not available')
class KitJavaVersionAndJvmArgs(unittest.TestCase):
    def major(self, *lines):
        quoted = ','.join("'" + l.replace("'", "''") + "'" for l in lines)
        code, out, err = ps(f'Get-JavaMajor @({quoted})')
        self.assertEqual(code, 0, err)
        return int(out)

    def test_plain_output(self):
        self.assertEqual(self.major('openjdk version "17.0.19" 2026-04-21', 'OpenJDK Runtime Environment'), 17)

    def test_banner_first_line(self):
        self.assertEqual(self.major(BANNER, 'openjdk version "21.0.4" 2024-07-16 LTS'), 21)

    def test_old_style_version(self):
        self.assertEqual(self.major('java version "1.8.0_391"'), 1)

    def test_no_version_line(self):
        self.assertEqual(self.major(BANNER, 'garbage'), 0)

    def jvm_args(self, file_text=None, inline=''):
        with tempfile.TemporaryDirectory() as d:
            if file_text is not None:
                (pathlib.Path(d) / 'jvm-args.txt').write_text(file_text, encoding='utf-8')
            inline_q = inline.replace("'", "''")
            code, out, err = ps(f"$script:Root='{d}'; (Get-JvmArgs '{inline_q}') -join '|'")
            return code, out, err

    def test_none(self):
        self.assertEqual(self.jvm_args(), (0, '', ''))

    def test_file_then_inline(self):
        code, out, err = self.jvm_args('# comment\n-Dgg.build=camp  # trailing\n\n-Xss1M\n', '-Dx=1 -Dy=2')
        self.assertEqual((code, out), (0, '-Dgg.build=camp|-Xss1M|-Dx=1|-Dy=2'), err)

    def test_flag_with_space_is_quoted(self):
        code, out, err = self.jvm_args('-Dname=a b\n')
        self.assertEqual((code, out), (0, '"-Dname=a b"'), err)

    def test_rejects_non_flag(self):
        code, out, err = self.jvm_args(None, '-Dok=1 evil.jar')
        self.assertNotEqual(code, 0)

    def test_start_scripts_use_the_helpers(self):
        ps1 = (KIT / 'start.ps1').read_text(encoding='utf-8')
        sh = (KIT / 'start.sh').read_text(encoding='utf-8')
        self.assertIn('Get-JavaMajor', ps1)
        self.assertIn('Get-JvmArgs $JvmArgs', ps1)
        self.assertNotIn('Select-Object -First 1', ps1.split('java -version')[1].split('\n')[0])
        self.assertIn('gg_jvm_args', sh)
        self.assertRegex(sh, r'version\[\[:space:\]\]')
        self.assertIsNone(re.search(r'java -version 2>&1 \| head -n 1', sh))


if __name__ == '__main__':
    unittest.main()
