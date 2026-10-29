# Minecraft 1.21.1 client verification

Final verification of 0.1.0-alpha.9.17-dev used a real NeoForge client with its integrated server, in a disposable flat survival world. The local automated test mod is deliberately excluded from the released mod JAR.

`evidence/summary.json` summarizes all 18 passing cases and hashes the 22 screenshots. `runtime-inputs.json` identifies the exact compiled classes, processed resources, and probe Java sources used. The class/resource digest is SHA-256 over sorted relative path UTF-8 bytes, a NUL byte, then the binary SHA-256 digest of each file. Those outputs were rechecked after the client shut down.

## Observed behavior

- Twelve actual inventory/chest hover cases exercised Shift off/on, stone, diamond, copper ingot, and breeze rod. Every case had zero scanned subjects, zero discovered aspects, and no held thaumometer. GUI scales 2 and 1 were exercised. Shift exposes the real aspect icons and counts; releasing Shift hides them.
- Six scanner cases cover an unscanned pig's aspect billboard and passive highlights, a new block scan, a repeat scan, a miss, an offhand block scan, and clearing the label when putting the tool away. Successful block/repeat/offhand uses create 10 rune particles each; a miss creates none. Every use requests the scan sound.
- Actual Minecraft item-use dispatch produces no attack swing immediately or at 2 and 6 ticks afterward. The screenshots show the centered, upright lens after use. All checked OpenGL error values are zero.
- The Stone tooltip title remains white with and without Shift: the fixed title crop contains 252 white pixels and zero green pixels in both captures. This catches the queued-text tint leakage found and fixed during the initial client run.

## Scope and reproduction

The probe uses actual GLFW input for mouse hover/Shift, actual container screens, and actual Minecraft item-use dispatch. Software OpenGL runs under Xvfb at 1280×720. OpenAL is initialized with its null output driver: recorded PlaySoundEvent values establish requested sound ID, category, volume 0.5, and pitch 1.0; this is not an auditory listening test.

`probe/` contains the complete test module sources and metadata. `launch-client.py` is the exact runner used in the working environment; its project, JDK, and extracted Xvfb paths are specific to that workspace and must be adjusted when running elsewhere. It expects ModDev's `prepareClientRun` outputs and cached dependencies. Keep the generated probe JAR in a separate disposable game directory. The input properties are `thaumcraft.probe.automate=true`, `thaumcraft.probe.scanner=true`, `thaumcraft.probe.output=<evidence-directory>`, and `thaumcraft.probe.xdotool=<xdotool-path>`. Set `THAUMCRAFT_PROBE_OUTPUT` to choose the runner's output directory.

World saves, runtime libraries, compiled probe classes, temporary profiles, and failed exploratory attempts are omitted from this verification archive.
