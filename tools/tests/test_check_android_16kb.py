"""Exercise ELF and APK packaging failure cases without Android build tools."""

import contextlib
import io
from pathlib import Path
import struct
import sys
import tempfile
import unittest
import zipfile

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
from check_android_16kb import PAGE_SIZE, check_artifact, check_elf, main


def make_elf(bits=64, endian="<", alignment=PAGE_SIZE, address=0, relro_end=0x8000):
    """Build a small ELF with configurable LOAD and RELRO program headers."""
    is_64 = bits == 64
    identification = b"\x7fELF" + bytes((2 if is_64 else 1, 1 if endian == "<" else 2, 1)) + bytes(9)
    header_size, ph_size = (64, 56) if is_64 else (52, 32)
    header = struct.pack(endian + ("HHIQQQIHHHHHH" if is_64 else "HHIIIIIHHHHHH"),
                         3, 183 if is_64 else 40, 1, 0, header_size, 0, 0,
                         header_size, ph_size, 2, 0, 0, 0)
    if is_64:
        load = struct.pack(endian + "IIQQQQQQ", 1, 6, 0, address, address, 512, 0x10000, alignment)
        relro = struct.pack(endian + "IIQQQQQQ", 0x6474E552, 4, 0, 0x4000, 0x4000,
                            0, relro_end - 0x4000, 1)
    else:
        load = struct.pack(endian + "IIIIIIII", 1, 0, address, address, 512, 0x10000, 6, alignment)
        relro = struct.pack(endian + "IIIIIIII", 0x6474E552, 0, 0x4000, 0x4000,
                            0, relro_end - 0x4000, 4, 1)
    return (identification + header + load + relro).ljust(512, b"\x00")


def write_artifact(path, data=None, aligned=False, compressed=False, prefix="lib/arm64-v8a/"):
    """Write one library with optional ZIP padding, or an archive without JNI."""
    with zipfile.ZipFile(path, "w") as archive:
        archive.writestr("AndroidManifest.xml", b"manifest")
        if data is None:
            return
        entry = zipfile.ZipInfo(prefix + "libsample.so")
        entry.compress_type = zipfile.ZIP_DEFLATED if compressed else zipfile.ZIP_STORED
        if aligned:
            local_header_end = archive.fp.tell() + 30 + len(entry.filename.encode("utf-8"))
            padding = (-local_header_end) % PAGE_SIZE
            if padding < 4:
                padding += PAGE_SIZE
            entry.extra = struct.pack("<HH", 0xFFFF, padding - 4) + bytes(padding - 4)
        archive.writestr(entry, data)


class ElfValidationTest(unittest.TestCase):
    """Cover both ELF classes and independent native alignment failures."""

    def test_valid_32_and_64_bit_elf_in_both_byte_orders(self):
        """Read native program-header layouts without assuming host architecture."""
        for bits in (32, 64):
            for endian in ("<", ">"):
                with self.subTest(bits=bits, endian=endian):
                    self.assertEqual([], check_elf(make_elf(bits, endian), "sample.so"))

    def test_four_k_load_alignment_fails(self):
        """Reject a load alignment that works only with smaller pages."""
        for bits in (32, 64):
            with self.subTest(bits=bits):
                self.assertIn("alignment 0x1000", "\n".join(check_elf(make_elf(bits, alignment=4096), "sample.so")))

    def test_incongruent_load_address_fails(self):
        """Reject segment addresses that cannot map the file on 16 KB pages."""
        self.assertIn("differ modulo 16 KB", "\n".join(check_elf(make_elf(address=4096), "sample.so")))

    def test_unaligned_relro_end_fails(self):
        """Catch a protection range ending on a 4 KB boundary within a page."""
        for bits in (32, 64):
            with self.subTest(bits=bits):
                self.assertIn("GNU_RELRO", "\n".join(check_elf(make_elf(bits, relro_end=0x5000), "sample.so")))

    def test_malformed_and_truncated_elf_fail(self):
        """Report corrupt headers and missing segment payloads without crashing."""
        valid = make_elf()
        for data in (b"not ELF", valid[:16], valid[:80], valid[:220]):
            with self.subTest(length=len(data)):
                self.assertTrue(check_elf(data, "sample.so"))

    def test_unsupported_identification_fails(self):
        """Reject unsupported class or byte order before unpacking integers."""
        for index, value in ((4, 3), (5, 3), (6, 0)):
            data = bytearray(make_elf())
            data[index] = value
            self.assertTrue(check_elf(bytes(data), "sample.so"))

    def test_missing_load_segment_fails(self):
        """Prevent a malformed ELF from passing because no LOAD was checked."""
        data = bytearray(make_elf())
        struct.pack_into("<I", data, 64, 0)
        self.assertIn("no PT_LOAD", "\n".join(check_elf(bytes(data), "sample.so")))


