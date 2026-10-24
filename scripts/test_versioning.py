"""Standalone regression tests; requires a JDK and Python's standard library."""
import os
from pathlib import Path
import shutil
import subprocess
import tempfile
import unittest


class VersioningTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.temporary = tempfile.TemporaryDirectory(prefix="ezan-versioning-tests-")
        cls.classes = Path(cls.temporary.name) / "classes"
        cls.classes.mkdir()
        java_home = os.environ.get("JAVA_HOME")
        cls.java = str(Path(java_home) / "bin/java") if java_home else "java"
        javac = str(Path(java_home) / "bin/javac") if java_home else "javac"
        cls.source = Path(__file__).with_name("Versioning.java")
        subprocess.run([javac, "-d", str(cls.classes), str(cls.source)], check=True, capture_output=True)

    @classmethod
    def tearDownClass(cls):
        cls.temporary.cleanup()

    def setUp(self):
        self.fixture = tempfile.TemporaryDirectory(prefix="project-", dir=self.temporary.name)
        self.root = Path(self.fixture.name)
        self.properties = self.root / "version.properties"
        self.properties.write_text("versionName=5.2.0\nversionCode=7\nsourceFingerprint=\n")
        self.source_file = self.root / "app/src/main/java/App.kt"
        self.source_file.parent.mkdir(parents=True)
        self.source_file.write_text("class App\n")
        scripts = self.root / "scripts"
        scripts.mkdir()
        shutil.copy(self.source, scripts / self.source.name)

    def tearDown(self):
        self.fixture.cleanup()

    def command(self, mode="prepare", bump="patch"):
        return [self.java, "-cp", str(self.classes), "Versioning", mode, str(self.root), bump]

    def version(self, mode="prepare", bump="patch"):
        return subprocess.run(self.command(mode, bump), check=True, capture_output=True, text=True).stdout.strip()

    def test_initial_baseline_and_same_source_do_not_bump(self):
        original = self.properties.read_bytes()
        self.assertEqual("5.2.0|7", self.version("current"))
        self.assertEqual(original, self.properties.read_bytes())
        self.assertEqual("5.2.0|7", self.version())
        baseline = self.properties.read_bytes()
        self.assertNotEqual(original, baseline)
        self.assertEqual("5.2.0|7", self.version())
        self.assertEqual(baseline, self.properties.read_bytes())

    def test_read_only_build_does_not_consume_source_change(self):
        self.version()
        baseline = self.properties.read_bytes()
        self.source_file.write_text("class ChangedApp\n")
        self.assertEqual("5.2.0|7", self.version("current"))
        self.assertEqual(baseline, self.properties.read_bytes())
        self.assertEqual("5.2.1|8", self.version())
        self.assertEqual("5.2.1|8", self.version())

    def test_generated_local_and_documentation_files_are_excluded(self):
        self.version()
        for name in ["app/build/generated/BuildConfig.java", ".gradle/cache", "local.properties", "README.md"]:
            file = self.root / name
            file.parent.mkdir(parents=True, exist_ok=True)
            file.write_text("unrelated build output\n")
        self.assertEqual("5.2.0|7", self.version())

    def test_binary_assets_and_deletions_bump_once(self):
        self.version()
        asset = self.root / "app/src/main/assets/model.bin"
        asset.parent.mkdir(parents=True)
        asset.write_bytes(bytes(range(256)))
        self.assertEqual("5.2.1|8", self.version())
        self.assertEqual("5.2.1|8", self.version())
        asset.unlink()
        self.assertEqual("5.2.2|9", self.version())

    def test_explicit_minor_and_major_changes_keep_monotonic_code(self):
        self.version()
        self.source_file.write_text("class NewFeature\n")
        self.assertEqual("5.3.0|8", self.version(bump="minor"))
        self.assertEqual("5.3.0|8", self.version(bump="minor"))
        self.source_file.write_text("class RedesignedApp\n")
        self.assertEqual("6.0.0|9", self.version(bump="major"))

    def test_concurrent_packaging_reserves_only_one_version(self):
        self.version()
        self.source_file.write_text("class ConcurrentUpdate\n")
        first = subprocess.Popen(self.command(), stdout=subprocess.PIPE, stderr=subprocess.PIPE, text=True)
        second = subprocess.Popen(self.command(), stdout=subprocess.PIPE, stderr=subprocess.PIPE, text=True)
        first_output, first_error = first.communicate(timeout=30)
        second_output, second_error = second.communicate(timeout=30)
        self.assertEqual(0, first.returncode, first_error)
        self.assertEqual(0, second.returncode, second_error)
        self.assertEqual("5.2.1|8", first_output.strip())
        self.assertEqual(first_output, second_output)

    def test_invalid_version_and_android_code_limit_leave_file_intact(self):
        self.properties.write_text("versionName=5.2\nversionCode=7\nsourceFingerprint=\n")
        malformed = self.properties.read_bytes()
        self.assertNotEqual(0, subprocess.run(self.command(), capture_output=True).returncode)
        self.assertEqual(malformed, self.properties.read_bytes())
        self.properties.write_text("versionName=5.2.0\nversionCode=2100000000\nsourceFingerprint=\n")
        self.version()
        baseline = self.properties.read_bytes()
        self.source_file.write_text("class BeyondLimit\n")
        self.assertNotEqual(0, subprocess.run(self.command(), capture_output=True).returncode)
        self.assertEqual(baseline, self.properties.read_bytes())


if __name__ == "__main__":
    unittest.main(verbosity=2)
