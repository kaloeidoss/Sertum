#include <aaudio/AAudio.h>
#include <android/log.h>
#include <jni.h>
#include <math.h>
#include <stdint.h>
#include <vector>

#define TAG "SertumSpike"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, TAG, __VA_ARGS__)
#define LOGW(...) __android_log_print(ANDROID_LOG_WARN, TAG, __VA_ARGS__)

static const int32_t RATES[] = {44100, 48000, 88200, 96000, 176400, 192000};
static constexpr int TONE_SECONDS = 3;

static bool playRate(AAudioStreamBuilder *builder, int32_t rate, aaudio_format_t format, int bitDepth) {
    AAudioStreamBuilder_setSampleRate(builder, rate);
    AAudioStreamBuilder_setChannelCount(builder, 2);
    AAudioStreamBuilder_setFormat(builder, format);
    AAudioStreamBuilder_setPerformanceMode(builder, AAUDIO_PERFORMANCE_MODE_LOW_LATENCY);
    // Try exclusive first; fall back to shared if the device refuses.
    AAudioStreamBuilder_setSharingMode(builder, AAUDIO_SHARING_MODE_EXCLUSIVE);

    AAudioStream *stream = nullptr;
    aaudio_result_t res = AAudioStreamBuilder_openStream(builder, &stream);
    if (res != AAUDIO_OK) {
        AAudioStreamBuilder_setSharingMode(builder, AAUDIO_SHARING_MODE_SHARED);
        res = AAudioStreamBuilder_openStream(builder, &stream);
    }
    if (res != AAUDIO_OK) {
        LOGW("aaudio open failed rate=%d bits=%d res=%d", rate, bitDepth, res);
        return false;
    }

    const int32_t actualRate = AAudioStream_getSampleRate(stream);
    const aaudio_format_t actualFormat = AAudioStream_getFormat(stream);
    const aaudio_performance_mode_t perf = AAudioStream_getPerformanceMode(stream);
    const aaudio_sharing_mode_t sharing = AAudioStream_getSharingMode(stream);
    const int32_t deviceId = AAudioStream_getDeviceId(stream);
    const int32_t burst = AAudioStream_getFramesPerBurst(stream);
    const int32_t capacity = AAudioStream_getBufferCapacityInFrames(stream);
    LOGI("aaudio stream rate=%d bits=%d actualRate=%d actualFormat=%d perf=%d sharing=%d device=%d burst=%d capacity=%d",
         rate, bitDepth, actualRate, actualFormat, perf, sharing, deviceId, burst, capacity);

    if (actualRate != rate) {
        LOGW("aaudio RATE MISMATCH requested=%d actual=%d", rate, actualRate);
    }
    if (actualFormat != format) {
        LOGW("aaudio FORMAT MISMATCH requested=%d actual=%d", format, actualFormat);
    }

    const int64_t totalFrames = static_cast<int64_t>(rate) * TONE_SECONDS;
    const int bytesPerFrame = (bitDepth == 16 ? 4 : 6);
    std::vector<uint8_t> pcm(totalFrames * bytesPerFrame, 0);
    const double amplitude = (bitDepth == 16 ? 32767.0 : 8388607.0) * 0.2;
    for (int64_t i = 0; i < totalFrames; ++i) {
        const int64_t v = static_cast<int64_t>(amplitude * sin(2.0 * M_PI * 1000.0 * i / rate));
        if (bitDepth == 16) {
            const int16_t s = static_cast<int16_t>(v);
            const size_t off = i * 4;
            pcm[off] = static_cast<uint8_t>(s & 0xFF);
            pcm[off + 1] = static_cast<uint8_t>((s >> 8) & 0xFF);
            pcm[off + 2] = pcm[off];
            pcm[off + 3] = pcm[off + 1];
        } else {
            const int32_t s = static_cast<int32_t>(v) & 0xFFFFFF;
            const size_t off = i * 6;
            pcm[off] = static_cast<uint8_t>(s & 0xFF);
            pcm[off + 1] = static_cast<uint8_t>((s >> 8) & 0xFF);
            pcm[off + 2] = static_cast<uint8_t>((s >> 16) & 0xFF);
            pcm[off + 3] = pcm[off];
            pcm[off + 4] = pcm[off + 1];
            pcm[off + 5] = pcm[off + 2];
        }
    }

    res = AAudioStream_requestStart(stream);
    if (res != AAUDIO_OK) {
        LOGW("aaudio start failed rate=%d bits=%d res=%d", rate, bitDepth, res);
        AAudioStream_close(stream);
        return false;
    }

    const int64_t bytesTotal = totalFrames * bytesPerFrame;
    const int bytesPerFrameActual = actualFormat == AAUDIO_FORMAT_PCM_I24_PACKED ? 6 : 4;
    int64_t written = 0;
    while (written < bytesTotal) {
        const aaudio_result_t w = AAudioStream_write(
            stream, reinterpret_cast<const void *>(pcm.data() + written),
            static_cast<int32_t>((bytesTotal - written) / bytesPerFrameActual), 0);
        if (w < 0) {
            LOGW("aaudio write failed rate=%d bits=%d res=%d", rate, bitDepth, w);
            break;
        }
        written += static_cast<int64_t>(w) * bytesPerFrameActual;
    }
    AAudioStream_requestStop(stream);
    AAudioStream_close(stream);
    LOGI("aaudio done rate=%d bits=%d frames=%lld", rate, bitDepth,
         static_cast<long long>(written / bytesPerFrameActual));
    return true;
}

extern "C" JNIEXPORT jint JNICALL
Java_com_sertum_player_spike1_Spike1NativeEngine_runMatrix(JNIEnv *, jobject) {
    LOGI("=== Spike-1 native AAudio matrix start ===");
    AAudioStreamBuilder *builder = nullptr;
    if (AAudio_createStreamBuilder(&builder) != AAUDIO_OK) {
        LOGW("builder create failed");
        return 0;
    }
    int played = 0;
    for (int32_t rate : RATES) {
        if (playRate(builder, rate, AAUDIO_FORMAT_PCM_I16, 16)) played++;
        if (playRate(builder, rate, AAUDIO_FORMAT_PCM_I24_PACKED, 24)) played++;
    }
    AAudioStreamBuilder_delete(builder);
    LOGI("=== Spike-1 native AAudio matrix done played=%d ===", played);
    return played;
}