class ArtifactValidationTest(unittest.TestCase):
    """Check APK local-header alignment independently from ELF alignment."""

    def setUp(self):
        """Isolate generated artifacts for each test."""
        self.directory = tempfile.TemporaryDirectory()
        self.addCleanup(self.directory.cleanup)
        self.root = Path(self.directory.name)

    def test_aligned_apk_passes(self):
        """Accept a correctly aligned uncompressed native library."""
        path = self.root / "good.apk"
        write_artifact(path, make_elf(), aligned=True)
        result = check_artifact(path)
        self.assertEqual(1, result.native_count)
        self.assertEqual([], result.issues)

    def test_misaligned_apk_fails_even_with_valid_elf(self):
        """Detect incorrect packaging separately from linker alignment."""
        path = self.root / "misaligned.apk"
        write_artifact(path, make_elf())
        self.assertIn("APK data offset", "\n".join(check_artifact(path).issues))

    def test_compressed_apk_skips_zip_alignment(self):
        """Allow libraries extracted during installation without ZIP alignment."""
        path = self.root / "compressed.apk"
        write_artifact(path, make_elf(), compressed=True)
        self.assertEqual([], check_artifact(path).issues)

    def test_compressed_library_still_requires_elf_alignment(self):
        """Compression cannot conceal an incompatible native load segment."""
        path = self.root / "compressed-bad.apk"
        write_artifact(path, make_elf(alignment=4096), compressed=True)
        self.assertIn("alignment 0x1000", "\n".join(check_artifact(path).issues))

    def test_aab_checks_elf_without_requiring_zip_alignment(self):
        """Inspect bundle modules while deferring final APK layout to bundletool."""
        path = self.root / "sample.aab"
        write_artifact(path, make_elf(), prefix="base/lib/arm64-v8a/")
        self.assertEqual([], check_artifact(path).issues)
        write_artifact(path, make_elf(relro_end=0x5000), prefix="feature/lib/arm64-v8a/")
        self.assertIn("GNU_RELRO", "\n".join(check_artifact(path).issues))

    def test_no_native_libraries_passes(self):
        """Treat Java-only artifacts as having no native alignment requirement."""
        path = self.root / "java-only.apk"
        write_artifact(path)
        result = check_artifact(path)
        self.assertEqual(0, result.native_count)
        self.assertEqual([], result.issues)

    def test_invalid_zip_and_invalid_library_fail(self):
        """Return actionable metadata for corrupt archives and JNI files."""
        path = self.root / "broken.apk"
        path.write_bytes(b"not ZIP")
        self.assertTrue(check_artifact(path).issues)
        write_artifact(path, b"not ELF", aligned=True)
        self.assertIn("ELF", "\n".join(check_artifact(path).issues))

    def test_missing_or_wrong_artifact_type_fails(self):
        """Avoid presenting nonexistent or unrelated archives as compatible."""
        self.assertTrue(check_artifact(self.root / "missing.apk").issues)
        self.assertTrue(check_artifact(self.root / "wrong.aar").issues)

    def test_cli_reports_failures_and_preserves_success_summary(self):
        """Make command exit status usable for release validation scripts."""
        good, bad = self.root / "good.apk", self.root / "bad.apk"
        write_artifact(good)
        write_artifact(bad, make_elf(alignment=4096), aligned=True)
        output = io.StringIO()
        with contextlib.redirect_stdout(output):
            self.assertEqual(0, main([str(good)]))
            self.assertEqual(1, main([str(good), str(bad)]))
        self.assertIn("PASS: good.apk", output.getvalue())
        self.assertIn("No packaged native libraries", output.getvalue())
        self.assertIn("FAIL: bad.apk", output.getvalue())


if __name__ == "__main__":
    unittest.main()
