#include <aaudio/AAudio.h>
#include <android/log.h>
#include <jni.h>
#include <atomic>
#include <mutex>
#include <unordered_map>

#define TAG "SertumAudio"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, TAG, __VA_ARGS__)
#define LOGW(...) __android_log_print(ANDROID_LOG_WARN, TAG, __VA_ARGS__)

static std::unordered_map<jlong, AAudioStream *> g_streams;
static std::mutex g_mu;
static std::atomic<jlong> g_next{1};

static aaudio_format_t formatForBits(int bits) {
    return bits >= 24 ? AAUDIO_FORMAT_PCM_I24_PACKED : AAUDIO_FORMAT_PCM_I16;
}

static int bytesPerFrame(AAudioStream *stream) {
    const int bytes = AAudioStream_getFormat(stream) == AAUDIO_FORMAT_PCM_I24_PACKED ? 3 : 2;
    return bytes * AAudioStream_getChannelCount(stream);
}

extern "C" JNIEXPORT jlong JNICALL
Java_com_sertum_player_audio_backend_AaudioNative_nativeOpen(
    JNIEnv *, jobject, jint sampleRate, jint channels, jint bits) {
    AAudioStreamBuilder *builder = nullptr;
    if (AAudio_createStreamBuilder(&builder) != AAUDIO_OK) {
        LOGW("builder create failed");
        return 0;
    }
    AAudioStreamBuilder_setDirection(builder, AAUDIO_DIRECTION_OUTPUT);
    AAudioStreamBuilder_setSampleRate(builder, sampleRate);
    AAudioStreamBuilder_setChannelCount(builder, channels);
    AAudioStreamBuilder_setFormat(builder, formatForBits(bits));
    AAudioStreamBuilder_setPerformanceMode(builder, AAUDIO_PERFORMANCE_MODE_LOW_LATENCY);
    AAudioStreamBuilder_setSharingMode(builder, AAUDIO_SHARING_MODE_EXCLUSIVE);

    AAudioStream *stream = nullptr;
    aaudio_result_t res = AAudioStreamBuilder_openStream(builder, &stream);
    if (res != AAUDIO_OK) {
        AAudioStreamBuilder_setSharingMode(builder, AAUDIO_SHARING_MODE_SHARED);
        res = AAudioStreamBuilder_openStream(builder, &stream);
    }
    AAudioStreamBuilder_delete(builder);
    if (res != AAUDIO_OK) {
        LOGW("open failed rate=%d bits=%d res=%d", sampleRate, bits, res);
        return 0;
    }

    const jlong handle = g_next.fetch_add(1);
    {
        std::lock_guard<std::mutex> lock(g_mu);
        g_streams[handle] = stream;
    }
    LOGI("open handle=%lld rate=%d actual=%d format=%d sharing=%d device=%d",
         static_cast<long long>(handle), sampleRate,
         AAudioStream_getSampleRate(stream), AAudioStream_getFormat(stream),
         AAudioStream_getSharingMode(stream), AAudioStream_getDeviceId(stream));
    return handle;
}

static AAudioStream *find(jlong handle) {
    std::lock_guard<std::mutex> lock(g_mu);
    auto it = g_streams.find(handle);
    return it == g_streams.end() ? nullptr : it->second;
}

extern "C" JNIEXPORT jboolean JNICALL
Java_com_sertum_player_audio_backend_AaudioNative_nativeStart(JNIEnv *, jobject, jlong handle) {
    AAudioStream *stream = find(handle);
    return stream != nullptr && AAudioStream_requestStart(stream) == AAUDIO_OK;
}

extern "C" JNIEXPORT jint JNICALL
Java_com_sertum_player_audio_backend_AaudioNative_nativeWrite(
    JNIEnv *env, jobject, jlong handle, jbyteArray data, jint offset, jint length) {
    AAudioStream *stream = find(handle);
    if (stream == nullptr) return -1;
    jbyte *buf = env->GetByteArrayElements(data, nullptr);
    if (buf == nullptr) return -2;
    const int frameBytes = bytesPerFrame(stream);
    if (frameBytes <= 0) {
        env->ReleaseByteArrayElements(data, buf, JNI_ABORT);
        return -3;
    }
    const int frames = length / frameBytes;
    int framesWritten = 0;
    while (framesWritten < frames) {
        const aaudio_result_t w = AAudioStream_write(
            stream, reinterpret_cast<const void *>(buf + offset + framesWritten * frameBytes),
            frames - framesWritten, 0);
        if (w < 0) {
            framesWritten = framesWritten == 0 ? static_cast<int>(w) : framesWritten;
            break;
        }
        framesWritten += w;
    }
    env->ReleaseByteArrayElements(data, buf, JNI_ABORT);
    return framesWritten;
}

extern "C" JNIEXPORT jboolean JNICALL
Java_com_sertum_player_audio_backend_AaudioNative_nativePause(JNIEnv *, jobject, jlong handle) {
    AAudioStream *stream = find(handle);
    return stream != nullptr && AAudioStream_requestPause(stream) == AAUDIO_OK;
}

extern "C" JNIEXPORT jboolean JNICALL
Java_com_sertum_player_audio_backend_AaudioNative_nativeFlush(JNIEnv *, jobject, jlong handle) {
    AAudioStream *stream = find(handle);
    return stream != nullptr && AAudioStream_requestFlush(stream) == AAUDIO_OK;
}

extern "C" JNIEXPORT jboolean JNICALL
Java_com_sertum_player_audio_backend_AaudioNative_nativeStop(JNIEnv *, jobject, jlong handle) {
    AAudioStream *stream = find(handle);
    return stream != nullptr && AAudioStream_requestStop(stream) == AAUDIO_OK;
}

extern "C" JNIEXPORT void JNICALL
Java_com_sertum_player_audio_backend_AaudioNative_nativeClose(JNIEnv *, jobject, jlong handle) {
    AAudioStream *stream = find(handle);
    if (stream != nullptr) {
        AAudioStream_close(stream);
        std::lock_guard<std::mutex> lock(g_mu);
        g_streams.erase(handle);
    }
}

#define GETTER(NAME, TYPE, CALL)                                                   \
    extern "C" JNIEXPORT TYPE JNICALL                                             \
    Java_com_sertum_player_audio_backend_AaudioNative_nativeGet##NAME(            \
        JNIEnv *, jobject, jlong handle) {                                        \
        AAudioStream *stream = find(handle);                                      \
        return stream == nullptr ? 0 : static_cast<TYPE>(CALL(stream));           \
    }

GETTER(ActualRate, jint, AAudioStream_getSampleRate)
GETTER(ActualFormat, jint, AAudioStream_getFormat)
GETTER(SharingMode, jint, AAudioStream_getSharingMode)
GETTER(DeviceId, jint, AAudioStream_getDeviceId)
GETTER(PerformanceMode, jint, AAudioStream_getPerformanceMode)
GETTER(FramesPerBurst, jint, AAudioStream_getFramesPerBurst)
