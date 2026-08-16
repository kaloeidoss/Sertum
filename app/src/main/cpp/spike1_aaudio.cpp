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

static bool playRate(AAudioStreamBuilder *builder, int32_t rate) {
    AAudioStreamBuilder_setSampleRate(builder, rate);
    AAudioStreamBuilder_setChannelCount(builder, 2);
    AAudioStreamBuilder_setFormat(builder, AAUDIO_FORMAT_PCM_I16);
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
        LOGW("aaudio open failed rate=%d res=%d", rate, res);
        return false;
    }

    const int32_t actualRate = AAudioStream_getSampleRate(stream);
    const aaudio_format_t actualFormat = AAudioStream_getFormat(stream);
    const aaudio_performance_mode_t perf = AAudioStream_getPerformanceMode(stream);
    const aaudio_sharing_mode_t sharing = AAudioStream_getSharingMode(stream);
    const int32_t deviceId = AAudioStream_getDeviceId(stream);
    const int32_t burst = AAudioStream_getFramesPerBurst(stream);
    const int32_t capacity = AAudioStream_getBufferCapacityInFrames(stream);
    LOGI("aaudio stream rate=%d actualRate=%d format=%d perf=%d sharing=%d device=%d burst=%d capacity=%d",
         rate, actualRate, actualFormat, perf, sharing, deviceId, burst, capacity);

    if (actualRate != rate) {
        LOGW("aaudio RATE MISMATCH requested=%d actual=%d", rate, actualRate);
    }

    const int64_t totalFrames = static_cast<int64_t>(rate) * TONE_SECONDS;
    std::vector<int16_t> pcm(totalFrames * 2, 0);
    const int16_t amplitude = static_cast<int16_t>(32767 * 0.2);
    for (int64_t i = 0; i < totalFrames; ++i) {
        const double v = amplitude * sin(2.0 * M_PI * 1000.0 * i / rate);
        pcm[2 * i] = static_cast<int16_t>(v);
        pcm[2 * i + 1] = static_cast<int16_t>(v);
    }

    res = AAudioStream_requestStart(stream);
    if (res != AAUDIO_OK) {
        LOGW("aaudio start failed rate=%d res=%d", rate, res);
        AAudioStream_close(stream);
        return false;
    }

    int64_t written = 0;
    const int64_t bytesTotal = totalFrames * 2 * sizeof(int16_t);
    while (written < bytesTotal) {
        const aaudio_result_t w = AAudioStream_write(
            stream, reinterpret_cast<const void *>(reinterpret_cast<const uint8_t *>(pcm.data()) + written),
            static_cast<int32_t>((bytesTotal - written) / (2 * sizeof(int16_t))), 0);
        if (w < 0) {
            LOGW("aaudio write failed rate=%d res=%d", rate, w);
            break;
        }
        written += static_cast<int64_t>(w) * 2 * sizeof(int16_t);
    }
    AAudioStream_requestStop(stream);
    AAudioStream_close(stream);
    LOGI("aaudio done rate=%d frames=%lld", rate, static_cast<long long>(written / (2 * sizeof(int16_t))));
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
        if (playRate(builder, rate)) played++;
    }
    AAudioStreamBuilder_delete(builder);
    LOGI("=== Spike-1 native AAudio matrix done played=%d ===", played);
    return played;
}
