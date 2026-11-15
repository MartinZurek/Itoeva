"""Restore the themed icon and separate provider authorities in the Godot 4.6.3 export."""

import argparse
from pathlib import Path
import struct
from zipfile import ZipFile


ICON = "res/mipmap-anydpi-v26/themed_icon.xml"
MANIFEST = "AndroidManifest.xml"


def values(fmt, data, at):
    return struct.unpack_from("<" + fmt, data, at)


def pack(fmt, *items):
    return struct.pack("<" + fmt, *items)


def fix_manifest(data):
    if values("HHI", data, 0) != (3, 8, len(data)):
        raise ValueError("Unsupported manifest header")
    kind, header, pool_size = values("HHI", data, 8)
    count, styles, flags, strings_start, styles_start = values("IIIII", data, 16)
    if (kind, header, styles, flags, styles_start, strings_start) != (1, 28, 0, 0, 0, 28 + 4 * count):
        raise ValueError("Expected the UTF-16 string pool used by Godot 4.6.3")
    offsets = values("I" * count, data, 36)
    contents_start = 8 + strings_start
    strings = []
    strings_end = 0
    for offset in offsets:
        at = contents_start + offset
        length, = values("H", data, at)
        if length >= 0x8000:
            raise ValueError("Unsupported string length")
        end = at + 2 + 2 * length
        strings.append(data[at + 2:end].decode("utf-16-le"))
        strings_end = max(strings_end, offset + 4 + 2 * length)

    nodes = bytearray(data[8 + pool_size:])
    authority = None
    at = 0
    while at < len(nodes):
        kind, header, size = values("HHI", nodes, at)
        if size < header or at + size > len(nodes):
            raise ValueError("Invalid manifest node")
        if kind == 0x102:
            ext = at + header
            name, = values("I", nodes, ext + 4)
            start, width, attributes = values("HHH", nodes, ext + 8)
            if strings[name] == "provider":
                fields = {}
                for index in range(attributes):
                    attribute = ext + start + index * width
                    _, key, raw, value_size, _, value_type, typed = values("IIIHBBI", nodes, attribute)
                    if value_size == 8 and value_type == 3:
                        fields[strings[key]] = (attribute, raw, strings[typed])
                if fields.get("name", (None, None, None))[2] == "androidx.startup.InitializationProvider":
                    attribute, raw, current = fields["authorities"]
                    if current.endswith(".androidx-startup"):
                        return data
                    if not current.endswith(".fileprovider") or authority is not None:
                        raise ValueError("Unexpected AndroidX provider authority")
                    authority = current.removesuffix(".fileprovider") + ".androidx-startup"
                    if raw != 0xffffffff:
                        struct.pack_into("<I", nodes, attribute + 8, count)
                    struct.pack_into("<I", nodes, attribute + 16, count)
        at += size
    if authority is None:
        raise ValueError("AndroidX startup provider not found")

    strings_data = data[contents_start:contents_start + strings_end]
    strings_data += pack("H", len(authority)) + authority.encode("utf-16-le") + b"\x00\x00"
    strings_data += b"\x00" * (-len(strings_data) % 4)
    pool = pack("HHIIIIII", 1, 28, strings_start + 4 + len(strings_data), count + 1, 0, flags, strings_start + 4, 0)
    pool += pack("I" * (count + 1), *offsets, strings_end) + strings_data
    return pack("HHI", 3, 8, 8 + len(pool) + len(nodes)) + pool + nodes


def repair(source, template, output):
    with ZipFile(source) as original, ZipFile(template) as stock, ZipFile(output, "x") as fixed:
        for entry in original.infolist():
            data = original.read(entry)
            fixed.writestr(entry, fix_manifest(data) if entry.filename == MANIFEST else data)
        if ICON not in original.namelist():
            fixed.writestr(stock.getinfo(ICON), stock.read(ICON))


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("source", type=Path)
    parser.add_argument("template", type=Path)
    parser.add_argument("output", type=Path)
    args = parser.parse_args()
    repair(args.source, args.template, args.output)
