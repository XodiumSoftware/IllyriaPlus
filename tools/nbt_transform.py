import os
from collections import defaultdict
from nbtlib import load, File
from nbtlib.tag import IntArray


# ----------------------------
# Trunk detection
# ----------------------------

def is_trunk_block(block, palette):
    """
    Detect trunk blocks:
    - tree trunks: log / wood
    - mushroom trunks: mushroom_stem
    """

    state = block["state"]
    entry = palette[state]
    name = str(entry["Name"]).lower()

    return (
        "log" in name or
        "wood" in name or
        "mushroom_stem" in name or
        "hyphae" in name
    )


# ----------------------------
# Origin detection (center of all trunks)
# ----------------------------

def detect_origin(blocks, palette):
    """
    Origin is computed from ALL trunk blocks:
    - XZ = centroid of all trunks
    - Y  = minimum Y of trunk blocks
    """

    xs = []
    ys = []
    zs = []

    for b in blocks:
        if not is_trunk_block(b, palette):
            continue

        x, y, z = b["pos"]

        xs.append(x)
        ys.append(y)
        zs.append(z)

    if not xs:
        raise ValueError("No trunk blocks found.")

    ox = sum(xs) // len(xs)
    oz = sum(zs) // len(zs)
    oy = min(ys)

    return ox, oy, oz


# ----------------------------
# Shift blocks
# ----------------------------

def shift_blocks(blocks, origin):
    ox, oy, oz = origin

    for b in blocks:
        x, y, z = b["pos"]

        b["pos"] = IntArray([
            x - ox,
            y - oy,
            z - oz
        ])


# ----------------------------
# Process single file
# ----------------------------

def process_file(path: str):
    nbt = load(path)

    blocks = nbt["blocks"]
    palette = nbt["palette"]

    origin = detect_origin(blocks, palette)

    shift_blocks(blocks, origin)

    File(nbt).save(path)

    print(f"Processed: {os.path.basename(path)} | origin={origin}")


# ----------------------------
# Batch folder mode
# ----------------------------

def process_folder(root_folder: str):
    """
    Recursively processes all .nbt files in all subfolders.
    """

    for current_path, _, files in os.walk(root_folder):
        for file in files:
            if not file.endswith(".nbt"):
                continue

            full_path = os.path.join(current_path, file)

            process_file(full_path)


# ----------------------------
# CLI
# ----------------------------

if __name__ == "__main__":
    folder = input("NBT folder path: ").strip()
    process_folder(folder)
    print("Done.")
