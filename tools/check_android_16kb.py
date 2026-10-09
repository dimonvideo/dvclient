#!/usr/bin/env python3
"""Check packaged native libraries against Android's 16 KB page-size requirements.

Uses only the Python standard library. APKs require ELF and stored-library ZIP
alignment; AABs require ELF alignment, because bundletool produces the final APK
layout. Check an AAB's PAGE_ALIGNMENT_16K setting separately with bundletool.
See https://developer.android.com/guide/practices/page-sizes.
"""

import argparse
from dataclasses import dataclass, field
from pathlib import Path
import struct
import sys
import zipfile


PAGE_SIZE = 16384
PT_LOAD = 1
PT_GNU_RELRO = 0x6474E552


@dataclass
class AuditResult:
    """Collect artifact metadata and validation failures without binary payloads."""

    path: Path
    native_count: int = 0
    issues: list[str] = field(default_factory=list)
    notes: list[str] = field(default_factory=list)


def check_elf(data: bytes, name: str) -> list[str]:
    """Validate ELF load boundaries and RELRO protection on 16 KB pages."""
    issues = []
    if len(data) < 16 or data[:4] != b"\x7fELF":
        return [f"{name}: invalid or truncated ELF identification"]
    elf_class, byte_order, version = data[4:7]
    if elf_class not in (1, 2) or byte_order not in (1, 2) or version != 1:
        return [f"{name}: unsupported ELF class, byte order, or version"]
    endian = "<" if byte_order == 1 else ">"
    header_format = endian + ("HHIIIIIHHHHHH" if elf_class == 1 else "HHIQQQIHHHHHH")
    header_size = 16 + struct.calcsize(header_format)
    if len(data) < header_size:
        return [f"{name}: truncated ELF header"]
    header = struct.unpack_from(header_format, data, 16)
    ph_offset, eh_size, ph_size, ph_count = header[4], header[7], header[8], header[9]
    ph_format = endian + ("IIIIIIII" if elf_class == 1 else "IIQQQQQQ")
    minimum_ph_size = struct.calcsize(ph_format)
    if (eh_size != header_size or ph_count in (0, 0xFFFF)
            or ph_size < minimum_ph_size or ph_offset < header_size):
        return [f"{name}: invalid or unsupported ELF program-header table"]
    if ph_offset + ph_size * ph_count > len(data):
        return [f"{name}: truncated ELF program-header table"]
    load_count = 0
    for index in range(ph_count):
        ph = struct.unpack_from(ph_format, data, ph_offset + index * ph_size)
        if elf_class == 1:
            kind, offset, address, _, file_size, memory_size, _, alignment = ph
        else:
            kind, _, offset, address, _, file_size, memory_size, alignment = ph
        if kind == PT_LOAD:
            load_count += 1
            label = f"{name}: PT_LOAD[{index}]"
            if alignment < PAGE_SIZE or alignment & (alignment - 1):
                issues.append(f"{label} alignment {alignment:#x} must be a power of two >= 0x4000")
            if offset % PAGE_SIZE != address % PAGE_SIZE:
                issues.append(f"{label} file offset and virtual address differ modulo 16 KB")
            if file_size > memory_size or offset + file_size > len(data):
                issues.append(f"{label} has an invalid or truncated file range")
        elif kind == PT_GNU_RELRO and (address + memory_size) % PAGE_SIZE:
            issues.append(f"{name}: GNU_RELRO[{index}] end {address + memory_size:#x} is not 16 KB aligned")
    if not load_count:
        issues.append(f"{name}: ELF has no PT_LOAD segments")
    return issues


def zip_data_offset(stream, entry: zipfile.ZipInfo) -> int:
    """Locate a ZIP entry's data using its actual local-header byte lengths."""
    stream.seek(entry.header_offset)
    header = stream.read(30)
    if len(header) != 30 or header[:4] != b"PK\x03\x04":
        raise ValueError("invalid or truncated ZIP local header")
    name_length, extra_length = struct.unpack_from("<HH", header, 26)
    return entry.header_offset + 30 + name_length + extra_length


def check_artifact(path: Path) -> AuditResult:
    """Check all packaged .so files and APK-only uncompressed ZIP alignment."""
    path = Path(path)
    result = AuditResult(path)
    suffix = path.suffix.lower()
    if suffix not in (".apk", ".aab"):
        result.issues.append("Expected an .apk or .aab artifact")
        return result
    try:
        with path.open("rb") as stream, zipfile.ZipFile(path) as archive:
            for entry in archive.infolist():
                if entry.is_dir() or not entry.filename.endswith(".so"):
                    continue
                result.native_count += 1
                name = entry.filename
                try:
                    result.issues.extend(check_elf(archive.read(entry), name))
                    if suffix == ".apk" and entry.compress_type == zipfile.ZIP_STORED:
                        offset = zip_data_offset(stream, entry)
                        if offset % PAGE_SIZE:
                            result.issues.append(f"{name}: uncompressed APK data offset {offset} is not 16 KB aligned")
                    result.notes.append(name)
                except (OSError, ValueError, RuntimeError, zipfile.BadZipFile, NotImplementedError) as error:
                    result.issues.append(f"{name}: cannot validate library ({type(error).__name__})")
    except (OSError, ValueError, zipfile.BadZipFile) as error:
        result.issues.append(f"Cannot read artifact ({type(error).__name__})")
    return result


def main(argv=None) -> int:
    """Print a compact audit report and return failure if any artifact fails."""
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("artifacts", nargs="+", type=Path, help="APK or AAB files to inspect")
    arguments = parser.parse_args(argv)
    failed = False
    for path in arguments.artifacts:
        result = check_artifact(path)
        failed |= bool(result.issues)
        status = "FAIL" if result.issues else "PASS"
        print(f"{status}: {path.name} ({result.native_count} native libraries)")
        for name in result.notes:
            print(f"  {name}")
        for issue in result.issues:
            print(f"  ERROR: {issue}")
        if not result.issues and not result.native_count:
            print("  No packaged native libraries; native page-size alignment is not required.")
        if path.suffix.lower() == ".aab":
            print("  AAB ZIP layout is not checked; verify PAGE_ALIGNMENT_16K with bundletool.")
    return int(failed)


if __name__ == "__main__":
    sys.exit(main())
