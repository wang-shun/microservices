/*
 * Copyright 2016 - 2017 Aitu Software Limited.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.aitusoftware.example.application.ipc.ping;

import org.HdrHistogram.Histogram;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public final class PongReceiverImpl implements PongReceiver
{
    private static final long MAX_VALUE = TimeUnit.SECONDS.toMicros(1);
    private static final long RESET_MASK = (1 << 16) - 1;
    private static final long WARMUP_MESSAGES = Config.WARMUP_MESSAGES;
    private static final long MEASUREMENT_MESSAGES = Config.MEASUREMENT_MESSAGES;

    private final Histogram histogram = new Histogram(MAX_VALUE, 5);
    private final long pauseBetweenMessages;
    private final CountDownLatch completeLatch = new CountDownLatch(1);
    private boolean finished = false;

    PongReceiverImpl(final long pauseBetweenMessages)
    {
        this.pauseBetweenMessages = toMicros(pauseBetweenMessages);
    }

    @Override
    public void receive(final long requestNanos, final long sequenceNumber,
                        final long padding0, final long padding1, final long padding2,
                        final long padding3, final long padding4, final long padding5)
    {
        if (finished)
        {
            return;
        }
        final long duration = System.nanoTime() - requestNanos;
        final boolean shouldReset = shouldReset(sequenceNumber);

        if (shouldReset && sequenceNumber < WARMUP_MESSAGES)
        {
            histogram.reset();
        }
        histogram.recordValue(safeValue(toMicros(duration)));
//        histogram.recordValueWithExpectedInterval(safeValue(toMicros(duration)), pauseBetweenMessages);

        if (sequenceNumber == MEASUREMENT_MESSAGES + WARMUP_MESSAGES)
        {
            histogram.outputPercentileDistribution(System.out, 1.0);
            completeLatch.countDown();
            finished = true;
        }
    }

    CountDownLatch getCompleteLatch()
    {
        return completeLatch;
    }

    private static long toMicros(final long nanos)
    {
        if (nanos < (long) Integer.MAX_VALUE)
        {
            return ((int) nanos) / 1000;
        }
        return nanos / 1000L;
    }

    private static boolean shouldReset(final long sequenceNumber)
    {
        return (sequenceNumber & RESET_MASK) == 0L;
    }

    private static long safeValue(final long value)
    {
        return Math.min(MAX_VALUE, value);
    }
}