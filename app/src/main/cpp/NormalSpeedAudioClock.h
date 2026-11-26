#pragma once

#include <algorithm>
#include <cmath>
#include <cstdint>

namespace MelonDSAndroid
{
inline constexpr double kNdsFramesPerSecond = 59.8260982880808;
inline constexpr double kMinRatio = 1.0;
inline constexpr double kMaxRatio = 16.0;
inline constexpr double kMaxValidDtSeconds = 0.5;
inline constexpr double kRatioEmaAlpha = 0.05;
inline constexpr double kFillErrorTimeConstantSeconds = 0.40;
inline constexpr double kFillErrorFallbackAlpha = 0.01;
inline constexpr double kFillErrorDeadband = 0.25;
inline constexpr double kBufferFeedbackGain = 0.08;
inline constexpr double kBufferFeedbackMin = 0.95;
inline constexpr double kBufferFeedbackMax = 1.05;

inline bool isNormalSpeedFastForwardAudioEligible(
    bool fastForwardActive, bool keepAudioNormalSpeed, bool muteOnFastForward,
    bool isDsMode, float fastForwardSpeedMultiplier) noexcept
{
    return fastForwardActive && keepAudioNormalSpeed && !muteOnFastForward && isDsMode
        && std::isfinite(fastForwardSpeedMultiplier)
        && (fastForwardSpeedMultiplier <= 0.0f || fastForwardSpeedMultiplier > 1.0f);
}

inline std::uint32_t toSoundClockFixed16_16(double rate) noexcept
{
    if (!std::isfinite(rate) || rate <= 0.0) return 0x10000u;
    return static_cast<std::uint32_t>(
        std::clamp(rate * 65536.0 + 0.5, 1.0, static_cast<double>(0x10000u)));
}

class NormalSpeedAudioClock
{
public:
    struct UpdateInput
    {
        bool eligible;
        float fastForwardSpeedMultiplier;
        double nowSeconds;
        int currentBufferFillFrames;
        std::uint32_t targetBufferFrames;
    };

    struct UpdateResult
    {
        bool active;
        bool transitioned;
        double measuredRatio;
        double appliedRatio;
        double soundClockRate;
        std::uint32_t soundClockFixed;
    };

    void reset() noexcept { *this = NormalSpeedAudioClock{}; }
    void onTimingDiscontinuity() noexcept
    {
        hasLastFrameTime = false;
        smoothedFillError = 0.0;
    }

    UpdateResult update(const UpdateInput& input) noexcept
    {
        const bool wasActive = active;
        if (!input.eligible)
        {
            reset();
            return {false, wasActive, 1.0, 1.0, 1.0, 0x10000u};
        }

        const bool limited = input.fastForwardSpeedMultiplier > 0.0f;
        const double nominalRatio = limited
            ? 60.0 * input.fastForwardSpeedMultiplier / kNdsFramesPerSecond : 0.0;
        double validDt = 0.0;
        if (!wasActive)
        {
            active = true;
            measuredRatio = limited ? std::clamp(nominalRatio, kMinRatio, kMaxRatio) : 2.0;
            smoothedFillError = 0.0;
        }
        else if (hasLastFrameTime && std::isfinite(input.nowSeconds))
        {
            const double dt = input.nowSeconds - lastFrameTimeSeconds;
            if (std::isfinite(dt) && dt > 1e-6 && dt < kMaxValidDtSeconds)
            {
                validDt = dt;
                const double instRatio = std::clamp((1.0 / dt) / kNdsFramesPerSecond,
                                                    kMinRatio, kMaxRatio);
                measuredRatio += (instRatio - measuredRatio) * kRatioEmaAlpha;
            }
        }
        lastFrameTimeSeconds = input.nowSeconds;
        hasLastFrameTime = std::isfinite(input.nowSeconds);

        double ratio = limited ? std::min(nominalRatio, measuredRatio) : measuredRatio;
        if (input.targetBufferFrames > 0u && input.currentBufferFillFrames >= 0)
        {
            const double target = static_cast<double>(input.targetBufferFrames);
            const double rawFillError =
                (static_cast<double>(input.currentBufferFillFrames) - target) / target;
            const double fillAlpha = validDt > 0.0
                ? (1.0 - std::exp(-validDt / kFillErrorTimeConstantSeconds))
                : kFillErrorFallbackAlpha;
            smoothedFillError += (rawFillError - smoothedFillError) * fillAlpha;
            double excess = 0.0;
            if (smoothedFillError > kFillErrorDeadband)
                excess = smoothedFillError - kFillErrorDeadband;
            else if (smoothedFillError < -kFillErrorDeadband)
                excess = smoothedFillError + kFillErrorDeadband;
            ratio *= std::clamp(1.0 + kBufferFeedbackGain * excess,
                                kBufferFeedbackMin, kBufferFeedbackMax);
        }
        ratio = std::clamp(ratio, kMinRatio, kMaxRatio);
        const double rate = 1.0 / ratio;
        return {true, !wasActive, measuredRatio, ratio, rate, toSoundClockFixed16_16(rate)};
    }

private:
    bool active = false;
    bool hasLastFrameTime = false;
    double lastFrameTimeSeconds = 0.0;
    double measuredRatio = 1.0;
    double smoothedFillError = 0.0;
};
}
