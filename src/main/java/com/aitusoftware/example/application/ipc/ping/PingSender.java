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

import com.aitusoftware.transport.ffi.Affinity;

public final class PingSender
{
    private final PingReceiver pingReceiver;
    private final long pauseBetweenMessages;
    private final int cpuAffinity;

    public PingSender(
            final PingReceiver pingReceiver, final long pauseBetweenMessages,
            final int cpuAffinity)
    {
        this.pingReceiver = pingReceiver;
        this.pauseBetweenMessages = pauseBetweenMessages;
        this.cpuAffinity = cpuAffinity;
    }

    void sendRequests()
    {
        long requestId = 0;
        new Affinity().setCurrentThreadCpuAffinityAndValidate(cpuAffinity);
        while (!Thread.currentThread().isInterrupted())
        {
            final long sendingTime = System.nanoTime();
            pingReceiver.send(sendingTime, requestId++,
                    1L, 2L, 5L,
                    7L, 11L, 13L);
            final long waitUntil = sendingTime + pauseBetweenMessages;
            while ((System.nanoTime() < waitUntil))
            {
                // spin
            }
            if (requestId > Config.MEASUREMENT_MESSAGES + Config.WARMUP_MESSAGES)
            {
                return;
            }
        }
    }
}