# Native player libs

## media3-decoder-ffmpeg.aar

Prebuilt Media3 `FfmpegAudioRenderer` + `libffmpegJNI.so` (arm64-v8a, armeabi-v7a, x86, x86_64).

**Why:** COTAS live HLS/TS carries `audio/mpeg-L2` (MP2). Android `MediaCodec` does not decode MP2, so ExoPlayer played video with no audio track. FFmpeg fills that gap (also AC-3 / E-AC-3).

Wired via `DefaultRenderersFactory.EXTENSION_RENDERER_MODE_ON` in `CotasPlayerFactory`.

LGPL FFmpeg audio decoders; keep this AAR when bumping Media3 and re-verify `ff_mp2_decoder` is present in the `.so`.
