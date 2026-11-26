#include "NormalSpeedAudioClock.h"

#include <cassert>
#include <iostream>
#include <limits>

using namespace MelonDSAndroid;

namespace
{
void checkNear(double actual, double expected)
{
    assert(std::abs(actual - expected) < 1e-7);
}

void testEligibility()
{
    const auto eligible = isNormalSpeedFastForwardAudioEligible;
    assert(!eligible(false, true, false, true, 2));
    assert(!eligible(true, false, false, true, 2));
    assert(!eligible(true, true, true, true, 2));
    assert(!eligible(true, true, false, false, 2));
    for (float speed : {1.0f, 0.5f, std::numeric_limits<float>::quiet_NaN(),
                        std::numeric_limits<float>::infinity(), -std::numeric_limits<float>::infinity()})
        assert(!eligible(true, true, false, true, speed));
    for (float speed : {-1.0f, 0.0f, 2.0f, 3.0f, 5.0f})
        assert(eligible(true, true, false, true, speed));
}

void testFixedPoint()
{
    for (double rate : {-1.0, 0.0, std::numeric_limits<double>::quiet_NaN(),
                        std::numeric_limits<double>::infinity()})
        assert(toSoundClockFixed16_16(rate) == 0x10000u);
    assert(toSoundClockFixed16_16(1e-20) == 1u);
    assert(toSoundClockFixed16_16(0.5) == 0x8000u);
    assert(toSoundClockFixed16_16(1.5 / 65536.0) == 2u);
    assert(toSoundClockFixed16_16(1.0) == 0x10000u);
    assert(toSoundClockFixed16_16(2.0) == 0x10000u);
    assert(toSoundClockFixed16_16(std::numeric_limits<double>::max()) == 0x10000u);
}

void testConvergence(float requested, double actual, double expected)
{
    NormalSpeedAudioClock clock;
    NormalSpeedAudioClock::UpdateInput input{true, requested, 0, 100, 100};
    auto result = clock.update(input);
    for (int i = 1; i <= 500; ++i)
    {
        input.nowSeconds = i / (actual * kNdsFramesPerSecond);
        result = clock.update(input);
        assert(result.active && !result.transitioned);
    }
    checkNear(result.measuredRatio, actual);
    checkNear(result.appliedRatio, expected);
    checkNear(result.soundClockRate, 1.0 / expected);
    assert(result.soundClockFixed == toSoundClockFixed16_16(1.0 / expected));
}

void testFeedback()
{
    auto steadyFeedback = [](int fill, unsigned target, double seconds = 3.0) {
        NormalSpeedAudioClock clock;
        const double dt = 1.0 / (2.0 * kNdsFramesPerSecond);
        const int steps = static_cast<int>(seconds / dt);
        NormalSpeedAudioClock::UpdateResult res{};
        for (int i = 0; i <= steps; ++i)
            res = clock.update({true, 0.0f, i * dt, fill, target});
        return res.appliedRatio;
    };

    checkNear(steadyFeedback(100, 100, 0.0), 2.0);
    // Inside deadband (+20% < 25%): remains neutral.
    checkNear(steadyFeedback(120, 100), 2.0);
    checkNear(steadyFeedback(80, 100), 2.0);

    // Sustained excess/deficit outside deadband nudges smoothly within [0.95, 1.05].
    const double high = steadyFeedback(200, 100);
    assert(high > 2.05 && high <= 2.0 * kBufferFeedbackMax + 1e-9);
    const double low = steadyFeedback(20, 100);
    assert(low < 1.95 && low >= 2.0 * kBufferFeedbackMin - 1e-9);

    // Extreme sustained values stay clamped to [0.95, 1.05].
    checkNear(steadyFeedback(1000, 100, 6.0), 2.0 * kBufferFeedbackMax);
    checkNear(steadyFeedback(0, 100, 6.0), 2.0 * kBufferFeedbackMin);

    // Invalid inputs stay neutral.
    checkNear(NormalSpeedAudioClock{}.update({true, 0.0f, 0.0, -1, 100}).appliedRatio, 2.0);
    checkNear(NormalSpeedAudioClock{}.update({true, 0.0f, 0.0, 100, 0}).appliedRatio, 2.0);
}

void testSub1xSlowdownClamp()
{
    NormalSpeedAudioClock clock;
    double t = 0.0;
    auto first = clock.update({true, 3.0f, t, 0, 1536});
    assert(first.appliedRatio >= 1.0 && first.soundClockRate <= 1.0);
    assert(first.soundClockFixed <= 0x10000u);

    // Simulate ~25 FPS slowdown (dt = 0.04s) and 300ms hitches with empty buffer.
    for (int i = 0; i < 80; ++i)
    {
        t += (i % 10 == 0) ? 0.30 : 0.04;
        const auto res = clock.update({true, 3.0f, t, 0, 1536});
        assert(res.measuredRatio >= 1.0);
        assert(res.appliedRatio >= 1.0);
        assert(res.soundClockRate <= 1.0);
        assert(res.soundClockFixed >= 1u && res.soundClockFixed <= 0x10000u);
    }
}

void testSoundTouchBurstRippleStability()
{
    auto checkBurstScenario = [](double centerOffsetFrames) {
        NormalSpeedAudioClock clock;
        const double dt = 1.0 / (3.0 * kNdsFramesPerSecond);
        constexpr int target = 1536;
        // 768-frame peak-to-peak (+/-384) sawtooth every 3 frames (~60 Hz at 180 FPS).
        const int sawtoothOffsets[3] = {-384, 0, 384};

        double minRatio = 1e9;
        double maxRatio = 0.0;
        for (int i = 0; i < 900; ++i)
        {
            const int fill = std::max(
                0, static_cast<int>(target + centerOffsetFrames + sawtoothOffsets[i % 3]));
            const auto res = clock.update({true, 3.0f, i * dt, fill, target});
            if (i >= 360)
            {
                minRatio = std::min(minRatio, res.appliedRatio);
                maxRatio = std::max(maxRatio, res.appliedRatio);
            }
        }
        assert((maxRatio - minRatio) / maxRatio < 0.001);
    };

    checkBurstScenario(0.0);
    checkBurstScenario(460.0); // Shifted +30% outside deadband.
}

void testDiscontinuities()
{
    NormalSpeedAudioClock clock;
    auto sample = [&](double now) { return clock.update({true, 0, now, 0, 0}).measuredRatio; };
    sample(0);
    const double baseline = sample(0.01);
    assert(baseline < 2.0);
    checkNear(sample(1.0), baseline);
    checkNear(sample(1.5), baseline);
    clock.onTimingDiscontinuity();
    checkNear(sample(1.6), baseline);
    checkNear(sample(std::numeric_limits<double>::quiet_NaN()), baseline);
    checkNear(sample(1.7), baseline);
    assert(sample(1.71) < baseline);
}

void testRapidTransitions()
{
    NormalSpeedAudioClock clock;
    for (int i = 0; i < 40; ++i)
    {
        const auto entry = clock.update({true, 0, i * 0.02, 0, 0});
        assert(entry.active && entry.transitioned && entry.measuredRatio == 2.0);
        assert(entry.soundClockFixed == 0x8000u);
        clock.update({true, 0, i * 0.02 + 0.005, 0, 0});
        const auto exit = clock.update({false, 0, i * 0.02 + 0.01, 0, 0});
        assert(!exit.active && exit.transitioned && exit.measuredRatio == 1.0);
        assert(exit.soundClockRate == 1.0 && exit.soundClockFixed == 0x10000u);
        assert(!clock.update({false, 0, i * 0.02 + 0.015, 0, 0}).transitioned);
    }
}
}

int main()
{
    testEligibility();
    testFixedPoint();
    for (float speed : {2.0f, 3.0f, 5.0f})
        testConvergence(speed, speed, speed);
    testConvergence(3, 2.4, 2.4);
    testConvergence(3, 4.0, 180.0 / kNdsFramesPerSecond);
    testConvergence(0, 4.2, 4.2);
    testFeedback();
    testSub1xSlowdownClamp();
    testSoundTouchBurstRippleStability();
    testDiscontinuities();
    testRapidTransitions();
    std::cout << "NormalSpeedAudioClock tests passed\n";
}
