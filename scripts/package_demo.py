"""Package the offline demo's source and complete tested native app image."""
from pathlib import Path
import hashlib
import json
import os
import subprocess
import tempfile
import zipfile


def package(root, output):
    root, output = Path(root).resolve(), Path(output).resolve()
    output.parent.mkdir(parents=True, exist_ok=True)
    names = subprocess.check_output(["git", "ls-files", "-z"], cwd=root).decode().split("\0")
    entries = []
    for name in filter(None, names):
        source = root / name
        if not source.is_file() or source.is_symlink() or not source.resolve().is_relative_to(root):
            raise ValueError("Invalid source path: " + name)
        entries.append(("source/" + Path(name).as_posix(), source))
    apk = root / "composeApp/build/outputs/apk/debug/composeApp-debug.apk"
    entries.append(("android/MZBotanicalsMock.apk", apk))
    image = root / "composeApp/build/compose/binaries/main/app/MzbotanicalsMock"
    if not (image / "MzbotanicalsMock.exe").is_file():
        raise ValueError("Build the complete Windows app image before packaging.")
    for source in sorted(image.rglob("*")):
        if source.is_symlink() or not source.resolve().is_relative_to(image):
            raise ValueError("Invalid Windows app image path.")
        if source.is_file():
            entries.append(("windows/MzbotanicalsMock/" + source.relative_to(image).as_posix(), source))
    descriptor, temporary_name = tempfile.mkstemp(prefix=output.name, suffix=".tmp", dir=output.parent)
    os.close(descriptor)
    temporary = Path(temporary_name)
    manifest = []
    try:
        with zipfile.ZipFile(temporary, "w", zipfile.ZIP_DEFLATED, compresslevel=6) as archive:
            for name, source in entries:
                data = source.read_bytes()
                archive.writestr(name, data)
                manifest.append({"path": name, "sha256": hashlib.sha256(data).hexdigest()})
            archive.writestr("MANIFEST.json", json.dumps(manifest, indent=2))
            archive.writestr("START_HERE.txt", "MZ Botanicals Mock — offline sales demonstration.\nInstall android/MZBotanicalsMock.apk on Android.\nOn Windows extract the entire ZIP, then open windows/MzbotanicalsMock/MzbotanicalsMock.exe. Keep its app and runtime directories beside it.\nAll customers, orders, payments, taxes and shipping are fictional. No money is charged.\nRead source/README.md and source/VALIDATION.md.\n")
        with zipfile.ZipFile(temporary) as archive:
            if archive.testzip() is not None:
                raise ValueError("Archive CRC validation failed.")
            for entry in manifest:
                if hashlib.sha256(archive.read(entry["path"])).hexdigest() != entry["sha256"]:
                    raise ValueError("Archive manifest validation failed.")
        digest = hashlib.sha256(temporary.read_bytes()).hexdigest()
        os.replace(temporary, output)
        output.with_suffix(output.suffix + ".sha256").write_text(digest + "  " + output.name + "\n")
        print(json.dumps({"path": str(output), "files": len(manifest), "bytes": output.stat().st_size, "sha256": digest}))
    finally:
        temporary.unlink(missing_ok=True)


if __name__ == "__main__":
    import argparse
    parser = argparse.ArgumentParser()
    parser.add_argument("--output", type=Path, required=True)
    args = parser.parse_args()
    package(Path(__file__).resolve().parents[1], args.output)
