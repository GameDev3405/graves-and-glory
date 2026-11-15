"""Runs PayloadRoundTripCheck (port lane A, P1.3): every payload codec round-trips, no game, no Gradle.

Classpath: the yarn-mapped 1.21.1 merged jar Loom put in this checkout's .gradle/loom-cache, the remapped Fabric networking
jar, and the vanilla libraries named in Loom's mojang_minecraft_info.json (found in the Gradle module cache). Needs one earlier
Gradle configure of this checkout (any compileJava) so the mapped jar exists.

Mode, chosen automatically:
- full: build/classes/java/main holds Payloads.class (the whole mod compiled), so the check runs against the real classes.
- stub (until every lane compiles): javac builds net/Payloads.java and net/RawBody.java with four generated id holders
  (GravesAndGlory, CommandNet, MotionEvents, RankSync) whose Identifier constants are copied from the real sources. Only
  the ids come from stubs; the codecs, records and RawBody are the real files.

Usage: python work/port/lane-a/run_payload_check.py [--stub]
"""
import json, os, pathlib, re, subprocess, sys, tempfile

ROOT = pathlib.Path(__file__).resolve().parents[3]
HOME = pathlib.Path.home()
JDK = pathlib.Path(os.environ.get("JAVA_HOME", r"C:\Program Files\Eclipse Adoptium\jdk-21.0.11.10-hotspot"))
SRC = ROOT / "src/main/java/gg/gravesandglory"
CHECK = pathlib.Path(__file__).with_name("PayloadRoundTripCheck.java")


def classpath():
    jars = []
    mapped = sorted((ROOT / ".gradle/loom-cache/minecraftMaven/net/minecraft").glob("minecraft-merged-*/1.21.1-*/*.jar"))
    mapped = [j for j in mapped if not j.name.endswith("-sources.jar")]
    if not mapped: sys.exit("No mapped 1.21.1 jar in .gradle/loom-cache; run compileJava once (through gradle_slot.py)")
    jars.append(mapped[-1])
    net = sorted((ROOT / ".gradle/loom-cache/remapped_mods").rglob("fabric-networking-api-v1-*.jar"))
    jars += net[-1:]
    info = json.loads((HOME / ".gradle/caches/fabric-loom/1.21.1/mojang_minecraft_info.json").read_text(encoding="utf8"))
    modules = HOME / ".gradle/caches/modules-2/files-2.1"
    for lib in info["libraries"]:
        parts = lib["name"].split(":")
        if len(parts) != 3: continue  # natives
        group, artifact, version = parts
        found = [j for j in (modules / group / artifact / version).glob(f"*/{artifact}-{version}.jar")]
        if found: jars.append(found[0])
    return os.pathsep.join(str(j) for j in jars)


def stub_sources(out):
    """Id holders with the real Identifier constants and nothing else."""
    holders = {"GravesAndGlory": SRC / "GravesAndGlory.java", "CommandNet": SRC / "CommandNet.java",
               "MotionEvents": SRC / "MotionEvents.java", "RankSync": SRC / "RankSync.java"}
    pattern = re.compile(r'public static final Identifier (\w+)\s*=\s*Identifier\.of\("([^"]+)",\s*"([^"]+)"\);')
    files = []
    for name, path in holders.items():
        lines = [f'    public static final Identifier {f} = Identifier.of("{ns}", "{p}");' for f, ns, p in pattern.findall(path.read_text(encoding="utf8"))]
        if name == "GravesAndGlory":  # aliases of MotionEvents ids, as in the real file
            lines += [f"    public static final Identifier {f} = MotionEvents.{f};" for f in re.findall(r"public static final Identifier (\w+) = MotionEvents\.\1;", path.read_text(encoding="utf8"))]
        stub = out / f"{name}.java"
        stub.write_text("package gg.gravesandglory;\nimport net.minecraft.util.Identifier;\n/** Generated id holder (run_payload_check.py stub mode). */\npublic final class "
                        + name + " {\n" + "\n".join(lines) + "\n}\n", encoding="utf8")
        files.append(stub)
    return files


def main():
    cp = classpath()
    work = pathlib.Path(tempfile.mkdtemp(prefix="payload-check-"))
    full = ROOT / "build/classes/java/main"
    if "--stub" not in sys.argv and (full / "gg/gravesandglory/net/Payloads.class").exists():
        mode, cp_run = "full", str(full) + os.pathsep + cp
        sources = [CHECK]
        compile_cp = cp_run
    else:
        mode = "stub"
        sources = [SRC / "net/Payloads.java", SRC / "net/RawBody.java", CHECK, *stub_sources(work)]
        compile_cp = cp
    classes = work / "classes"
    javac = [str(JDK / "bin/javac"), "--release", "21", "-nowarn", "-cp", compile_cp, "-d", str(classes), *map(str, sources)]
    if subprocess.run(javac).returncode: sys.exit("javac failed")
    run_cp = str(classes) + os.pathsep + (str(full) + os.pathsep if mode == "full" else "") + cp
    print("mode: " + mode, flush=True)
    sys.exit(subprocess.run([str(JDK / "bin/java"), "-cp", run_cp, "gg.gravesandglory.net.PayloadRoundTripCheck"]).returncode)


if __name__ == "__main__":
    main()
